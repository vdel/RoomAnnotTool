/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.annot.gui;

import com.annot.room.ObjectManager;
import com.annot.room.ObjectManager.ObjectException;
import com.annot.room.Room;
import com.common.MyVect;
import com.sun.j3d.utils.geometry.Primitive;
import java.io.File;
import java.util.LinkedList;
import javax.media.j3d.Transform3D;

/**
 *
 * @author vdelaitr
 */
public class Room3D extends Room {
    public Room3D(File file) throws ObjectException {
        super(file);
    }

    public Room3D(File file, int f) throws ObjectException {
        super(file, f);
    }
    
    @Override
    public void build() {
        super.build();
        
        ObjectInstance3D[] walls3D = new ObjectInstance3D[4];
        ObjectInstance3D floor3D   = (ObjectInstance3D)floor;
        ObjectInstance3D ceiling3D = (ObjectInstance3D)ceiling;
        walls3D[0] = (ObjectInstance3D)walls[0];
        walls3D[1] = (ObjectInstance3D)walls[1];
        walls3D[2] = (ObjectInstance3D)walls[2];
        walls3D[3] = (ObjectInstance3D)walls[3];
         
        LinkedList<ConstrainedResize> resizeBinds;        
        resizeBinds = new LinkedList<ConstrainedResize>();
        resizeBinds.add(new ConstrainedResize(floor3D, floor3D.getPart(0).getFace(ObjectManager.FaceType.LEFT), true));
        resizeBinds.add(new ConstrainedResize(ceiling3D, ceiling3D.getPart(0).getFace(ObjectManager.FaceType.LEFT), false));
        resizeBinds.add(new ConstrainedResize(walls3D[1], walls3D[1].getPart(0).getFace(ObjectManager.FaceType.LEFT), false));
        resizeBinds.add(new ConstrainedResize(walls3D[2], walls3D[2].getPart(0).getFace(ObjectManager.FaceType.BACK), false));
        resizeBinds.add(new ConstrainedResize(walls3D[3], walls3D[3].getPart(0).getFace(ObjectManager.FaceType.RIGHT), false));
        walls3D[0].setResizeBinds(resizeBinds);

        resizeBinds = new LinkedList<ConstrainedResize>();
        resizeBinds.add(new ConstrainedResize(floor3D, floor3D.getPart(0).getFace(ObjectManager.FaceType.BACK), true));
        resizeBinds.add(new ConstrainedResize(ceiling3D, ceiling3D.getPart(0).getFace(ObjectManager.FaceType.BACK), false));
        resizeBinds.add(new ConstrainedResize(walls3D[0], walls3D[0].getPart(0).getFace(ObjectManager.FaceType.RIGHT), false));
        resizeBinds.add(new ConstrainedResize(walls3D[2], walls3D[2].getPart(0).getFace(ObjectManager.FaceType.LEFT), false));
        resizeBinds.add(new ConstrainedResize(walls3D[3], walls3D[3].getPart(0).getFace(ObjectManager.FaceType.BACK), false));
        walls3D[1].setResizeBinds(resizeBinds);

        resizeBinds = new LinkedList<ConstrainedResize>();
        resizeBinds.add(new ConstrainedResize(floor3D, floor3D.getPart(0).getFace(ObjectManager.FaceType.RIGHT), true));
        resizeBinds.add(new ConstrainedResize(ceiling3D, ceiling3D.getPart(0).getFace(ObjectManager.FaceType.RIGHT), false));
        resizeBinds.add(new ConstrainedResize(walls3D[0], walls3D[0].getPart(0).getFace(ObjectManager.FaceType.BACK), false));
        resizeBinds.add(new ConstrainedResize(walls3D[1], walls3D[1].getPart(0).getFace(ObjectManager.FaceType.RIGHT), false));
        resizeBinds.add(new ConstrainedResize(walls3D[3], walls3D[3].getPart(0).getFace(ObjectManager.FaceType.LEFT), false));
        walls3D[2].setResizeBinds(resizeBinds);

        resizeBinds = new LinkedList<ConstrainedResize>();
        resizeBinds.add(new ConstrainedResize(floor3D, floor3D.getPart(0).getFace(ObjectManager.FaceType.FRONT), true));
        resizeBinds.add(new ConstrainedResize(ceiling3D, ceiling3D.getPart(0).getFace(ObjectManager.FaceType.FRONT), false));
        resizeBinds.add(new ConstrainedResize(walls3D[0], walls3D[0].getPart(0).getFace(ObjectManager.FaceType.LEFT), false));
        resizeBinds.add(new ConstrainedResize(walls3D[1], walls3D[1].getPart(0).getFace(ObjectManager.FaceType.BACK), false));
        resizeBinds.add(new ConstrainedResize(walls3D[2], walls3D[2].getPart(0).getFace(ObjectManager.FaceType.RIGHT), false));
        walls3D[3].setResizeBinds(resizeBinds);
    }
    
    @Override
    public ObjectInstance3D newObject(String obj) throws NoSuchObjectException {
        if (objectLibrary.containsKey(obj)) {
            return new ObjectInstance3D(objectLibrary.get(obj));
        }
        else {
            throw new NoSuchObjectException();
        }
    }
    
    @Override
    public ObjectInstance3D getFloor() {
        return (ObjectInstance3D)floor;
    }

    @Override
    public ObjectInstance3D getCeiling() {
        return (ObjectInstance3D)ceiling;
    }

    @Override
    public ObjectInstance3D getWall(int i) {
        return (ObjectInstance3D)walls[i];
    }
    
    @Override
    public void clear() {
        super.clear();
        Primitive.clearGeometryCache();
    }
    
    @Override
    public void refresh() {
        super.refresh();
        if (floor != null && 
            ((ObjectInstance3D)floor).getParentTransformGroup() != null) {
            Transform3D t = new Transform3D();
            t.setTranslation(new MyVect(-params.depth / 2, 
                                        -params.width / 2, 0));
            ((ObjectInstance3D)floor).getParentTransformGroup().setTransform(t);
        }
    }
}
