package com.dkarv.comframe.library.math;


public class Goertzel {
    private int n;
    private double realW;
    private double imagW;

    public Goertzel(int n, int bin) {
        this.n = n;
        realW = 2.0 * Math.cos(2.0 * Math.PI * bin / n);
        imagW = Math.sin(2.0 * Math.PI * bin / n);
    }

    /**
     * @param in raw buffer from the mic
     * @param out length=2, [0] is real part, [1] is imaginary part
     */
    public void goertzel(double[] in, double[] out) {
        double d1 = 0.0;
        double d2 = 0.0;
        for (int i = 0; i < n; i++) {
            double y = in[i] + realW * d1 - d2;
            d2 = d1;
            d1 = y;
        }
        out[0] = 0.5 * realW * d1 - d2;
        out[1] = imagW * d1;
    }

    public void goertzel(double[] in, int start, double[] out) {
        double d1 = 0.0;
        double d2 = 0.0;
        for (int i = 0; i < n; i++) {
            double y = in[(i + start) % in.length] + realW * d1 - d2;
            d2 = d1;
            d1 = y;
        }
        out[0] = 0.5 * realW * d1 - d2;
        out[1] = imagW * d1;
    }
}
