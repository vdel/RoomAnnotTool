package com.annot.room;


import com.common.ClippedImage;
import com.common.FloatImage;
import java.util.LinkedList;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author vdelaitr
 */
public class CannyEdges {
    // Implementation of Canny Edge detection inspired from Matlab 2009's implementation

    // Result
    int w, h;
    boolean[] weakEdges;
    boolean[] strongEdges;

    // Magic numbers
    private final double GaussianDieOff = .0001f;
    private final double PctPixNotEdges = .7f;

    // Filters
    private FloatImage gau, dgau2D;

    /**************************************************************************/

    public CannyEdges(ClippedImage image) {
        this(image, 0);
    }

    public CannyEdges(ClippedImage image, double highTh) {
        this(image, highTh, .4f * highTh);
    }

    public CannyEdges(ClippedImage image, double highTh, double lowTh) {
        this(image, highTh, lowTh, 1);
    }

    public CannyEdges(ClippedImage image, double highTh, double lowTh, double sigma) {
        w = image.getWidth();
        h = image.getHeight();
        FloatImage img = new FloatImage(image.getImage());
        
        int x0 = image.getXmin();
        int y0 = image.getYmin();
        int x1 = image.getXmax();
        int y1 = image.getYmax();

        // Filter image
        getFilters(sigma);
        img = img.filter(gau);
        img = img.filter(gau.transpose());
        FloatImage ix = img.filter(dgau2D);
        FloatImage iy = img.filter(dgau2D.transpose());        
        FloatImage mag = FloatImage.add(FloatImage.mult(ix, ix), FloatImage.mult(iy, iy));
        mag.sqrt();
        double magmax = mag.max();
        if (magmax > 0) {
            mag.mult(1 / magmax);
        }      

        // Eventually select threshold
        if (highTh == 0) {
            int stopAt = (int)(w * h * PctPixNotEdges);
            int[] hist = new int[64];
            int bin, sum;
            for (int i = 0; i < w * h; i++) {
                bin = (int)(mag.get(i) * 64);
                if (bin > 63) {
                    bin = 63;
                }
                hist[bin]++;
            }
            sum = 0;
            for (bin = 0; bin < 63; bin++) {
                sum += hist[0];
                if (sum > stopAt) {
                    break;
                }
            }
            highTh = (bin + 1) / 64;
            lowTh = highTh * .4f;
        }

        // Compute weak edges
        weakEdges = new boolean[w * h];
        LinkedList<Integer> strongMax = new LinkedList<Integer>();
        for (int i = 0; i < 4; i++) {
            LinkedList<Integer> localMax = getLocalMaxima(i, x0, y0, x1, y1, ix, iy, mag, lowTh);
            for (Integer k : localMax) {
                weakEdges[k] = true;
                if (mag.get(k) > highTh) {
                    strongMax.push(k);
                }
            }
        }

        // Compute strong edges
        strongEdges = new boolean[w * h];
        for (Integer k : strongMax) {
            followEdges(k);
        }

        // Prune edges
        for (int x = 0; x < w; x++) {
            for (int y = x % 2; y < h; y += 2) {
                if (strongEdges[x * h + y]) {
                    strongEdges[x * h + y] = keepPixel(x, y, true);
                }
            }
        }
        for (int x = 0; x < w; x++) {
            for (int y = (x + 1) % 2; y < h; y += 2) {
                if (strongEdges[x * h + y]) {
                    strongEdges[x * h + y] = keepPixel(x, y, false);
                }
            }
        }       
    }

    /**************************************************************************/

    final private void getFilters(double sigma) {
        double ssq = sigma * sigma;
        int width;
        for (width = 30; width > 0; width--) {
            if (Math.exp(-(width * width) / (2 * ssq)) > GaussianDieOff) {
                break;
            }
        }
        if (width == 0) {
            width = 1;
        }

        gau    = new FloatImage(2 * width + 1, 1);
        dgau2D = new FloatImage(2 * width + 1, 2 * width + 1);
        for (int x = -width; x <= width; x++) {
            gau.set(x + width, 0, Math.exp(-(x * x) / (2 * ssq)) / (2 * Math.PI * ssq));
            for (int y = -width; y <= width; y++) {
                dgau2D.set(x + width, y + width, -x * Math.exp(-(x * x + y * y) / (2 * ssq)) / (Math.PI * ssq));
            }
        }
    }

    /**************************************************************************/

