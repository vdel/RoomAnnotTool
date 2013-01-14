package com.common;


import java.awt.image.BufferedImage;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author vdelaitr
 */
public class FloatImage {

    private int w, h;

    private double[] pix;
    
    public FloatImage(int width, int height) {
        w = width;
        h = height;

        pix = new double[w * h];
    }

    public FloatImage(BufferedImage img) {
        w = img.getWidth();
        h = img.getHeight();
        int c, r, g, b;

        pix = new double[w * h];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                c = img.getRGB(x, y);
                b = c & 255;
                g = (c >>8) & 255;
                r = (c >> 16) & 255;
                pix[x * h + y] = ((double)b) / 255.f * 0.11f +
                                 ((double)g) / 255.f * 0.59f +
                                 ((double)r) / 255.f * 0.30f;
            }
        }
    }

    public void set(int x, int y, double v) {
        pix[x * h + y] = v;
    }

    public double get(int x, int y) {
        return pix[x * h + y];
    }

    public double get(int i) {
        return pix[i];
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public FloatImage transpose() {
        FloatImage r = new FloatImage(h, w);

        if (h == 1 || w == 1) {
            r.pix = pix.clone();
        }
        else {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    r.pix[y * w + x] = pix[x * h + y];
                }
            }
        }

        return r;
    }

    static public FloatImage add(FloatImage a, FloatImage b) {
        FloatImage r = new FloatImage(a.w, a.h);

        for (int i = 0; i < a.w * a.h; i++) {
            r.pix[i] = a.pix[i] + b.pix[i];
        }

        return r;
    }

    static public FloatImage mult(FloatImage a, FloatImage b) {
        FloatImage r = new FloatImage(a.w, a.h);

        for (int i = 0; i < a.w * a.h; i++) {
            r.pix[i] = a.pix[i] * b.pix[i];
        }

        return r;
    }

    public void mult(double s) {
        for (int i = 0; i < w * h; i++) {
            pix[i] = pix[i] * s;
        }
    }

    public void sqrt() {
        for (int i = 0; i < w * h; i++) {
            pix[i] = Math.sqrt(pix[i]);
        }
    }

    public double max() {
        double m = pix[0];
        for (int i = 1; i < w * h; i++) {
            if (pix[i] > m) {
                m = pix[i];
            }
        }
        return m;
    }

    public FloatImage filter(FloatImage filter) {
        FloatImage r = new FloatImage(w, h);
        
        int cx = filter.w / 2;
        int cy = filter.h / 2;
        int ix, iy;

        // Naive implementation
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double sum = 0;
                for (int fx = 0; fx < filter.w; fx++) {
                    for (int fy = 0; fy < filter.h; fy++) {
                        ix = x + fx - cx;
                        iy = y + fy - cy;
                        if (ix < 0) ix = 0; else if (ix >= w) ix = w - 1;
                        if (iy < 0) iy = 0; else if (iy >= h) iy = h - 1;
                        sum += pix[ix * h + iy] * filter.pix[fx * filter.h + fy];
                    }
                }
                r.pix[x * h + y] = sum;
            }
        }

        return r;
    }
}
