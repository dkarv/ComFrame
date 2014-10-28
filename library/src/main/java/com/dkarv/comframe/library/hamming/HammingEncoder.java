package com.dkarv.comframe.library.hamming;


import com.dkarv.comframe.library.tools.Bit;

public class HammingEncoder extends Hamming {

    public HammingEncoder(HammingCode code) {
        super(code);
    }

    public void encode(byte[] in, byte[] out) {
        switch (code) {
            case NO:
                for (int i = 0; i < in.length; i++) {
                    out[i] = in[i];
                }
                break;
            case HAMMING_7_4:
                encode_7_4(in, out);
                break;
            case HAMMING_15_11:
                encode_15_11(in, out);
                break;
        }
    }

    public int howManyHammings(int inputlength){
        return (int) Math.ceil(inputlength * 8.0 / inputSize());
    }

    public int outputByteCount(int inputlength){
        return (int) Math.ceil(howManyHammings(inputlength) * outputSize() / 8.0);
    }

    public void encode_7_4(byte[] in, byte[] out) {
        int len = howManyHammings(in.length);
        for (int i = 0; i < len; i++) {
            int b = i * 7;
            int a = i * 4;
            // now process 4 bits from the input, and output 7 bits
            // p1 = m1 ^ m2 ^ m4
            Bit.storeBigEndian(out, b, (getBit(in, a) ^ getBit(in, a + 1) ^ getBit(in, a + 3)));
            // p2 = m1 ^ m3 ^ m4
            Bit.storeBigEndian(out, b + 1, (getBit(in, a) ^ getBit(in, a + 2) ^ getBit(in, a + 3)));
            // p3 = m2 ^ m3 ^ m4
            Bit.storeBigEndian(out, b + 3, (getBit(in, a + 1) ^ getBit(in, a + 2) ^ getBit(in, a + 3)));
            // store the message bits also...:
            // m1
            Bit.storeBigEndian(out, b + 2, getBit(in, a));
            // m2
            Bit.storeBigEndian(out, b + 4, getBit(in, a + 1));
            // m3
            Bit.storeBigEndian(out, b + 5, getBit(in, a + 2));
            // m2
            Bit.storeBigEndian(out, b + 6, getBit(in, a + 3));
        }
    }

    public void encode_15_11(byte[] in, byte[] out) {
        int len = howManyHammings(in.length);
        for (int i = 0; i < len; i++) {
            int b = i * 15;
            int a = i * 11;

            // p1 = m1 ^ m2 ^ m4 ^ m5 ^ m7 ^ m9 ^ m11
            Bit.storeBigEndian(out, b, getBit(in, a) ^ getBit(in, a + 1) ^ getBit(in, a + 3) ^ getBit(in, a + 4) ^ getBit(in, a + 6) ^ getBit(in, a + 8) ^ getBit(in, a + 10));
            // p2 = m1 ^ m3 ^ m4 ^ m6 ^ m7 ^ m10 ^ m11
            Bit.storeBigEndian(out, b + 1, getBit(in, a) ^ getBit(in, a + 2) ^ getBit(in, a + 3) ^ getBit(in, a + 5) ^ getBit(in, a + 6) ^ getBit(in, a + 9) ^ getBit(in, a + 10));
            // p4 = m2 ^ m3 ^ m4 ^ m8 ^ m9 ^ m10 ^ m11
            Bit.storeBigEndian(out, b + 3, getBit(in, a + 1) ^ getBit(in, a + 2) ^ getBit(in, a + 3) ^ getBit(in, a + 7) ^ getBit(in, a + 8) ^ getBit(in, a + 9) ^ getBit(in, a + 10));
            // p8 = m5 ^ m6 ^ m7 ^ m8 ^ m9 ^ m10 ^ m11
            Bit.storeBigEndian(out, b + 7, getBit(in, a + 4) ^ getBit(in, a + 5) ^ getBit(in, a + 6) ^ getBit(in, a + 7) ^ getBit(in, a + 8) ^ getBit(in, a + 9) ^ getBit(in, a + 10));
            // m1
            Bit.storeBigEndian(out, b + 2, getBit(in, a));
            // m2
            Bit.storeBigEndian(out, b + 4, getBit(in, a + 1));
            // m3
            Bit.storeBigEndian(out, b + 5, getBit(in, a + 2));
            // m4
            Bit.storeBigEndian(out, b + 6, getBit(in, a + 3));
            // m5
            Bit.storeBigEndian(out, b + 8, getBit(in, a + 4));
            // m6
            Bit.storeBigEndian(out, b + 9, getBit(in, a + 5));
            // m7
            Bit.storeBigEndian(out, b + 10, getBit(in, a + 6));
            // m8
            Bit.storeBigEndian(out, b + 11, getBit(in, a + 7));
            // m9
            Bit.storeBigEndian(out, b + 12, getBit(in, a + 8));
            // m10
            Bit.storeBigEndian(out, b + 13, getBit(in, a + 9));
            // m11
            Bit.storeBigEndian(out, b + 14, getBit(in, a + 10));
        }
    }


    private boolean getBit(byte[] data, int pos) {
        if (pos >= data.length * 8) {
            // pad the data with if needed
            return false;
        }
        return 1 == Bit.getBitFromArray(data, pos);
    }
}
