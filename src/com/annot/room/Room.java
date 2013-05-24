/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.room;

import com.annot.room.ObjectInstance.ObjectFace;
import com.annot.room.ObjectManager.FaceType;
import com.annot.room.ObjectManager.ObjectException;
import com.annot.room.RefineVanishingPoints.Line;
import com.common.ClippedImage;
import com.common.FloatImage;
import com.common.J3DHelper.ImageWrapper;
import com.common.MyMatrix;
import com.common.MyVect;
import com.common.XML;
import com.common.XML.XMLDocument;
import com.common.XML.XMLException;
import com.common.XML.XMLNode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import javax.imageio.ImageIO;

/**
 *
 * @author vdelaitr
 */
public class Room {
    public static class NoSuchObjectException extends Exception {
        static final long serialVersionUID = 0L;
    }

    public final static int READ_ONLY = 1;
    int flags;

    /* Average root height: 3 foot = 2.7 meters */
    private double defaultHeight = 2.7f;

    /* Maximum image width: 600 pixels */
    private final int maximumSize = 600;

     /* Image to be annotated and its scale comparing to original image */
    private String fileName;
    private ClippedImage image;
    private double scale;
    private boolean hasImage;

    /* Annotations */
    private File annotsFile;

    /* Object libraby */
    protected HashMap<String, ObjectManager> objectLibrary;
    private HashMap<String, Integer> objectClasses;

    /* Room corners: x and y in [0, 1] */
    public static enum CornerType { TOPLEFT, TOPRIGHT, BOTTOMRIGHT, BOTTOMLEFT, DEPTHVP, PH};
    private MyVect[] corners;

    /* Vanishing point and room corner */
    private MyVect[] vp;
    private MyVect p0, pH, p1;   // lower origin corner, corresponding upper corner, opposite corner
    private RefineVanishingPoints refinedVP;

    /* Room parameters */
    protected RoomParameters params;
    private boolean roomOK;

    /* Point cloud from 3D data */
    private Vector<MyVect> cloud;
    
    /* Room floor, walls, ceiling */
    protected ObjectInstance floor;
    protected ObjectInstance[] walls;
    protected ObjectInstance ceiling;


    /**************************************************************************/

    public Room(File file) throws ObjectException {
        this(file, 0);
    }

    public Room(File file, int f) throws ObjectException {
        flags = f;
        
        // load library
        int nclasses = 0;
        objectClasses = new HashMap<String, Integer>();
        objectLibrary = ObjectManager.loadLibrary(this, file);
        
        //System.out.println(objectLibrary.size() + " objects loaded:");
        for (ObjectManager o : objectLibrary.values()) {
            if (o.hasClassName()) {
                if (!objectClasses.containsKey(o.getClassName())) {
                    objectClasses.put(o.getClassName(), nclasses);
                    nclasses++;
                }
                o.setClassID(objectClasses.get(o.getClassName()).intValue());
                //System.out.println(o.getName() + " (" + o.getClassName() + ")");
            }
            else {
                //System.out.println(o.getName() + " (no class)");
            }
        }

        /*
        System.out.println("\n" + objectClasses.size() + " classes loaded:");
        for (String c: objectClasses.keySet()) {
            System.out.println(c);
            int cID = objectClasses.get(c);
            for (ObjectManager o : objectLibrary.values()) {
                if (o.getClassID() == cID) {
                    System.out.println("--> " + o.getName());
                }
            }
        }

        System.out.println("\nObjects with no class:");
        for (ObjectManager o : objectLibrary.values()) {
            if (o.getClassID() == -1) {
                System.out.println("--> " + o.getName());
            }
        }
        */
        
        // set a new free room
        floor = null;
        ceiling = null;
        walls = null;
        clear();
    }

    /**************************************************************************/

    public boolean readOnly() {
        return (flags & READ_ONLY) != 0;
    }

    /**************************************************************************/

