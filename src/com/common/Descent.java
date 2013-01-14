/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import Jama.Matrix;

/**
 *
 * @author vdelaitr
 */
public class Descent {
    
    public static abstract class NewtonDescent {
        public abstract Matrix evaluateInitialX(Object params);
        public abstract Matrix evaluateFunction(Matrix x, Object params);
        public abstract Matrix evaluateJacobian(Matrix x, Object params);
    
        public Matrix solve() {
            return solve(null);
        }

        public Matrix solve(Object params) {
            return solve(params, 1e-3);
        }

        public Matrix solve(Object params, double stoppingCriteria) {
            return solve(params, stoppingCriteria, false);
        }
        
        public Matrix solve(Object params, double stoppingCriteria, boolean display) {
            return Descent.solve(this, params, stoppingCriteria, display);
        }
        
        /*--------------------------------------------------------------------*/
        
        Matrix __evaluateInitialX(Object params) {
            return evaluateInitialX(params);
        }
        
        Matrix __evaluateFunction(Matrix x, Object params) {
            return evaluateFunction(x, params);
        }
        
        Matrix __evaluateJacobian(Matrix x, Object params) {
            return evaluateJacobian(x, params);
        }
    }
    
    /*------------------------------------------------------------------------*/
    
    public static abstract class NewtonLogBarrierDescent extends NewtonDescent {
        public abstract double evaluateBarriers(Matrix x, Object params);
        public abstract Matrix evaluateBarsGrad(Matrix x, Object params);
    
        /*--------------------------------------------------------------------*/        

        double t;
        
        @Override
        Matrix __evaluateFunction(Matrix x, Object params) {
            Matrix f = evaluateFunction(x, params);
            double b = evaluateBarriers(x, params);
            
            int n = f.getRowDimension();
            Matrix r = new Matrix(n + 1, 1);
            for (int i = 0; i < n; i++) {
                r.set(i, 0, f.get(i, 0));                
            }
            r.set(n, 0, b * t);
            return r;
        }
        
        @Override
        Matrix __evaluateJacobian(Matrix x, Object params) {
            Matrix j = evaluateJacobian(x, params);
            Matrix g = evaluateBarsGrad(x, params);
            
            int n = j.getRowDimension();
            double[][] v = j.getArray();
            double[][] r = new double[n + 1][];
            System.arraycopy(v, 0, r, 0, n);            
            r[n] = g.getRowPackedCopy();
            for (int i = 0; i < r[n].length; i++) {
                r[n][i] *= t;
            }
            
            return new Matrix(r);
        }
        
        @Override
        public Matrix solve(Object params, double stoppingCriteria, boolean display) {
            return solve(params, stoppingCriteria, 0, display);
        }
        
        public Matrix solve(Object params, double stoppingCriteria, double t0, boolean display) {
            Matrix x;
            
            if (t0 > 0) {
                t = t0;

                x = Descent.solve(this, params, stoppingCriteria, display);
                Matrix oldx;

                do {
                    oldx = x;
                    t /= 10;
                    x = Descent.solve(this, params, stoppingCriteria, display);                
                } while (Math.abs(x.norm2() - oldx.norm2()) > stoppingCriteria);
            }
            else {
                t = 0;
                x = Descent.solve(this, params, stoppingCriteria, display);
            }
             
            return x;
        }
    }
    
    /*------------------------------------------------------------------------*/
    
    private static Matrix solve(NewtonDescent pb, Object params, double stoppingCriteria, boolean display) {
        int n;
        long i;
        double factor = 10;
        double obj, newobj, dobj, lambda, lambda0;
        Matrix f, jtf, j, jt, jtj, x, dx, newx;
        
        x = pb.__evaluateInitialX(params);
        n = x.getRowDimension();

        f = pb.__evaluateFunction(x, params);
        obj = f.norm2();
        dobj = Double.POSITIVE_INFINITY;

        j = pb.__evaluateJacobian(x, params);
        jt = j.transpose();
        jtj = jt.times(j);
        lambda0 = 1e-3 * jtj.trace() / n;
        lambda = lambda0;
        
        i = 1;
        do {
            jtf = jt.times(f).times(-1);
            
            while (true) {
                dx = jtj.plus(Matrix.identity(n, n).times(lambda)).solve(jtf);
                
                newx = x.plus(dx);
                f = pb.__evaluateFunction(newx, params);
                newobj = f.norm2();

                if (newobj < obj) {
                    dobj = obj - newobj;
                    obj = newobj;
                    x = newx;
                    lambda /= factor;
                    break;
                }
                else {
                    lambda *= factor;
                    if (lambda / lambda0 > 1e10) {
                        System.err.println("l / l0 = " + (lambda / lambda0) + ", jtf = " + jtf.norm2());                    
                        if (jtf.norm2() > 1e-3) {
                            System.err.println("Wrong gradient !");
                        }
                        dobj = 0;
                        break;
                    }
                }
            }                       

            j = pb.__evaluateJacobian(x, params);
            jt = j.transpose();
            jtj = jt.times(j);
            
            if (display) {
                System.out.format("i = %d  obj = %.2f  ||dx|| = %.2f  lambda = %.2f\n", i,  f.norm2(), dx.norm2(), lambda);
            }
            i++;

        } while(dobj / obj > stoppingCriteria);

        return x;
    }
}
