/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.gui;

import com.common.J3DHelper.ImageWrapper;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import javax.swing.JPanel;

/**
 *
 * @author vdelaitr
 */

public class VisuFrame extends Frame {
    static final long serialVersionUID = 0L;

    /* Estimated model */
    private Room3D room;

    /* J3D variables */
    protected MyCanvas3D canvas;
    private MyPanel panel;

    public VisuFrame(int x, int y, Room3D r) {
        super();
        room = r;

        // Create Window
        setSize(room.getImage().getWidth(), room.getImage().getHeight() + room.getImage().getClippedHeight());
        setLocation(x, y);
        setTitle("Room Geometry");
        setResizable(false);
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dispose();
            }
        });
        
        // Add components
        setLayout(new BorderLayout());

        if (room.hasAttachedImage()) {
            panel = new MyPanel(room.getImage());
            panel.setPreferredSize(new Dimension(room.getImage().getWidth(), room.getImage().getHeight()));
            add(panel, BorderLayout.PAGE_START);
        }
        else {
            panel = null;
        }
      
        JPanel pl = new JPanel();
        pl.setBackground(Color.black);
        pl.setPreferredSize(new Dimension(room.getImage().getXmin(), room.getImage().getClippedHeight()));
        add(pl, BorderLayout.LINE_START);
        
        canvas = new MyCanvas3D(room, panel);
        canvas.setPreferredSize(new Dimension(room.getImage().getClippedWidth(), room.getImage().getClippedHeight()));
        add(canvas, BorderLayout.CENTER);

        JPanel pr = new JPanel();
        pr.setBackground(Color.black);
        pr.setPreferredSize(new Dimension(room.getImage().getWidth() - room.getImage().getXmax() - 1, room.getImage().getClippedHeight()));
        add(pr, BorderLayout.LINE_END);

        pack();

        // Room was just loaded, re-compute if necessary
        refreshRoom();
    }

    public void close() {
        canvas.close();

        canvas = null;
        panel = null;

        dispose();
    }

    public void refreshImg() {
        panel.setImage(room.getImage());
        panel.repaint();
    }

    final public void refreshRoom() {
        room.refresh();
        canvas.setPickTool();
        canvas.setDefaultView(room.getParams());
    }

    public MyCanvas3D getCanvas() {
        return canvas;
    }

    public void addPose(double[] pose) {
        canvas.addPose(pose);
        panel.addPose(pose, room.getParams().K, room.getParams().R, room.getParams().t);
        panel.repaint();
    }

    public void clearPoses() {
        canvas.clearPoses();
        panel.clearPoses();
        panel.repaint();
    }
    
    public ImageWrapper screenCopy() {
        return new ImageWrapper(canvas);
    }
}