    public void load(File img, File annots) throws IOException, XMLException {
        loadImage(img);
        loadAnnots(annots);
    }

    /**************************************************************************/
    
    public void setRoomHeight(double ah) {
        defaultHeight = ah;
    }

    public double getRoomHeight() {
        return defaultHeight;
    }

    public boolean hasAttachedImage() {
        return hasImage;
    }

    /**************************************************************************/

    public void clear() {
        corners = new MyVect[6];
        vp = new MyVect[3];
        p0 = null;
        p1 = null;
        pH = null;

        params = null;
        roomOK = false;

        if (floor != null) {
            floor.clear();
        }
       
        floor = null;
        ceiling = null;
        walls = null;
        
        cloud = null;
    }

    /**************************************************************************/

    void loadImage(File file) throws IOException, XMLException {
        hasImage = true;
        fileName = file.getName();

        image = new ClippedImage(ImageIO.read(file));

        if (image.getWidth() > maximumSize) {
            scale = ((double)maximumSize) / image.getWidth();
            image.resize(scale);
        }
        else {
           scale = 1;
        }
    }

    /**************************************************************************/

    public Collection<ObjectManager> getObjects() {
        return objectLibrary.values();
    }

    public char[][] getClasses() {
        char[][] cs = new char[objectClasses.size()][];
        for (String c: objectClasses.keySet()) {
            cs[objectClasses.get(c)] = c.toCharArray();
        }
        return cs;
    }

    public int getImageMaximumSize() {
        return maximumSize;
    }

    public ObjectInstance newObject(String obj) throws NoSuchObjectException {
        if (objectLibrary.containsKey(obj)) {
            return new ObjectInstance(objectLibrary.get(obj));
        }
        else {
            throw new NoSuchObjectException();
        }
    }

    /**************************************************************************/

    public ClippedImage getImage() {
        return image;
    }

    public RefineVanishingPoints getRefinedVP() {
        return refinedVP;
    }

    public RoomParameters getParams() {
        return params;
    }

    public ObjectInstance getFloor() {
        return floor;
    }

    public ObjectInstance getCeiling() {
        return ceiling;
    }

    public ObjectInstance getWall(int i) {
        return walls[i];
    }
    
    public Vector<MyVect> getCloud() {
        return cloud;
    }

    /**************************************************************************/
    
    private boolean isCornerVisible(CornerType ct) {
        int i = ct.ordinal();
        return ((double)image.getXmin())/image.getWidth() <= corners[i].x && corners[i].x <= ((double)image.getXmax())/image.getWidth() &&
               ((double)image.getYmin())/image.getHeight() <= corners[i].y && corners[i].y <= ((double)image.getYmax())/image.getHeight();
    }

    /**************************************************************************/
    
    public void setCornerImage(CornerType ct, int x, int y) {
        double w = image.getWidth();
        double h = image.getHeight();
        int i = ct.ordinal();
        corners[i] = new MyVect(((double)x) / w, ((double)y) / h, 1);
    }

    /**************************************************************************/

    public MyVect getCornerImage(CornerType ct) {
        double w = image.getWidth();
        double h = image.getHeight();
        int i = ct.ordinal();
        return new MyVect(corners[i].x * w, corners[i].y * h, 1);
    }

    /**************************************************************************/

    private MyVect getCorner(CornerType ct) {
        double w = image.getWidth();
        double h = image.getHeight();
        int i = ct.ordinal();
        return image.imageToDirectCoord(new MyVect(corners[i].x * w, corners[i].y * h, 1));
    }

    /**************************************************************************/

    public boolean isSetCorners() {
        for (int i = 0; i < 6; i++) {
            if (corners[i] == null) {
                return false;
            }
        }
        return true;
    }

    public boolean isSetVP() {
        return vp[0] != null && vp[1] != null && vp[2] != null && p0 != null && p1 != null && pH != null;
    }

