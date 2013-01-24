/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.annot.gui;

import com.annot.room.MyBox;
import com.annot.room.MyBox.Face;
import com.annot.room.ObjectManager;
import com.annot.room.ObjectManager.FaceType;
import com.annot.room.ObjectManager.Node;
import com.annot.room.ObjectManager.Part;
import com.annot.room.RoomParameters;
import com.common.J3DHelper;
import com.common.MyMatrix;
import com.common.MyVect;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Primitive;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;

/**
 *
 * @author vdelaitr
 */
public class MyBox3D extends MyBox {
    
    public static class J3DBox extends Box {
        MyBox3D me;
        
        public J3DBox(MyBox3D me) {
             super(0.5f, 0.5f, 0.5f,                
              Primitive.GENERATE_NORMALS +
              Primitive.GENERATE_TEXTURE_COORDS +
              Primitive.ENABLE_GEOMETRY_PICKING +
              Primitive.ENABLE_APPEARANCE_MODIFY,
              J3DHelper.getDefaultAppearance(me.getColor()), 1);
            setCapability(ALLOW_PICKABLE_READ);
            this.me = me;
        }        
        
        protected J3DBox(MyBox3D me, float size, int flags, Appearance a) {
             super(size, size, size, flags, a);
             setCapability(ALLOW_PICKABLE_READ);
             this.me = me;
        }
    }
     
    public static class MySelectionBox extends J3DBox {
        BranchGroup bg;
        
        MySelectionBox(MyBox3D me) {
            super(me, 0.51f, 
                  Primitive.GENERATE_NORMALS +
                  Primitive.GENERATE_TEXTURE_COORDS +
                  Primitive.ENABLE_GEOMETRY_PICKING +
                  Primitive.ENABLE_APPEARANCE_MODIFY, 
                  getDefaultAppearance());
            bg = J3DHelper.newBranchGroup();
            bg.addChild(this);
            me.scale.addChild(bg);
        }
        
        void detach() {
            bg.detach();
        }
        
        static private Appearance getDefaultAppearance() {
            Appearance ap = new Appearance();

            Color3f black = new Color3f(0f, 0f, 0f);
            Color3f white = new Color3f(10f, 10f, 10f);

            ap.setMaterial(new Material(white, black, white, black, 70));

            PolygonAttributes pa = new PolygonAttributes();
            pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
            ap.setPolygonAttributes(pa);

            LineAttributes la = new LineAttributes();
            la.setLineWidth(1);
            ap.setLineAttributes(la);

            /*
            TransparencyAttributes ta = new TransparencyAttributes();
            ta.setTransparencyMode (ta.BLENDED);
            ta.setTransparency(0f);
            ap.setTransparencyAttributes(ta);
             */

            return ap;
        }
    }
    
    public static class Face3D extends Face {
        private TransformGroup tg;
        
        Face3D(MyBox3D p, Node fn, FaceType ft) {            
            super(p, fn, ft);
            
            tg = J3DHelper.newTransformGroup();
            p.tg.addChild(tg);
        }
        
        void variableUpdated() {
            double d = Math.abs(parent.getDims().dot(getNormal()) / 2);
            Transform3D t = new Transform3D();
            t.setTranslation(getNormal().mul(d));
            tg.setTransform(t);            
        }
        
        TransformGroup getTransformGroup() {
            return tg;
        }
        
        @Override
        public MyBox3D getParentBox() {
            return (MyBox3D)parent;
        }
        
