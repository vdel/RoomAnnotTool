/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ImageFrame.java
 *
 * Created on 1 févr. 2012, 01:21:13
 */

package com.annot.gui;

import com.annot.room.Room;
import com.common.ClippedImage;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.JFrame;

/**
 *
 * @author vdelaitr
 */
public class PanelLayout extends javax.swing.JFrame implements MouseListener, MouseMotionListener, ComponentListener{
    static final long serialVersionUID = 0L;

    private PanelMain main;
    private MyExtendedPanel panel;

    /* Corner of the room */
    private int nCorners;
    private Point[] corners;

    /* Indicate if dragging */
    private int dragging;

    /* Point clicked & dragged */
    private Point click;
    boolean dragged;

    /* Room */
    Room room;

    private class MyExtendedPanel extends MyPanel {
        static final long serialVersionUID = 0L;

        public MyExtendedPanel(ClippedImage I) {
            super(I.getImage());
            setTitle("Room Layout");
        }

        @Override
        public void paint(Graphics g)
        {
            super.paint(g);
            g.setColor(Color.RED);
            int radius = 10;
            for(int i = 0; i < nCorners; i++) {
                if (i == 4) {
                    g.setColor(Color.getHSBColor(0.f, 0.7f, 0.7f));
                }
                g.fillOval(corners[i].x + x0 - radius / 2,
                           corners[i].y + y0 - radius / 2,
                           radius, radius);
            }

            switch(nCorners) {
                case 2:
                case 3:
                    g.setColor(Color.BLUE); // always convex
                    drawSegment(g, 0, 1);
                    if(nCorners == 3)
                    {
                        drawSegment(g, 1, 2);
                        drawSegment(g, 2, 0);
                    }
                    break;
                case 4:
                case 5:
                case 6: {
                    g.setColor(computetConvexEnvelopOrder() ? Color.BLUE : Color.RED);

                    drawSegment(g, 0, 1);
                    drawSegment(g, 1, 2);
                    drawSegment(g, 2, 3);
                    drawSegment(g, 3, 0);
                    if (nCorners >= 5) {
                        drawHalfLine(g, 4, 0);
                        drawHalfLine(g, 4, 1);
                        drawHalfLine(g, 4, 2);
                        drawHalfLine(g, 4, 3);
                    }
                    if (nCorners >= 6) {                        
                        drawSegment(g, isCornerVisible(3) ? 3 : 2, 5);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        private void drawSegment(Graphics g, int a, int b) {
            if (b == 5) {
                g.setColor(Color.WHITE);
            }
            else if (isCornerVisible(a) || isCornerVisible(b)) {
                g.setColor(Color.BLUE);
            }
            else {
                g.setColor(Color.RED);
            }
            g.drawLine(corners[a].x + x0, corners[a].y + y0, corners[b].x + x0, corners[b].y + y0);
        }

        private void drawHalfLine(Graphics g, int a, int b) {
            if (true || isCornerVisible(b)) {
                g.setColor(Color.BLUE);                        
                Point v = getVector(corners[a], corners[b]);
                int norm = (int)Math.sqrt(v.x * v.x + v.y * v.y);
                int dim  = Math.max(getWidth() * 3, getHeight() * 3);
                v.x = (v.x * dim) / norm + corners[a].x;
                v.y = (v.y * dim) / norm + corners[a].y;
                g.drawLine(corners[a].x + x0, corners[a].y + y0, v.x + x0, v.y + y0);
            }
        }

        public void applyOffset(MouseEvent e) {
            e.translatePoint(-x0, -y0);
        }

        public void translate(int x, int y) {
            x0 = Math.max(0, x0 + x);
            y0 = Math.max(0, y0 + y);
            setPreferredSize(new Dimension(Math.max(x0 + image.getWidth(), getWidth()),
                                           Math.max(y0 + image.getHeight(), getHeight())));
            setSize(new Dimension(Math.max(x0 + image.getWidth(), getWidth()),
                                           Math.max(y0 + image.getHeight(), getHeight())));
        }

        public void frameResize() {
            if(x0 + image.getWidth() > getWidth())
                x0 = Math.max(getWidth() - image.getWidth(), 0);
            if(y0 + image.getHeight() > getHeight())
                y0 = Math.max(getHeight() - image.getHeight(), 0);
        }
    }

    /** Creates new form ImageFrame */
    public PanelLayout(PanelMain p, Room r) {
        main = p;
        room = r;
        corners = new Point[6];
        if(room.isSetCorners())
        {
            nCorners = 6;
            corners[0] = room.getCornerImage(Room.CornerType.TOPLEFT).toPoint();
            corners[1] = room.getCornerImage(Room.CornerType.TOPRIGHT).toPoint();
            corners[2] = room.getCornerImage(Room.CornerType.BOTTOMRIGHT).toPoint();
            corners[3] = room.getCornerImage(Room.CornerType.BOTTOMLEFT).toPoint();
            corners[4] = room.getCornerImage(Room.CornerType.DEPTHVP).toPoint();
            corners[5] = room.getCornerImage(Room.CornerType.PH).toPoint();
        }
        else
        {
            nCorners = 0;
            for(int i = 0; i < 6; i++)
                corners[i] = new Point();
        }
        click = new Point();
        dragging = -1;

        setLocation(main.getLocation().x + main.getWidth(), main.getLocation().y);
        panel = new MyExtendedPanel(room.getImage());
        add(panel);
        pack();
        setMinimumSize(new Dimension(getWidth(), getHeight()));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                main.close();
            }
        });        
        addComponentListener(this);
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
    }