    public boolean isSetParams() {
        return params != null;

    }

    public boolean isSetRoom() {
        return roomOK && isSetParams();
    }

    /**************************************************************************/

    private void computeVP() {
        int horizontalVPID;
        Vector<Line> vanishingLines = new Vector<Line>();
        
        if (isCornerVisible(CornerType.BOTTOMLEFT)) {
            pH = getCorner(CornerType.PH);
            p0 = getCorner(CornerType.BOTTOMLEFT);            
            p1 = getCorner(CornerType.BOTTOMRIGHT);

            MyVect p = getCorner(CornerType.TOPLEFT);
            MyVect v = p.sub(p0);
            MyVect h = getCorner(CornerType.TOPRIGHT).sub(p);
            pH = MyVect.intersect(p0, v, pH, h);
            
            vp[0] = getCorner(CornerType.DEPTHVP);
            int count = 1;
            count += isCornerVisible(CornerType.TOPLEFT)     ? 1 : 0;
            count += isCornerVisible(CornerType.BOTTOMRIGHT) ? 1 : 0;
            count += isCornerVisible(CornerType.TOPRIGHT)    ? 1 : 0;
            if (count < 2) {
                vp[0].z = 0;
            }
            horizontalVPID = 1;
        }
        else {
            pH = getCorner(CornerType.PH);
            p0 = getCorner(CornerType.BOTTOMRIGHT);
            p1 = getCorner(CornerType.BOTTOMLEFT);

            MyVect p = getCorner(CornerType.TOPRIGHT);
            MyVect v = p.sub(p0);
            MyVect h = getCorner(CornerType.TOPLEFT).sub(p);
            pH = MyVect.intersect(p0, v, pH, h);

            vp[1] = getCorner(CornerType.DEPTHVP);
            int count = 0;
            count += isCornerVisible(CornerType.TOPLEFT)     ? 1 : 0;
            count += isCornerVisible(CornerType.BOTTOMRIGHT) ? 1 : 0;
            count += isCornerVisible(CornerType.TOPRIGHT)    ? 1 : 0;
            if (count < 2) {
                vp[1].z = 0;
            }
            horizontalVPID = 0;
        }        

        MyVect h1 = MyVect.sub(getCorner(CornerType.BOTTOMRIGHT), getCorner(CornerType.BOTTOMLEFT));
        MyVect h2 = MyVect.sub(getCorner(CornerType.TOPRIGHT), getCorner(CornerType.TOPLEFT));
        vp[horizontalVPID] = MyVect.intersect(getCorner(CornerType.BOTTOMLEFT), h1, getCorner(CornerType.TOPLEFT), h2);
        if ((!isCornerVisible(CornerType.BOTTOMRIGHT) && !isCornerVisible(CornerType.BOTTOMLEFT)) ||
            (!isCornerVisible(CornerType.TOPRIGHT) && !isCornerVisible(CornerType.TOPLEFT))) {
            vp[horizontalVPID].z = 0;
        }

        MyVect v1 = MyVect.sub(getCorner(CornerType.TOPRIGHT), getCorner(CornerType.BOTTOMRIGHT));
        MyVect v2 = MyVect.sub(getCorner(CornerType.TOPLEFT), getCorner(CornerType.BOTTOMLEFT));
        vp[2] = MyVect.intersect(getCorner(CornerType.BOTTOMRIGHT), v1, getCorner(CornerType.BOTTOMLEFT), v2);
        if ((!isCornerVisible(CornerType.TOPRIGHT) && !isCornerVisible(CornerType.BOTTOMRIGHT)) ||
            (!isCornerVisible(CornerType.TOPLEFT) && !isCornerVisible(CornerType.BOTTOMLEFT))) {
            vp[2].z = 0;
        }
        
        System.out.println("Vanishing points estimation from geometry:");
        System.out.format("X-axis: (%.2f, %.2f)\n", vp[0].x, vp[0].y);
        System.out.format("Y-axis: (%.2f, %.2f)\n", vp[1].x, vp[1].y);
        System.out.format("Z-axis: (%.2f, %.2f)\n", vp[2].x, vp[2].y);

        addLine(vanishingLines, CornerType.BOTTOMLEFT);
        addLine(vanishingLines, CornerType.TOPLEFT);
        addLine(vanishingLines, CornerType.TOPRIGHT);
        addLine(vanishingLines, CornerType.BOTTOMRIGHT);
        addLine(vanishingLines, CornerType.BOTTOMLEFT, CornerType.BOTTOMRIGHT);
        addLine(vanishingLines, CornerType.TOPLEFT, CornerType.TOPRIGHT);
        addLine(vanishingLines, CornerType.TOPRIGHT, CornerType.BOTTOMRIGHT);
        addLine(vanishingLines, CornerType.TOPLEFT, CornerType.BOTTOMLEFT);

        refinedVP = new RefineVanishingPoints(image, vp, vanishingLines);

        System.out.println("Vanishing points re-estimation from image edges:");
        System.out.format("X-axis: (%.2f, %.2f)\n", vp[0].x, vp[0].y);
        System.out.format("Y-axis: (%.2f, %.2f)\n", vp[1].x, vp[1].y);
        System.out.format("Z-axis: (%.2f, %.2f)\n", vp[2].x, vp[2].y);
    }

