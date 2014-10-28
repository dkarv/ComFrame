package com.dkarv.comframe.library;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.dkarv.comframe.library.dbpsk.DeModulator;
import com.dkarv.comframe.library.hamming.HammingCode;
import com.dkarv.comframe.library.hamming.HammingDecoder;
import com.dkarv.comframe.library.tools.Bit;

import java.util.ArrayList;

/**
 * create an instance of the ComFrameReceiver to receive the data sent by the ComFrameSender
 */
public class ComFrameReceiver {
    /**
     * set this flag to enable basic logging to the Log
     */
    public boolean debug = false;
    /**
     * if you set this flag ComFrame will log more data via Log.d("ComFrameReceiver", ... )
     */
    public boolean verbose = false;
    // the three parameters needed to save and analyze the incoming sound
    // for further documentation look at the javadoc of the default values in ComFrame
    private int frequency = ComFrame.DEFAULT_FREQUENCY;
    private int bufferSize = ComFrame.DEFAULT_FFT_SIZE;
    private int sampleRate = ComFrame.DFAULT_SAMPLE_RATE;

    // the three possible listener. not all of them have to be initialized,
    // but you can set more than one if useful for your application
    private ComFrame.BitListener bitListener;
    private ComFrame.MessageListener msgListener;
    private ComFrame.StreamListener streamListener;

    /**
     * the audio record where we will read the input data from
     */
    private AudioRecord audioRecord;

    /**
     * a buffer for the raw data read by audioRecord
     */
    private short[] buffer;

    /**
     * flag if the receiver is prepared and prepared to start listening.
     * will be set when prepare() is called (either automatic when startListening(),
     * or manually by you)
     */
    private boolean prepared = false;

    private boolean running = false;

    private DeModulator deModulator;

    private HammingCode hammingCode = HammingCode.NO;

    private HammingDecoder decoder;
    private int receivedCounter = 0;
    private byte receivedByte;

    private Thread t;

    /**
     * a list used to collect the bytes until the end of the message is reached
     */
    private ArrayList<Byte> byteCollector;

    /**
     * start listening for data
     * call this when you want the receiver to start analyzing the input sound
     */
    public synchronized void startListening() {
        if (!prepared) {
            prepare();
        }
        if (debug || verbose) {
            Log.d("ComFrameReceiver", "start listening...");
        }

        running = true;

        t = new AudioThread();
        t.start();
    }

    /**
     * stop listening for data
     * call this if you don't expect further messages
     */
    public synchronized void stopListening() {
        if (debug || verbose) {
            Log.d("ComFrameReceiver", "stop listening...");
        }

        running = false;

        try {
            t.join();
        } catch (InterruptedException e) {
            Log.e("ComFrameReceiver", "got interrupted during join()");
        }

        reset();
    }

    /**
     * call this method if all parameters and options have been set,
     * after a call to prepare() you aren't able to change any options.
     */
    public synchronized void prepare() {
        if (prepared) {
            Log.e("ComFrameReceiver", "call prepare() only once!");
            return;
        }
        // retrieve the minBufferSize from the audio record
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (debug || verbose) {
            Log.d("ComFrameReceiver", "minBufferSize: " + minBufferSize);
        }

        minBufferSize = Math.max(minBufferSize, bufferSize);
        if (debug || verbose) {
            Log.d("ComFrameReceiver", "buffer will be set to: " + minBufferSize);
        }
        // we ensure that the buffer is big enough to store also larger parts by setting it to
        // the min size * 16
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 16);

        buffer = new short[bufferSize];

        deModulator = new DeModulator(bufferSize, frequency, sampleRate, audioRecord);
        deModulator.debug = debug;
        deModulator.verbose = verbose;

        decoder = new HammingDecoder(hammingCode);

        byteCollector = new ArrayList<Byte>();

