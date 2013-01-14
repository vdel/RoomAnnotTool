/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.room;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import com.common.ClippedImage;
import com.common.Descent.NewtonDescent;
import com.common.MyMatrix;
import com.common.MyVect;

/**
 *
 * @author vdelaitr
 */

public class RoomParameters extends NewtonDescent {
    final private int AINDEX = 0;
    final private int TINDEX = 3;
    private boolean sameFocalXY;

    double origwidth, origdepth, origheight;

    public double fx, fy, g;    
    public double width, depth, height;
    public MyMatrix F, invF, R, invR, K, invK;
    public MyVect t, cameraPosition, cameraAngle;
    
    static class DescentParams {
        Matrix V, W;
    }

    RoomParameters(ClippedImage img, MyVect focal, MyVect camAngle, MyVect camPos, double depth, double width, double height) {
        fx = focal.x;
        fy = focal.y;
        g = focal.z;
        cameraAngle = new MyVect(camAngle);
        cameraPosition = new MyVect(camPos);

        t = new MyVect();
        updateFRParams();
        t = R.mul(camPos).mul(-1);

        origdepth = this.depth = depth;
        origwidth = this.width = width;
        origheight = this.height = height;
    }

    RoomParameters(ClippedImage img, MyVect focal, MyVect camAngle, MyVect camPos, MyVect p1, boolean visibleLeft, boolean visibleRight, double H) {
        height = H;

        fx = focal.x;
        fy = focal.y;
        g = focal.z;
        cameraAngle = new MyVect(camAngle);

        t = new MyVect();
        updateFRParams();

        t = R.mul(camPos).mul(-1);

        setCameraAndRoomParams(img, p1, visibleLeft, visibleRight);
    }

    
    RoomParameters(ClippedImage img, MyVect[] vp, MyVect p0, MyVect pH, MyVect p1, boolean visibleLeft, boolean visibleRight, double H, boolean sameF) {
        height = H;
        sameFocalXY = sameF;
        cameraAngle = new MyVect();
        t = new MyVect();

        solve(vp, p0, pH, visibleLeft);

        setCameraAndRoomParams(img, p1, visibleLeft, visibleRight);
    }

    final void setCameraAndRoomParams(ClippedImage img, MyVect p1, boolean visibleLeft, boolean visibleRight) {
        cameraPosition = invR.mul(t).mul(-1);

        if (visibleLeft && visibleRight) {
            width = projectX(p1, 0);
            depth = cameraPosition.x;
        } else if (visibleLeft) {
            width = Math.max(cameraPosition.y, Math.max(
                             projectX(new MyVect(img.getXmax(), img.getYmin(), 1), 0),
                             projectX(new MyVect(img.getXmax(), img.getYmax(), 1), 0)));
            depth = cameraPosition.x;
        }
        else  {
            width = cameraPosition.y;
            depth = Math.max(cameraPosition.x, Math.max(
                             projectY(new MyVect(img.getXmin(), img.getYmin(), 1), 0),
                             projectY(new MyVect(img.getXmin(), img.getYmax(), 1), 0)));
        }


        origdepth = depth;
        origwidth = width;
        origheight = height;
    }

    final double projectX(MyVect p, double x) {
        p.z = 1;
        MyVect ray = invK.mul(p);
        double lambda = (x - cameraPosition.x) / ray.x;
        MyVect p3D = cameraPosition.add(ray.mul(lambda));
        return p3D.y;
    }

    final double projectY(MyVect p, double y) {
        p.z = 1;
        MyVect ray = invK.mul(p);
        double lambda = (y - cameraPosition.y) / ray.y;
        MyVect p3D = cameraPosition.add(ray.mul(lambda));
        return p3D.x;
    }