    /**************************************************************************/

    private void addLine(Vector<Line> vanishingLines, CornerType c1, CornerType c2) {
        if (isCornerVisible(c1) || isCornerVisible(c2)) {
            MyVect vc1 = getCornerImage(c1);
            MyVect vc2 = getCornerImage(c2);            
            vanishingLines.add(new Line(vc1, vc2));
        }
    }

    private void addLine(Vector<Line> vanishingLines, CornerType c) {
        if (isCornerVisible(c)) {
            MyVect vc = getCornerImage(c);
            MyVect vp = getCornerImage(CornerType.DEPTHVP);
            MyVect v = vc.sub(vp);
            v.mul(image.getClippedHeight() / 2 / v.norm());
            vanishingLines.add(new Line(vc, vc.add(v)));
        }
    }

    /**************************************************************************/

    public void computeRoomParameters(boolean sameFocalXY) {
        computeVP();
        
        boolean restore = params != null;
        double prev_depth = 0;
        double prev_width = 0;
        double prev_height = 0;

        if (restore) {
            prev_depth = params.depth;
            prev_width = params.width;
            prev_height = params.height;
        }

        params = new RoomParameters(image, vp, p0, pH, p1,
                                    isCornerVisible(CornerType.BOTTOMLEFT),
                                    isCornerVisible(CornerType.BOTTOMRIGHT),
                                    defaultHeight, sameFocalXY);
        if (restore) {
            params.depth = prev_depth;
            params.width = prev_width;
            params.height = prev_height;
        }

        System.out.println("Vanishing points re-estimation from geometric constraints:");
        displayRoomParameters();
    }
    
    /**************************************************************************/

    public void restoreSize() {
        params.depth = params.origdepth;
        params.width = params.origwidth;
        params.height = params.origheight;
    }

    /**************************************************************************/

    private void displayRoomParameters() {
        MyVect px = params.KR.getCol(0);
        MyVect py = params.KR.getCol(1);
        MyVect pz = params.KR.getCol(2);
        System.out.format("X-axis: (%.02f, %.02f)\n", px.x / px.z, px.y / px.z);
        System.out.format("Y-axis: (%.02f, %.02f)\n", py.x / py.z, py.y / py.z);
        System.out.format("Z-axis: (%.02f, %.02f)\n", pz.x / pz.z, pz.y / pz.z);

        System.out.format("Focal distance: fx = %.2f, fy = %.2f\n", params.fx, params.fy);
        System.out.format("Gamma = %.2f\n", params.g);
        System.out.format("Camera position: (x, y, z) = (%.2fm, %.2fm, %.2fm)\n", params.cameraPosition.x, params.cameraPosition.y, params.cameraPosition.z);
        System.out.format("Camera angle (az1, ay, az2) = (%.2frad, %.2frad, %.2frad)\n", params.cameraAngle.x, params.cameraAngle.y, params.cameraAngle.z);
        System.out.format("Room size (depth, width, height) = (%.2fm, %.2fm, %.2fm)\n", params.depth, params.width, params.height);
    }

