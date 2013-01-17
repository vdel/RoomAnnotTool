/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.room;

import com.annot.room.ObjectManager.FaceType;
import com.annot.room.ObjectManager.Node;
import com.annot.room.ObjectManager.Part;
import com.common.BBox2D;
import com.common.BBox3D;
import com.common.Expression;
import com.common.Expression.EvaluateException;
import com.common.MyMatrix;
import com.common.MyVect;
import java.util.HashMap;
import java.util.LinkedList;
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
}
