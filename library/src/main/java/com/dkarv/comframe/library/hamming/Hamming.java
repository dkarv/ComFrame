package com.dkarv.comframe.library.hamming;

public class Hamming {
    HammingCode code;

    public Hamming(HammingCode code) {
        this.code = code;
    }

    public int outputSize() {
        switch (code) {
            case NO:
                return 1;
            case HAMMING_7_4:
                return 7;
            case HAMMING_15_11:
                return 15;
            default:
                return 1;
        }
    }

    public int inputSize() {
        switch (code) {
            case NO:
                return 1;
            case HAMMING_7_4:
                return 4;
            case HAMMING_15_11:
                return 11;
            default:
                return 1;
        }
    }
}