    /**************************************************************************/

    public void build() {        
        if (floor != null) {
            floor.clear();
        }
        if (ceiling != null) {
            ceiling.clear();
        }
        if (walls != null) {
            for (int i = 0; i < 4; i++) {
                walls[i].clear();
            }
        }
        
        try {
            floor = newObject("floor");
            ceiling = newObject("ceiling");
            ceiling.attachTo(floor.getPart(0), FaceType.TOP);
            walls = new ObjectInstance[4];
            for (int i = 0; i < 4; i++) {
                walls[i] = newObject("wall");
                walls[i].attachTo(floor.getPart(0), FaceType.TOP);                
            }
        }
        catch (NoSuchObjectException e) {
            throw new RuntimeException(e.getMessage());
        }    
        
        walls[0].setAngle(ObjectInstance.ROT_90);                
        walls[1].setAngle(ObjectInstance.ROT_0);
        walls[2].setAngle(ObjectInstance.ROT_270);
        walls[3].setAngle(ObjectInstance.ROT_180);                        
   
        refresh();
    }

    public void refresh() {
        floor.setVariable("width", params.width);
        floor.setVariable("depth", params.depth);
        floor.setPosition(new MyVect(params.depth / 2, params.width / 2, 0));

        ceiling.setVariable("width", params.width);
        ceiling.setVariable("depth", params.depth);
        
        walls[0].setVariable("width", params.depth);
        walls[0].setFacePosition(-params.width/2, 0, ObjectInstance.ROT_90);
        walls[1].setVariable("width", params.width);
        walls[1].setFacePosition(0, -params.depth/2, ObjectInstance.ROT_0);
        walls[2].setVariable("width", params.depth);
        walls[2].setFacePosition(params.width/2, 0, ObjectInstance.ROT_270);
        walls[3].setVariable("width", params.width);
        walls[3].setFacePosition(0, params.depth/2, ObjectInstance.ROT_180);   
    }

    /**************************************************************************/

    public void loadEmptyRoom(double depth, double width, double height) {
        hasImage = false;
        image = new ClippedImage(maximumSize, maximumSize * 2 / 3);
        MyVect f = new MyVect(400, 400, 0);
        MyVect a = new MyVect(-Math.PI / 2, 0, 0);
        MyVect p = new MyVect(depth / 2, width / 2, 9);
        params = new RoomParameters(image, f, a, p, depth, width, height);

        displayRoomParameters();
    }

    /**************************************************************************/

    void loadAnnots(File f) throws XMLException {
        annotsFile = f;
        XMLNode root;
        try {
            root = XML.open(annotsFile);
        }
        catch (IOException e) {
            System.err.println("Unable to open file " + annotsFile.getPath());
            return;
        }
        int version = root.getIntegerAttribute("version");
        switch(version) {
            case 1:
                loadAnnots_1(root);
                break;
            default: {
                throw new XMLException("Unknow version number '" + version + "'.");
            }
        }
        if (isSetParams()) {
            displayRoomParameters();
        }
    }

