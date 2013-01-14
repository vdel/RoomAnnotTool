/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.gui;

import com.annot.room.ObjectInstance;
import com.common.BBox2D;
import com.common.MyVect;

/**
 *
 * @author vdelaitr
 */
public class LineConstrainedMotion extends ConstrainedMotion {
    MyVect normal;
    MyVect origin;
    BBox2D expandedBound;
    ObjectInstance3D object3D;

    LineConstrainedMotion(ObjectInstance3D o, MyVect origin, MyVect normal) {
        super(o);
        object3D = o;
        this.normal = object.getAttachedRotation().mul(object.getRotationMatrix().mul(normal));
        this.origin = origin;

        expandedBound = new BBox2D();
        for (BBox2D b : bounds) {
            expandedBound.expand(b);
        }

        for (ConstrainedResize cr : object3D.getResizeBinds()) {
            cr.updateOriginalVars();
        }
    }

    @Override
    void update(MyVect rayOrigin, MyVect ray) {  // only used when moving walls
        if (normal != null) {
            MyVect p = computeRayLineInstersection(rayOrigin, ray, origin, normal);
            if (p != null) {
                MyVect motion = p.sub(origin).mul(0.5);
                double def = motion.dot(normal);  // floor's origin moves
                boolean ok = checkBoundConstraint(originalVars, originalPos, motion);

                for (ConstrainedResize cr : object3D.getResizeBinds()) {
                    ok = ok && cr.updatesize((cr.object.isFloor() ? 2 : 1) * def, false);
                }

                if (ok && Math.abs(def) > 1e-5) {
                    object.setPosition(originalPos.add(motion));
                }
            }
        }
    }

    static MyVect computeRayLineInstersection(MyVect rayOrigin, MyVect ray, MyVect planeOrigin, MyVect normal) {        
        double uu = MyVect.dot(ray, ray);
        double vv = MyVect.dot(normal, normal);
        double uv = MyVect.dot(ray, normal);
        double d  = uu * vv - uv * uv;
        if (d == 0) { // vectors are colinear
            return planeOrigin;
        }
        else {
            MyVect p   = MyVect.sub(rayOrigin, planeOrigin);
            double up = MyVect.dot(ray, p);
            double vp = MyVect.dot(normal, p);
            double l2 = (vp * uu - up * uv) / d;
            return planeOrigin.add(normal.mul(l2));
        }
    }
}