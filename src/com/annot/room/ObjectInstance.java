 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.room;

import com.annot.room.MyBox.Face;
import com.annot.room.ObjectManager.FaceType;
import com.annot.room.Room.NoSuchObjectException;
import com.common.BBox2D;
import com.common.BBox3D;
import com.common.Expression;
import com.common.MyMatrix;
import com.common.MyVect;
import com.common.XML.XMLException;
import com.common.XML.XMLNode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import javax.vecmath.Color3f;

/**
 *
 * @author vdelaitr
 */
public class ObjectInstance {
    public static class NoSuchVariableException extends Exception {
        static final long serialVersionUID = 0L;
        NoSuchVariableException(String name, String var) {
            super("Object " + name + ": no such variable: " + var);
        }   
    }
        
    public static class NoSupportFaceException extends Exception {
        static final long serialVersionUID = 0L;        
        NoSupportFaceException(String msg) {
            super(msg);
        }
    }

    public static class Link {
        int part;
        FaceType face;
        ObjectInstance child;

        Link(int p, FaceType f, ObjectInstance c) {
            part = p;
            face = f;
            child = c;
        }
    }
    
    public static class Transform {
        public MyVect transl;
        public double angleZ;
        
        Transform() {
            transl = new MyVect();
            angleZ = 0;
        }
        
        Transform(MyVect t, double a) {
            transl = new MyVect(t);
            angleZ = a;
        }
        
        Transform mul(Transform t) {
            return new Transform(
                    MyMatrix.rotationZ(angleZ).mul(t.transl).add(transl),
                    angleZ + t.angleZ);
        }
    }

    static double ROT_0 = 0;
    static double ROT_90 = Math.PI / 2;
    static double ROT_180 = Math.PI;
    static double ROT_270 = -Math.PI / 2;

    protected ObjectManager objm;    
    protected HashMap<String, Double> variables;
    protected HashMap<String, Double> coeffs;
    
    protected Color3f color;
    protected MyVect position = new MyVect(0, 0, 0);
    protected double angle = 0;
    protected MyBox[] parts;
    protected MyBox parentBox = null;
    protected FaceType parentFT = null;
    protected LinkedList<Link> children = new LinkedList<Link>();

    public ObjectInstance(ObjectManager om) {        
        setupVariables(om);
        
        color = om.getDefaultColor();
        
        parts = new MyBox[om.parts.size()];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new MyBox(this, om.parts.get(i));
        }
        
