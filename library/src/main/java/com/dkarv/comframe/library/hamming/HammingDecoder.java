package com.dkarv.comframe.library.hamming;


public class HammingDecoder extends Hamming {

    private boolean[] input;
    private boolean[] output;
    private int inputCounter = 0;

    public HammingDecoder(HammingCode code) {
        super(code);

        // switch length because input to decoder is output from encoder
        input = new boolean[outputSize()];
        output = new boolean[inputSize()];
    }

    /**
     *
     * @param newBit 0 or 1
     */
    public boolean[] decode(int newBit) {
        return this.decode(newBit == 1);
    }

    public boolean[] decode(boolean newBit) {
        input[inputCounter] = newBit;
        inputCounter = (inputCounter + 1) % input.length;
        if (inputCounter == 0) {
            // input is full, startListening decoding
            switch (code) {
                case NO:
                    output[0] = input[0];
                    break;
                case HAMMING_7_4:
                    decode_7_4(input, output);
                    break;
                case HAMMING_15_11:
                    decode_15_11(input, output);
                    break;
            }
            return output;
        }
        return new boolean[0];
    }

    private void decode_7_4(boolean[] in, boolean[] out) {
        // calculate the check bits
        // c1 = p1 ^ m1 ^ m2 ^ m4
        boolean c1 = in[0] ^ in[2] ^ in[4] ^ in[6];
        // c2 = p2 ^ m1 ^ m3 ^ m4
        boolean c2 = in[1] ^ in[2] ^ in[5] ^ in[6];
        // c3 = p3 ^ m2 ^ m3 ^ m4
        boolean c3 = in[3] ^ in[4] ^ in[5] ^ in[6];
        // now check if there was a false bit, if so flip the bit
        if (c1 && c2 && c3) {
            // all 3 check bits are wrong. this means the mistake is in bit m4, flip it
            in[6] ^= true;
        } else if (c1 && c2) {
            in[2] ^= true;
        } else if (c1 && c3) {
            in[4] ^= true;
        } else if (c2 && c3) {
            in[5] ^= true;
        }
        out[0] = in[2];
        out[1] = in[4];
        out[2] = in[5];
        out[3] = in[6];
    }

    private void decode_15_11(boolean[] in, boolean[] out) {
        //c1 = p1 ^ m1 ^ m2 ^ m4 ^ m5 ^ m7 ^ m9 ^ m11
        boolean c1 = in[0] ^ in[2] ^ in[4] ^ in[6] ^ in[8] ^ in[10] ^ in[12] ^ in[14];
        //c2 = p2 ^ m1 ^ m3 ^ m4 ^ m6 ^ m7 ^ m10 ^ m11
        boolean c2 = in[1] ^ in[2] ^ in[5] ^ in[6] ^ in[9] ^ in[10] ^ in[13] ^ in[14];
        //c3 = p4 ^ m2 ^ m3 ^ m4 ^ m8 ^ m9 ^ m10 ^ m11
        boolean c3 = in[3] ^ in[4] ^ in[5] ^ in[6] ^ in[11] ^ in[12] ^ in[13] ^ in[14];
        //c4 = p8 ^ m5 ^ m6 ^ m7 ^ m8 ^ m9 ^ m10 ^ m11
        boolean c4 = in[7] ^ in[8] ^ in[9] ^ in[10] ^ in[11] ^ in[12] ^ in[13] ^ in[14];

        if (c1 && c2 && c3 && c4) {
            in[14] ^= true;
        } else if (c2 && c3 && c4) {
            in[13] ^= true;
        } else if (c1 && c3 && c4) {
            in[12] ^= true;
        } else if (c3 && c4) {
            in[11] ^= true;
        } else if (c1 && c2 && c4) {
            in[10] ^= true;
        } else if (c2 && c4) {
            in[9] ^= true;
        } else if (c1 && c4) {
            in[8] ^= true;
        } else if (c1 & c2 & c3) {
            in[6] ^= true;
        } else if (c2 & c3) {
            in[5] ^= true;
        } else if (c1 & c3) {
            in[4] ^= true;
        } else if (c1 & c2) {
            in[2] ^= true;
        }
        out[0] = in[2];
        out[1] = in[4];
        out[2] = in[5];
        out[3] = in[6];
        out[4] = in[8];
        out[5] = in[9];
        out[6] = in[10];
        out[7] = in[11];
        out[8] = in[12];
        out[9] = in[13];
        out[10] = in[14];
    }


    public void reset() {
        inputCounter = 0;
    }
}
