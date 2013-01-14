/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.gui;

import com.annot.room.ObjectInstance;
import com.common.BBox2D;
import com.common.MyMatrix;
import com.common.MyVect;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author vdelaitr
 */
public abstract class ConstrainedMotion {
    ObjectInstance object;
    MyVect vx, vy, vn;
    MyMatrix motionProj;
    MyVect originalPos;
    HashMap<String, Double> originalVars;
    BBox2D bound;
    LinkedList<BBox2D> bounds;

    ConstrainedMotion(ObjectInstance o) {
        object = o;
        updateOriginalVars();

        bound = object.getAttachedFaceBound();
        bounds = new LinkedList<BBox2D>();

        vx = object.getAttachedFaceX();
        vy = object.getAttachedFaceY();
        vn = object.getAttachedFaceNormal();

        motionProj = o.getAttachedRotation();
        motionProj.transpose();

        if (object.getAttachedFace() != null) {
            for (ObjectInstance obj : object.getAttachedFace().getChildren()) {
                if (obj != object && !obj.isPrivate()) {
                    bounds.add(obj.getBound2D());
                }
            }
        }
    }

    boolean checkBoundConstraint(HashMap<String, Double> variables, MyVect position, MyVect motion) {
        BBox2D b;
        double x, y;
        boolean ok = true;

        MyVect mCopy = motionProj.mul(motion);

        for (BBox2D bd : bounds) {
            b = object.getBound2D(variables, position.add(mCopy));
            ok = correctCollision(vx, vy, b, bd, mCopy) && ok;
        }
        

        if (!object.isPrivate()) {
            b = object.getBound2D(variables, position.add(mCopy));

            x = Math.max(0, bound.xmin - b.xmin) +
                Math.min(0, bound.xmax - b.xmax);
            y = Math.max(0, bound.ymin - b.ymin) +
                Math.min(0, bound.ymax - b.ymax);
            if (Math.abs(x) > 1e-5) {
                ok = false;
                mCopy.selfAdd(vx.mul(x));
            }
            if (Math.abs(y) > 1e-5) {
                ok = false;
                mCopy.selfAdd(vy.mul(y));
            }
        }

        motion.x = mCopy.x;
        motion.y = mCopy.y;
        motion.z = mCopy.z;

        return ok;
    }
    
    boolean correctCollision(MyVect vx, MyVect vy, BBox2D b1, BBox2D b2, MyVect motion) {
        double x = 0, y = 0;
        if (b1.xmin < b2.xmax && b1.xmax > b2.xmin &&
            b1.ymin < b2.ymax && b1.ymax > b2.ymin) {
            double x1 = b2.xmax - b1.xmin;
            double x2 = b2.xmin - b1.xmax;
            double x0 = Math.abs(x1) < Math.abs(x2) ? x1 : x2;
            double y1 = b2.ymax - b1.ymin;
            double y2 = b2.ymin - b1.ymax;
            double y0 = Math.abs(y1) < Math.abs(y2) ? y1 : y2;
            if (Math.abs(x0) < Math.abs(y0)) {
                x = x0;
            }
            else {
                y = y0;
            }
        }

        boolean ok = true;           
        if (Math.abs(x) > 1e-5 && Math.abs(x) < 0.1) {
            ok = false;
            motion.selfAdd(vx.mul(x));
        }
        if (Math.abs(y) > 1e-5 && Math.abs(y) < 0.1) {
            ok = false;
            motion.selfAdd(vy.mul(y));
        }

        return ok;
    }

    final void updateOriginalVars() {
        originalPos = object.getPosition();
        originalVars = new HashMap<String, Double>(object.getVariables());
    }

    abstract void update(MyVect rayOrigin, MyVect ray);
}
