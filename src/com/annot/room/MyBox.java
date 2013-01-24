/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.room;

import com.annot.room.ObjectInstance.ObjectFace;
import com.annot.room.ObjectManager.FaceType;
import com.annot.room.ObjectManager.Node;
import com.annot.room.ObjectManager.Part;
import com.common.BBox2D;
import com.common.BBox3D;
import com.common.Expression;
import com.common.Expression.EvaluateException;
import com.common.MyMatrix;
import com.common.MyVect;
import java.awt.Point;
import java.awt.Polygon;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.vecmath.Color3f;

/**
 *
 * @author vdelaitr
 */
public class MyBox {
    public static class Face {
        protected MyBox parent;
        protected FaceType type;
        protected Node faceNode;
        protected MyVect x, y, n;
        protected LinkedList<ObjectInstance> children;

        public Face(MyBox p, Node fn, FaceType ft) {
            parent = p;
            type = ft;
            faceNode = fn;
            x = new MyVect();
            y = new MyVect();
            n = new MyVect();
            children = new LinkedList<ObjectInstance>();
            
            switch (ft) {
                case BACK:
                    x = new MyVect(0, -1, 0);
                    y = new MyVect(0, 0, -1);
                    n = new MyVect(-1, 0, 0);
                    break;
                case BOTTOM:
                    x = new MyVect(0, 1, 0);
                    y = new MyVect(-1, 0, 0);
                    n = new MyVect(0, 0, -1);
                    break;
                case LEFT:
                    x = new MyVect(1, 0, 0);
                    y = new MyVect(0, 0, -1);
                    n = new MyVect(0, -1, 0);
                    break;
                case FRONT:
                    x = new MyVect(0, 1, 0);
                    y = new MyVect(0, 0, -1);
                    n = new MyVect(1, 0, 0);
                    break;
                case TOP:
                    x = new MyVect(0, 1, 0);
                    y = new MyVect(1, 0, 0);
                    n = new MyVect(0, 0, 1);
                    break;
                case RIGHT:                
                    x = new MyVect(-1, 0, 0);
                    y = new MyVect(0, 0, -1);
                    n = new MyVect(0, 1, 0);
                    break;
                default:
                    throw new RuntimeException("Unknown face !");
            }         
        }

        public MyBox getParentBox() {
            return parent;
        }

        public FaceType getType() {
            return type;
        }

        void addChild(ObjectInstance o) {
            children.add(o);
        }

        void removeChild(ObjectInstance o) {
            children.remove(o);
        }

        public LinkedList<ObjectInstance> getChildren() {
            return children;
        }

        public ObjectInstance[] getChildrenArray() {
            ObjectInstance[] c = new ObjectInstance[children.size()];

            int i = 0;
            for (ObjectInstance o : children) {
                c[i] = o;
                i++;
            }

            return c;
        }

        public MyVect getX() {
            return x;
        }

        public MyVect getY() {
            return y;
        }

        public MyVect getNormal() {
            return n;
        }

        BBox2D getBound(MyVect dims) {
            double bx = Math.abs(dims.dot(x)) / 2;
            double by = Math.abs(dims.dot(y)) / 2;
            return new BBox2D(-bx, -by, bx, by);
        }

        public HashMap<String, Double> getVariables() {
            return faceNode.getVariablesToParent();
        }
        
        void addFace(List<ObjectFace> ofl, Room room, MyMatrix camRot, MyVect camTrans, MyVect dims) {
            MyVect vx = getX().mul(Math.abs(dims.dot(getX()) / 2));
            MyVect vy = getY().mul(Math.abs(dims.dot(getY()) / 2));
            MyVect vz = getNormal().mul(Math.abs(dims.dot(getNormal()) / 2));

            double znear = -0.001;
            MyVect[] v = new MyVect[4];
            v[0] = camRot.mul(vz.sub(vx).sub(vy)).add(camTrans);
            v[1] = camRot.mul(vz.sub(vx).add(vy)).add(camTrans);
            v[2] = camRot.mul(vz.add(vx).add(vy)).add(camTrans);
            v[3] = camRot.mul(vz.add(vx).sub(vy)).add(camTrans);            
            
            Polygon p = new Polygon();
            MyVect p0 = null;
            // this loop may not be very effective but we have to somehow deal with face going beyond the projective plane
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

                if (p.npoints == 0) {
                    p0 = new MyVect(p1);
                }
                
                p1 = room.getImage().directToImageCoord(room.getParams().K.mul(p1));
                p2 = room.getImage().directToImageCoord(room.getParams().K.mul(p2));
                Point pp1 = p1.toPoint();
                Point pp2 = p2.toPoint();
                
                if (p.npoints == 0) {
                    p.addPoint(pp1.x, pp1.y);
                    p.addPoint(pp2.x, pp2.y);
                }  
                else {
                    if (p.xpoints[p.npoints - 1] != pp1.x ||
                        p.ypoints[p.npoints - 1] != pp1.y) {
                        p.addPoint(pp1.x, pp1.y);    
                    }
                    if (i < 3 ||
                        p.xpoints[0] != pp2.x ||
                        p.ypoints[0] != pp2.y) {    
                        p.addPoint(pp2.x, pp2.y);    
                    }
                }
            }
            