    final private LinkedList<Integer> getLocalMaxima(int direction,
                                                     int x0, int y0,
                                                     int x1, int y1,
                                                     FloatImage ix,
                                                     FloatImage iy,
                                                     FloatImage mag,
                                                     double lowTh) {
        LinkedList<Integer> localMax = new LinkedList<Integer>();

        double ixv, iyv, d, mag1, mag2;

        switch (direction) {
            case 0:
                for (int x = x0 + 2; x <= x1 - 2; x++) {
                    for (int y = y0 + 2; y <= y1 - 2; y++) {
                        if (mag.get(x, y) > lowTh) {
                            ixv = ix.get(x, y);
                            iyv = iy.get(x, y);
                            if ((iyv <= 0 && ixv > -iyv) ||
                                (iyv >= 0 && ixv < -iyv)) {
                                d = Math.abs(iyv / ixv);
                                mag1 = mag.get(x + 1, y) * (1 - d) + mag.get(x + 1, y - 1) * d;
                                mag2 = mag.get(x - 1, y) * (1 - d) + mag.get(x - 1, y + 1) * d;
                                if (mag.get(x, y) > mag1 &&
                                    mag.get(x, y) > mag2) {
                                    localMax.push(x * h + y);
                                }
                            }
                        }
                    }
                }
                break;
            case 1:
                for (int x = x0 + 2; x <= x1 - 2; x++) {
                    for (int y = y0 + 2; y <= y1 - 2; y++) {
                        if (mag.get(x, y) > lowTh) {
                            ixv = ix.get(x, y);
                            iyv = iy.get(x, y);
                            if ((ixv > 0 && -iyv >= ixv) ||
                                (ixv < 0 && -iyv <= ixv)) {
                                d = Math.abs(ixv / iyv);
                                mag1 = mag.get(x, y - 1) * (1 - d) + mag.get(x + 1, y - 1) * d;
                                mag2 = mag.get(x, y + 1) * (1 - d) + mag.get(x - 1, y + 1) * d;
                                if (mag.get(x, y) > mag1 &&
                                    mag.get(x, y) > mag2) {
                                    localMax.push(x * h + y);
                                }
                            }
                        }
                    }
                }
                break;
            case 2:
                for (int x = x0 + 2; x <= x1 - 2; x++) {
                    for (int y = y0 + 2; y <= y1 - 2; y++) {
                        if (mag.get(x, y) > lowTh) {
                            ixv = ix.get(x, y);
                            iyv = iy.get(x, y);
                            if ((ixv <= 0 && ixv > iyv) ||
                                (ixv >= 0 && ixv < iyv)) {
                                d = Math.abs(ixv / iyv);
                                mag1 = mag.get(x, y - 1) * (1 - d) + mag.get(x - 1, y - 1) * d;
                                mag2 = mag.get(x, y + 1) * (1 - d) + mag.get(x + 1, y + 1) * d;
                                if (mag.get(x, y) > mag1 &&
                                    mag.get(x, y) > mag2) {
                                    localMax.push(x * h + y);
                                }
                            }
                        }
                    }
                }
                break;
            default:
                for (int x = x0 + 2; x <= x1 - 2; x++) {
                    for (int y = y0 + 2; y <= y1 - 2; y++) {
                        if (mag.get(x, y) > lowTh) {
                            ixv = ix.get(x, y);
                            iyv = iy.get(x, y);
                            if ((iyv < 0 && ixv <= iyv) ||
                                (iyv > 0 && ixv >= iyv)) {
                                d = Math.abs(iyv / ixv);
                                mag1 = mag.get(x - 1, y) * (1 - d) + mag.get(x - 1, y - 1) * d;
                                mag2 = mag.get(x + 1, y) * (1 - d) + mag.get(x + 1, y + 1) * d;
                                if (mag.get(x, y) > mag1 &&
                                    mag.get(x, y) > mag2) {
                                    localMax.push(x * h + y);
                                }
                            }
                        }
                    }
                }
                break;
        }

        return localMax;
    }

    /**************************************************************************/

    private void followEdges(int i) {
        if (!strongEdges[i]) {
            strongEdges[i] = true;
            int x0 = i / h;
            int y0 = i % h;
            for (int x = x0 - 1; x < x0 + 1 && x < w; x++) { // no edge on the image boundaries
                for (int y = y0 - 1; y < y0 + 1 && y < h; y++) {
                    i = x * h + y;
                    if (weakEdges[i]) {
                        followEdges(i);
                    }
                }
            }
        }
    }

    /**************************************************************************/

    private boolean keepPixel(int x, int y, boolean firstSubfield) {
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, -1, -1, -1, 0, 1, 1, 1};
        if ((firstSubfield && (
                strongEdges[(x + dx[0]) * h + (y + dy[0])] && (
                strongEdges[(x + dx[1]) * h + (y + dy[1])] ||
                strongEdges[(x + dx[2]) * h + (y + dy[2])] ||
               !strongEdges[(x + dx[7]) * h + (y + dy[7])]))) ||
            (!firstSubfield && (
                strongEdges[(x + dx[4]) * h + (y + dy[4])] && (
                strongEdges[(x + dx[5]) * h + (y + dy[5])] ||
                strongEdges[(x + dx[6]) * h + (y + dy[6])] ||
               !strongEdges[(x + dx[3]) * h + (y + dy[3])])))) {
            int n0 = 0;
            int n1 = 0;
            int n2 = 0;
            for (int i = 0; i < 4; i++) {
                if (!strongEdges[(x + dx[2 * i]) * h + (y + dy[2 * i])] && (
                     strongEdges[(x + dx[2 * i + 1]) * h + (y + dy[2 * i + 1])] ||
                     strongEdges[(x + dx[(2 * i + 2) % 8]) * h + (y + dy[(2 * i + 2) % 8])])) {
                    n0++;
                }
                if (strongEdges[(x + dx[2 * i]) * h + (y + dy[2 * i])] ||
                    strongEdges[(x + dx[2 * i + 1]) * h + (y + dy[2 * i + 1])]) {
                    n1++;
                }
                if (strongEdges[(x + dx[2 * i + 1]) * h + (y + dy[2 * i + 1])] ||
                    strongEdges[(x + dx[(2 * i + 2) % 8]) * h + (y + dy[(2 * i + 2) % 8])]) {
                    n2++;
                }
                if (n0 > 1 || (n1 > 3 && n2 > 3)) {
                    return true;
                }
            }
            return !(n0 == 1 && (n1 >= 2 && n2 >= 2));
        }
        else {
            return true;
        }
    }
}
