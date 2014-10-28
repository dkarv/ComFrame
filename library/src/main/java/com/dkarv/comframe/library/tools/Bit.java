package com.dkarv.comframe.library.tools;

public class Bit {
    public static byte getBitFromArray(int[] data, int pos) {
        int posInt = pos / 32;
        int posBit = pos % 32;
        int valInt = data[posInt];
        byte valRe = (byte) (valInt >> (32 - (posBit + 1)) & 1);
        return valRe;
    }

    public static byte getBitFromArray(byte[] data, int pos) {
        int posByte = pos / 8;
        int posBit = pos % 8;
        byte valByte = data[posByte];
        byte valRe = (byte) (valByte >> (8 - (posBit + 1)) & 1);
        return valRe;
    }

    public static boolean getBit(byte b, int pos) {
        return (b >> (8 - (pos + 1)) & 1) == 1;
    }

    public static boolean getBit(int n, int pos) {
        return (n >> (32 - (pos + 1)) & 1) == 1;
    }

    public static byte[] intToByteArray(int[] intArray) {
        byte[] byteArray = new byte[intArray.length * 4];
        for (int i = 0; i < intArray.length; i++) {
            byteArray[i * 4] = intToByte(intArray[i], 3);
            byteArray[i * 4 + 1] = intToByte(intArray[i], 2);
            byteArray[i * 4 + 2] = intToByte(intArray[i], 1);
            byteArray[i * 4 + 3] = intToByte(intArray[i], 0);
        }
        return byteArray;
    }

    public static byte intToByte(int in, int whichbyte) {
        return (byte) (in >> (8 * whichbyte));
    }

    /**
     * stores the bit to the byte. pos=0 is the most left bit of the first byte in store
     *
     * @param store byte array where bit is set
     * @param pos   pos at which this method saves the bit
     * @param bit
     */
    public static void storeBigEndian(byte[] store, int pos, boolean bit) {
        int byteN = pos / 8;
        int bitN = pos % 8;
        if (bit) {
            store[byteN] |= 1 << (7 - bitN);
        } else {
            store[byteN] &= ~(1 << (7 - bitN));
        }
    }

    /**
     * stores the bit to the byte. pos=0 is the most left bit
     *
     * @param store byte where bit is set
     * @param pos   0 to 7
     * @param bit
     */
    public static byte storeBigEndian(byte store, int pos, boolean bit) {
        if (bit) {
            store |= 1 << (7 - pos);
        } else {
            store &= ~(1 << (7 - pos));
        }
        return store;
    }

    /**
     * calculate how many bit errors there are in check compared to right
     *
     * @param byte1
     * @param byte2
     * @return the number of bits where both bytes differ from each other
     */
    public static int countBitErrors(byte byte1, byte byte2) {
        int count = 0;
        byte1 = (byte) (byte1 ^ byte2);
        for (int i = 0; i < 8; i++) {
            count += byte1 & 1;
            byte1 >>= 1;
        }
        return count;
    }
}
