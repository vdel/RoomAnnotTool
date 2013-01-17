/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.annot.gui;

import Jama.Matrix;
import com.annot.gui.MyBox3D.J3DBox;
import com.annot.room.ObjectInstance.NoSupportFaceException;
import com.annot.room.ObjectManager.FaceType;
import com.annot.room.Room.NoSuchObjectException;
import com.annot.room.RoomParameters;
import com.pose.Pose3DVisu;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewInfo;
import com.sun.j3d.utils.universe.ViewingPlatform;
import com.common.J3DHelper;
import com.common.MyMatrix;
import com.common.MyVect;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import java.awt.Color;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.media.j3d.ViewPlatform;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

/**
 *
 * @author vdelaitr
 */
public class MyCanvas3D extends Canvas3D implements DropTargetListener {

    public interface MyCanvas3DListener {
        void setModified(ObjectInstance3D object);
        void objectSelected(ObjectInstance3D object);
        void objectDeselected(ObjectInstance3D object);
    }
    
    static final long serialVersionUID = 0L;
    private Room3D room;
    private SimpleUniverse universe;
    private BranchGroup rootGroup;
    private TransformGroup rootTransform;
    private TransformGroup floorTransform;   
    private BranchGroup cloudGroup;
    private boolean cloudVisible;
    private MyBehavior behavior;
    private View view;
    private ViewInfo viewInfo;
    private MyPanel panel;
    private PickCanvas pickCanvas;
    private ObjectInstance3D object;
    private Shape3D attachedShape;
    private ConstrainedMotion motion;
    private LinkedList<MyCanvas3DListener> listeners;
    private LinkedList<BranchGroup> poses;

    static public class MyPickResult {

        MyBox3D box;
        Shape3D shape;

        MyPickResult(MyBox3D b, Shape3D s) {
            box = b;
            shape = s;
        }

        static MyPickResult pick(PickCanvas pc, ObjectInstance3D currentObject, int x, int y) {
            try {
                pc.setShapeLocation(x, y);
                PickResult[] result = pc.pickAllSorted();

                if (result != null) {
                    for (int i = 0; i < result.length; i++) {
                        Node n = result[i].getNode(PickResult.PRIMITIVE);
                        if (n.getClass().isAssignableFrom(J3DBox.class)) {
                            MyBox3D b = ((J3DBox)n).me;
                            Shape3D s = (Shape3D) result[i].getNode(PickResult.SHAPE3D);
                            if (b != null && s != null) {
                                ObjectInstance3D o = b.getParentObject();
                                if (o != currentObject) {
                                    return new MyPickResult(b, s);
                                }
                            }
                        }
                    }
                    return null;
                } else {
                    return null;
                }

            } catch (NullPointerException e) {
                return null;
            }
        }
    };

    public MyCanvas3D(Room3D r, MyPanel p) {
        super(SimpleUniverse.getPreferredConfiguration());
        room = r;
        panel = p;

        attachedShape = null;
        motion = null;

        listeners = new LinkedList<MyCanvas3DListener>();
        poses = new LinkedList<BranchGroup>();

        // initialize universe
        createUniverse();

        // Set mouse and keyboard
        createBehaviourInteractors(room);

        // Centrate floor center on origin
        Transform3D t = new Transform3D();
        t.setTranslation(new MyVect(-room.getParams().depth / 2, 
                                    -room.getParams().width / 2, 0));
        floorTransform.setTransform(t);
        room.getFloor().branchGroupAttach(floorTransform);
        
        // Set up point cloud
        if (room.getCloud() != null) {            
            RoomParameters params = room.getParams();
            Vector<MyVect> cloud = room.getCloud();
            BufferedImage image = room.getImage().getImage();
            
            for (int i = 0; i < room.getCloud().size(); i += 16) {
                MyVect vpos = params.K.mul(params.R.mul(cloud.get(i)).add(params.t));
                Point pos = room.getImage().directToImageCoord(vpos).toPoint();
                Color3f color;
                if (0 <= pos.x && pos.x < room.getImage().getWidth() &&
                    0 <= pos.y && pos.y < room.getImage().getHeight()) {
                    color = new Color3f(new Color(image.getRGB(pos.x, pos.y)));
                }
                else {
                    color = new Color3f(1.f, 1.f, 1.f);
                }                
                t = new Transform3D();
                t.setTranslation(cloud.get(i));
                TransformGroup tg = J3DHelper.newTransformGroup(t);
                tg.addChild(new Sphere(0.025f, Primitive.GENERATE_NORMALS, 
                                       4, J3DHelper.getDefaultAppearance(color)));
                cloudGroup.addChild(tg);
            }
        }
        floorTransform.addChild(cloudGroup);
        cloudVisible = true;
        
        // Attach to universe
        universe.addBranchGraph(rootGroup);        
    }