            ofl.add(new ObjectFace(this, p, p0, camRot.mul(getNormal())));
        }
    }
    
    //========================================================================//
        
    protected ObjectInstance parent;
    protected int boxID;
    protected Face[] faces;
    protected Expression posX, posY, posZ;
    protected Expression depth, width, height;
    protected MyVect translation;
    protected MyVect dimensions;
    protected Color3f color;
    
    public MyBox(ObjectInstance o, Part part) {
        this(o, part, true);
    }
    
    protected MyBox(ObjectInstance o, Part part, boolean initFaces) {
        parent = o;
        boxID = part.getBoxID();        
        depth = part.depth;
        width = part.width;
        height = part.height;
        posX = part.posX;
        posY = part.posY;
        posZ = part.posZ;
        
        color = o.getColor3f();

        if (initFaces) {
            faces = new Face[6];        
            for (int i = 0; i < 6; i++) {          
                faces[i] = new Face(this, part.getFace(FaceType.values()[i]), FaceType.values()[i]);
            }
        }
        
        setTransDims();
    }

    void clear() {
        for (int i = 0; i < 6; i++) {
            faces[i].children.clear();
            faces[i].parent = null;
        }
        parent = null;
        faces = null;
    }

    public int getBoxID() {
        return boxID;
    }
    
    public ObjectInstance getParentObject() {
        return parent;
    }
    
    final void setTransDims() {
        translation = getTrans(parent.getVariables());
        dimensions = getDims(parent.getVariables());
    }
    
    protected void variableUpdated() {
        setTransDims();
    }

    public Face getFace(FaceType ft) {
        return faces[ft.ordinal()];
    }
    
    public final MyVect getTrans() {
        return translation;
    }
    
    public final MyVect getTrans(HashMap<String, Double> variables) {
        try {
            return new MyVect(posX.evaluate(variables),
                              posY.evaluate(variables),
                              posZ.evaluate(variables));
        }
        catch (EvaluateException e) {
            throw new RuntimeException(e);
        }            
    }

    public final MyVect getDims() {
        return dimensions;
    }

    public final MyVect getDims(HashMap<String, Double> variables) {
        try {
            MyVect v =  new MyVect(depth.evaluate(variables),
                                   width.evaluate(variables),
                                   height.evaluate(variables));
            return v;
        }
        catch (EvaluateException e) {
            throw new RuntimeException(e);
        }
    }
    
    public Color3f getColor() {
        return color;
    }
    
    public void setColor(Color3f c) {
        color = c;
    }

    double getMinProjOnAxis(MyMatrix objRot, MyVect axis) {
        MyVect d = objRot.mul(dimensions).mul(0.5);
        return axis.dot(translation) + Math.min(axis.dot(d), -axis.dot(d));
    }
    
    // Return bounding box in room coordinate
    BBox3D getBound3D(HashMap<String, Double> vars, MyMatrix objRot, MyVect objTrans) {
        objTrans = objRot.mul(translation).add(objTrans);        
        MyVect d = objRot.mul(dimensions).mul(0.5);
        return new BBox3D(objTrans.sub(d), objTrans.add(d));
    }

    // Return bounding box of the object projected on the parent surface coordinate
    BBox2D getBound2D(HashMap<String, Double> vars, MyVect x, MyVect y, MyMatrix objRot, MyVect objTrans) {
        BBox3D bb = getBound3D(vars, objRot, objTrans);
        return new BBox2D(bb.min.dot(x), bb.min.dot(y), bb.max.dot(x), bb.max.dot(y));
    }

    BBox2D getBoundingBox(Room room, MyMatrix objRot, MyVect objTrans) {
        RoomParameters params = room.getParams();
        
        MyMatrix rot = new MyMatrix(params.R);
        rot.mul(objRot);
        
        objTrans = objRot.mul(translation).add(objTrans);  
        MyVect trans = params.R.mul(objTrans).add(params.t);

        double znear = -0.001;
        BBox2D bound = new BBox2D();
        double[] dx = new double[] {-1, 1,-1, 1,-1, 1,-1, 1};
        double[] dy = new double[] {-1,-1, 1, 1,-1,-1, 1, 1};
        double[] dz = new double[] {-1,-1,-1,-1, 1, 1, 1, 1};
        for (int i = 0; i < 8; i++) {  
            MyVect v = new MyVect(dimensions.x * dx[i] / 2,
                                  dimensions.y * dy[i] / 2,
                                  dimensions.z * dz[i] / 2);
            v = rot.mul(v).add(trans);
            
            if (v.z < znear) {
                 v = params.K.mul(v);
                 bound.expand(room.getImage().directToImageCoord(v).toPoint());
            }
        }
        return bound;
    }
    
    void getVisibleFaces(Room room, MyMatrix objRot, MyVect objTrans, List<ObjectFace> l) {
        objTrans = objRot.mul(translation).add(objTrans);  
        
        MyMatrix rot = new MyMatrix(room.getParams().R);
        rot.mul(objRot);        
        MyVect trans = room.getParams().R.mul(objTrans).add(room.getParams().t);
        
        MyMatrix invObjRot = new MyMatrix(objRot);
        invObjRot.invert();
        MyVect obj2Cam = invObjRot.mul(room.getParams().cameraPosition.sub(objTrans));
        
        for (int i = 0; i < 6; i++) {
            MyVect obj2Face = faces[i].getNormal().mul(Math.abs(faces[i].getNormal().dot(getDims()) / 2));
            if (obj2Cam.sub(obj2Face).dot(faces[i].getNormal()) >= 0) {
                faces[i].addFace(l, room, rot, trans, getDims());
            }
        }
    }
}
