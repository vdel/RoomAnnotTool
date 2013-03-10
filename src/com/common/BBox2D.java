/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import java.awt.Point;

/**
 *
 * @author vdelaitr
 */
public class BBox2D {
    public double xmin, xmax;
    public double ymin, ymax;

    public BBox2D() {
        xmin = Double.POSITIVE_INFINITY;
        ymin = Double.POSITIVE_INFINITY;
        xmax = Double.NEGATIVE_INFINITY;
        ymax = Double.NEGATIVE_INFINITY;
    }

    public BBox2D(double x1, double y1,
                  double x2, double y2) {
        xmin = Math.min(x1, x2);
        ymin = Math.min(y1, y2);
        xmax = Math.max(x1, x2);
        ymax = Math.max(y1, y2);
    }

    public void expand(BBox2D b) {
        xmin = Math.min(xmin, b.xmin);
        ymin = Math.min(ymin, b.ymin);
        xmax = Math.max(xmax, b.xmax);
        ymax = Math.max(ymax, b.ymax);
    }

    public void expand(Point p) {
        xmin = Math.min(xmin, p.x);
        ymin = Math.min(ymin, p.y);
        xmax = Math.max(xmax, p.x);
        ymax = Math.max(ymax, p.y);
    }
    
    public void expand(double x, double y) {
        xmin = Math.min(xmin, x);
        ymin = Math.min(ymin, y);
        xmax = Math.max(xmax, x);
        ymax = Math.max(ymax, y);
    }
}
