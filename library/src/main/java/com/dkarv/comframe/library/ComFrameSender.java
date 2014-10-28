package com.dkarv.comframe.library;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.dkarv.comframe.library.dbpsk.Modulator;
import com.dkarv.comframe.library.hamming.HammingCode;
import com.dkarv.comframe.library.hamming.HammingEncoder;
import com.dkarv.comframe.library.tools.Bit;

/**
 * create an instance of the ComFrameSender to be able to send data via sound.
 */
public class ComFrameSender {
    /**
     * bits [1 ... 8] from {@link #pre}
     */
    public static final byte PRE = -44;
    public boolean debug = false;
    public boolean verbose = false;
    /**
     * indicates if it is prepared to call send
     */
    public boolean prepared = false;
    private int frequency = ComFrame.DEFAULT_FREQUENCY;
    private int bufferSize = ComFrame.DEFAULT_FFT_SIZE;
    private int sampleRate = ComFrame.DFAULT_SAMPLE_RATE;
    private Modulator modulator;
    /**
     * array to store the sound samples that will be sent
     */
    private short[] samples;
    private AudioTrack audioTrack;
    private HammingCode hammingCode = HammingCode.NO;
    private HammingEncoder encoder;
    private byte[] hammingOutput;
    // the start sequence
    private boolean[] pre = {false, true, true, false, true, false, true, false, false};
    /**
     * an end sequence. we're currently just sending some useless bits because the last phases
     * are always trash on the receiver side, maybe because of some underlying Android
     * implementation....
     */
    private boolean[] after = {false, true, false, true, false, true, false, true, false, true,
            false};

    /**
     * call this method if all parameters and options have been set,
     * after a call to prepare() you aren't able to change any options.
     */
    public synchronized void prepare() {
        // the user has set all settings, prepare to send data
        samples = new short[bufferSize];
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                AudioTrack.MODE_STREAM);

        modulator = new Modulator(frequency, sampleRate);
        modulator.debug = debug;
        modulator.verbose = verbose;

        encoder = new HammingEncoder(hammingCode);

        prepared = true;
    }

    /**
     * will broadcast the data, method call returns when everything is sent
     * important: this function will pad 0's to your data depending on the HammingCode you chose,
     * so it's better to send large arrays than short ones
     *
     * @param data the byte array containing the data
     */
    public synchronized void send(byte[] data) {
        if (!prepared) {
            prepare();
        }

        if (debug) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < 8; j++) {
                    b.append(Bit.getBit(data[i], j) ? "1" : "0");
                }
                b.append(' ');
            }
            Log.d("ComFrameSender", "send: " + b.toString());
        }

        // now encode the data with a hamming code
        hammingOutput = new byte[encoder.outputByteCount(data.length)];
        encoder.encode(data, hammingOutput);
        int hammingLength = encoder.howManyHammings(data.length) * encoder.outputSize();

        if (debug || verbose) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < hammingLength; i++) {
                if (i % 8 == 0 && i != 0) {
                    b.append(' ');
                }
                b.append(Bit.getBitFromArray(hammingOutput, i));
            }
            Log.d("ComFrameSender", "hamming: " + b.toString());
        }

        int len = (int) Math.ceil(hammingLength / 8.0);
        if (len > 255 || len <= 0) {
            throw new IllegalArgumentException("the length of the data isn't allowed to be > 256 " +
                    "or zero bytes after hamming encoding!");
        }

        if (debug || verbose) {
            Log.d("ComFrameSender", "length of data: " + len);
        }
        // now convert the length integer to bits
        boolean[] decodedLength = new boolean[8];
        for (int i = 0; i < 8; i++) {
            decodedLength[i] = Bit.getBit(len, 24 + i);
        }

        // ensure that the modulator starts from scratch
        modulator.reset();

        // start playing, now we can write sound to the audioTrack
        audioTrack.play();

        for (int i = -pre.length - decodedLength.length; i < hammingLength + after.length; i++) {
            if (i < -decodedLength.length) {
                // send the bits from pre
                sendBit(pre[pre.length + i + decodedLength.length]);
            } else if (i < 0) {
                // now send the length of the data
                sendBit(decodedLength[decodedLength.length + i]);
            } else if (i < hammingLength) {
                // send the data
                sendBit(Bit.getBitFromArray(hammingOutput, i) == 1);
            } else {
                // and send the after bits
                sendBit(after[i - (hammingLength)]);
            }
        }

        // after ready with sending, stop the playing
        audioTrack.stop();
    }


    /**
     * important: call this function once you don't need the sender any more to stop draining the battery
     */
    public synchronized void close() {
        if(audioTrack != null){
            audioTrack.release();
        }
        prepared = false;
    }

    /**
     * pro users only
     * call only if you know what you're doing and be sure to set the value to the same on the receiver side!
     *
     * @param bufferSize
     */
    public synchronized void setBufferSize(int bufferSize) {
        checkPrepared();
        this.bufferSize = bufferSize;
    }

    /**
     * set a different hammingCode, important: set the same hamming code for the receiver
     *
     * @param hammingCode
     */
    public synchronized void setHammingCode(HammingCode hammingCode) {
        if (!prepared) {
            this.hammingCode = hammingCode;
        } else {
            throw new RuntimeException("you can't set parameters after calling prepare() or send()!");
        }
    }

    /**
     * pro users only
     * call only if you know what you're doing and be sure to set the value to the same on the receiver side
     */
    public synchronized void setSampleRate(int sampleRate) {
        if (prepared) {
            throw new RuntimeException("sampleRate may not be set after calling prepare(), please set all parameters before!");
        }
        this.sampleRate = sampleRate;
    }

    /**
     * sends a single bit
     *
     * @param bit
     */
    private synchronized void sendBit(boolean bit) {
        if (verbose) {
            Log.d("ComFrameSender", "send bit: " + (bit ? "1" : "0"));
        }

        modulator.fillArray((bit ? ComFrame.PHASE_SHIFT_1 : ComFrame.PHASE_SHIFT_0), samples, bufferSize);
        audioTrack.write(samples, 0, bufferSize);
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

}