        void drawOnPanel(MyPanel panel, MyMatrix camProj, MyMatrix camRot, MyVect camTrans, MyVect dims, Color color) {
            MyVect vx = getX().mul(Math.abs(dims.dot(getX()) / 2));
            MyVect vy = getY().mul(Math.abs(dims.dot(getY()) / 2));
            MyVect vz = getNormal().mul(Math.abs(dims.dot(getNormal()) / 2));

            double znear = -0.001;
            MyVect[] v = new MyVect[4];
            v[0] = camRot.mul(vz.sub(vx).sub(vy)).add(camTrans);
            v[1] = camRot.mul(vz.sub(vx).add(vy)).add(camTrans);
            v[2] = camRot.mul(vz.add(vx).add(vy)).add(camTrans);
            v[3] = camRot.mul(vz.add(vx).sub(vy)).add(camTrans);

            for (int i = 0; i < 4; i++) {
                MyVect p1 = new MyVect(v[i]);
                MyVect p2 = new MyVect(v[(i + 1) % 4]);

                if (p1.z > znear && p2.z > znear) {
                    continue;
                }
                else if (p1.z > znear) {
                    MyVect v21 = p1.sub(p2);
                    double l = (znear - p2.z) / v21.z;
                    p1 = p2.add(v21.mul(l));
                }
                else if (p2.z > znear) {
                    MyVect v12 = p2.sub(p1);
                    double l = (znear - p1.z) / v12.z;
                    p2 = p1.add(v12.mul(l));
                }

                p1 = camProj.mul(p1);
                p2 = camProj.mul(p2);

                panel.addLine(panel.image.directToImageCoord(p1).toPoint(), 
                              panel.image.directToImageCoord(p2).toPoint(), color);
            }           
        }
        
        MyVect getIntersection(MyVect origin, MyVect ray, MyVect dimensions, boolean faceCoordinate) {
            MyVect faceOrigin = getNormal().mul(Math.abs(getNormal().dot(dimensions) / 2));
            if (faceCoordinate) {
                MyVect p = PlaneConstrainedMotion.computeRayPlaneInstersection(origin, ray, faceOrigin, getNormal());
                p = p.sub(faceOrigin);
                return new MyVect(p.dot(getX()), p.dot(getY()), p.dot(getNormal()));
            }
            else {
                return PlaneConstrainedMotion.computeRayPlaneInstersection(origin, ray, faceOrigin, getNormal());
            }
        }
    }
        
    private J3DBox box;
    protected boolean isSelectionBox = false;
    private BufferedImage[] textures;
    private MySelectionBox selectionBox; 
    private TransformGroup tg;
    private TransformGroup scale;

    MyBox3D(ObjectInstance3D o, Part part) {
        super(o, part, false);
        
        tg = J3DHelper.newTransformGroup();
        scale = J3DHelper.newTransformGroup();
                       
        box = new J3DBox(this);        
        scale.addChild(box);
        tg.addChild(scale);
        
        textures = o.getManager().getPart(boxID).getTextures();
        
        faces = new Face3D[6];        
        for (int i = 0; i < 6; i++) {
            Shape3D shape = box.getShape(FaceType2ShapeType(FaceType.values()[i]));            
            shape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
            updateTexture(FaceType.values()[i]);

            faces[i] = new Face3D(this, part.getFace(FaceType.values()[i]), FaceType.values()[i]);
        }
        o.getTransformGroup().addChild(tg);
        
        variableUpdated();
    }
    
    @Override
    protected final void variableUpdated() {
        super.variableUpdated();
        for (int i = 0; i < 6; i++) {
            ((Face3D)faces[i]).variableUpdated();
        }
        Transform3D t = new Transform3D();
        t.setScale(dimensions);
        scale.setTransform(t);
        t = new Transform3D();
        t.setTranslation(translation);
        tg.setTransform(t);
    }
    
    Box getBox() {
        return box;
    }
    
    TransformGroup getTransformGroup() {
        return tg;
    }
    
    @Override
    public ObjectInstance3D getParentObject() {
        return (ObjectInstance3D)parent;
    }
    
    final int FaceType2ShapeType(ObjectManager.FaceType ft) {
        switch (ft) {
            case BACK:   return Box.LEFT;
            case BOTTOM: return Box.BACK;
            case LEFT:   return Box.BOTTOM;
            case FRONT:  return Box.RIGHT;
            case TOP:    return Box.FRONT;
            default:     return Box.TOP;
        }
    }
        
    @Override
    public void setColor(Color3f c) {
        super.setColor(c);
        for (int i = 0; i < 6; i++) {
            updateTexture(ObjectManager.FaceType.values()[i]);
        }
    }