        variableUpdated();
    }
    
    final void setupVariables(ObjectManager om) {
        objm = om;               
        variables = new HashMap<String, Double>(om.variables.toValues());
        coeffs = new HashMap<String, Double>(om.variables.toCoeffs()); 
    }

    public void setVariables(HashMap<String, Double> vSet) {
        for (String var : vSet.keySet()) {
            if(variables.containsKey(var)) {
                variables.put(var, vSet.get(var));
            }
        }     
        variableUpdated();
    }

    public void setVariable(String var, double value) {
        if (variables.containsKey(var)) {
            variables.put(var, value);
        }  
        variableUpdated();
    }
            
    protected final void variableUpdated() {
        for (int i = 0; i < parts.length; i++) {
            parts[i].variableUpdated();
        }
    }

    public HashMap<String, Double> getVariables() {
        return (HashMap<String, Double>)variables.clone();
    }
        
    public double getVariable(String var) throws NoSuchVariableException {
        if (variables.containsKey(var)) {
            return variables.get(var).doubleValue();
        }
        else {
            throw new NoSuchVariableException(objm.getName(), var);
        }
    }
    
    public double getCoeff(String var) throws NoSuchVariableException {
        if (coeffs.containsKey(var)) {
            return coeffs.get(var).doubleValue();
        }
        else {
            throw new NoSuchVariableException(objm.getName(), var);
        }
    }    

    public void clear() {
        parentBox = null;
        for (Link l : children) {
            l.child.clear();
        }
        children.clear();        
        for (int i = 0; i < parts.length; i++) {
            parts[i].clear();
        }
        parts = null;
    }
    
    public double[] getColor() {
        double[] c = new double[3];
        c[0] = color.x;
        c[1] = color.y;
        c[2] = color.z;
        return c;
    }
    
    public Color3f getColor3f() {
        return color;
    }
    
    void setColor(Color3f c) {
        color = c;
        for (int i = 0; i < parts.length; i++) {
            getPart(i).setColor(color);
        }
    }

    void addChild(MyBox p, FaceType ft, ObjectInstance child) {
        int part = -1;
        for (int i = 0; i < parts.length; i++) {
            if (p == parts[i]) {
                part = i;
                break;
            }
        }
        if (part != -1) {
            children.add(new Link(part, ft, child));
            p.getFace(ft).addChild(child);
        }
    }

    void removeChild(ObjectInstance child) {
        for (Link l : children) {
            if (l.child == child) {
                parts[l.part].getFace(l.face).removeChild(child);                        
                children.remove(l);
                break;
            }
        }

    }

    public void setFacePosition(double x, double y) {
        double ang;
        Face face = getAttachedFace();
        switch(face.getType()) {
            case LEFT:
                ang = ROT_270;
                break;
            case RIGHT:
                ang = ROT_90;
                break;
            case BACK:
                ang = ROT_180;
                break;
            case FRONT:
                ang = ROT_0;
                break;
            default:
                ang = angle;
                break;
        }
        setFacePosition(x, y, ang);
    }

    public void setFacePosition(double x, double y, double ang) {
        Face face = getAttachedFace();
        if (face != null) {
            angle = ang;
            position = face.getX().mul(x).add(
                       face.getY().mul(y)).add(
                       face.getNormal().mul(
                       -getMinProjOnAxis(face.getNormal())));
            positionUpdated();
        }
        else {
            System.err.println("Warning: set position of non attached object.");
        }
    }
  
    public boolean attachTo(MyBox p, FaceType ft) {
        try {
            getSupportForAttach(ft); // check this is authorized
            p.getParentObject().addChild(p, ft, this);   
            parentBox = p;
            parentFT = ft;
            return true;
        }
        catch (NoSupportFaceException e) {
            return false;
        }
    }

    public void detach() {
        if(parentBox != null) {
            parentBox.getParentObject().removeChild(this);            
            parentBox = null;
        }
    }

    // Overrided by ObjectInstance3D
    public void positionUpdated() {
    }
    
    public void setPositionAngle(MyVect pos, double ang) {
        position = pos;   
        angle = ang;
        positionUpdated();
    }    
    
    public void setPosition(MyVect pos) {
        position = pos;   
        positionUpdated();
    }    
    
    public MyVect getPosition() {
        return position;
    }    
    
    public void setAngle(double ang) {
        angle = ang;
        positionUpdated();
    }

    public double getAngle() {
        return angle;
    }

    public void rotate() {
        angle += ROT_90;
        positionUpdated();
    }

    public void translate(MyVect v) {
        position = position.add(v);
        positionUpdated();
    }

    public MyMatrix getRotationMatrix() {
        return MyMatrix.rotationZ(angle);         
    }

    public String getName() {
        return objm.getName();
    }

    public char[] getNameArray() {
        return objm.getName().toCharArray();
    }

    public char[] getClassNameArray() {
        if (objm.hasClassName()) {
            return objm.getClassName().toCharArray();
        }
        else {
            return null;
        }
    }

    public MyBox getPart(int i) {
        return parts[i];
    }

    public Transform getTransfromFromOrigin() {
        Transform t;
        if (parentBox == null) {
            t = new Transform();
        }
        else {
            t = parentBox.getParentObject().getTransfromFromOrigin();
            MyVect n = getAttachedFaceNormal();
            MyVect faceTrans = n.mul(parentBox.getDims().dot(n) / 2);            
            MyVect boxtrans = parentBox.getTrans();
            t = t.mul(new Transform(boxtrans.add(faceTrans), 0));            
        }
        t = t.mul(new Transform(position, angle));
             
        return t;
    }

    double getMinProjOnAxis(MyVect axis) {
        MyMatrix rot = getRotationMatrix();
        double min = parts[0].getMinProjOnAxis(rot, axis);
        for (int i = 1; i < parts.length; i++) {
            double m = parts[i].getMinProjOnAxis(rot, axis);
            if (m < min) {
                min = m;
            }
        }
        return min;
    }
    
    // Return bounding box of the object projected on the parent surface coordinate
    public BBox3D getBound3D() {     
        Transform t = getTransfromFromOrigin();        
        MyMatrix objRot = MyMatrix.rotationZ(t.angleZ);
        
        BBox3D bb = new BBox3D();
        for (int i = 0; i < parts.length; i++) {           
            bb.expand(parts[i].getBound3D(variables, objRot, t.transl));
        }

        return bb;
    }

    // Return bounding box of the object projected on the parent surface coordinate
    public BBox2D getBound2D(HashMap<String, Double> var, MyVect pos) {        
        MyVect x = getAttachedFaceX();
        MyVect y = getAttachedFaceY();
           
        BBox2D bb = new BBox2D();
        MyMatrix rot = getRotationMatrix();
        for (int i = 0; i < parts.length; i++) {           
            bb.expand(parts[i].getBound2D(var, x, y, rot, pos));
        }

        return bb;
    }

    // Return bounding box of the object projected on the parent surface coordinate
    public BBox2D getBound2D() {
        return getBound2D(variables, position);
    }
    
    // Return bounding box of the object projected on the image plane coordinate
    public BBox2D getBoundingBox(Room room) {
        Transform t = getTransfromFromOrigin();        
        MyMatrix objRot = MyMatrix.rotationZ(t.angleZ);

        BBox2D bound = new BBox2D();
        for (int i = 0; i < parts.length; i++) {
            bound.expand(parts[i].getBoundingBox(room, objRot, t.transl));
        }
        return bound;
    }

    public Face getAttachedFace() {
        if (parentBox != null) {
            return parentBox.getFace(parentFT);
        }
        else {
            return null;
        }
    }

    public MyVect getAttachedFaceX() {
        if (parentBox != null) {
            return parentBox.getFace(parentFT).getX();
        }
        else {
            return new MyVect(1, 0, 0);
        }
    }

    public MyVect getAttachedFaceY() {
        if (parentBox != null) {
            return parentBox.getFace(parentFT).getY();
        }
        else {
            return new MyVect(0, 1, 0);
        }
    }

    public MyVect getAttachedFaceNormal() {
        if (parentBox != null) {
            return parentBox.getFace(parentFT).getNormal();
        }
        else {
            return new MyVect(0, 0, 1);
        }
    }

    public MyMatrix getAttachedRotation() {
        if (parentBox != null) {
            ObjectInstance o = parentBox.getParentObject();
            MyMatrix m = new MyMatrix(o.getAttachedRotation());
            m.mul(o.getRotationMatrix());
            return m;
        }
        else {
            return new MyMatrix(1, 1, 1);
        }
    }

    public BBox2D getAttachedFaceBound() {
        if (parentBox != null) {
            return parentBox.getFace(parentFT).getBound(parentBox.getDims());
        }
        else {
            return getBound2D();
        }
    }

    public FaceType getSupportForAttach(FaceType ft) throws NoSupportFaceException {
        switch (ft) {
            case TOP:
                return findSupportFace(FaceType.BOTTOM);
            case BOTTOM:
                return findSupportFace(FaceType.TOP);
            default:
                FaceType f;
                if ((f = findSupportFace(FaceType.FRONT)) != null) {
                    return f;
                }
                else if ((f = findSupportFace(FaceType.BACK)) != null) {
                    return f;
                }   
                else if ((f = findSupportFace(FaceType.LEFT)) != null) {
                    return f;
                }
                else if ((f = findSupportFace(FaceType.RIGHT)) != null) {
                    return f;
                }
                else {
                    throw new NoSupportFaceException("Attaching to this face is not authorized.");
                }
        }
    }

    FaceType findSupportFace(FaceType ft) {
        for (FaceType suppf : objm.suppFaces) {
            if (ft == suppf) {
                return suppf;
            }
        }
        return null;
    }

    public boolean isPrivate() {
        return (objm.flags & ObjectManager.PRIVATE) != 0;
    }

    public boolean isStatic() {
        return (objm.flags & ObjectManager.STATIC) != 0;
    }

    public boolean isWall() {
        return (objm.flags & ObjectManager.WALL) != 0;
    }

    public boolean isFloor() {
        return (objm.flags & ObjectManager.FLOOR) != 0;
    }

    public boolean isCeiling() {
        return (objm.flags & ObjectManager.CEILING) != 0;
    }

    public int getClassID() {
        return objm.getClassID();
    }

    public String getClassName() {
        return objm.getClassName();
    }

    public boolean hasClassName() {
        return objm.hasClassName();
    }
    
    public ObjectManager getManager() {
        return objm;
    }

    void toXML(XMLNode root) {
        FaceType[] fts = FaceType.values();
        for (int i = 0; i < parts.length; i++) {
            XMLNode p = null;

            for (int j = 0; j < 6; j++) {
                XMLNode f = null;

                List<ObjectInstance> l = getPart(i).getFace(fts[j]).getChildren();
                for (ObjectInstance child : l) {

                    if (!child.isPrivate()) {
                        if (p == null) {
                            p = new XMLNode("part");
                            p.setAttribute("id", Integer.toString(i));
                            root.addContent(p);
                        }
                        if (f == null) {
                            f = new XMLNode("face");
                            f.setAttribute("type", FaceType.values()[j].name());
                            p.addContent(f);
                        }
                        
                        XMLNode o = new XMLNode("object");
                        o.setAttribute("name", child.getName());
                        f.addContent(o);

                        XMLNode pos = XMLNode.fromVect("position", child.position);
                        pos.addContent(Double.toString(child.angle));
                        o.addContent(pos);

                        XMLNode bound = new XMLNode("bounds");
                        BBox2D b = child.getBound2D();
                        bound.setAttribute("xmin", b.xmin);
                        bound.setAttribute("xmax", b.xmax);
                        bound.setAttribute("ymin", b.ymin);
                        bound.setAttribute("ymax", b.ymax);
                        o.addContent(bound);

                        XMLNode vars = new XMLNode("variables");
                        for (String v : child.variables.keySet()) {
                            XMLNode var = new XMLNode("var");
                            var.setAttribute("name", v);
                            var.addContent(child.variables.get(v).toString());
                            vars.addContent(var);
                        }
                        o.addContent(vars);

                        child.toXML(o);                        
                    }
                }
            }
        }        
    }

    void fromXML(Room room, XMLNode root) throws XMLException {
        for (XMLNode part : root.getChildren("part")) {            
            for (XMLNode face : part.getChildren("face")) {
                MyBox b = getPart(part.getIntegerAttribute("id"));
                FaceType ft = FaceType.valueOf(
                              face.getAttribute("type").
                              toUpperCase());
                for (XMLNode node : face.getChildren("object")) {
                    try {
                        ObjectInstance o = room.newObject(node.getAttribute("name"));
                        o.attachTo(b, ft);
                        
                        XMLNode pos = node.getChild("position");
                        o.setPositionAngle(pos.toVect(), pos.getDoubleContent());

                        for (XMLNode var : node.getChild("variables").getChildren("var")) {
                            o.setVariable(var.getAttribute("name"), var.getDoubleContent());
                        }

                        o.fromXML(room, node);
                    }
                    catch (NoSuchObjectException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
    }
    
    // Check variable constraints
    public boolean checkConstraints(HashMap<String, Double> var) {
        try {
            Vector<Expression> varConstr = objm.getVarConstr();
            for (int i = 0; i < varConstr.size(); i++) {
                if (!varConstr.get(i).evaluateBoolean(var)) {
                    return false;
                }
            }
            return true;
        }
        catch (Expression.EvaluateException e) {
            return false;
        }
    }
}