    private void loadAnnots_1(XMLNode root) throws XMLException  {
        XMLNode node;
        try {
            node = root.getChild("image");
            image.setDistortion(node.getDoubleAttribute("distortion"));
            image.setClipping(node.getIntegerAttribute("xmin"),
                              node.getIntegerAttribute("ymin"),
                              node.getIntegerAttribute("xmax"),
                              node.getIntegerAttribute("ymax"));
        }
        catch (XMLException e) {
        }
        
        try {
            node = root.getChild("corners");
            load_corner(node, CornerType.TOPLEFT);
            load_corner(node, CornerType.TOPRIGHT);
            load_corner(node, CornerType.BOTTOMRIGHT);
            load_corner(node, CornerType.BOTTOMLEFT);
            load_corner(node, CornerType.DEPTHVP);
            try {
                load_corner(node, CornerType.PH);
            }
            catch(XMLException e) {
                if (isCornerVisible(CornerType.BOTTOMLEFT)) {
                    corners[CornerType.PH.ordinal()] = new MyVect(corners[CornerType.TOPLEFT.ordinal()]);
                }
                else {
                    corners[CornerType.PH.ordinal()] = new MyVect(corners[CornerType.TOPRIGHT.ordinal()]);
                }
            }            
        }
        catch (XMLException e) {
        }
        
        try {
            node = root.getChild("vps");                
            vp[0] = node.getChild("vpx").toVect();
            vp[1] = node.getChild("vpy").toVect();
            vp[2] = node.getChild("vpz").toVect();                        
            p0 = node.getChild("p0").toVect();
            p1 = node.getChild("p1").toVect();
            if (node.hasChild("pH")) {
                pH = node.getChild("pH").toVect();    
            }
            else if(isSetCorners()) {
                pH = new MyVect(corners[CornerType.PH.ordinal()]);
            }
        }
        catch (XMLException e) {
        }
                
        try {
            node = root.getChild("pointcloud");
            cloud = new Vector<MyVect>();
            for (XMLNode n : node.getChildren("point")) {
                cloud.add(n.toVect());
            }
        }
        catch (XMLException e) {
            cloud = null;
        }

        try {
            if (isSetVP()) {
                node = root.getChild("params");
                MyVect p = node.getChild("camPosition").toVect();
                MyVect f = node.getChild("camFocal").toVect();
                MyVect a = node.getChild("camAngle").toVect();
                f = f.mul(1. / (image.getWidth() / 2));
                params = new RoomParameters(image, f, a, p, p1,
                                        isCornerVisible(CornerType.BOTTOMLEFT),
                                        isCornerVisible(CornerType.BOTTOMRIGHT),
                                        defaultHeight);
            }
            else {
                throw new XMLException("trying new format");
            }
        }
        catch (XMLException e1) {            
            try {
                node = root.getChild("camParams");
                MyMatrix K = node.getChild("K").toMatrix();
                MyMatrix R = node.getChild("R").toMatrix();
                MyVect t = node.getChild("t").toVect();
                params = new RoomParameters(image, K, R, t, cloud, defaultHeight);
            }
            catch (XMLException e2) {
                return;
            }
        }
        
        try {
            node = root.getChild("room");
            roomOK = true;
        }
        catch (XMLException e) {
            roomOK = false;
            return;
        }
        MyVect dims = node.getChild("dimensions").toVect();
        params.depth = dims.x;
        params.width = dims.y;
        params.height = dims.z;

        build();

        XMLNode floorNode = node.getChild("floor");
        floor.fromXML(this, floorNode);
        XMLNode ceilingNode = node.getChild("ceiling");
        ceiling.fromXML(this, ceilingNode);
        for (int i = 0; i < 4; i++) {
            XMLNode wallNode = node.getChild("wall"+i);
            walls[i].fromXML(this, wallNode);
        } 
    }

    private void load_corner(XMLNode root, CornerType type) throws XMLException {
        int i = type.ordinal();
        XMLNode c = root.getChild(type.toString().toLowerCase());
        corners[i]= new MyVect(c.getChild("x").getDoubleContent(),
                             c.getChild("y").getDoubleContent());
    }

    /**************************************************************************/

