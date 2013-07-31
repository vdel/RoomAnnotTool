/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.gui;

import com.common.ClippedImage;
import com.common.MyMatrix;
import com.common.MyVect;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.util.LinkedList;

/**
 *
 * @author vdelaitr
 */
public class MyPanel extends javax.swing.JPanel {
    static final long serialVersionUID = 0L;

    protected int x0, y0;
    protected ClippedImage image;
    private LinkedList<Line> lines;
    private LinkedList<double[]> poses;

    public class Line {
        Point a, b;
        Color color;

        Line(Point a, Point b, Color color) {
            this.a = a;
            this.b = b;
            this.color = color;
        }
    }

    public MyPanel(ClippedImage I) {
        super(new BorderLayout());
        setLocation(0, 0);
        setPreferredSize(new Dimension(I.getWidth(), I.getHeight()));
        image = I;
        x0 = 0;
        y0 = 0;
        lines = new LinkedList<Line>();
        poses = new LinkedList<double[]>();
        
        // uniform for screen capture background
        if (false) {
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    image.getImage().setRGB(x, y, (new Color(0.57f, 0.66f, 1f)).getRGB());
                }
            }
        }
    }
    
    public void setImage(ClippedImage img) {
        image = img;
        repaint();
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        g.drawImage(image.getImage(), x0, y0, null);
        
        int[] j1 = {2, 3, 9, 8, 0, 1, 3, 4, 6, 7, 9, 10, 12};
        int[] j2 = {3, 9, 8, 2, 1, 2, 4, 5, 7, 8, 10, 11, 13};

        for (double[] pose : poses) {
            double avglen = 0;
            for (int s = 0; s < j1.length; ++s) {
                avglen += Math.sqrt(Math.pow(pose[j1[s] * 3 + 0] - pose[j2[s] * 3 + 0], 2) +
                                    Math.pow(pose[j1[s] * 3 + 1] - pose[j2[s] * 3 + 1], 2));
            }             
            avglen /= j1.length;
            double coeff = 0.3 * avglen;
            double offset = coeff * 0.3;
            
            drawLine(g, pose, Color.BLACK,  coeff, offset, 2, 3, 9, 8, 2);  // Body
            drawLine(g, pose, Color.YELLOW, coeff, 0, 2, 3, 9, 8, 2);       // Body            

            drawLine(g, pose, Color.BLACK, coeff, offset, 0, 1, 2);         // Rleg
            drawLine(g, pose, Color.RED, coeff, 0, 0, 1, 2);                // Rleg            
            drawLine(g, pose, Color.BLACK, coeff, offset, 3, 4, 5);         // LLeg
            drawLine(g, pose, Color.BLUE, coeff, 0, 3, 4, 5);               // LLeg
            
            drawLine(g, pose, Color.BLACK, coeff, offset, 6, 7, 8);         // Rarm
            drawLine(g, pose, Color.MAGENTA, coeff, 0, 6, 7, 8);            // Rarm
            drawLine(g, pose, Color.BLACK, coeff, offset, 9, 10, 11);       // LArm            
            drawLine(g, pose, Color.CYAN, coeff, 0, 9, 10, 11);             // LArm
            
            drawLine(g, pose, Color.BLACK, coeff, offset, 12, 13);          // Head
            drawLine(g, pose, Color.GREEN, coeff, 0, 12, 13);               // Head
        }
        
        for (Line l : lines) {
            g.setColor(l.color);
            g.drawLine(l.a.x, l.a.y, l.b.x, l.b.y);
        }
    }

    private void drawLine(Graphics g, double[] pose, Color c, double wcoeff, double woffset, int ... points) {
        int i = -1;

        // draw big circles
        for (int j : points) {
            double x = pose[j * 3 + 0];
            double y = pose[j * 3 + 1];
            double r = (wcoeff + woffset) / 2;
            g.setColor(c);
            g.fillOval((int)(x - r), (int)(y - r), (int)(r * 2), (int)(r * 2));
        }

        // draw lines
        for (int j : points) {
            if (i != -1) {
                drawLine(g, pose[i * 3 + 0], pose[i * 3 + 1],
                            pose[j * 3 + 0], pose[j * 3 + 1],
                            wcoeff + woffset, c);
            }
            i = j;
        }

        // draw small circles
        for (int j : points) {
            double x = pose[j * 3 + 0];
            double y = pose[j * 3 + 1];
            double r = wcoeff / 4;
            g.setColor(Color.BLACK);
            g.fillOval((int)(x - r), (int)(y - r), (int)(r * 2), (int)(r * 2));
        }
    }

    private void drawLine(Graphics g, double x1, double y1, double x2, double y2, double w, Color c) {
        int[] cx = new int[4];
        int[] cy = new int[4];
        double nx = y1 - y2;
        double ny = x2 - x1;
        double nn = Math.sqrt(Math.pow(nx, 2) + Math.pow(ny, 2));
        w = Math.min(100, w / 2);
        nx = nx * w / nn;
        ny = ny * w / nn;
        cx[0] = (int)(x1 + nx);
        cy[0] = (int)(y1 + ny);
        cx[1] = (int)(x1 - nx);
        cy[1] = (int)(y1 - ny);
        cx[2] = (int)(x2 - nx);
        cy[2] = (int)(y2 - ny);
        cx[3] = (int)(x2 + nx);
        cy[3] = (int)(y2 + ny);
        g.setColor(c);
        g.fillPolygon(cx, cy, 4);
    }

    public void addLine(Point a, Point b, Color color) {
        lines.add(new Line(a, b, color));
    }

    public void clearLines() {
        lines.clear();
    }

    public void addPose(double[] pose, MyMatrix F, MyMatrix R, MyVect t) {
        double[] pose2D = new double[3 * 14];

        for (int i = 0; i < 14; i++) {
            MyVect p = new MyVect(pose[3 * i + 0], pose[3 * i + 1], pose[3 * i + 2]);
            p = R.mul(p).add(t);
            pose2D[3 * i + 2] = p.z;
            p = F.mul(p);
            Point coord =  new Point((int)Math.round(p.x * image.getWidth() / (2 * p.z)), (int)Math.round(p.y * image.getWidth() / (2 * p.z)));
            pose2D[3 * i + 0] = image.getWidth() / 2 + coord.x;
            pose2D[3 * i + 1] = image.getHeight() / 2 - coord.y;
        }

        poses.add(pose2D);
    }

    public void clearPoses() {
        poses.clear();
    }
}