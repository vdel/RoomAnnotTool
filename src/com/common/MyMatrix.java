/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import com.common.MyVect;
import javax.vecmath.Matrix3d;

/**
 *
 * @author vdelaitr
 */
public class MyMatrix extends Matrix3d {
    static final long serialVersionUID = 0L;

    public enum Dimension {X, Y, Z};

    public MyMatrix() {
        super();
    }

    public MyMatrix(MyMatrix M) {
        super(M);
    }

    public MyMatrix(double d1, double d2, double d3) {  // diagonal matrix
        this();
        set(0, 0, d1);
        set(1, 1, d2);
        set(2, 2, d3);
    }

    public MyMatrix(double d1, double d2, double d3, double c12, double c13, double c23) {  // upper-triangular matrix
        this(d1, d2, d3);            
        set(0, 1, c12);
        set(0, 2, c13);
        set(1, 2, c23);
    }

    public MyMatrix(double d) { // diagonal matrix
        this(d, d, d);
    }

    public MyMatrix(double[] c) {
        super(c);
    }

    public void set(int i, int j, double v) {
        setElement(i, j, v);
    }

    public double get(int i, int j) {
        return getElement(i, j);
    }

    public MyVect getRow(int i) {
        return new MyVect(get(i, 0), get(i, 1), get(i, 2));
    }

    public MyVect getCol(int i) {
        return new MyVect(get(0, i), get(1, i), get(2, i));
    }

    public void print() {
        for (int i = 0; i < 3; i++) {
            System.out.println(get(i, 0) + "\t" + get(i, 1) + "\t" + get(i, 2));
        }
    }

    public MyVect mul(MyVect v) {
        MyVect r = new MyVect();
        r.x = get(0, 0) * v.x + get(0, 1) * v.y + get(0, 2) * v.z;
        r.y = get(1, 0) * v.x + get(1, 1) * v.y + get(1, 2) * v.z;
        r.z = get(2, 0) * v.x + get(2, 1) * v.y + get(2, 2) * v.z;
        return r;
    }

    public static MyMatrix Identity() {
        return new MyMatrix(1, 1, 1);
    }

    public static MyMatrix rotationX(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        MyMatrix R = new MyMatrix(1, c, c);
        R.set(1, 2, -s);
        R.set(2, 1,  s);
        return R;
    }

    public static MyMatrix rotationY(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        MyMatrix R = new MyMatrix(c, 1, c);
        R.set(0, 2,  s);
        R.set(2, 0, -s);
        return R;
    }

    public static MyMatrix rotationZ(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        MyMatrix R = new MyMatrix(c, c, 1);
        R.set(0, 1, -s);
        R.set(1, 0,  s);
        return R;
    }

    public static MyMatrix derivRotX(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        MyMatrix R = new MyMatrix(0, -s, -s);
        R.set(1, 2, -c);
        R.set(2, 1,  c);
        return R;
    }

    public static MyMatrix derivRotY(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        MyMatrix R = new MyMatrix(-s, 0, -s);
        R.set(0, 2,  c);
        R.set(2, 0, -c);
        return R;
    }

    public static MyMatrix derivRotZ(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        MyMatrix R = new MyMatrix(-s, -s, 0);
        R.set(0, 1, -c);
        R.set(1, 0,  c);
        return R;
    }

    public double norm() {
        return norm(this);
    }

    public static double norm(MyMatrix m) {
        return Math.sqrt(normSQ(m));
    }

    public double normSQ() {
        return normSQ(this);
    }

    static public double normSQ(MyMatrix m) {
        return m.getCol(0).normSQ() +
               m.getCol(1).normSQ() +
               m.getCol(2).normSQ();
    }

    static public class RQDecomposition {
        MyMatrix R;
        MyMatrix Q;

        public RQDecomposition(MyMatrix M) {  // Decompose M in to an upper-triangular
                                     // matrix U and a rotation matrix M s.t.
                                     // M = UR
            MyMatrix Qaxis;
            R = new MyMatrix(M);
            Q = new MyMatrix(1, 1, 1);
            Qaxis = getRotation(Dimension.X, R.get(2, 1), R.get(2, 2));
            R.mul(R, Qaxis);
            Qaxis.transpose();
            Q.mul(Qaxis, Q);

            Qaxis = getRotation(Dimension.Y, R.get(2, 0), R.get(2, 2));
            R.mul(R, Qaxis);
            Qaxis.transpose();
            Q.mul(Qaxis, Q);

            Qaxis = getRotation(Dimension.Z, R.get(1, 0), R.get(1, 1));
            R.mul(R, Qaxis);
            Qaxis.transpose();
            Q.mul(Qaxis, Q);
        }

        final public MyMatrix getRotation(Dimension d, double a1, double a2) {
            double c =-a2 / Math.sqrt(a1 * a1 + a2 * a2);
            double s = a1 / Math.sqrt(a1 * a1 + a2 * a2);
            MyMatrix Q;
            switch (d) {
                case X:  
                    Q = new MyMatrix(1, c, c);
                    Q.set(1, 2, -s);
                    Q.set(2, 1,  s);
                    break;
                case Y:
                    Q = new MyMatrix(c, 1, c);
                    Q.set(0, 2,  s);
                    Q.set(2, 0, -s);
                    break;
                default:
                    Q = new MyMatrix(c, c, 1);
                    Q.set(0, 1, -s);
                    Q.set(1, 0,  s);
                    break;                    
            }
            return Q;
        }
    }

    public double[] toArray() {
        double[] r = new double[9];
        int k = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++, k++) {
                r[k] = get(j, i);
            }
        }

        return r;
    }
}