    final MyMatrix Matrix2MyMatrix(Matrix m) {
        MyMatrix mm = new MyMatrix();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                mm.set(j, i, m.get(j, i));
            }
        }
        return mm;
    }
    
    @Override
    public Matrix evaluateInitialX(Object params) {
        return param2VectFR();
    }
    
    @Override
    public Matrix evaluateFunction(Matrix x, Object params) {
        DescentParams p = (DescentParams)params;
        return computeG(x, p.V, p.W);        
    }
    
    @Override
    public Matrix evaluateJacobian(Matrix x, Object params) {
        DescentParams p = (DescentParams)params;
        return computeJG(x, p.W);        
    }
  
    private void solve(MyVect[] vp, MyVect p0, MyVect pH, boolean visibleLeft) {
        intialize(vp, p0, pH, visibleLeft);

        double vx, vy;
        DescentParams params = new DescentParams();
        params.V = new Matrix(2, 5);
        params.W = new Matrix(5, 5);
        for (int k = 0; k < 3; k++) {
            vx = vp[k].x / vp[k].z;
            vy = vp[k].y / vp[k].z;
            params.V.set(0, k, vx);
            params.V.set(1, k, vy);
            //W.set(k, k, Math.pow(1 / (vx * vx + vy * vy), 0.25));
        }
        vx = p0.x / p0.z;
        vy = p0.y / p0.z;
        params.V.set(0, 3, vx);
        params.V.set(1, 3, vy);
        //W.set(3, 3, Math.pow(1 / (vx * vx + vy * vy), 0.25));
        vx = pH.x / pH.z;
        vy = pH.y / pH.z;
        params.V.set(0, 4, vx);
        params.V.set(1, 4, vy);
        //W.set(4, 4, Math.pow(1 / (vx * vx + vy * vy), 0.25));
        params.W = Matrix.identity(5, 5);

        vect2ParamFR(solve(params));

        updateFRParams();
    }

    private void updateFRParams() {
        Matrix x = param2VectFR();

        Matrix _F, _R, _K;
        _F = computeF(x);
        F = Matrix2MyMatrix(_F);
        invF = Matrix2MyMatrix(_F.inverse());
        _R = computeR(x);
        R = Matrix2MyMatrix(_R);
        invR = Matrix2MyMatrix(_R.inverse());
        _K = _F.times(_R);
        K = Matrix2MyMatrix(_K);
        invK = Matrix2MyMatrix(_K.inverse());
    }

    private void intialize(MyVect[] vp, MyVect p0, MyVect pH, boolean visibleLeft) {
        // see "Multiple View Geometry", example 8.27
        Matrix V = new Matrix(3, 3);
        Matrix A = new Matrix(3, 1);
        Matrix one = new Matrix(3, 1); 

        Matrix w = null;

        double[] n = new double[3];
        n[0] = vp[0].norm();
        n[1] = vp[1].norm();
        n[2] = vp[2].norm();

        int far1 = n[0] < n[1] ? 1 : 0;
        int far2 = 1 - far1;
        if (n[2] > n[far1]) {
            far2 = far1;
            far1 = 2;
        }
        else if (n[2] > n[far2]) {
            far2 = 2;
        }

        int attempt = 0;
        while (attempt < 4) {           
            switch (attempt) {
                case 0:  break;
                case 1:  vp[far1].x = -vp[far1].x; vp[far1].y = -vp[far1].y; break;
                case 2:  vp[far2].x = -vp[far2].x; vp[far2].y = -vp[far2].y; break;
                default: vp[far1].x = -vp[far1].x; vp[far1].y = -vp[far1].y; break;
            }
            attempt++;
            
            int k = 0;
            for (int i = 0; i < 2; i++) {
                for (int j = i + 1; j < 3; j++, k++) {
                    A.set(k, 0, vp[i].x * vp[j].x + vp[i].y * vp[j].y);                   
                    one.set(k, 0, -1);
                }
            }

            w = A.solve(one);

            if (w.get(0, 0) > 0) {
                break;
            }
        }

        // previous while may fail. If so we continue with those values and pray.
        fx = fy = Math.sqrt(1 / Math.abs(w.get(0, 0)));
        g = 0;
     
        for (int i = 0; i < 3; i++) {
            V.set(0, i, vp[i].x);
            V.set(1, i, vp[i].y);
            V.set(2, i, 1);
        }        

        Matrix F = computeF(param2VectFR());
        Matrix RL = F.inverse().times(V);
        double l1 = RL.getMatrix(0, 2, 0, 0).norm2();
        double l2 = RL.getMatrix(0, 2, 1, 1).norm2();
        double l3 = RL.getMatrix(0, 2, 2, 2).norm2();

        // we assume z points towards the top of the image
        if (RL.get(1, 2) < 0) {
            l3 = -l3;
        }
        if (visibleLeft) {
            // x should point towards us
            if (RL.get(2, 0) < 0) {
                l1 = -l1;
            }
            if (RL.det() / (l1 * l3) < 0) {
                l2 = -l2;
            }
        }
        else {
            // y should point towards us
            if (RL.get(2, 1) < 0) {
                l2 = -l2;
            }
            if (RL.det() / (l2 * l3) < 0) {
                l1 = -l1;
            }
        }        

        // Find rotation matrix
        Matrix L = new Matrix(3, 3);
        L.set(0, 0, l1);
        L.set(1, 1, l2);
        L.set(2, 2, l3);

        Matrix R = RL.times(L.inverse());

        cameraAngle.x = Math.atan2(R.get(1, 2), R.get(0, 2));
        cameraAngle.y = Math.atan2(R.get(0, 2) / Math.cos(cameraAngle.x), R.get(2, 2));
        cameraAngle.z = Math.atan2(R.get(2, 1) / Math.sin(cameraAngle.y), R.get(2, 0) / -Math.sin(cameraAngle.y));

        // Create rotation and projection matrices
        updateFRParams();

        Matrix B = new Matrix(3, 3);

        // correct noise by projecting over z
        MyVect zproj = invK.mul(pH.sub(p0));
        zproj.x = 0;
        zproj.y = 0;
        zproj = K.mul(zproj);

        MyVect midpoint = p0.add(pH).mul(0.5);
        p0 = midpoint.sub(zproj.mul(0.5));
        pH = midpoint.add(zproj.mul(0.5));
        p0.z = 1;
        pH.z = 1;

        B.set(0, 0, K.get(0, 2));
        B.set(1, 0, K.get(1, 2));
        B.set(2, 0, K.get(2, 2));
        B.set(0, 1, p0.x);
        B.set(1, 1, p0.y);
        B.set(2, 1, 1);
        B.set(0, 2, -pH.x);
        B.set(1, 2, -pH.y);
        B.set(2, 2, -1);

        SingularValueDecomposition svd = B.transpose().times(B).svd();
        Matrix v = svd.getV().getMatrix(0, 2, 2, 2);

        double l4 = v.get(0, 0) / (height * v.get(1, 0));
        t = invF.mul(p0).mul(1 / l4);
    }

    private Matrix param2VectFR() {
        Matrix p = new Matrix(sameFocalXY ? 8 : 9, 1);
        p.set(0, 0, cameraAngle.x);
        p.set(1, 0, cameraAngle.y);
        p.set(2, 0, cameraAngle.z);
        p.set(3, 0, t.x);
        p.set(4, 0, t.y);
        p.set(5, 0, t.z);
        p.set(6, 0, fx);
        p.set(7, 0, g);
        if (!sameFocalXY) {
            p.set(8, 0, fy);
        }
        return p;
    }

    private void vect2ParamFR(Matrix p) {
        cameraAngle.x = p.get(0, 0);
        cameraAngle.y = p.get(1, 0);
        cameraAngle.z = p.get(2, 0);
        t.x = p.get(3, 0);
        t.y = p.get(4, 0);
        t.z = p.get(5, 0);
        fx = p.get(6, 0);                
        g  = p.get(7, 0);
        if (sameFocalXY) {
            fy = fx;       
        }
        else {
            fy = p.get(8, 0);
        }
    }

    private Matrix computeG(Matrix x, Matrix V, Matrix W) {
        Matrix mF  = computeF(x);
        Matrix mRT = computeRT(x);
        Matrix mL  = computeL(x);

        Matrix gx3D =  mF.times(mRT).times(mL);
        Matrix gx2D = new Matrix(2, 5);
        for (int i = 0; i < 5; i++) {
            gx2D.set(0, i, gx3D.get(0, i) / gx3D.get(2, i));
            gx2D.set(1, i, gx3D.get(1, i) / gx3D.get(2, i));
        }

        gx2D = gx2D.minus(V);     
        gx2D = gx2D.times(W);

        double[] array = gx2D.getColumnPackedCopy();
        return new Matrix(array, array.length);
    }

    private Matrix computeF(Matrix x) {
        Matrix mF = new Matrix(3, 3);
        mF.set(0, 0, x.get(6, 0));
        mF.set(1, 1, x.get(sameFocalXY ? 6 : 8, 0));
        mF.set(0, 1, x.get(7, 0));
        mF.set(2, 2, -1);
        return mF;
    }

    private Matrix computeRT(Matrix x) {
        Matrix RT = new Matrix(3, 4);        
        RT.setMatrix(0, 2, 0, 2, computeR(x));
        RT.set(0, 3, x.get(TINDEX + 0, 0));
        RT.set(1, 3, x.get(TINDEX + 1, 0));
        RT.set(2, 3, x.get(TINDEX + 2, 0));
        return RT;
    }

    private Matrix computeR(Matrix x) {
        Matrix Qz1 = computeQz(x.get(AINDEX + 0, 0));
        Matrix Qy  = computeQy(x.get(AINDEX + 1, 0));
        Matrix Qz2 = computeQz(x.get(AINDEX + 2, 0));
        return Qz1.times(Qy.times(Qz2));
    }

    private Matrix computeL(Matrix x) {
        Matrix P = new Matrix(4, 5);
        P.set(0, 0, 1);
        P.set(1, 1, 1);
        P.set(2, 2, 1);
        P.set(3, 3, 1);
        P.set(3, 4, 1);
        P.set(2, 4, height);
        return P;
    }

    private Matrix computeQz(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        Matrix Q = new Matrix(3, 3);
        Q.set(0, 0, c);
        Q.set(1, 1, c);
        Q.set(0, 1,-s);
        Q.set(1, 0, s);
        Q.set(2, 2, 1);
        return Q;
    }

    private Matrix computeQy(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        Matrix Q = new Matrix(3, 3);
        Q.set(0, 0, c);
        Q.set(2, 2, c);
        Q.set(0, 2, s);
        Q.set(2, 0,-s);
        Q.set(1, 1, 1);
        return Q;
    }

    private Matrix computeJG(Matrix x, Matrix W) {
        int nparams = x.getRowDimension();
        double[][] J = new double[nparams][];

        for (int i = 0; i < nparams; i++) {
            J[i] = computeDG(x, W, i);
        }

        return new Matrix(J).transpose();
    }

    private double[] computeDG(Matrix x, Matrix W, int i) {
        Matrix mF, mRT, mL;
        Matrix DF, DRT;
        mF  = computeF(x);
        mRT = computeRT(x);
        mL  = computeL(x);
        DF  = computeDF(i);
        DRT = computeDRT(x, i);

        Matrix gx3D =  mF.times(mRT).times(mL);
        Matrix dgx3D = DF.times(mRT).plus(mF.times(DRT)).times(mL);
        Matrix dgx2D = new Matrix(2, 5);
        for (int j = 0; j < 5; j++) {
            for (int k = 0; k < 2; k++) {
                double f  = gx3D.get(k, j);
                double df = dgx3D.get(k, j);
                double g  = gx3D.get(2, j);
                double dg = dgx3D.get(2, j);
                dgx2D.set(k, j, (df * g - dg * f) / (g * g));
            }
        }

        dgx2D = dgx2D.times(W);

        return dgx2D.getColumnPackedCopy();
    }

    private Matrix computeDF(int i) {
        Matrix mF = new Matrix(3, 3);
        mF.set(0, 0, i == 6 ? 1 : 0);
        mF.set(1, 1, i == (sameFocalXY ? 6 : 8) ? 1 : 0);
        mF.set(0, 1, i == 7 ? 1 : 0);
        return mF;
    }

    private Matrix computeDRT(Matrix x, int i) {
        Matrix DRT = new Matrix(3, 4);
        DRT.setMatrix(0, 2, 0, 2, computeDR(x, i));
        DRT.set(0, 3, i == (TINDEX + 0) ? 1 : 0);
        DRT.set(1, 3, i == (TINDEX + 1) ? 1 : 0);
        DRT.set(2, 3, i == (TINDEX + 2) ? 1 : 0);
        return DRT;
    }

    private Matrix computeDR(Matrix x, int i) {
        Matrix Qz1, Qy, Qz2;
        Matrix DQz1, DQy, DQz2;
        Qz1 = computeQz(x.get((AINDEX + 0), 0));
        Qy  = computeQy(x.get((AINDEX + 1), 0));
        Qz2 = computeQz(x.get((AINDEX + 2), 0));
        DQz1 = computeDQz(x.get((AINDEX + 0), 0), i == (AINDEX + 0));
        DQy  = computeDQy(x.get((AINDEX + 1), 0), i == (AINDEX + 1));
        DQz2 = computeDQz(x.get((AINDEX + 2), 0), i == (AINDEX + 2));

        return DQz1.times(Qy.times(Qz2)).plus(
               Qz1.times(DQy.times(Qz2)).plus(
               Qz1.times(Qy.times(DQz2))));
    }

    private Matrix computeDQz(double a, boolean itsMe) {
        Matrix Q = new Matrix(3, 3);
        if (itsMe) {
            double dc = -Math.sin(a);
            double ds =  Math.cos(a);
            Q.set(0, 0, dc);
            Q.set(1, 1, dc);
            Q.set(0, 1,-ds);
            Q.set(1, 0, ds);
        }
        return Q;
    }

    private Matrix computeDQy(double a, boolean itsMe) {
        Matrix Q = new Matrix(3, 3);
        if (itsMe) {
            double dc = -Math.sin(a);
            double ds =  Math.cos(a);
            Q.set(0, 0, dc);
            Q.set(2, 2, dc);
            Q.set(0, 2, ds);
            Q.set(2, 0,-ds);
        }
        return Q;
    }
}
