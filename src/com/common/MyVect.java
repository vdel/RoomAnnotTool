/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.common;

import java.awt.Point;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 *
 * @author vdelaitr
 */
public class MyVect extends Vector3d {
    static final long serialVersionUID = 0L;
    
    public MyVect() {
        super();
    }

    public MyVect(double x, double y, double z) {
        super(x, y, z);
    }

    public MyVect(double x, double y) {
        super(x, y, 1);
    }

    public MyVect(Point p) {
        this(p.x, p.y);
    }

    public MyVect(Point3d p) {
        this(p.x, p.y, p.z);
    }

    public MyVect(Vector3d v) {
        super(v.x, v.y, v.z);
    }

    public Point toPoint() {
        return new Point((int)Math.round(x / z), (int)Math.round(y / z));
    }

    public void selfAdd(MyVect u) {
        x += u.x;
        y += u.y;
        z += u.z;
    }

    public MyVect add(MyVect u) {
        return add(this, u);
    }

    static public MyVect add(MyVect u, MyVect v) {
        MyVect r = new MyVect(u.x + v.x, u.y + v.y, u.z + v.z);
        return r;
    }

    public MyVect sub(MyVect u) {
        return sub(this, u);
    }

    static public MyVect sub(MyVect u, MyVect v) {
        MyVect r = new MyVect(u.x - v.x, u.y - v.y, u.z - v.z);
        return r;
    }

    public MyVect mul(double s) {
        return mul(this, s);
    }

    static public MyVect mul(MyVect u, double s) {
        MyVect r = new MyVect(u.x * s, u.y * s, u.z * s);
        return r;
    }

    public double dot(MyVect u) {
        return dot(this, u);
    }

    static public double dot(MyVect u, MyVect v) {
        return u.x * v.x + u.y * v.y + u.z * v.z;
    }

    public MyVect det(MyVect u) {
        return det(this, u);
    }

    static public MyVect det(MyVect u, MyVect v) {
        return new MyVect(u.y * v.z - u.z * v.y,
                        u.z * v.x - u.x * v.z,
                        u.x * v.y - u.y * v.x);
    }


    public double norm() {
        return norm(this);
    }

    static public double norm(MyVect u) {
        return Math.sqrt(normSQ(u));
    }

    public double normSQ() {
        return normSQ(this);
    }

    static public double normSQ(MyVect u) {
        return u.dot(u);
    }

    static public MyVect intersect(MyVect a, MyVect u, MyVect b, MyVect v) {
        // compute min_l1,l2 (a + l1 u - b - l2 v)
        // returns mid-point of [a + l1^* u, b + l2^* v]
        // If u and v are coplanar, it corresponds to intersection point
        double uu = MyVect.dot(u, u);
        double vv = MyVect.dot(v, v);
        double uv = MyVect.dot(u, v);
        double d  = uu * vv - uv * uv;
        if (d == 0) { // vectors are colinear
            return new MyVect(u.x, u.y, 0);
        }
        else {
            MyVect p   = MyVect.sub(a, b);
            double up = MyVect.dot(u, p);
            double vp = MyVect.dot(v, p);
            double l1 = (vp * uv - up * vv) / d;
            double l2 = (vp * uu - up * uv) / d;
            return MyVect.add(a.add(u.mul(l1)), b.add(v.mul(l2))).mul(0.5f);
        }
    }

    static public double sum(MyVect u) {
        return u.x + u.y + u.z;
    }

    public double sum() {
        return x + y + z;
    }

    public void print() {
        System.out.println(x + "\t" + y + "\t" + z);
    }

    public double[] toArray() {
        double[] r = new double[3];
        r[0] = x;
        r[1] = y;
        r[2] = z;

        return r;
    }
}
