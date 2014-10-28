package com.dkarv.comframe.library;

/**
 * contains some constants that are needed on both the receiver and sender side
 * and also the listener you can use to get the data from the receiver.
 */
public class ComFrame {

    /**
     * WATCH OUT!
     * has to be a frequency where {@code (fftSize * (frequency / (double) sampleRate)) % 1.0} is
     * as little as possible, such that we hit a single fftbin at
     * {@link com.dkarv.comframe.library.tools.FFT#getFFTBin(int, int, int)}
     */
    public static final int DEFAULT_FREQUENCY = 18433;

    /**
     * DEFAULT_FFT_SIZE, and BUFFER_SIZE for receiver and sender
     * is only default value, can be set by setBufferSize() in receiver and sender;
     * but WATCH OUT to set also a corresponding frequency as described at {@link #DEFAULT_FREQUENCY}
     */
    public static final int DEFAULT_FFT_SIZE = 256;

    /**
     * DFAULT_SAMPLE_RATE of receiver and sender
     * 44100 should be supported by most devices
     */
    public static final int DFAULT_SAMPLE_RATE = 44100;

    /**
     * phase shift when sending a 1.
     * HALF_PI because where using SDBPSK
     */
    public static final double PHASE_SHIFT_1 = Math.PI / 2;

    /**
     * the phase shift for a 0
     * = - HALF_PI
     */
    public static final double PHASE_SHIFT_0 = 3 * Math.PI / 2;


    /**
     * this interface can be used if you want to communicate with messages. it collects all bytes
     * received and hands them to you in one array.
     */
    public interface MessageListener {
        public void onMessageReceived(byte[] msg);
    }

    /**
     * this interface is the best if your communication is some kind of streaming. you will be
     * notified immediately if a new byte was received by {@link #onByteReceived}
     *
     * if we reached the end of one block of data, {@link #ready()} will be called.
     */
    public interface StreamListener {
        public void onByteReceived(byte b);

        public void ready();
    }

    /**
     * implement this interface if you want to do further debug,
     * notices you about every single bit received before decoding with hamming
     */
    public interface BitListener {
        public void onBitReceived(boolean bit);
    }
}