    public void close() {
        super.stopRenderer();
        behavior.clear();
        universe.getViewingPlatform().detach();
        rootGroup.detach();

        rootGroup = null;
        rootTransform = null;
        floorTransform = null;
        cloudGroup = null;
        behavior = null;
        view = null;
        viewInfo = null;
        panel = null;
        pickCanvas = null;
        if (object != null) {
            object.clear();
        }
        object = null;
        attachedShape = null;
        motion = null;
        listeners = null;
        poses = null;

        universe.cleanup();
        universe = null;
    }

    private void createUniverse() {
        universe = new SimpleUniverse(this);
        view = universe.getViewer().getView();
        //view.setCompatibilityModeEnable(true);        
        viewInfo = new ViewInfo(view, ViewInfo.PLATFORM_AUTO_UPDATE
                | ViewInfo.CANVAS_AUTO_UPDATE);

        if (view.getCompatibilityModeEnable()) {
            Transform3D t = new Transform3D();
            viewInfo.getViewPlatformToEye(this, t, null);
            view.setVpcToEc(t);
        }

        ViewingPlatform viewingPlatform = universe.getViewingPlatform();
        ViewPlatform vp = viewingPlatform.getViewPlatform();
        viewingPlatform.detach();
        vp.setCapability(ViewPlatform.ALLOW_POLICY_READ);
        vp.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
        universe.addBranchGraph(viewingPlatform);

        rootGroup = J3DHelper.newBranchGroup();
        rootTransform = J3DHelper.newTransformGroup();        
        floorTransform = J3DHelper.newTransformGroup();
        cloudGroup = J3DHelper.newBranchGroup();
        rootGroup.addChild(rootTransform);
        rootTransform.addChild(floorTransform);
        
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY);

        // Light 1
        DirectionalLight light = new DirectionalLight(new Color3f(1f, 1f, 1f), new Vector3f(-4.0f, -7.0f, -12.0f));
        light.setInfluencingBounds(bounds);
        floorTransform.addChild(light);

        // Light 2
        light = new DirectionalLight(new Color3f(1f, 1f, 1f), new Vector3f(8.0f, 9.0f, 2.0f));
        light.setInfluencingBounds(bounds);
        floorTransform.addChild(light);

