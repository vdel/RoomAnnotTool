/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pose;

import com.sun.j3d.utils.geometry.Cylinder;
import com.common.J3DHelper;
import com.common.J3DHelper.SimpleFrame;
import com.common.MyVect;
import java.util.LinkedList;
import java.util.List;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;

/**
 *
 * @author vdelaitr
 */
public class Pose3DVisu extends Pose3D {
    
    private class VisuSegment {
        Segment s;
        Joint j1, j2;       
        Color3f color;
        TransformGroup tg = J3DHelper.newTransformGroup();              
        
        VisuSegment(BranchGroup root, SegmentType st) {
            root.addChild(tg);
            this.s = findSegment(st);
            this.j1 = s.from;
            this.j2 = s.to;           
            switch (st) {
                case LOWER_TRUNK:
                case MIDDLE_TRUNK:
                case UPPER_TRUNK:
                case L_SHOULDER:
                case R_SHOULDER:
                case L_HIP:
                case R_HIP:
                    this.color = new Color3f(255, 255, 0);
                    break;
                case HEAD:
                    this.color = new Color3f(0, 255, 0);
                    break;
                case L_UPPERARM:
                case L_FOREARM:
                    this.color = new Color3f(255, 0, 255);
                    break;
                case R_UPPERARM:
                case R_FOREARM:
                    this.color = new Color3f(0, 255, 255);
                    break;
                case L_THIGH:
                case L_SHANK:
                    this.color = new Color3f(0, 0, 255);
                    break;        
                case R_THIGH:
                case R_SHANK:
                    this.color = new Color3f(255, 0, 0);
                    break;
            }
        }
                
        TransformGroup update(double radius) {
            MyVect p1 = new MyVect(j1.pos.x, j1.pos.y, j1.pos.z);
            MyVect p2 = new MyVect(j2.pos.x, j2.pos.y, j2.pos.z);
            MyVect v = p1.sub(p2);
            MyVect center = p1.add(p2).mul(0.5);
            
            Cylinder c = new Cylinder((float)radius, (float)v.norm(), 
                                      J3DHelper.getDefaultAppearance(color));

            double vxy = Math.sqrt(v.x * v.x + v.y * v.y);

            Transform3D t = new Transform3D();
            Transform3D r = new Transform3D();
            t.setTranslation(center);
            double az = Math.atan2(v.y, v.x) - Math.PI / 2;
            r.rotZ(az);
            t.mul(r);
            double ax = Math.atan2(v.z, vxy);
            r.rotX(ax);
            t.mul(r);                
            
            tg.setTransform(t);
            tg.removeAllChildren();
            BranchGroup g = J3DHelper.newBranchGroup();
            g.addChild(c);
            tg.addChild(g);

            return tg;
        }
    }
    
    //------------------------------------------------------------------------//
    
    BranchGroup bg = J3DHelper.newBranchGroup();
    List<VisuSegment> visuseg = new LinkedList<VisuSegment>();
    double radiusCoeff = 40;
    
    public Pose3DVisu(double[] pose) {
        this();
        loadFrom3D(pose);
        update();
    }
    
    public Pose3DVisu() {        
        super();
                        
        for (Segment s : segments) {
            if (s.st != SegmentType.TRUNK) {
                visuseg.add(new VisuSegment(bg, s.st));    
            }
        }
    }
    
    public final void update() {             
        for (VisuSegment s : visuseg) {
            s.update(scale.getVal() * radiusCoeff);
        }
    }
    
    public BranchGroup getBranchGroup() {
        return bg;
    }
    
    public SimpleFrame showPose() {
        SimpleFrame frame = new SimpleFrame(300, 300);
        showPose(frame);
        return frame;
    }
    
    public SimpleFrame showPose(SimpleFrame frame) {
        frame.setInvisible();
        
        update();
        
        MyVect center = new MyVect();
        MyVect min = null;
        MyVect max = null;
        for (Joint j : joints) {
            if (min == null) {
                min = new MyVect(j.pos);
                max = new MyVect(j.pos);
            }
            center = center.add(j.pos);
            min.x = Math.min(min.x, j.pos.x);
            min.y = Math.min(min.y, j.pos.y);
            min.z = Math.min(min.z, j.pos.z);
            max.x = Math.max(max.x, j.pos.x);
            max.y = Math.max(max.y, j.pos.y);
            max.z = Math.max(max.z, j.pos.z);
        }
        center = center.mul(1. / joints.size());
        double width = max.x - min.x;
        double height = max.y - min.y;
        double sc = 1.5 / Math.max(width, height);
   
        Transform3D t = new Transform3D();
        t.set(sc, center.mul(-sc));
        TransformGroup tg = J3DHelper.newTransformGroup(t);
        tg.addChild(getBranchGroup());
        frame.getCanvas().getTransformGroup().addChild(tg);
        
        frame.setVisible();
        
        return frame;
    }
 
    /*
    public static void main(String[] args) {
        double[] pose1 = {-149.8242, -251.8750, 0, // hidden
                          -191.8959, -191.8750, 0, // hidden
                          -145.9995, -179.8750, 1,
                          -64.4059,  -179.8750, 1,
                          -111.5772, -220.8750, 1,
                          -167.6728, -280.8750, 0, // hidden
                          -196.9955, -207.8750, 0, // hidden
                          -199.5453, -158.8750, 0, // hidden
                          -168.9477, -115.8750, 1,
                          -91.1788,  -110.8750, 1,
                          -80.9796,  -178.8750, 1,
                          -153.6489, -223.8750, 1,
                          -128.1509, -128.8750, 1,
                          -143.4497, -73.8750,  1}; 
        
        double[] pose2 = {109.4079, -49.1078, 1, // hidden
                          147.0275,  -45.1439, 1,
                          114.5089,  -40.6137, 1,
                          101.1189,  -26.4568, 1, // hidden
                          135.5503,  -27.0231, 1,
                          100.4813,  -25.3242, 1,
                          142.5642,  -31.5533, 1,
                          120.2475,  -31.5533, 1,
                          133.6375,   -7.2034, 1,
                          120.8851,    5.8209, 1,
                          114.5089,  -15.6976, 1, // hidden
                          132.3622,  -21.3603, 1,
                          131.0870,    0.1581, 1,
                          155.9542,   10.3511, 1};
                          
        double[] pose3 = {-71.6760,  -32.6858, 1,
                        -80.6027,      5.2546, 0,
                        -113.7589,    10.3511, 1,
                        -89.5294,     20.5440, 0,
                        -58.9236,     18.8452, 1,
                        -47.4465,    -13.9987, 0,
                        -77.4146,     25.6405, 0,
                        -97.8184,     14.8813, 1,
                        -106.1075,    44.3276, 1,
                        -79.3274,     46.5927, 1,
                        -65.2998,     25.0742, 0,
                        -90.8046,     10.3511, 0,
                        -90.1670,     47.1590, 1,
                        -89.5294,     72.0751, 1};

        double[] pose = pose2;
         
        Pose3DVisu p3Dref = new Pose3DVisu();
        p3Dref.loadFrom2D(pose);                
        p3Dref.showPose();
        
        Pose3DVisu p3D = new Pose3DVisu();
        p3D.loadFrom2D(pose);
              
        p3D.lift3D(new MyVect(0, 1, 0));
        p3D.showPose();
    }
    */    
}
