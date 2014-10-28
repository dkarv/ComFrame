package com.dkarv.comframe.library.dbpsk;

import android.media.AudioRecord;
import android.util.Log;

import com.dkarv.comframe.library.ComFrame;
import com.dkarv.comframe.library.ComFrameSender;
import com.dkarv.comframe.library.math.Goertzel;
import com.dkarv.comframe.library.tools.Bit;
import com.dkarv.comframe.library.tools.FFT;

public class DeModulator {
    /**
     * the size of our raw buffer history
     */
    private static final int RAW_BUFFER_SIZE = 9;
    private static final double MAGNITUDE_THRESHOLD = 0.03;
    private static final double ALIGNMENT_MAX_ERROR = 0.3;
    /**
     * here we store the phaseOffset computed by {@link FFT#getPhaseOffset(int, int, int)}
     * for more information about this value see the javadoc of that method
     */
    private final double phaseOffset;
    /**
     * the audioRecord. is only needed to skip some bits after alignment to the sender,
     * before and after that the raw data will be read in
     * {@link com.dkarv.comframe.library.ComFrameReceiver}
     */
    private final AudioRecord audioRecord;
    public boolean debug = false;
    public boolean verbose = false;
    /**
     * to remember in which state we are currently and what we're expecting to receive currently
     */
    private State state = State.WAITING;
    private int fftSize;
    private double lastPhase;
    private Goertzel goertzel;
    private double[] goertzelOutput = new double[2];
    private double[] goertzelInput;
    private double[] rawBuffer;
    private int rawBufferCounter = 0;
    private int readLength = 0;
    private int length = 0;
    private int receiveCountDown = 0;

    public DeModulator(int fftSize, int frequency, int sampleRate, AudioRecord audioRecord) {
        this.fftSize = fftSize;
        this.audioRecord = audioRecord;

        goertzel = new Goertzel(fftSize, FFT.getFFTBin(frequency, fftSize, sampleRate));
        goertzelInput = new double[fftSize];

        rawBuffer = new double[RAW_BUFFER_SIZE * fftSize];

        // calculate the phase offset. why we get this offset is described in the javadoc of
        // C.getPhaseOffset(...)
        phaseOffset = FFT.getPhaseOffset(frequency, sampleRate, fftSize);
        if (debug || verbose) {
            Log.d("DeModulator", "phaseOffset: " + phaseOffset);
        }
    }