    void setTexture(ObjectManager.FaceType ft, BufferedImage image) {
        textures[ft.ordinal()] = image;
        updateTexture(ft);
    }

    private void updateTexture(ObjectManager.FaceType ft) {
        Shape3D shape = box.getShape(FaceType2ShapeType(ft));        

        Appearance ap = J3DHelper.newAppearance();
        J3DHelper.setAppearance(ap, color, textures[ft.ordinal()]);
        
        shape.setAppearance(ap);
    }

    FaceType getFaceType(Shape3D s) {
        for (int i = 0; i < 5; i++) {
            if (s == box.getShape(FaceType2ShapeType(FaceType.values()[i]))) {
                return FaceType.values()[i];
            }
        }
        return FaceType.values()[5];
    }
    
    @Override
    public Face3D getFace(FaceType ft) {
        return (Face3D)faces[ft.ordinal()];
    }    
    
    void select() {
        selectionBox = new MySelectionBox(this);
    }

    void deselect() {
        if (selectionBox != null) {
            selectionBox.detach();
            selectionBox = null;
        }
    }

    void drawOnPanel(MyPanel panel, RoomParameters params, Transform3D parentT) {
        Transform3D t = new Transform3D();
        Transform3D transform = new Transform3D(parentT);
        tg.getTransform(t);
        transform.mul(t);
        
        MyMatrix objRot = new MyMatrix();
        MyVect objTrans = new MyVect();
        J3DHelper.extractRotTrans(transform, objRot, objTrans, null);

        MyMatrix rot = new MyMatrix(params.R);
        rot.mul(objRot);

        MyVect trans = params.R.mul(objTrans).add(params.t);

        MyMatrix invObjRot = new MyMatrix(objRot);
        invObjRot.invert();
        MyVect obj2Cam = invObjRot.mul(params.cameraPosition.sub(objTrans));

        Color c;
        c = new Color(80, 80, 80);
        for (int i = 0; i < 6; i++) {
            MyVect obj2Face = faces[i].getNormal().mul(Math.abs(faces[i].getNormal().dot(getDims()) / 2));
            if (obj2Cam.sub(obj2Face).dot(faces[i].getNormal()) < 0) {
                ((Face3D)faces[i]).drawOnPanel(panel, params.K, rot, trans, getDims(), c);
            }
        }
        c = new Color(255, 255, 255);
        for (int i = 0; i < 6; i++) {
            MyVect obj2Face = faces[i].getNormal().mul(Math.abs(faces[i].getNormal().dot(getDims()) / 2));
            if (obj2Cam.sub(obj2Face).dot(faces[i].getNormal()) >= 0) {
                ((Face3D)faces[i]).drawOnPanel(panel, params.K, rot, trans, getDims(), c);
            }
        }
    }
    

    MyVect getIntersection(FaceType ft, MyVect origin, MyVect ray, boolean faceCoordinate) {
        Transform3D transform = getParentObject().getTransfrom3DFromOrigin();
        Transform3D t = new Transform3D();
        tg.getTransform(t);
        transform.mul(t);

        MyMatrix objRot = new MyMatrix();
        MyVect objTrans = new MyVect();
        J3DHelper.extractRotTrans(transform, objRot, objTrans, null);

        origin = origin.sub(objTrans);
        MyMatrix invObjRot = new MyMatrix(objRot);
        invObjRot.invert();

        origin = invObjRot.mul(origin);
        ray = invObjRot.mul(ray);

        MyVect p = ((Face3D)faces[ft.ordinal()]).getIntersection(origin, ray, getDims(), faceCoordinate);
        
        if (!faceCoordinate) {
            p = objRot.mul(p);
            p = p.add(objTrans);
        }

        return p;
    }

    MyVect getIntersection(FaceType ft, MyVect origin, MyVect ray) {
        return getIntersection(ft, origin, ray, false);
    }

    ConstrainedMotion getMotion(FaceType face, MyVect origin, MyVect ray) {
        MyVect motionOrigin = getIntersection(face, origin, ray);
        return ((ObjectInstance3D)parent).getMotion(motionOrigin, getFace(face));
    }    
}
