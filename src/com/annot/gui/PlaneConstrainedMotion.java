/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.gui;

import com.annot.room.ObjectInstance;
import com.common.MyVect;

/**
 *
 * @author vdelaitr
 */
public class PlaneConstrainedMotion extends ConstrainedMotion {
    MyVect normal;
    MyVect origin;

    PlaneConstrainedMotion(ObjectInstance3D o, MyVect origin) {
        super(o);        
        normal = object.getAttachedRotation().mul(object.getAttachedFaceNormal());
        this.origin = origin;
    }

    void update(MyVect rayOrigin, MyVect ray) {
        if (normal != null) {
            MyVect p = computeRayPlaneInstersection(rayOrigin, ray, origin, normal);
            if (p != null) {
                MyVect motion = p.sub(origin);
                checkBoundConstraint(object.getVariables(), originalPos, motion);
                object.setPosition(originalPos.add(motion));
            }
        }
    }

    static MyVect computeRayPlaneInstersection(MyVect rayOrigin, MyVect ray, MyVect planeOrigin, MyVect normal) {
        MyVect v = planeOrigin.sub(rayOrigin);
        double d = ray.dot(normal);
        if (d == 0) {
            return planeOrigin;
        }
        else {
            double l = v.dot(normal) / d;
            if (l < 0) {
                return null;
            }
            else {
                return rayOrigin.add(ray.mul(l));
            }
        }
    }
}