    public void saveAnnots() {
        XMLDocument doc = new XMLDocument();

        XMLNode annot = new XMLNode("annotations");
        annot.setAttribute("version", "1");
        doc.setRootElement(annot);

        XMLNode imageNode = new XMLNode("image");
        imageNode.setAttribute("width",  image.getWidth());
        imageNode.setAttribute("height", image.getHeight());
        imageNode.setAttribute("xmin", image.getXmin());
        imageNode.setAttribute("xmax", image.getXmax());
        imageNode.setAttribute("ymin", image.getYmin());
        imageNode.setAttribute("ymax", image.getYmax());
        imageNode.setAttribute("scale", scale);
        imageNode.setAttribute("distortion", image.getDistortion());
        imageNode.addContent(fileName);
        annot.addContent(imageNode);

        if (isSetCorners()) {
            System.out.println("Corners saved.");
            XMLNode cornersNode = new XMLNode("corners");
            cornersNode.addContent(save_corner(CornerType.TOPLEFT));
            cornersNode.addContent(save_corner(CornerType.TOPRIGHT));
            cornersNode.addContent(save_corner(CornerType.BOTTOMRIGHT));
            cornersNode.addContent(save_corner(CornerType.BOTTOMLEFT));
            cornersNode.addContent(save_corner(CornerType.DEPTHVP));
            cornersNode.addContent(save_corner(CornerType.PH));
            annot.addContent(cornersNode);
        }

        if (isSetVP()) {
            System.out.println("Vanishing points saved.");
            XMLNode vpNode = new XMLNode("vps");
            vpNode.addContent(XMLNode.fromVect("vpx", vp[0]));
            vpNode.addContent(XMLNode.fromVect("vpy", vp[1]));
            vpNode.addContent(XMLNode.fromVect("vpz", vp[2]));            
            vpNode.addContent(XMLNode.fromVect("p0", p0));
            vpNode.addContent(XMLNode.fromVect("p1", p1));
            vpNode.addContent(XMLNode.fromVect("pH", pH));
            annot.addContent(vpNode);
        }
        
        if (cloud != null) {
            XMLNode cloudNode = new XMLNode("pointcloud");                     
            for (MyVect p : cloud) {
                cloudNode.addContent(XMLNode.fromVect("point", p));
            }
            annot.addContent(cloudNode);
        }
        
        if (isSetParams()) {
            System.out.println("Geometry saved.");
            XMLNode paramsNode = new XMLNode("params");                                                            
            paramsNode.addContent(XMLNode.fromMatrix("K", params.K));
            paramsNode.addContent(XMLNode.fromMatrix("R", params.R));
            paramsNode.addContent(XMLNode.fromVect("t", params.t));
            annot.addContent(paramsNode);

            XMLNode roomNode = save_room();
            annot.addContent(roomNode);

            XMLNode floorNode = new XMLNode("floor");
            floor.toXML(floorNode);
            roomNode.addContent(floorNode);

            XMLNode ceilingNode = new XMLNode("ceiling");
            ceiling.toXML(ceilingNode);
            roomNode.addContent(ceilingNode);

            for (int i = 0; i < 4; i++) {
                XMLNode wallNode = new XMLNode("wall" + i);
                walls[i].toXML(wallNode);
                roomNode.addContent(wallNode);
            }
        }
        
        doc.save(annotsFile);
    }

    private XMLNode save_corner(CornerType ct) {
        int i = ct.ordinal();
        XMLNode e = new XMLNode(ct.toString().toLowerCase());
        XMLNode x = new XMLNode("x");
        XMLNode y = new XMLNode("y");
        x.addContent(String.valueOf(corners[i].x));
        y.addContent(String.valueOf(corners[i].y));
        e.addContent(x);
        e.addContent(y);
        return e;
    }