        // Background
        Background back = new Background(0.57f, 0.66f, 1f);
        back.setApplicationBounds(bounds);
        rootGroup.addChild(back);
    }

    private void createBehaviourInteractors(Room3D room) {
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY);
        behavior = new MyBehavior(room, this, rootTransform);
        behavior.setSchedulingBounds(bounds);
        rootGroup.addChild(behavior);
    }
    
    public void togglePointCloud() {
        if (cloudVisible) {
            cloudGroup.detach();
            cloudVisible = false;
        }
        else {
            floorTransform.addChild(cloudGroup);
            cloudVisible = true;
        }            
    }

    public void setPickTool() {
        pickCanvas = new PickCanvas(this, room.getFloor().getBranchGroup());
        pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);
        pickCanvas.setTolerance(0f);
    }

    public void addMyCanvas3DListener(MyCanvas3DListener l) {
        listeners.add(l);
    }

    public void removeMyCanvas3DListener(MyCanvas3DListener l) {
        listeners.remove(l);
    }

    void setModified(ObjectInstance3D object) {
        if (panel != null) { // panel may be null for 3D visualization only
            object.drawOnPanel(panel, room);
        }
        for (MyCanvas3DListener l : listeners) {
            l.setModified(object);
        }
    }

    void objectSelected(ObjectInstance3D object) {
        if (panel != null) { // panel may be null for 3D visualization only
            object.drawOnPanel(panel, room);
        }
        if (!object.isPrivate()) {
            for (MyCanvas3DListener l : listeners) {
                l.objectSelected(object);
            }
        }
    }

    void objectDeselected(ObjectInstance3D object) {
        if (panel != null) { // panel may be null for 3D visualization only
            panel.clearLines();
            panel.repaint();
        }
        for (MyCanvas3DListener l : listeners) {
            l.objectDeselected(object);
        }
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        if (behavior.selectedBox != null) {
            behavior.deselect();
        }
        try {
            if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String name = (String) dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
                try {
                    object = room.newObject(name);
                    object.select();
                } catch (NoSuchObjectException e) {
                    dtde.rejectDrag();
                }
            } else {
                dtde.rejectDrag();
            }
        } catch (IOException e) {
            dtde.rejectDrag();
        } catch (UnsupportedFlavorException e) {
            dtde.rejectDrag();
        }
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        detach();
        object.clear();
        object = null;
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        MyPickResult r = pick(dtde.getLocation().x, dtde.getLocation().y);
        if (r == null) {
            dtde.rejectDrag();
            detach();
        } else if (attachedShape == null || attachedShape != r.shape) {
            FaceType ft = r.box.getFaceType(r.shape);
            if ((r.box.getParentObject().isFloor() && ft != FaceType.TOP)
                || (r.box.getParentObject().isWall() && 
                    r.box.getParentObject().isPrivate() && 
                    ft != FaceType.FRONT) 
                || (r.box.getParentObject().isWall() && 
                    !r.box.getParentObject().isPrivate() &&
                    (ft == FaceType.TOP || ft == FaceType.BOTTOM))
                || (r.box.getParentObject().isCeiling() && 
                    ft != FaceType.BOTTOM)) {
                dtde.rejectDrag();
                detach();
            } else {
                try {
                    FaceType suppf = object.getSupportForAttach(ft);

                    dtde.acceptDrag(dtde.getDropAction());
                    MyVect ray = getRay(dtde.getLocation().x, dtde.getLocation().y);
                    MyVect p = r.box.getIntersection(ft, behavior.getCameraPos(), ray, true);
                    if (attachedShape != r.shape) {
                        object.detach();
                    }
                    attachedShape = r.shape;
                    object.attachTo(r.box, ft);
                    object.setFacePosition(p.x, p.y);
                    p = r.box.getIntersection(ft, behavior.getCameraPos(), ray);
                    motion = object.getMotion(p, null);  // 2nd argument won't be used
                } catch (NoSupportFaceException e) {
                    dtde.rejectDrag();
                    detach();
                }
            }
        } else if (motion != null) {
            MyVect ray = getRay(dtde.getLocation().x, dtde.getLocation().y);
            motion.update(behavior.getCameraPos(), ray);
            if (panel != null) { // panel may be null for 3D visualization only
                object.drawOnPanel(panel, room);
            }
        }
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        if (object != null) {
            setModified(object);
            requestFocus();
        } else if (panel != null) { // panel may be null for 3D visualization only
            panel.clearLines();
            panel.repaint();
        }
        object = null;
        motion = null;
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        detach();
    }

    MyPickResult pick(int x, int y) {
        return MyPickResult.pick(pickCanvas, object, x, y);
    }

    void detach() {
        if (attachedShape != null) {
            object.detach();
            if (panel != null) { // panel may be null for 3D visualization only
                panel.clearLines();
                panel.repaint();
            }
            attachedShape = null;
            motion = null;
        }
    }

    public void removeSelectedObject() {
        behavior.removeSelectedObject();
    }

    MyVect getRay(int x, int y) {
        Point3d eyePos = new Point3d();
        Point3d mousePos = new Point3d();
        getCenterEyeInImagePlate(eyePos);
        getPixelLocationInImagePlate(x, y, mousePos);

        Transform3D screen2cam = new Transform3D();
        getImagePlateToVworld(screen2cam);

        MyVect v = new MyVect();
        v.sub(mousePos, eyePos);

        MyMatrix rot = new MyMatrix();
        J3DHelper.extractRotTrans(screen2cam, rot, null, null);
        v = behavior.getInvR().mul(rot.mul(v));

        return v;
    }

    public void setDefaultView(RoomParameters params) {
        Transform3D t = new Transform3D();
        double fovx = 1 / room.getParams().fx;
        double aspect = ((double) room.getImage().getWidth()) / room.getImage().getHeight();
        double znear = 0.001;
        double zfar = 1000;
        double alpha = (zfar - znear) / (2 * znear * zfar);
        double beta = (zfar + znear) / (2 * znear * zfar);

        view.setFieldOfView(Math.atan(fovx) * 2);
        view.setFrontClipDistance(znear);
        view.setBackClipDistance(zfar);

        if (view.getCompatibilityModeEnable()) {
            Matrix P = new Matrix(4, 4);
            P.set(0, 0, fovx);
            P.set(1, 1, fovx / aspect * room.getParams().fx / room.getParams().fy);
            //P.set(0, 1, f * room.params.g);
            P.set(2, 2, 1);
            P.set(3, 2, alpha);
            P.set(3, 3, beta);
            P = P.inverse();
            t.set(P.getRowPackedCopy());
            view.setLeftProjection(t);
            view.setRightProjection(t);

            double[] c = new double[16];
            viewInfo.getEyeToImagePlate(this, t, null);
            t.get(c);
            setLeftManualEyeInImagePlate(new Point3d(c[3], c[7], c[11]));
            setRightManualEyeInImagePlate(new Point3d(c[3], c[7], c[11]));
        }

        behavior.setRotation(params.cameraAngle);
        behavior.setCameraPos(params.cameraPosition);
    }

    public MyBehavior getBehavior() {
        return behavior;
    }

    public void addPose(double[] pose) {
        Pose3DVisu p = new Pose3DVisu(pose);
        floorTransform.addChild(p.getBranchGroup());
        poses.add(p.getBranchGroup());
    }

    public void clearPoses() {
        for (BranchGroup bg : poses) {
            bg.detach();
        }
        poses.clear();
    }
}