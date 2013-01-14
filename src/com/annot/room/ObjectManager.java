/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.room;

import com.common.Expression;
import com.common.Expression.EvaluateException;
import com.common.Expression.ParseException;
import com.common.XML;
import com.common.XML.XMLException;
import com.common.XML.XMLNode;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.vecmath.Color3f;
import javax.vecmath.Point2d;

/**
 *
 * @author vdelaitr
 */
public class ObjectManager {
    static public class ObjectException extends Exception {
        static final long serialVersionUID = 0L;

        ObjectException(String msg) {
            super(msg);
        }

        ObjectException(Exception e) {
            super(e);
        }
    }

    static public enum FaceType {FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM};
    static public enum AxisType {X, Y, Z};

    static class VarMap extends HashMap<String, Point2d> {
        static final long serialVersionUID = 0L;
        
        VarMap() {
            super();
        }

        VarMap(VarMap m) {
            super(m);
        }

        HashMap<String, Double> toValues() {
            HashMap<String, Double> r = new HashMap<String, Double>();
            for (String v : keySet()) {
                r.put(v, get(v).x);
            }
            return r;
        }

        HashMap<String, Double> toCoeffs() {
            HashMap<String, Double> r = new HashMap<String, Double>();
            for (String v : keySet()) {
                r.put(v, get(v).y);
            }
            return r;
        }
    }

    static class Edge {
        Node to;
        Expression expr;

        Edge(Node to, Expression expr) {
            this.to = to;
            this.expr = expr;
        }
    }

    public static class Node {
        private int box;
        private String face;
        private Expression position;
        private Node parent;
        private Expression eFromParent;
        private Vector<Edge> edges;

        Node(int box, String face) {
            this.box = box;
            this.face = face;
            parent = null;
            edges = new Vector<Edge>();
        }

        void clearVisites() {
            position = null;
        }

        void setParent(Node p, Expression e) throws ObjectException {
            if (parent == null) {
                parent = p;
                eFromParent = e;
            }
            else {
                throw new ObjectException("Face '" + face + "' of box '" + box + "' already has a parent.");
            }
        }

        void checkConstraints(Expression p) throws ObjectException {
            if (position != null) {
                throw new ObjectException("Over-constrained face '" + face + "' for box '" + box + "'.");
            }
            else {                
                position = p.simplify();                
                for (int i = 0; i < edges.size(); i++) {
                    try  {
                        edges.get(i).to.checkConstraints(Expression.Add(position, edges.get(i).expr));
                    }
                    catch (ParseException e) {
                    }
                }
            }
        }

        Expression getPosition() {
            return position;
        }