    private XMLNode save_room() {
        XMLNode roomNode = new XMLNode("room");
        XMLNode orig = XMLNode.fromVect("origin", params.origin);
        XMLNode dims = XMLNode.fromVect("dimensions", new MyVect(params.depth, 
                                                               params.width,
                                                               params.height));
        roomNode.addContent(orig);
        roomNode.addContent(dims);
        return roomNode;
    }
    
    // Returns a depth map in meter for camera center    
    public ImageWrapper getDepthMap(boolean ignoreNoClass) {
        List<ObjectFace> l = getVisibleFaces(ignoreNoClass);

        int w = image.getWidth();
        int h = image.getHeight();
        FloatImage depth = new FloatImage(w, h);
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                depth.set(x, y, Double.POSITIVE_INFINITY);
            }
        }
        
        for (ObjectFace f : l) {
            Rectangle r = f.poly.getBounds();
            for (int x = Math.max(0, r.x); x < Math.min(w, r.x + r.width); ++x) {
                for (int y = Math.max(0, r.y); y < Math.min(h, r.y + r.height); ++y) {
                    if (f.poly.contains(x, y)) {
                        MyVect ray = params.invK.mul(image.imageToDirectCoord(new MyVect(x, y, 1)));
                        MyVect p = f.intersect(ray);
                        if (p != null && -p.z < depth.get(x, y)) {
                            depth.set(x, y, -p.z);
                        }
                    }
                }
            }
        }
        
        return new ImageWrapper(depth);
    }
    
    // Returns an image with R channel classID, green channel boxID, blue channel FaceID        
    // Returns a depth map in meter for camera center    
    public ImageWrapper getLabelMap(boolean ignoreNoClass) {
        List<ObjectFace> l = getVisibleFaces(ignoreNoClass);

        int w = image.getWidth();
        int h = image.getHeight();
        FloatImage depth = new FloatImage(w, h);
        BufferedImage labels = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);        
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                depth.set(x, y, Double.POSITIVE_INFINITY);
            }
        }
        
        for (ObjectFace f : l) {
            Rectangle r = f.poly.getBounds();
            for (int x = Math.max(0, r.x); x < Math.min(w, r.x + r.width); ++x) {
                for (int y = Math.max(0, r.y); y < Math.min(h, r.y + r.height); ++y) {
                    if (f.poly.contains(x, y)) {
                        MyVect ray = params.invK.mul(image.imageToDirectCoord(new MyVect(x, y, 1)));
                        MyVect p = f.intersect(ray);
                        if (p != null && -p.z < depth.get(x, y)) {
                            depth.set(x, y, -p.z);
                            Color c = new Color(f.classID, f.boxID, f.faceID);
                            labels.setRGB(x, y, c.getRGB());
                        }
                    }
                }
            }
        }
        
        return new ImageWrapper(labels);
    }
    
    public List<ObjectFace> getVisibleFaces(boolean ignoreNoClass) {
        List<ObjectFace> l = new LinkedList<ObjectFace>();
        
        getVisibleFaces(floor, l);
        getVisibleFaces(ceiling, l);
        for (int i = 0; i < 4; i++) {
            getVisibleFaces(walls[i], l);
        }
        
        if (ignoreNoClass) {
            List<ObjectFace> l2 = new LinkedList<ObjectFace>();    
            for (ObjectFace f : l) {
                if (f.classID != -1) {
                    l2.add(f);
                }
            }
            l = l2;
        }
        
        return l;
    }   
    
    public void getVisibleFaces(ObjectInstance o, List<ObjectFace> l) {
        o.getVisibleFaces(this, l);
        
        for (int i = 0; i < o.parts.length; i++) {
            for (int j = 0; j < 6; j++) {
                List<ObjectInstance> lo = o.getPart(i).getFace(FaceType.values()[j]).getChildren();                
                for (ObjectInstance child : lo) {
                    if (!child.isPrivate()) {
                        getVisibleFaces(child, l);
                    }
                }
            }
        }
    }
}