    /**
     * takes the raw data obtained from the mic and tries to decode data from it
     *
     * @param buffer raw sound data
     * @param len    length of buffer
     * @return length may vary, may also be 0
     */
    public boolean[] decodeRawData(short[] buffer, int len) {
        // translate the shorts to double:
        for (int i = 0; i < len; i++) {
            goertzelInput[i] = (double) buffer[i] / Short.MAX_VALUE;
        }

        goertzel.goertzel(goertzelInput, goertzelOutput);

        double phase = Math.atan2(goertzelOutput[0], goertzelOutput[1]);
        double magnitude = Math.sqrt(goertzelOutput[0] * goertzelOutput[0] + goertzelOutput[1] *
                goertzelOutput[1]);

        // ensure that the phase is positive:
        if (phase < 0) {
            phase += FFT.TWO_PI;
        }

        // calculate the phase difference to the last phase, there's our information
        // decoded.
        double phaseDiff = getPhaseDiff(lastPhase, phase);//(lastPhase - phase - phaseOffset) % TWO_PI;
        lastPhase = phase;

        switch (state) {
            case WAITING:
                // save the new raw data to the buffer
                for (int i = 0; i < fftSize; i++) {
                    rawBuffer[i + (rawBufferCounter % RAW_BUFFER_SIZE) * fftSize] = goertzelInput[i];
                }
                rawBufferCounter++;

                if (rawBufferCounter >= RAW_BUFFER_SIZE) {
                    // enough samples collected. now analyze the collected samples and try to
                    // find the best phase alignment
                    int startAt = rawBufferCounter % RAW_BUFFER_SIZE;
                    int bestAlignment = findPhaseAlignment(startAt, rawBuffer);
                    if (bestAlignment >= 0) {
                        // now decode the data in the buffer, but only continue if they contain the
                        // starting sequence!
                        byte b = decodeBuffer(rawBuffer, startAt, bestAlignment);
                        // search for start sequence
                        if (b == ComFrameSender.PRE) {
                            if (debug || verbose) {
                                Log.d("DeModulator", "found start of data block!!");
                            }
                            // the challenge is now to get a fine phase in order to save it in
                            // lastPhase
                            // copy the relevant part of buffer to the beginning of buffer. will
                            // fill up the rest in the next if statement
                            for (int i = bestAlignment, j = 0; i < fftSize; i++, j++) {
                                buffer[j] = buffer[i];
                                //yetAnotherBuffer[j] = buffer[i];
                            }
                            // skip some raw data to get in phase
                            if (bestAlignment != 0) {
                                //audioRecord.read(yetAnotherBuffer, fftSize - bestAlignment,
                                //        bestAlignment);
                                audioRecord.read(buffer, fftSize - bestAlignment,
                                        bestAlignment);
                            }

                            for (int i = 0; i < fftSize; i++) {
                                goertzelInput[i] = (double) buffer[i] / Short.MAX_VALUE;
                                //anotherBuffer[i] = (double) yetAnotherBuffer[i] / Short.MAX_VALUE;
                            }
                            // start decoding and save the phase in lastPhase,
                            // then in the next call we will be able to compute a phase
                            // difference and therefor decode a bit
                            goertzel.goertzel(goertzelInput, 0, goertzelOutput);
                            lastPhase = Math.atan2(goertzelOutput[0], goertzelOutput[1]);

                            // reset this state, clean start when will reaching it again...
                            rawBufferCounter = 0;
                            state = State.READ_LENGTH;
                        }
                    }
                }
                break;
            case READ_LENGTH:
                boolean bitL = decodePhaseDifference(phaseDiff);
                if (bitL) {
                    length += 1 << (7 - readLength); //Math.pow(2, (7 - readLength));
                    if (debug || verbose) {
                        Log.d("DeModulator", "add 2^" + (7 - readLength));
                    }
                }
                readLength++;
                if (readLength == 8) {
                    // we received the whole length, reset this state and go on to the next state
                    if (debug || verbose) {
                        Log.d("DeModulator", "data block length: " + length);
                    }
                    readLength = 0;
                    // length is given in bytes
                    receiveCountDown = length * 8;
                    length = 0;
                    state = State.RECEIVING;
                }
                break;
            case RECEIVING:
                Log.d("DeModulator", "r: " + phaseDiff);
                // once the phase is aligned, we decode as much bits as given in length
                boolean bit = decodePhaseDifference(phaseDiff);
                boolean[] re = {bit};

                receiveCountDown--;

                if (receiveCountDown == 0) {
                    if (debug || verbose) {
                        Log.d("DeModulator", "prepared with receiving!!");
                    }
                    reset();
                }
                return re;
        }

        return new boolean[0];
    }

    /**
     * because the receiver and sender side can't use the same clock to synchronize,
     * we need another possibility to align the phase shifts between the two devices. this is
     * done by trying different alignment values and try to find the best one out of them
     *
     * @param startAt were start to read in the buffer
     * @param buffer  the buffer containing the raw values read from the mic
     * @return -1 when no useful alignment value was found, the alignment value otherwise
     */
    private int findPhaseAlignment(int startAt, double[] buffer) {
        /* try PARTS different alignments. 16 seems to be more than enough
        * making this value bigger results in a lot more computing time and therefor battery
        * consumption, making it lower results in worse alignment
        */
        final int PARTS = 16;
        double lPhase, phase;
        int best = 0;
        double minDifference = Double.MAX_VALUE;
        for (int i = 0; i < PARTS; i++) {
            // try PARTS different goertzel beginnings
            int start = (i * (fftSize / PARTS));
            lPhase = 0.0;
            double maxError = 0;
            for (int j = 0; j < 8; j++) {
                // decode always all 7 bits (the first phase is not useful because we can't
                // compute a phase difference when we have no lastPhase)
                goertzel.goertzel(buffer, fftSize * (j + startAt) + start, goertzelOutput);
                phase = Math.atan2(goertzelOutput[0], goertzelOutput[1]);
                double phaseD = getPhaseDiff(lPhase, phase);

                if (j != 0) {
                    // throw away first bit, start calculating the difference to shifts as we
                    // expect them
                    double diff_0 = Math.abs(phaseD - ComFrame.PHASE_SHIFT_0);
                    double diff_1 = Math.abs(phaseD - ComFrame.PHASE_SHIFT_1);

                    if (diff_0 < diff_1) {
                        maxError = Math.abs(phaseD - ComFrame.PHASE_SHIFT_0) < maxError ?
                                maxError : Math.abs(phaseD - ComFrame.PHASE_SHIFT_0);
                    } else {
                        maxError = Math.abs(phaseD - ComFrame.PHASE_SHIFT_1) < maxError ?
                                maxError : Math.abs(phaseD - ComFrame.PHASE_SHIFT_1);
                    }
                }
                lPhase = phase;
            }
            if (maxError < minDifference) {
                best = i;
                minDifference = maxError;
            }
        }
        if (minDifference < ALIGNMENT_MAX_ERROR) {
            return best * (fftSize / PARTS);
        } else {
            return -1;
        }

    }

