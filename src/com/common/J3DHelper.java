/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.behaviors.vp.ViewPlatformBehavior;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewInfo;
import com.sun.j3d.utils.universe.ViewingPlatform;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.lang.reflect.Field;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

/**
 *
 * @author vdelaitr
 */
public class J3DHelper {
    public static BranchGroup newBranchGroup() {
        BranchGroup newbg = new BranchGroup();
        newbg.setCapability(BranchGroup.ALLOW_DETACH);
        newbg.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        newbg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        newbg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        return newbg;
    }

    public static TransformGroup newTransformGroup(Transform3D t) {
        TransformGroup newtg = newTransformGroup();
        newtg.setTransform(t);
        return newtg;
    }
    
    public static TransformGroup newTransformGroup() {
        TransformGroup newtg = new TransformGroup();
        newtg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        newtg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        newtg.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        newtg.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        newtg.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        return newtg;
    }
    
    public static Appearance newAppearance() {
        Appearance ap = new Appearance();
        ap.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_READ);
        ap.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_WRITE);
        ap.setCapability(Appearance.ALLOW_TEXTURE_READ);
        ap.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
        ap.setCapability(Appearance.ALLOW_MATERIAL_READ);
        ap.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        ap.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        ap.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
        return ap;
    }

    public static Appearance getDefaultAppearance(Color3f color) {
        Appearance ap = newAppearance();
        setAppearance(ap, color, null);
        return ap;
    }
    
    public static void setAppearance(Appearance ap, Color3f color, BufferedImage textureImg) {
        Color3f black = new Color3f(0f, 0f, 0f);

        Texture texture;
        TextureAttributes texAttr = new TextureAttributes();
        if (textureImg != null) {
            TextureLoader loader = new TextureLoader(textureImg, "RGBA",
                                       TextureLoader.ALLOW_NON_POWER_OF_TWO);
            texture = loader.getTexture();
            texture.setBoundaryModeS(Texture.WRAP);
            texture.setBoundaryModeT(Texture.WRAP);
            texAttr.setTextureMode(TextureAttributes.REPLACE);
            texture.setBoundaryColor(new Color4f(1f, 1f, 1f, 0f));
            ap.setTextureAttributes(texAttr);
            ap.setTexture(texture);
        }
        
        ap.setMaterial(new Material(color, black, color, black, 70));
       
/*
        ap.setColoringAttributes(new ColoringAttributes(color,
                                     ColoringAttributes.NICEST));
 */
    }
    
    public static double extractRotTrans(Transform3D t, MyMatrix rot, MyVect trans, MyVect persp) {
        double[] c = new double[16];
        t.get(c);
        if (rot != null) {
            t.getRotationScale(rot);
        }
        if (trans != null) {
            trans.x = c[3];
            trans.y = c[7];
            trans.z = c[11];
        }
        if (persp != null) {
            persp.x = c[12];
            persp.y = c[13];
            persp.z = c[14];
        }
        return c[15];
    }
    
    public static class SimpleCanvas3D extends Canvas3D {
        protected SimpleUniverse universe;
        protected BranchGroup rootGroup;
        protected TransformGroup rootTransform;
        protected BoundingSphere defaultBound;
        protected ViewPlatformBehavior behavior;
        protected View view;
        protected ViewInfo viewInfo;   
        protected ViewingPlatform viewingPlatform;
        
        public SimpleCanvas3D() {
            this(SimpleUniverse.getPreferredConfiguration());
        }
    
        public SimpleCanvas3D(GraphicsConfiguration config) {
            super(config);
            
            // initialize universe
            createUniverse();

            // Set mouse and keyboard
            behavior = new OrbitBehavior(this, OrbitBehavior.REVERSE_ALL);
            behavior.setSchedulingBounds(defaultBound);
            viewingPlatform.setViewPlatformBehavior(behavior);   
            
            // Set view
            viewingPlatform.setNominalViewingTransform();
        }
        
        public void close() {
            super.stopRenderer();            
            rootGroup.detach();
            universe.getViewingPlatform().detach();            

            rootGroup = null;
            rootTransform = null;
            behavior = null;
            view = null;
            viewInfo = null;
            
            universe.cleanup();
            universe = null;
        }        
        
        private void createUniverse() {
            universe = new SimpleUniverse(this);
            viewingPlatform = universe.getViewingPlatform();
            
            rootGroup = J3DHelper.newBranchGroup();
            rootTransform = J3DHelper.newTransformGroup();
            rootGroup.addChild(rootTransform);

            defaultBound = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY);
            
            // Light 1
            DirectionalLight light = new DirectionalLight(new Color3f(1f, 1f, 1f), new Vector3f(-4.0f, -7.0f, -12.0f));
            light.setInfluencingBounds(defaultBound);
            rootTransform.addChild(light);

            // Light 2
            light = new DirectionalLight(new Color3f(1f, 1f, 1f), new Vector3f(8.0f, 9.0f, 2.0f));
            light.setInfluencingBounds(defaultBound);
            rootTransform.addChild(light);

            // Background
            Background back = new Background(1f, 1f, 1f);
            back.setApplicationBounds(defaultBound);
            rootGroup.addChild(back);
        }
        
        public TransformGroup getTransformGroup() {
            return rootTransform;
        }
    }
    
    /**************************************************************************/
    
    public static class SimpleFrame extends Frame {
        static final long serialVersionUID = 0L;

        /* J3D variables */
        protected SimpleCanvas3D canvas;

        public SimpleFrame(int width, int height) {
            super();

            // Create Window
            setSize(width, height);                               

            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    close();
                }
            });

            // Add components
            setLayout(new BorderLayout());

            canvas = new SimpleCanvas3D();
            canvas.setPreferredSize(new Dimension(width, height));
            add(canvas, BorderLayout.CENTER);

            pack();
        }
        
        public void setVisible() {
            canvas.universe.addBranchGraph(canvas.rootGroup);   
            setVisible(true);
        }
        
        public void setInvisible() {
            setVisible(false);
            canvas.rootGroup.detach();
        }        
        
        public void close() {
            canvas.close();

            canvas = null;
            dispose();
        }

        public SimpleCanvas3D getCanvas() {
            return canvas;
        }
        
        public ImageWrapper screenCopy() {
            return new ImageWrapper(canvas);
        }
    }
        
    /**************************************************************************/
    
    public static class ImageWrapper {
        public int width;
        public int height;
        public double[] pixels;
        
        private void img2array(BufferedImage img) {
            DataBuffer buff = img.getData().getDataBuffer();
            assert(buff.getNumBanks() == 1);
            
            Color color;
            int row, col, pix;
            pixels = new double[3 * buff.getSize()];                        
            width = img.getWidth();
            height = img.getHeight();
            for (int i = 0; i < buff.getSize(); i++) {
                color = new Color(buff.getElem(i));
                // Matlab format
                row = i / width;
                col = i % width;
                pix = col * height + row;
                pixels[pix + 0 * buff.getSize()] = color.getRed();
                pixels[pix + 1 * buff.getSize()] = color.getGreen();
                pixels[pix + 2 * buff.getSize()] = color.getBlue();
            }            
        }
        
        public ImageWrapper(BufferedImage img) {
            img2array(img);
        }
        
        public ImageWrapper(FloatImage img) {
            width = img.getWidth();
            height = img.getHeight();
            pixels = img.getPix();
        }
        
        public ImageWrapper(Canvas3D canvas) {
            Canvas3D coff = new Canvas3D(SimpleUniverse.getPreferredConfiguration(), true);
            coff.getScreen3D().setSize(canvas.getScreen3D().getSize());
            coff.getScreen3D().setPhysicalScreenWidth(
                    canvas.getScreen3D().getPhysicalScreenWidth());
            coff.getScreen3D().setPhysicalScreenHeight(
                    canvas.getScreen3D().getPhysicalScreenHeight());
            coff.setOffScreenLocation(canvas.getLocation());
            canvas.getView().addCanvas3D(coff);
    
            BufferedImage img = new BufferedImage(canvas.getWidth(), 
                                                  canvas.getHeight(), 
                                                  BufferedImage.TYPE_INT_RGB);
            ImageComponent2D ic = new ImageComponent2D(ImageComponent2D.FORMAT_RGB, img);
            coff.setOffScreenBuffer(ic);
            coff.renderOffScreenBuffer();
            coff.waitForOffScreenRendering();
            
            img2array(coff.getOffScreenBuffer().getImage());
        }
    }
    
    
    
    /**************************************************************************/
    
    static public void setupJavaPath(File pathToLib) {
        String LibToJ3D = "lib/j3d-1.5.2";
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        String path = null;
        if (os.startsWith("Linux")) {
            if (arch.compareTo("x86") == 0 || arch.compareTo("i386") == 0) {
                path = "linux/i386";
            }
            else if (arch.compareTo("x86_64") == 0 || arch.compareTo("amd64") == 0) {
                path = "linux/amd64";
            }
        }
        else if (os.startsWith("Mac OS")) {
            path = "";
        }
        else if (os.startsWith("Solaris")) {
            System.err.println("Solaris distribution of Java is not included in the tool.");
        }
        else if (os.startsWith("Windows")) {
            if (arch.compareTo("x86") == 0 || arch.compareTo("i386") == 0) {
                path = "windows/i386";
            }
            else if (arch.compareTo("x86_64") == 0 || arch.compareTo("amd64") == 0) {
                path = "windows/amd64";
            }
        }
        if (path == null) {
            System.err.println("Java 3D does not support your architecture (OS: " + os + ", arch: " + arch + ").");
        }
        else {
            if (path.compareTo("") != 0) {
                if (!pathToLib.isDirectory()) {
                    pathToLib = pathToLib.getParentFile();
                }
               
                path = (new File(new File(pathToLib, LibToJ3D), path)).getPath();                
                System.out.println("Path to J3D set to " + path);
                System.setProperty("java.library.path", System.getProperty("java.library.path") + ":" + path);
                try {
                    Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
                    fieldSysPath.setAccessible( true );
                    fieldSysPath.set( null, null );
                }
                catch (Exception e) {
                    System.err.println("Unable to change library path.");
                }
            }
        }        
    }
}
