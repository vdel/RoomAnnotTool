/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.gui;

import com.annot.gui.MyBox3D.Face3D;
import com.annot.room.ObjectInstance.NoSuchVariableException;
import com.annot.room.ObjectManager.FaceType;
import com.common.MyVect;
import java.util.HashMap;

/**
 *
 * @author vdelaitr
 */
public class ConstrainedResize extends LineConstrainedMotion {
    HashMap<String, Double> coeff;
    double coeffTranslate;
    double nCoeff;
    HashMap<String, Double> newVars;

    ConstrainedResize(ObjectInstance3D o, Face3D face, boolean translate) {
        this(o, face, null, translate);
    }

    ConstrainedResize(ObjectInstance3D o, Face3D face, MyVect origin, boolean translate) {        
        super(o, origin, face.getNormal());
        coeff = face.getVariables();

        nCoeff = 0;
        if (coeff.isEmpty()) {
            coeffTranslate = 1;            
        }
        else {
            coeffTranslate = 0;           
            HashMap<String, Double> newCoeff = new HashMap<String, Double>();
            for (String var : coeff.keySet()) {
                try {
                    double c = coeff.get(var).doubleValue();
                    double ct = translate ? object.getCoeff(var) : 0;
                    double cr = 1 - ct;
                    coeffTranslate += ct;
                    c = Math.abs(cr / c); // normal direction already accounts for sign                    
                    if (c > 1e-5) {
                        newCoeff.put(var, c);
                        nCoeff++;                        
                    }
                }
                catch (NoSuchVariableException e) {
                }
            }
            coeffTranslate = Math.abs(coeffTranslate); // normal direction already accounts for sign
            if (coeffTranslate < 1e-5) {
                coeffTranslate = 0;
            }
            coeff = newCoeff;            
        }
    }

    ConstrainedResize(ObjectInstance3D o, MyBox3D b, FaceType ft, MyVect rayOrigin, MyVect ray) {
        this(o, b.getFace(ft), b.getIntersection(ft, rayOrigin, ray), true);
    }

    @Override
    void update(MyVect rayOrigin, MyVect ray) {
        if (normal != null && coeff != null) {
            MyVect p = computeRayLineInstersection(rayOrigin, ray, origin, normal);
            if (p != null) {
                updatesize(p.sub(origin).dot(normal), true);
            }
        }
    }

    boolean updatesize(double def, boolean check) {
        boolean ok = true;        
        newVars = new HashMap<String, Double>(originalVars);

        for (String var : coeff.keySet()) {
            double c = coeff.get(var).doubleValue(); 
            double d = newVars.get(var).doubleValue();
            double v = d + def * c / nCoeff;
            if (v <= 0) {
                ok = false;
                break;
            }
            newVars.put(var, v);
        }

        if (ok && (!check || object.checkConstraints(newVars))) {
            MyVect motion = normal.mul(def * coeffTranslate); // normal direction already accounts for sign
            if ((!check || checkBoundConstraint(newVars, originalPos, motion))) {
                object.setPosition(originalPos.add(motion));
                object.setVariables(newVars);
            }
        }
        return ok;
    }
}
