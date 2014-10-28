package com.dkarv.comframe.library.dbpsk;

import android.util.Log;

import com.dkarv.comframe.library.tools.FFT;

public class Modulator {
    public boolean debug = false;
    public boolean verbose = false;

    /**
     * set the amplitude to maximum value as default.
     * we highly discourage a change here, because even on the highest volume it's sometimes
     * quite hard to decode the data for the receiver.
     */
    private short amplitude = Short.MAX_VALUE;

    /**
     * add or subtract the phaseShifts from this value. will be added to every phase later in the
     * {@link #fillArray(double, short[], int)} method
     */
    private double phaseShift = 0;

    /**
     * counter to remember at which part of the sine wave we're currently sending
     */
    private int m = 0;

    /**
     * frequency / samplerate
     */
    private double fr_sr;

    public Modulator(int frequency, int sampleRate) {
        this.fr_sr = (frequency * 1.0) / sampleRate;
    }

    /**
     * fill a short array with a sine wave
     *
     * @param addShift the offset that will be added to the phase
     * @param sound    array to store the sound data in. sound.length has to be >= len
     * @param len      how many shorts to write in the array
     */
    public void fillArray(double addShift, short[] sound, int len) {
        if (verbose) {
            Log.d("Modulator", "fill sound array with new shift: " + addShift);
        }

        phaseShift += addShift;
        double ph;

        // compute the sound
        for (int i = 0; i < len; i++) {
            ph = m * fr_sr * FFT.TWO_PI + phaseShift;
            sound[i] = (short) (Math.sin(ph) * amplitude);
            m++;
        }
    }

    /**
     * reset the Modulator as if it would start from scratch to save time instead of creating a new
     * Modulator every time
     */
    public void reset() {
        m = 0;
        phaseShift = 0;
    }
}