        prepared = true;
    }

    /**
     * this method is used to reset the ComFrameReceiver so that it will start recording the next
     * time as if it was created new
     */
    private synchronized void reset() {
        if (verbose) {
            Log.d("ComFrameReceiver", "reset()");
        }

        receivedByte = 0;
        receivedCounter = 0;
        if (byteCollector != null) {
            byteCollector.clear();
        }
        deModulator.reset();
        decoder.reset();
    }


    /**
     * important: call this function once you don't need the sender any more to stop draining the battery
     */
    public synchronized void close() {
        this.stopListening();
        if(audioRecord != null){
            audioRecord.release();
        }
        prepared = false;
    }

    /**
     * @return if the ComFrameReceiver is currently running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * collects bits until a byte was received, and sends this byte to the listener...
     *
     * @param decodedBit the new decoded bit
     */
    private void receivedNewBit(boolean decodedBit) {
        receivedByte = Bit.storeBigEndian(receivedByte, receivedCounter, decodedBit);
        receivedCounter = (receivedCounter + 1) % 8;
        if (receivedCounter == 0) {
            // received a complete byte, send to the listener
            if (streamListener != null) {
                streamListener.onByteReceived(receivedByte);
            }
            if (debug || verbose) {
                // print to the log:
                String b = "";
                for (int i = 0; i < 8; i++) {
                    b += Bit.getBit(receivedByte, i) ? "1" : "0";
                }
                Log.d("Receiver", "received: " + b);
            }
            if (msgListener != null) {
                collectMessage(receivedByte);
            }
        }
    }

    /**
     * collects a whole message that will be given to the msgListener then
     */
    private void collectMessage(byte newByte) {
        byteCollector.add(newByte);
    }

    /**
     * is called when we're not receiving any data. sends them to the msgListener if it is set
     */
    private void ready() {
        if (msgListener != null && !byteCollector.isEmpty()) {
            final int n = byteCollector.size();
            byte data[] = new byte[n];
            for (int i = 0; i < n; i++) {
                data[i] = byteCollector.get(i);
            }
            msgListener.onMessageReceived(data);
        }

        reset();
    }

    /**
     * checks if receiver is already prepared, will throw an exception if it isn't
     */
    private void checkPrepared() {
        if (prepared) {
            throw new UnsupportedOperationException("Don't set options after prepare() was " +
                    "called! Set all options before starting to receive or sending!");
        }
    }

    /**
     * set the BitListener. For further documentation which listener to use,
     * see {@link com.dkarv.comframe.library.ComFrame}
     * this one is only for debugging
     *
     * @param bitListener
     */
    public void setBitListener(ComFrame.BitListener bitListener) {
        checkPrepared();
        this.bitListener = bitListener;
    }

    /**
     * set the MessageListener. For further documentation which listener to use,
     * see {@link com.dkarv.comframe.library.ComFrame}
     *
     * @param msgListener
     */
    public void setMsgListener(ComFrame.MessageListener msgListener) {
        checkPrepared();
        this.msgListener = msgListener;
    }

    /**
     * set the StreamListener. For further documentation which listener to use,
     * see {@link com.dkarv.comframe.library.ComFrame}
     *
     * @param streamListener
     */
    public void setStreamListener(ComFrame.StreamListener streamListener) {
        checkPrepared();
        this.streamListener = streamListener;
    }

    /**
     * set the HammingCode. has to be the same as used on the sender part
     *
     * @param hammingCode
     */
    public void setHammingCode(HammingCode hammingCode) {
        this.hammingCode = hammingCode;
    }

    /**
     * set if you want to send on another frequency.
     * important: choose a frequency that works well with the chosen fftSize.
     * see {@link com.dkarv.comframe.library.ComFrame#DEFAULT_FREQUENCY} for further details
     *
     * @param frequency
     */
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    /**
     * set if you want to use another bufferSize.
     * important: choose a bufferSize that works well with the chosen frequency.
     * see {@link com.dkarv.comframe.library.ComFrame#DEFAULT_FREQUENCY} for further details
     *
     * @param bufferSize
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * this thread will record the audio, and analyze it asynchronous
     */
    private class AudioThread extends Thread {

        @Override
        public void run() {
            audioRecord.startRecording();

            int result;
            boolean[] bits;
            boolean[] decodedBits;
            // remember if we were receiving something the last loop
            boolean started = false;
            while (running) {
                result = audioRecord.read(buffer, 0, bufferSize);
                // the decoding happens in the modulator after this call
                bits = deModulator.decodeRawData(buffer, result);

                if (bitListener != null) {
                    for (int i = 0; i < bits.length; i++) {
                        bitListener.onBitReceived(bits[i]);
                    }
                }

                for (int i = 0; i < bits.length; i++) {
                    decodedBits = decoder.decode(bits[i]);
                    for (int j = 0; j < decodedBits.length; j++) {
                        receivedNewBit(decodedBits[j]);
                    }
                }

                boolean receiving = deModulator.isReceiving();
                if (receiving) {
                    if (!started) {
                        started = true;
                        if (debug) {
                            Log.d("ComFrameReceiver", "start receiving data");
                        }
                    }
                } else if (started) {
                    if (debug) {
                        Log.d("ComFrameReceiver", "receiving no more data");
                    }
                    started = false;
                    ready();
                }

            }
            audioRecord.stop();
        }
    }

}
