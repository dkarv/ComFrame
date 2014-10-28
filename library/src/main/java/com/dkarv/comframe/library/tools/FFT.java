package com.dkarv.comframe.library.tools;

/**
 * helper class for different calculation or values related to the FFT.
 */
public class FFT {
    public static double TWO_PI = Math.PI * 2;
    public static double HALF_PI = Math.PI / 2;

    /**
     * calculates in which fft bin the values for the corresponding function is,
     * given the other parameters
     *
     * @param frequency
     * @param fftSize
     * @param sampleRate
     * @return
     */
    public static int getFFTBin(int frequency, int fftSize, int sampleRate) {
        return (int) Math.round(fftSize * (frequency / (double) sampleRate));
    }

    /**
     * because we don't analyse only whole phases every time we do the FFT on some input, the phase
     * result of the next FFT will have some offset which is the same every time and can be calculated with this method
     *
     * @param frequency
     * @return the offset we get every FFT WINDOW
     */
    public static double getPhaseOffset(int frequency, int sampleRate, int arraySize) {
        // how many seconds fit in one array with the length fftSize
        double partOfSecond = arraySize / ((double) sampleRate);

        // now calculate how many phases this part of a second contains
        // this value is not an integer, because we don't always collect whole phases per array,
        // what is the origin of the problem.
        double phasesInArraySize = partOfSecond * frequency;

        // we aren't interested in the phase count, but in the part of phases that is not
        // completely inside the array every time.
        // by taking the modulo, we calculate how much of the last phase is contained in this array
        double partOfLastPhaseInThisArray = phasesInArraySize % 1.0;

        return partOfLastPhaseInThisArray * TWO_PI;
    }
}