    /**
     * helper method calculating the phase diff between two phases. we compute this value in a
     * helper method because taking the modulo in java returns negative values also,
     * but we always want the phase (and phase difference) between 0 and TWO_PI
     *
     * @param firstPhase
     * @param secondPhase
     * @return the diff between the given phases, value is >=0 and <= TWO_PI
     */
    private double getPhaseDiff(double firstPhase, double secondPhase) {
        double phaseDiff = (firstPhase - secondPhase - phaseOffset) % FFT.TWO_PI;
        return phaseDiff < 0 ? phaseDiff + FFT.TWO_PI : phaseDiff;
    }

    /**
     * decodes one byte from the raw buffer. ensure that there are 9*fftSize values in rawBuffer
     * such that it can decode 8 bits
     *
     * @param rawBuffer
     * @param startAt
     * @param bestAlignment
     * @return
     */
    private byte decodeBuffer(double[] rawBuffer, int startAt, int bestAlignment) {
        int highMagnitudes = 0;
        double phase;
        double lPhase = 0.0;
        byte b = 0;
        for (int i = startAt, j = -1; j < 8; i++, j++) {
            goertzel.goertzel(rawBuffer, fftSize * i + bestAlignment, goertzelOutput);
            phase = Math.atan2(goertzelOutput[0], goertzelOutput[1]);
            if (j != -1 && Math.sqrt(goertzelOutput[0] * goertzelOutput[0] +
                    goertzelOutput[1] * goertzelOutput[1]) > MAGNITUDE_THRESHOLD) {
                highMagnitudes++;
            }
            double phaseD = getPhaseDiff(lPhase, phase);

            // throw away first bit, because we can decode a phase diff the first time when we
            // received 2 phases
            if (j != -1) {
                boolean bit = decodePhaseDifference(phaseD);
                b = Bit.storeBigEndian(b, j, bit);
            }
            lPhase = phase;
        }
        if (highMagnitudes > 7) {
            // only return a useful value when 8 out of 8 magnitudes were higher than MAGNITUDE_THRESHOLD
            return b;
        }
        return 0;
    }

    /**
     * takes the phase shift to decode a bit
     *
     * @param phaseDiff
     * @return the bit decoded from the phase difference
     */
    private boolean decodePhaseDifference(double phaseDiff) {
        double diff_0 = Math.abs(phaseDiff - ComFrame.PHASE_SHIFT_0);
        double diff_1 = Math.abs(phaseDiff - ComFrame.PHASE_SHIFT_1);

        if (diff_0 < diff_1) {
            return false;
        }
        return true;
    }


    /**
     * resets this DeModulator, as if it was created newly
     */
    public void reset() {
        state = State.WAITING;
        rawBufferCounter = 0;
        lastPhase = 0.0;
        length = 0;
        readLength = 0;
    }

    /**
     * @return if we are currently receiving data
     */
    public boolean isReceiving() {
        return state == State.RECEIVING;
    }

    private enum State {WAITING, READ_LENGTH, RECEIVING}
}