    void refreshImg() {
        panel.setImage(room.getImage().getImage());
        panel.repaint();
    }

    void updateRoomCorners(Room room) {
        computetConvexEnvelopOrder();

        room.setCornerImage(Room.CornerType.TOPLEFT,     corners[0].x, corners[0].y);
        room.setCornerImage(Room.CornerType.TOPRIGHT,    corners[1].x, corners[1].y);
        room.setCornerImage(Room.CornerType.BOTTOMRIGHT, corners[2].x, corners[2].y);
        room.setCornerImage(Room.CornerType.BOTTOMLEFT,  corners[3].x, corners[3].y);
        room.setCornerImage(Room.CornerType.DEPTHVP,     corners[4].x, corners[4].y);
        room.setCornerImage(Room.CornerType.PH,          corners[5].x, corners[5].y);
    }

    boolean isCornerVisible(int i) {
        return room.getImage().getXmin() <= corners[i].x &&
               corners[i].x <= room.getImage().getXmax() &&
               room.getImage().getYmin() <= corners[i].y &&
               corners[i].y <= room.getImage().getYmax();
    }

    private Point getVector(Point a, Point b) {
        return new Point(b.x - a.x, b.y - a.y);
    }

    private int scalarProduct(Point v1, Point v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }

    private int determinant(Point v1, Point v2) {
        return v1.x * v2.y - v1.y * v2.x;
    }

    boolean computetConvexEnvelopOrder() {
        boolean[] selected = new boolean[4];
        int[] order = new int[4];
        int i, j, n;
        int cID;

        Point v = new Point(1, 1);
        int min = scalarProduct(v, corners[0]);
        cID = 0;
        for(i = 1; i < 4; i++)
        {
            int sp = scalarProduct(v, corners[i]);
            if(sp < min) {
                min = sp;
                cID = i;
            }
        }

        n = 0;
        do {
            if (selected[cID]) {
                return false;  // the quadrilatere is not convex
            }
            selected[cID] = true;
            order[n] = cID;
            n++;

            if(n == 4) break;

            j = (cID == 0 ? 1 : 0);
            for(i = j + 1;  i < 4; i++) {
                if(i != cID &&
                   determinant(getVector(corners[cID], corners[i]),
                               getVector(corners[cID], corners[j])) > 0) {
                    j = i;
                }
            }

            cID = j;
        } while(true);

        Point[] newCorners = new Point[6];
        newCorners[4] = nCorners >= 5 ? corners[4] : new Point();
        newCorners[5] = nCorners >= 6 ? corners[5] : new Point();
        for(i = 0; i < 4; i++)
        {
            newCorners[i] = corners[order[i]];
        }
        corners = newCorners;

        if (dragging != -1 && dragging != 4) {
            for(i = 0; i < 4; i++) {
                if (order[i] == dragging) {
                    dragging = i;
                    break;
                }
            }
        }

        return true;
    }

    private int get_corner(MouseEvent e) {
        int drag = -1;
        double min_dist = 0;
        for(int i = 0; i < nCorners; i++) {
            double dist = Math.sqrt(Math.pow(e.getPoint().x - corners[i].x, 2) +
                                    Math.pow(e.getPoint().y - corners[i].y, 2));
            if(dist < 20 && (drag == -1 || dist < min_dist)) {
                drag = i;
                min_dist = dist;
            }
        }
        return drag;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        dragged = false;
        click.x = e.getPoint().x;
        click.y = e.getPoint().y;
        panel.applyOffset(e);
        dragging = get_corner(e);
    }

    public void mouseReleased(MouseEvent e) {
        if(!dragged) {
            panel.applyOffset(e);
            if(dragging == -1 && nCorners < 6) {
                corners[nCorners].x = e.getPoint().x;
                corners[nCorners].y = e.getPoint().y;
                nCorners++;
                this.setCursor(new Cursor(Cursor.MOVE_CURSOR));
            }
            else
                dragging = -1;
            paint(this.getGraphics());
            
            main.setLayoutReady(nCorners == 6);
        }
        else {
            dragged = false;
        }
    }

    public void mouseDragged(MouseEvent e) {
        if(dragging != -1) {
            panel.applyOffset(e);
            corners[dragging].x = e.getPoint().x;
            corners[dragging].y = e.getPoint().y;
            paint(this.getGraphics());
        }
        else {
            dragged = true;
            panel.translate(e.getPoint().x - click.x, e.getPoint().y - click.y);
            click.x = e.getPoint().x;
            click.y = e.getPoint().y;
            pack();
            paint(this.getGraphics());
        }
    }

    public void mouseMoved(MouseEvent e) {
        panel.applyOffset(e);
        if(dragging == -1)
        {
            int cID = get_corner(e);
            if(cID == -1)
                this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            else
                this.setCursor(new Cursor(Cursor.MOVE_CURSOR));
        }
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        if(!dragged)
            panel.frameResize();
    }

    public void componentShown(ComponentEvent e) {
    }
}
