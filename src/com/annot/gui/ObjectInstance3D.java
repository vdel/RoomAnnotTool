/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.annot.gui;

import com.annot.gui.MyBox3D.Face3D;
import com.annot.room.MyBox;
import com.annot.room.ObjectInstance;
import com.annot.room.ObjectManager;
import com.annot.room.Room;
import com.common.J3DHelper;
import com.common.MyVect;
import java.util.LinkedList;
import java.util.List;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

/**
 *
 * @author vdelaitr
 */
public class ObjectInstance3D extends ObjectInstance {
    private BranchGroup bg;
    private TransformGroup tg;    
    private TransformGroup parentTG;
    
    private boolean selected;
    private boolean locked;
    private boolean isAttached;
    
    private List<ConstrainedResize> resizeBinds;
    
    ObjectInstance3D(ObjectManager om) {
        super(om);
        
        isAttached = false;
        parentTG = null;
        
        bg = J3DHelper.newBranchGroup();
        tg = J3DHelper.newTransformGroup();
        bg.addChild(tg);
        
        parts = new MyBox3D[parts.length];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new MyBox3D(this, om.getPart(i));
        }
        
        selected = false;
        locked = false;
        
        resizeBinds = new LinkedList<ConstrainedResize>();
    }
    
    @Override
    public MyBox3D getPart(int i) {
        return (MyBox3D)parts[i];
    }
    
    @Override
    public void positionUpdated() {
        Transform3D t = new Transform3D();
        Transform3D r = new Transform3D();
        t.setTranslation(position);
        r.rotZ(angle);
        t.mul(r);
        tg.setTransform(t);
    }
    
    void select() {
        if (!selected) {
            selected = true;
            for (int i = 0; i < parts.length; i++) {
                getPart(i).select();
            }
        }
    }

    void deselect() {
        if (selected) {
            selected = false;
            for (int i = 0; i < parts.length; i++) {
                getPart(i).deselect();
            }
        }
    }
       
    @Override
    public void clear() {       
       deselect();
       branchGroupDetach();        
       super.clear();
    }    
    
    @Override
    public boolean attachTo(MyBox p, ObjectManager.FaceType ft) {
        if (super.attachTo(p, ft)) {            
            branchGroupAttach(((MyBox3D)p).getFace(ft).getTransformGroup());
            return true;
        }
        else {
            return false;
        }        
    }

    Transform3D getTransform(MyVect pos) {
        Transform3D t = new Transform3D();
        Transform3D transform = new Transform3D();
        transform.setTranslation(pos);
        t.set(getRotationMatrix());
        transform.mul(t);
        return transform;
    }
    
    void lock() {
        locked = true;
    }

    void unlock() {
        locked = false;
    }

    boolean isLocked() {
        return locked;
    }    
    
    BranchGroup getBranchGroup() {
        return bg;
    }
    
    boolean isAttached() {
        return isAttached;
    }        

    synchronized void branchGroupDetach() {
        if (isAttached) {
            bg.detach();
            isAttached = false;
        }
    }

    synchronized void branchGroupAttach(TransformGroup tg) {
        if (!isAttached) {
            parentTG = tg;
            tg.addChild(bg);
            isAttached = true;
        }
    }

    TransformGroup getParentTransformGroup() {
        return parentTG;
    }
    
    TransformGroup getTransformGroup() {
        return tg;
    } 
    
    ConstrainedMotion getMotion(MyVect motionOrigin, Face3D faceOrigin) {
        if (isPrivate() && isWall()) {
            return new LineConstrainedMotion(this, motionOrigin, faceOrigin.getNormal().mul(-1)); // -1 because we want to agree with floor and ceiling normal (see resizeBinds)
        }
        else {
            return new PlaneConstrainedMotion(this, motionOrigin);
        }
    }      
    
    void setResizeBinds(List<ConstrainedResize> rb) {
        resizeBinds = rb;
    }

    List<ConstrainedResize> getResizeBinds() {
        return resizeBinds;
    }   
    
    void drawOnPanel(MyPanel panel, Room room) {        
        Transform3D transform = getTransfrom3DFromOrigin();
        
        panel.clearLines();
        for (int i = 0; i < parts.length; i++) {
            getPart(i).drawOnPanel(panel, room.getParams(), transform);
        }
        panel.repaint();
    }   
    
    Transform3D getTransfrom3DFromOrigin() {
        Transform fromRoot = getTransfromFromOrigin();
        
        Transform3D trans = new Transform3D();
        trans.setTranslation(fromRoot.transl);
        
        Transform3D rot = new Transform3D();
        rot.rotZ(fromRoot.angleZ);
        
        Transform3D transform = new Transform3D();
        transform.mul(trans, rot);
        return transform;
    }
}
