/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.gui;

import com.annot.gui.MyBox3D.Face3D;
import com.annot.gui.MyCanvas3D.MyPickResult;
import com.annot.room.ObjectInstance;
import com.annot.room.ObjectManager.FaceType;
import com.annot.room.Room.NoSuchObjectException;
import com.common.BBox2D;
import com.common.J3DHelper;
import com.common.MyMatrix;
import com.common.MyVect;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Enumeration;
import javax.media.j3d.Behavior;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedFrames;

/**
 *
 * @author vdelaitr
 */
public class MyBehavior extends Behavior implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    Room3D room;
    WakeupCondition condition;
    MyCanvas3D canvas;
    TransformGroup transformGroup;
    MyVect translation;
    MyVect rotation;
    boolean[] pressed;
    Point clicked;
    Point lastMousePos;
    double step;
    boolean dragged;
    boolean potentialDeselect;
    MyBox3D selectedBox;
    FaceType selectedFace;
    ConstrainedMotion motion;
    ConstrainedResize resize;
    private boolean locked;
    private boolean controlPressed;

    MyBehavior(Room3D r, MyCanvas3D c, TransformGroup tg) {
        room = r;
        canvas = c;
        transformGroup = tg;        

        translation = new MyVect();
        rotation = new MyVect();
        clicked = new Point();
        lastMousePos = new Point();
        pressed = new boolean[3];
        pressed[0] = false;
        pressed[1] = false;
        pressed[2] = false;
        step = 0.33;
        selectedBox = null;
        dragged = false;
        potentialDeselect = false;
        motion = null;
        resize = null;
        locked = false;
        controlPressed = false;

        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
        canvas.addKeyListener(this);
        
        condition = new WakeupOnElapsedFrames(0, true);   
    }

    void clear() {
        this.setEnable(false);        
        canvas.removeMouseListener(this);
        canvas.removeMouseMotionListener(this);
        canvas.removeMouseWheelListener(this);
        canvas.removeKeyListener(this);
        room = null;
        canvas = null;
        transformGroup = null;
    }

    @Override
    public void initialize() {        
        wakeupOn(condition);
    }

    @Override
    public void processStimulus(Enumeration criteria) {
        if (this.getEnable()) {
            applyTransform();
        }
        wakeupOn(condition);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                translation = translation.add(getCameraY().mul(-step));
                break;
            case KeyEvent.VK_DOWN:
                translation = translation.add(getCameraY().mul(step));
                break;
            case KeyEvent.VK_LEFT:
                translation = translation.add(getCameraX().mul(step));
                break;
            case KeyEvent.VK_RIGHT:
                translation = translation.add(getCameraX().mul(-step));
                break;    
            case KeyEvent.VK_DELETE:
                if (!room.readOnly()) {
                    removeSelectedObject();
                }
                break;                
            case KeyEvent.VK_CONTROL:
                controlPressed = true;
                break;
            case KeyEvent.VK_ALT:
                if (!pressed[1]) {
                    mousePressed(MouseEvent.BUTTON2, lastMousePos);
                }
                break;
            case KeyEvent.VK_SHIFT:                
                if (!pressed[2]) {
                    mousePressed(MouseEvent.BUTTON3, lastMousePos);
                }
                break;   
            case KeyEvent.VK_SPACE:
                 canvas.setDefaultView(room.getParams());
                break; 
            case KeyEvent.VK_C:
                canvas.togglePointCloud();
                break;
            default:
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                controlPressed = false;
                break;                
            case KeyEvent.VK_ALT:
                mouseReleased(MouseEvent.BUTTON2);
                break;
            case KeyEvent.VK_SHIFT:
                mouseReleased(MouseEvent.BUTTON3);
                break;                         
            default:
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {        
    }

    @Override
    public void mouseClicked(MouseEvent e) {    
    }

    @Override
    public void mouseDragged(MouseEvent e) {        
        dragged = true;

        if(pressed[0]) {
            if (!room.readOnly() && selectedBox != null && 
                    !selectedBox.getParentObject().isStatic()) {
                ObjectInstance3D o = selectedBox.getParentObject();
                if (motion == null) {
                    if (!o.isPrivate() && controlPressed) {
                        try {
                            ObjectInstance3D ocopy = room.newObject(o.getName());
                            ocopy.setVariables(o.getVariables());
                            ocopy.setPosition(o.getPosition());
                            ocopy.setAngle(o.getAngle());
                            ocopy.attachTo(o.getAttachedFace().getParentBox(), 
                                           o.getAttachedFace().getType());
                            select(ocopy.getPart(selectedBox.getBoxID()));
                        }
                        catch (NoSuchObjectException e2) {
                            throw new RuntimeException(e2.getMessage());
                        }
                    }
                    if (o.isPrivate() && o.isWall()) {
                        lockFade();
                    }
                    motion = selectedBox.getMotion(selectedFace, getCameraPos(), canvas.getRay(e.getX(), e.getY()));
                }
                else {        
                    motion.update(getCameraPos(), canvas.getRay(e.getX(), e.getY()));
                    canvas.setModified(o);
                }
            }
        }

        if(pressed[1]) {
            rotation.z += (double)(e.getPoint().x - clicked.x) / (double)canvas.getWidth() * (float)Math.PI;
            rotation.y -= (double)(e.getPoint().y - clicked.y) / (double)canvas.getHeight() * (float)Math.PI / 2.f;
            clicked.x = e.getPoint().x;
            clicked.y = e.getPoint().y;
        }
        
        if(pressed[2]) {
            if (!room.readOnly() && selectedBox != null && resize != null) {
                resize.update(getCameraPos(), canvas.getRay(e.getX(), e.getY()));
                canvas.setModified(selectedBox.getParentObject());                
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed(e.getButton(), e.getPoint());
    }
    
    public void mousePressed(int button, Point p) {
        if(button == MouseEvent.BUTTON2) {
            pressed[1] = true;
            clicked.x = p.x;
            clicked.y = p.y;
        }
        
        if(button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3) {                      
            MyPickResult r = canvas.pick(p.x, p.y);
            if (r != null) {
                selectedFace = r.box.getFaceType(r.shape);
                if (r.box != selectedBox && !r.box.isSelectionBox) {
                    deselect();
                    select(r.box);
                    potentialDeselect = false;
                }
                else {
                    potentialDeselect = true;
                }
            }
            else {
                deselect();
            }
            
            if(button == MouseEvent.BUTTON1) {
                pressed[0] = true;
                pressed[2] = false;                
            }
            if(button == MouseEvent.BUTTON3) {
                pressed[2] = true;
                pressed[0] = false;

                if (!room.readOnly() && selectedBox != null && 
                        !selectedBox.getParentObject().isPrivate()) {
                   resize = new ConstrainedResize(selectedBox.getParentObject(), 
                                                  selectedBox, selectedFace,
                                                  getCameraPos(), canvas.getRay(p.x, p.y));
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseReleased(e.getButton());
    }
    
    public void mouseReleased(int button) {    
        if(button == MouseEvent.BUTTON1) {
            if (!room.readOnly() && 
                    selectedBox != null && 
                    selectedBox.getParentObject().isPrivate() && 
                    selectedBox.getParentObject().isWall()) {
                MyVect defaultFloorPos = new MyVect(room.getParams().depth / 2, 
                                                    room.getParams().width / 2, 0);
                MyVect floorPosOffset = room.getFloor().getPosition().sub(defaultFloorPos);
                
                canvas.stopRenderer();
                
                for (ObjectInstance o : room.getFloor().getPart(0).getFace(FaceType.TOP).getChildren()) {
                    if (!o.isPrivate()) {
                        o.translate(floorPosOffset.mul(-1));
                    }
                }                

                unlockFade();                
                translation = translation.add(getR().mul(floorPosOffset));

                ObjectInstance3D o = selectedBox.getParentObject();
                MyMatrix rot = o.getAttachedRotation();
                rot.mul(o.getRotationMatrix());
                MyVect n = rot.mul(selectedBox.getFace(selectedFace).getNormal());

                if (Math.abs(n.x) > Math.abs(n.y)) {
                    room.getParams().depth += n.dot(floorPosOffset) * -2; // 50/50 translation and resize
                }
                else {
                    room.getParams().width += n.dot(floorPosOffset) * -2; // 50/50 translation and resize
                }
                System.out.format("Room size (depth, width, height) = (%.2fm, %.2fm, %.2fm)\n", room.getParams().depth, room.getParams().width, room.getParams().height);

                defaultFloorPos = new MyVect(room.getParams().depth / 2, 
                                             room.getParams().width / 2, 0);
                room.getFloor().setPosition(defaultFloorPos);
                Transform3D t = new Transform3D();
                t.setTranslation(defaultFloorPos.mul(-1));
                room.getFloor().getParentTransformGroup().setTransform(t);
                
                if (n.x > 0.5 || n.y > 0.5) {
                    room.getParams().cameraPosition = room.getParams().cameraPosition.add(floorPosOffset.mul(-2)); // 50/50 translation and resize
                    room.getParams().t = room.getParams().R.mul(room.getParams().cameraPosition).mul(-1);
                }
                
                canvas.startRenderer();
            }
            pressed[0] = false;
            motion = null;
            if (selectedBox != null && potentialDeselect && !dragged) {
                deselect();
            }
        }
        if(button == MouseEvent.BUTTON2) {
            pressed[1] = false;            
        }
        if(button == MouseEvent.BUTTON3) {
            pressed[2] = false;
            resize = null;
            ObjectInstance3D parent = selectedBox.getParentObject();
            if (!room.readOnly() && selectedBox != null && 
                    potentialDeselect && !dragged && !parent.isPrivate() && 
                    parent.getAttachedFace().getType() == FaceType.TOP) {                
                parent.rotate();
                canvas.setModified(selectedBox.getParentObject());
            }
        }
        dragged = false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        lastMousePos = e.getPoint();
        if (pressed[0] || pressed[1] || pressed[2]) {
            mouseDragged(e);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {       
        translation = translation.add(getCameraZ().mul(e.getWheelRotation() * step));
    }

    void select(MyBox3D b) {
        deselect();
        selectedBox = b;        
        selectedBox.getParentObject().select();
        canvas.objectSelected(selectedBox.getParentObject());
    }

    void deselect() {
        if (selectedBox != null) {
            selectedBox.getParentObject().deselect();
            canvas.objectDeselected(selectedBox.getParentObject());
            selectedBox = null;
            motion = null;
            resize = null;            
        }
    }

    void removeSelectedObject() {
        if (selectedBox != null && 
            !selectedBox.getParentObject().isPrivate()) {
            ObjectInstance3D o = selectedBox.getParentObject();
            deselect();
            o.detach();
            o.clear();
        }
    }

    private void applyTransform() {
        Transform3D transform = new Transform3D();
        Transform3D r = new Transform3D();
        r.set(getR());       
        transform.setTranslation(translation);
        transform.mul(r);
        transformGroup.setTransform(transform);

        if (!isFadeLocked()) {
            checkFade(room.getCeiling(), room.getCeiling().getPart(0).getFace(FaceType.BOTTOM));
            for (int i = 0; i < 4; i++) {
                checkFade(room.getWall(i), room.getWall(i).getPart(0).getFace(FaceType.FRONT));
            }
        }
    }

    // Check if 'face' is visible or not (hide the object in this case)
    void checkFade(ObjectInstance3D object, Face3D face) {
        Transform3D transform = object.getTransfrom3DFromOrigin();
        Transform3D t = new Transform3D();
        face.getParentBox().getTransformGroup().getTransform(t);
        transform.mul(t);
        
        MyMatrix objRot = new MyMatrix();
        MyVect objTrans = new MyVect();
        J3DHelper.extractRotTrans(transform, objRot, objTrans, null);

        double nNorm = Math.abs(face.getNormal().dot(face.getParentBox().getDims()) / 2);
        MyVect normal = objRot.mul(face.getNormal());
        objTrans = objTrans.add(normal.mul(nNorm));
        MyVect obj2Cam = getCameraPos().sub(objTrans);
        
        if (obj2Cam.dot(normal) <= 1e-3) {
            if (!object.isLocked()) {
                object.lock();
                object.branchGroupDetach();
            }
        }
        else {            
            if (!object.isAttached()) {
                object.branchGroupAttach(object.getParentTransformGroup());
                object.unlock();
            }
        }        
    }
    
    void setRotation(double x, double y, double z) {
        setRotation(new MyVect(x, y, z));
    }

    void setTranslation(double x, double y, double z) {
        setTranslation(new MyVect(x, y, z));
    }

    void setRotation(MyVect r) {
        rotation = new MyVect(r);
        applyTransform();
    }

    void setTranslation(MyVect t) {
        translation = new MyVect(t);
        applyTransform();
    }

    public void setCameraPos(MyVect pos) {
        MyVect offset = new MyVect(room.getParams().depth / 2, room.getParams().width / 2, 0);
        setTranslation(getR().mul(offset.sub(pos)));
        applyTransform();
    }

    MyVect getCameraPos() {
        MyVect offset = new MyVect(room.getParams().depth / 2, room.getParams().width / 2, 0);
        return getInvR().mul(translation).mul(-1).add(offset);
    }

    MyMatrix getR() {
        MyMatrix r = new MyMatrix();
        r.mul(MyMatrix.rotationY(rotation.y), MyMatrix.rotationZ(rotation.z));
        r.mul(MyMatrix.rotationZ(rotation.x), r);
        return r;
    }

    MyMatrix getInvR() {
        MyMatrix r = new MyMatrix();
        r.mul(MyMatrix.rotationY(-rotation.y), MyMatrix.rotationZ(-rotation.x));
        r.mul(MyMatrix.rotationZ(-rotation.z), r);
        return r;
    }

    MyVect getCameraX() {
        return new MyVect(1, 0, 0);
    }

    MyVect getCameraY() {
        return new MyVect(0, 1, 0);
    }

    MyVect getCameraZ() {
        return new MyVect(0, 0, 1);
    }

    private void lockFade() {
        locked = true;
    }

    private void unlockFade() {
        locked = false;
    }

    private boolean isFadeLocked() {
        return locked;
    }
}