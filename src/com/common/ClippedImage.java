/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 *
 * @author vdelaitr
 */
public class ClippedImage {
    private BufferedImage originalImage;
    private BufferedImage image;

    private boolean doclip;

    private int xmin, xmax;
    private int ymin, ymax;

    double scale;
    double distortion;
    double maxintensity = 0.08;

    public ClippedImage(BufferedImage img) {
        this(img, 0, true);
    }

    public ClippedImage(BufferedImage img, double dist, boolean clipping) {
        originalImage = img;
        image = img;
        scale = 1;
        doclip = clipping;
        if (dist != 0) {
            setDistortion(dist);
        }

        computeClipping();
    }

    public ClippedImage(int w, int h) {
        this(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), 0, false);
    }

    public void setClipping(int xm, int ym, int xM, int yM) {
        xmin = xm;
        ymin = ym;
        xmax = xM;
        ymax = yM;
    }

    final void computeClipping() {
        int w = image.getWidth();
        int h = image.getHeight();
        
        xmin = 0;
        ymin = 0;
        xmax = w - 1;
        ymax = h - 1;
        
        if (!doclip) {
            return;
        }

        // Remove black border on images        
        FloatImage intensity = new FloatImage(image);                
        
        while (xmin < w) {
            int y;
            for (y = 0; y < h; y++) {
                if (intensity.get(xmin, y) > maxintensity) {
                    break;
                }
            }
            if (y != h) {
                break;
            }
            xmin++;
        }
        while (xmax >= 0) {
            int y;
            for (y = 0; y < h; y++) {
                if (intensity.get(xmax, y) > maxintensity) {
                    break;
                }
            }
            if (y != h) {
                break;
            }
            xmax--;
        }
        while (ymin < h) {
            int x;
            for (x = 0; x < w; x++) {
                if (intensity.get(x, ymin) > maxintensity) {
                    break;
                }
            }
            if (x != w) {
                break;
            }
            ymin++;
        }
        while (ymax >= 0) {
            int x;
            for (x = 0; x < w; x++) {
                if (intensity.get(x, ymax) > maxintensity) {
                    break;
                }
            }
            if (x != w) {
                break;
            }
            ymax--;
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public short[] toArray() {
        final int oB = 0;
        final int oG = 8;
        final int oR = 16;
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        short[] img = new short[w * h * 3];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                img[(0 * w + x) * h + y] = (short)((pixels[y * w + x] >> oR) & 255);
            }
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                img[(1 * w + x) * h + y] = (short)((pixels[y * w + x] >> oG) & 255);
            }
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                img[(2 * w + x) * h + y] = (short)((pixels[y * w + x] >> oB) & 255);
            }
        }

        return img;
    }

    public int getXmin() {
        return xmin;
    }

    public int getXmax() {
        return xmax;
    }

    public int getYmin() {
        return ymin;
    }

    public int getYmax() {
        return ymax;
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public int getClippedWidth() {
        return xmax - xmin + 1;
    }

    public int getClippedHeight() {
        return ymax - ymin + 1;
    }

    public void resize(double s) {
        scale = s;
        BufferedImage img = new BufferedImage((int)Math.round(image.getWidth() * scale),
                                              (int)Math.round(image.getHeight() * scale),
                                              BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = img.createGraphics();
        AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics2D.drawImage(image, xform, null);
        graphics2D.dispose();
        image = img;

        System.out.println("Resized to " + image.getWidth() + "x" + image.getHeight());
        computeClipping();
    }

    public double getScale() {
        return scale;
    }

    public double getDistortion() {
        return distortion;
    }

    public void setDistortion(double d) {
        distortion = d;
        if (distortion == 0) {
            BufferedImage img = image;
            image = originalImage;
            if (img.getWidth() != image.getWidth()) {
                resize(((double)img.getWidth()) / image.getWidth());
            }
        }
        else {
            BufferedImage img = new BufferedImage(image.getWidth(),
                                                  image.getHeight(),
                                                  BufferedImage.TYPE_INT_RGB);
            int oxc = originalImage.getWidth() / 2;
            int oyc = originalImage.getHeight() / 2;
            int xc = image.getWidth() / 2;
            int yc = image.getHeight() / 2;
            double oCoordScale = 1. / Math.max(oxc, oyc);
            double coordScale = 1. / Math.max(xc, yc);
            double R = 1; //Math.sqrt(Math.pow(xc * coordScale, 2) + Math.pow(yc * coordScale, 2));
            double p = 1 / distortion;
            double q = - R / distortion;
            double delta = q * q + 4. * p * p * p / 27.;
            double rmax = cbrt((-q + Math.sqrt(delta)) / 2.) +
                          cbrt((-q - Math.sqrt(delta)) / 2.);
            double rescale = (1 + distortion * rmax * rmax);

            for (int x = 0; x < img.getWidth(); x++) {
                double xf = ((double)(x - xc)) * coordScale;
                for (int y = 0; y < img.getHeight(); y++) {
                    double yf = ((double)(y - yc)) * coordScale;
                    R = Math.sqrt(xf * xf + yf * yf);
                    q = - R / distortion;
                    delta = q * q + 4. * p * p * p / 27.;
                    double r = cbrt((-q + Math.sqrt(delta)) / 2.) +
                               cbrt((-q - Math.sqrt(delta)) / 2.);
                    double nxf = xf / (1 + distortion * r * r) * rescale;
                    double nyf = yf / (1 + distortion * r * r) * rescale;
                    double nx = nxf / oCoordScale + oxc;
                    double ny = nyf / oCoordScale + oyc;
                    img.setRGB(x, y, interpolatePixel(originalImage, nx, ny));
                }
            }

            image = img;
        }
    }

    private double cbrt(double x) {
        if (x < 0) {
            return -Math.pow(-x, 1. / 3.);
        }
        else {
            return Math.pow(x, 1. / 3.);
        }
    }

    private static int interpolatePixel(BufferedImage img, double x, double y) {
        int xi = (int)Math.floor(x);
        int yi = (int)Math.floor(y);
        double coeffx = x - xi;
        double coeffy = y - yi;
        Color c1 = new Color(getPixelSafe(img, xi, yi));
        Color c2 = new Color(getPixelSafe(img, xi + 1, yi));
        Color c3 = new Color(getPixelSafe(img, xi, yi + 1));
        Color c4 = new Color(getPixelSafe(img, xi + 1 , yi + 1));

        int red = (int)Math.round(c1.getRed() * (1 - coeffx) * (1 - coeffy) +
                                  c2.getRed() * coeffx * (1 - coeffy) +
                                  c3.getRed() * (1 - coeffx) * coeffy +
                                  c4.getRed() * coeffx * coeffy);
        int green = (int)Math.round(c1.getGreen() * (1 - coeffx) * (1 - coeffy) +
                                  c2.getGreen() * coeffx * (1 - coeffy) +
                                  c3.getGreen() * (1 - coeffx) * coeffy +
                                  c4.getGreen() * coeffx * coeffy);
        int blue = (int)Math.round(c1.getBlue() * (1 - coeffx) * (1 - coeffy) +
                                  c2.getBlue() * coeffx * (1 - coeffy) +
                                  c3.getBlue() * (1 - coeffx) * coeffy +
                                  c4.getBlue() * coeffx * coeffy);
        return (new Color(red, green, blue)).getRGB();
    }

    private static int getPixelSafe(BufferedImage img, int x, int y) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) {
            return 0;
        }
        else {
            return img.getRGB(x, y);
        }
    }
}
