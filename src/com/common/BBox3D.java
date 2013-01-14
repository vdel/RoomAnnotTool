/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

/**
 *
 * @author vdelaitr
 */
public class BBox3D {
    public MyVect min;
    public MyVect max;

    public BBox3D() {
        double ip = Double.POSITIVE_INFINITY;
        double in = Double.NEGATIVE_INFINITY;
        min = new MyVect(ip, ip, ip);
        max = new MyVect(in, in, in);
    }
    
    public BBox3D(MyVect corner1, MyVect corner2) {
        this(corner1.x, corner1.y, corner1.z,
             corner2.x, corner2.y, corner2.z);
    }

    public BBox3D(double x1, double y1, double z1,
                  double x2, double y2, double z2) {
        min = new MyVect();
        max = new MyVect();
        min.x = Math.min(x1, x2);
        min.y = Math.min(y1, y2);
        min.z = Math.min(z1, z2);
        max.x = Math.max(x1, x2);
        max.y = Math.max(y1, y2);
        max.z = Math.max(z1, z2);
    }

    public void expand(BBox3D b) {
        min.x = Math.min(min.x, b.min.x);
        min.y = Math.min(min.y, b.min.y);
        min.z = Math.min(min.z, b.min.z);
        max.x = Math.max(max.x, b.max.x);
        max.y = Math.max(max.y, b.max.y);
        max.z = Math.max(max.z, b.max.z);  
    }
}