        HashMap<String, Double> getVariablesToParent() {
            try {
                if (eFromParent == null) {
                    return new HashMap<String, Double>();
                }
                else {
                    HashMap<String, Double> v = eFromParent.getVariables();
                    if (v.isEmpty()) {
                        if (parent == null) {
                            return new HashMap<String, Double>();
                        }
                        else {
                            return parent.getVariablesToParent();
                        }
                    }
                    else {
                        return v;
                    }
                }
            }
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        HashMap<String, Double> getVariablesToOrigin() {
            try {              
                HashMap<String, Double> v = position.getVariables();
                if (v.isEmpty()) {
                    if (parent == null) {
                        return new HashMap<String, Double>();
                    }
                    else {
                        return parent.getVariablesToOrigin();
                    }
                }
                else {
                    return v;
                }
            }
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Part {
        private int boxID;
        private Node[] faces;
        public Expression width, depth, height;
        public Expression posX, posY, posZ;
        BufferedImage[] textures;

        Part(int boxID) {
            this.boxID = boxID;
            faces = new Node[6];
            textures = new BufferedImage[6];
            for (int i = 0; i < 6; i++) {
                faces[i] = new Node(boxID, FaceType.values()[i].name());                
            }
        }

        public Node getFace(FaceType ft) {
            return faces[ft.ordinal()];
        }

        public int getBoxID() {
            return boxID;
        }
        
        public BufferedImage[] getTextures() {
            return (BufferedImage[])textures.clone();
        }

        void setTexture(FaceType ft, String img) {
            try {
                textures[ft.ordinal()] = ImageIO.read(new File(texturePath, img));
            } catch (IOException e) {
                System.err.println("Failed to load texture: " + img);
            }
        }

        void clearVisites() {
            for (int i = 0; i < 6; i++) {
                faces[i].clearVisites();
            }
        }

        void checkVisite(HashMap<String, Double> variables) throws ObjectException {
            for (int i = 0; i < 6; i++) {
                if (faces[i].position == null) {
                    throw new ObjectException("Under-constrained face '" + faces[i].face + "' for box '" + boxID + "'.");
                }
            }
            try {
                depth = Expression.Sub(faces[FaceType.FRONT.ordinal()].position, faces[FaceType.BACK.ordinal()].position).simplify();
                width = Expression.Sub(faces[FaceType.RIGHT.ordinal()].position, faces[FaceType.LEFT.ordinal()].position).simplify();
                height = Expression.Sub(faces[FaceType.TOP.ordinal()].position, faces[FaceType.BOTTOM.ordinal()].position).simplify();
                posX = Expression.Add(faces[FaceType.BACK.ordinal()].position, depth.Times(0.5)).simplify();
                posY = Expression.Add(faces[FaceType.LEFT.ordinal()].position, width.Times(0.5)).simplify();
                posZ = Expression.Add(faces[FaceType.BOTTOM.ordinal()].position, height.Times(0.5)).simplify();

                if (depth.evaluate(variables) < 0) {
                    throw new ObjectException("Negative depth for box '" + boxID + "'.");
                }
                else if (width.evaluate(variables) < 0) {
                    throw new ObjectException("Negative width for box '" + boxID + "'.");
                }
                else if (height.evaluate(variables) < 0) {
                    throw new ObjectException("Negative height for box '" + boxID + "'.");
                }
                
            }
            catch (ParseException e) {
            }
            catch (EvaluateException e) {
            }
        }
    }

    class Constraint {
        Node face1, face2;

        Constraint(Vector<Part> parts, int b1, String f1, int b2, String f2, Expression expr) throws XMLException {
            FaceType _face1, _face2;
            try {
                _face1 = FaceType.valueOf(f1);
            }
            catch(IllegalArgumentException e) {
                throw new XMLException("Bad face name: '" + f1 + "'.\n");
            }
            try {
                _face2 = FaceType.valueOf(f2);
            }
            catch(IllegalArgumentException e) {
                throw new XMLException("Bad face name: '" + f2 + "'.\n");
            }
            if (getAxis(_face1) != getAxis(_face2)) {
                throw new XMLException("Bad face correspondance: '" + f1 + "' <-> '" + f2 + "' in <faceconstraint> tag.");
            }
            if (b1 < 0 || b1 >= parts.size()) {
                throw new XMLException("Bad box number: '" + b1 + "' in <faceconstraint> tag.");
            }
            if (b2 < 0 || b2 >= parts.size()) {
                throw new XMLException("Bad box number: '" + b2 + "' in <faceconstraint> tag.");
            }
            face1 = parts.get(b1).faces[_face1.ordinal()];
            face2 = parts.get(b2).faces[_face2.ordinal()];

            try {
                setEdge(face1, face2, expr);
            }
            catch (ObjectException e) {
                throw new XMLException(e.getMessage());
            }
        }

        Constraint(Vector<Part> parts, Node[] roots, int b, String f, Expression expr) throws XMLException {
            int aID;
            FaceType face;
            try {
                face = FaceType.valueOf(f);
            }
            catch(IllegalArgumentException e) {
                throw new XMLException("Bad face name: '" + f + "'.\n");
            }
            if (b < 0 || b >= parts.size()) {
                throw new XMLException("Bad box number: '" + b + "' in <supportconstraint> tag.");
            }
            switch (getAxis(face)) {
                case X:  aID = 0; break;
                case Y:  aID = 1; break;
                default: aID = 2; break;
            }
            face1 = roots[aID];
            face2 = parts.get(b).faces[face.ordinal()];

            try {
                setEdge(face1, face2, expr);
            }
            catch (ObjectException e) {
                throw new XMLException(e.getMessage());
            }
        }

        final void setEdge(Node face1, Node face2, Expression expr) throws ObjectException {
            face1.edges.add(new Edge(face2, expr));
            face2.setParent(face1, expr);
        }

        final AxisType getAxis(FaceType at) {
            switch (at) {
                case FRONT: return AxisType.X;
                case BACK:  return AxisType.X;
                case LEFT:  return AxisType.Y;
                case RIGHT: return AxisType.Y;
                case TOP:   return AxisType.Z;
                default:    return AxisType.Z;
            }
        }       
    }

    Node[] roots;
    String name;
    String className;
    int classID;
    Vector<Part> parts;
    VarMap variables;
    Color3f defaultColor;
    Vector<Constraint> faceConstr;
    Vector<Expression> varConstr;
    LinkedList<FaceType> suppFaces;
    int flags;

    static String texturePath;

    static final public int PRIVATE = 1;
    static final public int STATIC = 2;
    static final public int FLOOR = 4;
    static final public int WALL = 8;
    static final public int CEILING = 16;

    /**************************************************************************/

    ObjectManager(String objectName, Color3f color, boolean checkName) {
        name = objectName;
        classID = -1;
        roots = new Node[3];
        roots[0] = new Node(-1, "rootX");
        roots[1] = new Node(-1, "rootY");
        roots[2] = new Node(-1, "rootZ");
        parts = new Vector<Part>();
        variables = new VarMap();
        defaultColor = color;
        faceConstr = new Vector<Constraint>();
        varConstr = new Vector<Expression>();
        suppFaces = new LinkedList<FaceType>();
        flags = 0;
    }

    ObjectManager(String objectName, Color3f color) {
        this(objectName, color,  true);
    }

    private ObjectManager(XMLNode object) throws ObjectException {
        this("", new Color3f(0, 0, 0), false);
        
        try {
            /* Name */
            name = object.getAttribute("name");

            try {
                className = object.getAttribute("class");
            }
            catch (XMLException e) {
                className = null;
            }

            if (name.compareTo("") == 0) {
                throw new XMLException("Empty 'name' attribute in <object> tag");
            }

            XMLNode c = object.getChild("color");
            defaultColor = new Color3f(c.getIntegerAttribute("r") / 255f,
                                       c.getIntegerAttribute("g") / 255f,
                                       c.getIntegerAttribute("b") / 255f);
        
            /* Boxes */
            for (int i = 0; i < object.getIntegerAttribute("nboxes"); i++) {
                addPart();
            }

            /* Variables */
            List<XMLNode> vars = object.getChildren("variable");
            for (XMLNode variable : vars) {
                String var = variable.getAttribute("name");
                double val = variable.getDoubleContent();
                double coeff = variable.getDoubleAttribute("coefforigin");
                if (coeff < 0 || coeff > 1) {
                    throw new XMLException("Bad 'coefforigin' attribute in <variable> tag: '" + coeff + "' is not between 0 and 1.");
                }
                addVariable(var, val, coeff);
            }

            /* Face constraints */
            List<XMLNode> fcsts = object.getChildren("faceconstraint");
            for (XMLNode fcst : fcsts) {
                int box1     = fcst.getIntegerAttribute("box1");
                String face1 = fcst.getAttribute("face1");
                int box2     = fcst.getIntegerAttribute("box2");
                String face2 = fcst.getAttribute("face2");
                String exprStr = fcst.getContent();
                try {
                    Expression expr = Expression.parse(exprStr);
                    if (!expr.isBoolean()) {
                        addFaceConstr(box1, face1, box2, face2, expr);
                    }
                    else {
                        throw new XMLException("Bad scalar expression '" + exprStr + "' in <faceconstraint> tag.");
                    }
                }
                catch (Expression.ParseException e) {
                    throw new XMLException("Bad expression '" + exprStr + "' in <faceconstraint> tag.");
                }
            }

            /* Support constraints */
            List<XMLNode> scsts = object.getChildren("supportconstraint");
            for (XMLNode scst : scsts) {
                String exprStr = scst.getContent();
                try {
                    Expression expr = Expression.parse(exprStr);
                    if (!expr.isBoolean()) {
                        addFaceConstr(scst.getIntegerAttribute("box"), scst.getAttribute("face"), expr);
                    }
                    else {
                        throw new XMLException("Bad scalar expression '" + exprStr + "' in <supportconstraint> tag.");
                    }
                }
                catch (Expression.ParseException e) {
                    throw new XMLException("Bad expression '" + exprStr + "' in <supportconstraint> tag.");
                }
            }

            /* Variable constraints */
            List<XMLNode> vcsts = object.getChildren("varconstraint");
            for (XMLNode vcst : vcsts) {
                String exprStr = vcst.getContent();
                try {
                    Expression expr = Expression.parse(exprStr);
                    if (expr.isBoolean()) {
                        if (expr.evaluateBoolean(variables.toValues())) {
                            addVarConstr(expr);
                        }
                        else {
                            throw new XMLException("Default variable value do not satisfy '" + exprStr + "' in <varconstraint> tag.");
                        }
                    }
                    else {
                        throw new XMLException("Bad boolean expression '" + exprStr + "' in <varconstraint> tag.");
                    }
                }
                catch (Expression.ParseException e) {
                    throw new XMLException("Bad expression '" + exprStr + "' in <varconstraint> tag.");
                }
                catch (Expression.EvaluateException e) {
                    throw new XMLException("Bad expression '" + exprStr + "' in <varconstraint> tag.");
                }
            }

            /* support faces */
            List<XMLNode> supfs = object.getChildren("supportface");
            if (supfs.isEmpty()) {
                FaceType[] ft = FaceType.values();
                for (int i = 0; i < ft.length; i++) {
                    addSupportFace(ft[i]);
                }
            }
            else {
                for (XMLNode supf : supfs) {
                    String f = supf.getContent();
                    try {
                        addSupportFace(FaceType.valueOf(f.toUpperCase()));
                    }
                    catch (IllegalArgumentException e) {
                        throw new XMLException("Bad support face '" + f + "' in <supportface> tag.");
                    }
                }
            }

            /* textures */
            List<XMLNode> textures = object.getChildren("texture");
            for (XMLNode texture : textures) {
                int box = texture.getIntegerAttribute("box");
                if (box >= 0 && box < parts.size()) {
                    FaceType ft = FaceType.valueOf(texture.getAttribute("face").toUpperCase());
                    parts.get(box).setTexture(ft, texture.getContent());
                }
            }
        }
        catch(XMLException e) {
            throw new ObjectException("Object '" + name +"': " + e.getMessage());
        }       

        checkConstraints();
    }

    /**************************************************************************/

    public boolean isPrivate() {
        return (flags & PRIVATE) != 0;
    }

    public boolean isStatic() {
        return (flags & STATIC) != 0;
    }

    public boolean isWall() {
        return (flags & WALL) != 0;
    }

    public boolean isFloor() {
        return (flags & FLOOR) != 0;
    }

    public boolean isCeiling() {
        return (flags & CEILING) != 0;
    }

    /**************************************************************************/

    final void checkConstraints() throws ObjectException {
        for (int i = 0; i < 3; i++) {
            roots[i].clearVisites();
        }
        for (int i = 0; i < parts.size(); i++) {
            parts.get(i).clearVisites();
        }

        for (int i = 0; i < 3; i++) {
            roots[i].checkConstraints(Expression.Zero());
        }

        for (int i = 0; i < parts.size(); i++) {
            parts.get(i).checkVisite(variables.toValues());
        }
    }

    /**************************************************************************/

    final void addPart() {
        parts.add(new Part(parts.size()));
    }

    final void delPart(int index) {
        parts.remove(index);
    }

    final void addVariable(String var, double defaultValue, double coefforigin) {
        variables.put(var, new Point2d(defaultValue, coefforigin));
    }

    final void delVariable(String var) {
        variables.remove(var);
    }

    final void addFaceConstr(int box1, String face1, int box2, String face2, Expression expr)  throws XMLException {
        faceConstr.add(new Constraint(parts, box1, face1, box2, face2, expr));
    }

    final void addFaceConstr(int box, String face, Expression expr) throws XMLException {
        faceConstr.add(new Constraint(parts, roots, box, face, expr));
    }

    final void delFaceConstr(int index) {
        faceConstr.remove(index);
    }

    final void addVarConstr(Expression expr) {
        varConstr.add(expr);
    }

    final void delVarConstr(int index) {
        varConstr.remove(index);
    }
    
    public Vector<Expression> getVarConstr() {
        return varConstr;
    }

    final void addSupportFace(FaceType ft) {
        suppFaces.add(ft);
    }

    /**************************************************************************/

    public String getName() {
        return name;
    }

    void setClassID(int id) {
        classID = id;
    }

    public int getClassID() {
        return classID;
    }

    public String getClassName() {
        return className;
    }

    public boolean hasClassName() {
        return className != null;
    }

    static public FaceType getFaceType(int i) {
        return FaceType.values()[i];
    }
    
    public Color3f getDefaultColor() {
        return defaultColor;
    }
    
    public Part getPart(int i) {
        return parts.get(i);
    }

    /**************************************************************************/

    /* Load library from file */
    static HashMap<String, ObjectManager> loadLibrary(Room room, File file) throws ObjectException {
        HashMap<String, ObjectManager> library = new HashMap<String, ObjectManager>();
        texturePath = (new File(file.getParent(), "textures")).getPath();
        double averageHeight;
        try {
            XMLNode root;
            try {
                root = XML.open(file);
            }
            catch (IOException e) {
                throw new ObjectException("Unable to open file " + file.getPath());                
            }
            int version = root.getIntegerAttribute("version");            
            switch(version) {
                case 1:
                  averageHeight = loadLibrary_1(root, library);
                  break;
            default: 
                throw new XMLException("Unknow version number '" + version + "'.");
            }
        }
        catch (XMLException e) {
            throw new ObjectException(e.getMessage());
        }

        room.setRoomHeight(averageHeight);

        if (!library.containsKey("floor")) {
            // Template for floor
            ObjectManager floor = new ObjectManager("floor", new Color3f(0.5f, 0.5f, 0.5f));
            floor.addPart();
            floor.addVariable("width", 1, 0.5);
            floor.addVariable("depth", 1, 0.5);
            try {
                floor.addFaceConstr(0, "TOP", 0, "BOTTOM", Expression.parse("-0.1"));
                floor.addFaceConstr(0, "TOP", Expression.parse("0"));                
                floor.addFaceConstr(0, "BACK", Expression.parse("-depth/2"));
                floor.addFaceConstr(0, "LEFT", Expression.parse("-width/2"));
                floor.addFaceConstr(0, "FRONT", Expression.parse("depth/2"));
                floor.addFaceConstr(0, "RIGHT", Expression.parse("width/2"));

                floor.checkConstraints();
                
                floor.flags = PRIVATE | STATIC | FLOOR;
                floor.className = "Floor";
                library.put(floor.name, floor);
            }
           catch (Exception e) {
                throw new ObjectException("Object 'floor': " + e.getMessage());
           }
        }
        else {
            throw new ObjectException("Object 'floor' is reserved, please remove it from library.");
        }        

        if (!library.containsKey("ceiling")) {
            // Template for ceiling
            ObjectManager ceiling = new ObjectManager("ceiling", new Color3f(0.25f, 0.25f, 0.25f));
            ceiling.addPart();
            ceiling.addVariable("width", 1, 0.5);
            ceiling.addVariable("depth", 1, 0.5);
            try {
                ceiling.addFaceConstr(0, "BOTTOM", 0, "TOP", Expression.parse("0.1"));
                ceiling.addFaceConstr(0, "BOTTOM", Expression.parse(Double.toString(averageHeight)));
                ceiling.addFaceConstr(0, "BACK", Expression.parse("-depth/2"));
                ceiling.addFaceConstr(0, "LEFT", Expression.parse("-width/2"));
                ceiling.addFaceConstr(0, "FRONT", Expression.parse("depth/2"));
                ceiling.addFaceConstr(0, "RIGHT", Expression.parse("width/2"));

                ceiling.checkConstraints();

                ceiling.flags = PRIVATE | STATIC;
                ceiling.className = "Ceiling";
                library.put(ceiling.name, ceiling);
            }
            catch (Exception e) {
                throw new ObjectException("Object 'ceiling': " + e.getMessage());
            }
        }
        else {
            throw new ObjectException("Object 'ceiling' is reserved, please remove it from library.");
        }

        if (!library.containsKey("wall")) {
            // Template for walls
            ObjectManager wall = new ObjectManager("wall", new Color3f(0.75f, 0.75f, 0.75f));
            wall.addPart();
            wall.addVariable("width", 1, 0.5);
            try {
                wall.addFaceConstr(0, "BOTTOM", 0, "TOP", Expression.parse(Double.toString(averageHeight)));
                wall.addFaceConstr(0, "FRONT", 0, "BACK", Expression.parse("-0.1"));
                wall.addFaceConstr(0, "BOTTOM", Expression.parse("0"));
                wall.addFaceConstr(0, "FRONT", Expression.parse("0"));
                wall.addFaceConstr(0, "LEFT", Expression.parse("-width/2"));
                wall.addFaceConstr(0, "RIGHT", Expression.parse("width/2"));
                wall.addVarConstr(Expression.parse("width > 0.3"));

                wall.checkConstraints();

                wall.flags = PRIVATE | WALL;
                wall.className = "Wall";
                library.put(wall.name, wall);
            }
            catch (Exception e) {
                throw new ObjectException("Object 'wall': " + e.getMessage());
            }           
        }
        else {
            throw new ObjectException("Object 'wall' is reserved, please remove it from library.");
        }

        if (!library.containsKey("Wall")) {
            // Template for walls
            ObjectManager wall = new ObjectManager("Wall", new Color3f(0.75f, 0.75f, 0.75f));
            wall.addPart();
            wall.addVariable("width", 1, 0.5);
            wall.addVariable("depth", 0.1, 0.5);
            try {
                wall.addFaceConstr(0, "BOTTOM", 0, "TOP", Expression.parse(Double.toString(averageHeight)));
                wall.addFaceConstr(0, "FRONT", Expression.parse("depth/2"));
                wall.addFaceConstr(0, "BACK", Expression.parse("-depth/2"));
                wall.addFaceConstr(0, "BOTTOM", Expression.parse("0"));
                wall.addFaceConstr(0, "LEFT", Expression.parse("-width/2"));
                wall.addFaceConstr(0, "RIGHT", Expression.parse("width/2"));
                wall.addVarConstr(Expression.parse("width > 0.05"));
                wall.addVarConstr(Expression.parse("depth > 0.05"));
                wall.addSupportFace(FaceType.BOTTOM);

                wall.checkConstraints();

                wall.flags = WALL;
                wall.className = "Wall";
                library.put(wall.name, wall);
            }
            catch (Exception e) {
                throw new ObjectException("Object 'Wall': " + e.getMessage());
            }
        }
        else {
            throw new ObjectException("Object 'Wall' is reserved, please remove it from library.");
        }

        if (!library.containsKey("Open Wall")) {
            // Template for walls
            ObjectManager owall = new ObjectManager("Open Wall", new Color3f(0.75f, 0.75f, 0.75f));
            owall.addPart();
            owall.addPart();
            owall.addPart();
            owall.addVariable("width", 2.8, 0.5);
            owall.addVariable("leftwidth", 1, 0);
            owall.addVariable("openheight", 2, 0);
            owall.addVariable("openwidth", 1, 0);
            owall.addVariable("thickness", 0.1, 0.5);
            try {
                owall.addFaceConstr(0, "FRONT", Expression.parse("thickness/2"));
                owall.addFaceConstr(0, "BACK", Expression.parse("-thickness/2"));
                owall.addFaceConstr(0, "BOTTOM", Expression.parse("0"));
                owall.addFaceConstr(0, "LEFT", Expression.parse("-width/2"));
                owall.addFaceConstr(0, "BOTTOM", 0, "TOP", Expression.parse(Double.toString(averageHeight)));
                owall.addFaceConstr(0, "LEFT", 0, "RIGHT", Expression.parse("leftwidth"));

                owall.addFaceConstr(1, "FRONT", Expression.parse("thickness/2"));
                owall.addFaceConstr(1, "BACK", Expression.parse("-thickness/2"));
                owall.addFaceConstr(1, "BOTTOM", Expression.parse("openheight"));
                owall.addFaceConstr(0, "RIGHT", 1, "LEFT", Expression.parse("0"));
                owall.addFaceConstr(1, "BOTTOM", 1, "TOP", Expression.parse(Double.toString(averageHeight) + "-openheight"));
                owall.addFaceConstr(1, "LEFT", 1, "RIGHT", Expression.parse("openwidth"));

                owall.addFaceConstr(2, "FRONT", Expression.parse("thickness/2"));
                owall.addFaceConstr(2, "BACK", Expression.parse("-thickness/2"));
                owall.addFaceConstr(2, "BOTTOM", Expression.parse("0"));
                owall.addFaceConstr(2, "RIGHT", Expression.parse("width/2"));
                owall.addFaceConstr(2, "BOTTOM", 2, "TOP", Expression.parse(Double.toString(averageHeight)));
                owall.addFaceConstr(1, "RIGHT", 2, "LEFT", Expression.parse("0"));

                owall.addVarConstr(Expression.parse("thickness > 0.03"));
                owall.addVarConstr(Expression.parse("openwidth > 0.3"));
                owall.addVarConstr(Expression.parse("leftwidth + openwidth < width"));
                owall.addVarConstr(Expression.parse("width > openwidth+0.1"));

                owall.addSupportFace(FaceType.BOTTOM);

                owall.checkConstraints();

                owall.flags = WALL;
                owall.className = "Wall";
                library.put(owall.name, owall);
            }
            catch (Exception e) {
                throw new ObjectException("Object 'Open Wall': " + e.getMessage());
            }
        }
        else {
            throw new ObjectException("Object 'Open Wall' is reserved, please remove it from library.");
        }
        
        return library;
    }

    private static double loadLibrary_1(XMLNode root, HashMap<String, ObjectManager> library) throws XMLException, ObjectException {
        double ah = root.getDoubleAttribute("roomheight");
        List<XMLNode> objects = root.getChildren("object");
        for (XMLNode object : objects) {
            ObjectManager o = new ObjectManager(object);
            if (library.containsKey(o.name)) {
                throw new XMLException("Duplicated object '" + o.name + "'");
            }
            library.put(o.name, o);
        }
        return ah;
    }
}
