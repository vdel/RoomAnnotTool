/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.room;

import Jama.Matrix;
import com.common.ClippedImage;
import com.common.MyVect;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Vector;

/**
 *
 * @author vdelaitr
 */
public class RefineVanishingPoints {
    
    public BufferedImage alledges;
    public BufferedImage strongEdges;
    public BufferedImage alllines;
    public BufferedImage mergedLines;
    public BufferedImage candidateVPLines;
    public BufferedImage vpLines;

    private static class NextPoint {
        Point point;
        int direction;

        NextPoint(Point p, int dir) {
            point = p;
            direction = dir;
        }
    }

    private static class MaxDeviation {
        double deviation;
        int index;

        MaxDeviation(double dev, int i) {
            deviation = dev;
            index = i;
        }
    }

    static class Line {
        MyVect p1;
        MyVect p2;

        MyVect v;
        double angle;

        int vpID;
        double matchScore;

        Line(MyVect _p1, MyVect _p2) {
            p1 = _p1;
            p2 = _p2;

            v = MyVect.sub(p2, p1);
            angle = Math.atan(v.y / v.x);

            vpID = -1;
            matchScore = Double.POSITIVE_INFINITY;
        }
    }

    /**************************************************************************/

    RefineVanishingPoints(ClippedImage image, MyVect[] vp, Vector<Line> vanishingLines) {
        alledges = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        strongEdges = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        alllines = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        mergedLines = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        candidateVPLines = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        vpLines = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        refineFromEdges(image, vp, vanishingLines);
    }

    /**************************************************************************/

    void refineFromEdges(ClippedImage image, MyVect[] vp, Vector<Line> vanishingLines) {
        double highTh = 0.1f;
        int minlen = 30;  // pixels
        int devtol = 2;  // pixels
        double angtol = 1 / 180 * Math.PI;  // radians
        int linkrad = 4;  // pixels
        double angleThFindCandidates = 5 / 180. * Math.PI;  // radians
        double angleThAgreement = 1 / 180. * Math.PI;  // radians
        double[] angleTh= new double[3];
        boolean[] dovp = new boolean[3];
        boolean doOne = false;

        for (int i = 0; i < 3; i++) {
            if (vp[i].z == 0 || vp[i].norm() > image.getWidth() * 2) {
                angleTh[i] = angleThFindCandidates * 3;
                vp[i].z = 1;
                dovp[i] = true;
                doOne = true;
            }
            else {
                angleTh[i] = angleThFindCandidates;
                dovp[i] = false;
            }
        }

        if (doOne) {
            CannyEdges edges = new CannyEdges(image, highTh);
            drawEdges(alledges, edges.strongEdges);

            LinkedList< Vector<Point> > edgeList = extractEdges(edges.strongEdges, edges.w, edges.h, minlen);
            drawEdges(strongEdges, edgeList);

            Vector<Line> lines = extractLines(edgeList, devtol, minlen);
            drawEdges(alllines, lines);

            lines = mergeLines(lines, devtol, angtol, linkrad);
            drawEdges(mergedLines, lines);

            for (Line l : vanishingLines) {
                lines.add(l);
            }

            for (int i = 0; i < 3; i++) {
                findLinesBelongingToVP(lines, vp[i].directToImageCoord(image.getWidth(), image.getHeight()), angleTh[i], i);
            }
            for (int i = 0; i < 3; i++) {
                Vector<Line> drawlines = new Vector<Line>();
                for (Line l : lines) {
                    if (l.vpID == i) {
                        drawlines.add(l);
                    }
                }
                drawEdges(candidateVPLines, drawlines, i == 0 ? 255 * 256 * 256 : (i == 1 ? 255 * 256 : 255));
            }

            for (int i = 0; i < 3; i++) {
                Vector<Line> vplines = new Vector<Line>();
                for (Line l : lines) {
                    if (l.vpID == i) {
                        vplines.add(l);
                    }
                }

                if (dovp[i]) {
                    vplines = reEstimateVP(vplines, vp[i], angleThAgreement, image.getWidth(), image.getHeight());
                }
                else {
                    vplines = findLinesBelongingToVP(vplines, vp[i].directToImageCoord(image.getWidth(), image.getHeight()), angleThAgreement, -1);
                }

                drawEdges(vpLines, vplines, i == 0 ? 255 * 256 * 256 : (i == 1 ? 255 * 256 : 255));
            }
        }
    }

    /**************************************************************************/
    
    private void drawEdges(BufferedImage image, boolean[] edgeMap) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, edgeMap[x * image.getHeight() + y] ? 256 * (256 * 255 + 255) + 255 : 0);
            }
        }
    }

    /**************************************************************************/

    private void drawEdges(BufferedImage image, LinkedList< Vector<Point> > edgeList) {
        for (Vector<Point> e : edgeList) {
            int r = (int)(Math.random() * 128 + 128);
            int g = (int)(Math.random() * 128 + 128);
            int b = (int)(Math.random() * 128 + 128);
            int c = 256 * (256 * r + g) + b;
            for (Point p : e) {
                image.setRGB(p.x, p.y, c);
            }            
        }
    }

    /**************************************************************************/

    private void drawEdges(BufferedImage image, Vector<Line> lines) {
        drawEdges(image, lines, -1);
    }

    private void drawEdges(BufferedImage image, Vector<Line> lines, int color) {
        int c;
        for (Line l : lines) {
            if (color == -1) {
                int r = (int)(Math.random() * 128 + 128);
                int g = (int)(Math.random() * 128 + 128);
                int b = (int)(Math.random() * 128 + 128);
                c = 256 * (256 * r + g) + b;
            }
            else {
                c = color;
            }
            if (Math.abs(l.v.x) > Math.abs(l.v.y)) {
                double len = l.p2.x - l.p1.x + 1;
                int incr = l.p1.x < l.p2.x ? 1 : -1;
                for (int x = (int)l.p1.x; ; x += incr) {
                    int y = (int)(l.p1.y + l.v.y * (x - l.p1.x) / len);
                    if (x >= 0 && x < image.getWidth() &&
                        y >= 0 && y < image.getHeight()) {
                        image.setRGB(x, y, c);
                        
                    }
                    if ((incr >= 0 && x > l.p2.x) ||
                        (incr < 0 && x < l.p2.x)) {
                        break;
                    }
                }
            }
            else {
                double len = l.p2.y - l.p1.y + 1;
                int incr = l.p1.y < l.p2.y ? 1 : -1;
                for (int y = (int)l.p1.y; ; y += incr) {
                    int x = (int)(l.p1.x + l.v.x * (y - l.p1.y) / len);
                    if (x >= 0 && x < image.getWidth() &&
                        y >= 0 && y < image.getHeight()) {
                        image.setRGB(x, y, c);
                    }
                    if ((incr >= 0 && y > l.p2.y) ||
                        (incr < 0 && y < l.p2.y)) {
                        break;
                    }
                }
            }
        }
    }

    /**************************************************************************/

    private LinkedList< Vector<Point> > extractEdges(boolean[] edges, int w, int h, int minLength) {
        NextPoint np;
        boolean[] edgeMap = edges.clone();
        LinkedList< Vector<Point> > edgePoints = new LinkedList< Vector<Point> >();
        
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (edgeMap[x * h + y]) {
                    LinkedList<Point> e = new LinkedList<Point>();

                    np = new NextPoint(new Point(x, y), 0);
                    do {
                        e.addLast(np.point);
                        edgeMap[np.point.x * h + np.point.y] = false;
                    }
                    while ((np = nextPoint(edgeMap, np, w, h)) != null);

                    np = new NextPoint(new Point(x, y), 0);
                    while ((np = nextPoint(edgeMap, np, w, h)) != null) {
                        e.addFirst(np.point);
                        edgeMap[np.point.x * h + np.point.y] = false;
                    }

                    if (e.size() > minLength) {
                        edgePoints.add(new Vector<Point>(e));
                    }
                }
            }
        }

        return edgePoints;
    }

    /**************************************************************************/

    private NextPoint nextPoint(boolean[] edgeMap, NextPoint prevPoint, int w, int h) {
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, -1, -1, -1, 0, 1, 1, 1};
        int[] dd = {0, 1, 7, 2, 6, 3, 5, 4};
        int d;
        for (int i = 0; i < 8; i++) {
            d = (prevPoint.direction + dd[i]) % 8;
            int x0 = prevPoint.point.x + dx[d];
            int y0 = prevPoint.point.y + dy[d];
            if (0 <= x0 && x0 < w && 0 <= y0 && y0 < h && edgeMap[x0 * h + y0]) {
                return new NextPoint(new Point(x0, y0), d);
            }
        }
        return null;
    }
    
    /**************************************************************************/

    private Vector<Line> extractLines(LinkedList< Vector<Point> > edgeList, int maxdev, int minlen) {
        LinkedList<Line> lineList = new LinkedList<Line>();
        int beg, end;
        MaxDeviation md;

        for(Vector<Point> e : edgeList) {
            beg = 0;
            while(beg < e.size()) {
                end = e.size() - 1;
                while ((md = getMaxDeviation(e, beg, end)).deviation > maxdev) {
                    end = md.index;
                }
                MyVect p1 = new MyVect(e.get(beg));
                MyVect p2 = new MyVect(e.get(end));
                if (MyVect.sub(p2, p1).norm() > minlen) {
                    lineList.add(new Line(p1, p2));
                }
                beg = end + 1;
            }
        }

        return new Vector<Line>(lineList);
    }

    /**************************************************************************/

    private MaxDeviation getMaxDeviation(Vector<Point> e, int beg, int end) {
        if (end == beg) {
            return new MaxDeviation(0, beg);
        }
        else {
            int index = beg;
            double maxdev = 0;
            // line eq: x*(y1-y2) + y*(x2-x1) + y2*x1 - y1*x2 = 0
            int x2mx1 = e.get(end).x - e.get(beg).x;
            int y1my2 = e.get(beg).y - e.get(end).y;
            int C = e.get(end).y * e.get(beg).x - e.get(beg).y * e.get(end).x;
            double D = pointDist(e.get(beg), e.get(end));
                    
            for (int i = beg + 1; i < end; i++) {
                double dev = Math.abs(e.get(i).x * y1my2 + e.get(i).y * x2mx1 + C) / D;

                if (dev > maxdev) {
                    index = i;
                    maxdev = dev;
                }
            }

            return new MaxDeviation(maxdev, index);
        }
    }

    /**************************************************************************/

    private Vector<Line> mergeLines(Vector<Line> lines, int devtol, double angletol, int linkrad) {
        Vector<Line> merged = new Vector<Line>(lines);
        boolean didMerge = false;
        Line l;

        for (int i = 0; i < merged.size(); i++) {
            for (int j = i + 1; j < merged.size(); j++) {
                l = tryMergeLines(merged.get(i), merged.get(j), devtol, angletol, linkrad);
                if (l != null) {                    
                    didMerge = true;
                    merged.set(i, l);
                    merged.remove(j);
                    break;
                }
            }          
        }

        if (didMerge) {
            return mergeLines(merged, devtol, angletol, linkrad);
        }
        else {
            return merged;
        }
    }

    /**************************************************************************/

    private Line tryMergeLines(Line l1, Line l2, int devtol, double angletol, int linkrad) {
        double dangle = Math.abs(l1.angle - l2.angle);
        if (dangle > Math.PI / 2) {
            dangle = Math.abs(dangle - Math.PI);
        }

        if (dangle < angletol) {            
            if (l1.v.x * l2.v.x + l1.v.y * l2.v.y < 0) { // reverse lines
                l2 = new Line(l2.p2, l2.p1);
            }            
            if (pointDist(l1.p2.toPoint(), l2.p1.toPoint()) < linkrad) {
                Vector<Point> nl = new Vector<Point>(4);
                nl.setSize(4);
                nl.set(0, l1.p1.toPoint());
                nl.set(1, l1.p2.toPoint());
                nl.set(2, l2.p1.toPoint());
                nl.set(3, l2.p2.toPoint());
                if(getMaxDeviation(nl, 0, 3).deviation < devtol) {
                    return new Line(l1.p1, l2.p2);
                }
            }
            else if (pointDist(l2.p2.toPoint(), l1.p1.toPoint()) < linkrad) {
                Vector<Point> nl = new Vector<Point>(4);
                nl.setSize(4);
                nl.set(0, l2.p1.toPoint());
                nl.set(1, l2.p2.toPoint());
                nl.set(2, l1.p1.toPoint());
                nl.set(3, l1.p2.toPoint());
                if(getMaxDeviation(nl, 0, 3).deviation < devtol) {
                    return new Line(l2.p1, l1.p2);
                }
            }
        }

        return null;
    }

    /**************************************************************************/

    private double pointDist(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.x - p1.x, 2) +
                         Math.pow(p2.y - p1.y, 2));
    }

    /**************************************************************************/

    private Vector<Line> findLinesBelongingToVP(Vector<Line> lines, MyVect vp, double angTh, int vpID) {
        Vector<Line> vplines = new Vector<Line>();

        for (Line l : lines) {
            MyVect midpoint = MyVect.add(l.p1, l.p2).mul(0.5f);
            MyVect l1 = MyVect.sub(vp, midpoint);
            MyVect l2 = MyVect.sub(l.p2, midpoint);
            double angle = Math.acos(l1.dot(l2) / l1.norm() / l2.norm());
            if (angle > Math.PI / 2) {
                angle = Math.PI - angle;
            }
            if (Math.abs(angle) < angTh) {
                vplines.add(l);
                if (vpID != -1) {
                    if(Math.abs(angle) < l.matchScore)
                    {
                        l.matchScore = Math.abs(angle);
                        l.vpID = vpID;
                    }
                }
            }
        }

        return vplines;
    }

    /**************************************************************************/

    private Vector<Line> reEstimateVP(Vector<Line> lines, MyVect vp, double angTh, double w, double h) {
        double maxLength = 0;
        for (Line l : lines) {
            double length = l.v.norm();
            if (length > maxLength) {
                maxLength = length;
            }
        }

        int numiter = 1000;
        int ncomb = lines.size() * (lines.size() - 1) / 2;
        int k;
        Vector<Line> l1 = new Vector<Line>();
        Vector<Line> l2 = new Vector<Line>();

        if (numiter > ncomb) {
            l1.setSize(ncomb);
            l2.setSize(ncomb);
            k = 0;
            for (int i = 0; i < lines.size(); i++) {
                for (int j = i + 1; j < lines.size(); j++, k++) {
                    l1.set(k, lines.get(i));
                    l2.set(k, lines.get(j));
                }
            }
        }
        else {
            l1.setSize(numiter);
            l2.setSize(numiter);
            for (k = 0; k < numiter; k++) {
                int i = 0, j = 0;
                while (i == j) {
                    i = (int)Math.floor(Math.random() * lines.size());
                    j = (int)Math.floor(Math.random() * lines.size());
                }
                l1.set(k, lines.get(i));
                l2.set(k, lines.get(j));
            }
        }

        MyVect bestP = vp.directToImageCoord(w, h);
        double scoreMax = getVPScore(lines, bestP, angTh, maxLength);
        double score;

        for (int i = 0; i < k; i++) {
            MyVect p = MyVect.intersect(l1.get(i).p1, l1.get(i).v, l2.get(i).p1, l2.get(i).v);
            score = getVPScore(lines, p, angTh, maxLength);
            if (score > scoreMax) {
                scoreMax = score;
                bestP = p;
            }
        }

        Vector<Line> vplines = findLinesBelongingToVP(lines, bestP, angTh, -1);
        Matrix A = new Matrix(vplines.size(), 2);
        Matrix B = new Matrix(vplines.size(), 1);
        for (int i = 0; i < vplines.size(); i++) {
            MyVect v = vplines.get(i).v;
            double n = v.norm();
            double nx = -v.y / n;
            double ny =  v.x / n;
            double c = -(vplines.get(i).p1.x * nx +
                         vplines.get(i).p1.y * ny);
            A.set(i, 0, nx);
            A.set(i, 1, ny);
            B.set(i, 0, -c);
        }

        try {
            Matrix xy = A.solve(B);
            bestP.x = xy.get(0, 0);
            bestP.y = xy.get(1, 0);
            bestP.z = 1;
        }
        catch (RuntimeException e) {
        }

        bestP = bestP.imageToDirectCoord(w, h);
        vp.x = bestP.x;
        vp.y = bestP.y;
        vp.z = bestP.z;

        return vplines;
    }

    /**************************************************************************/

    private double getVPScore(Vector<Line> lines, MyVect vp, double angTh, double maxLength) {
        double score = 0;
        
        for (Line l : lines) {
            MyVect midpoint = MyVect.add(l.p1, l.p2).mul(0.5f);
            MyVect l1 = MyVect.sub(vp, midpoint);
            MyVect l2 = MyVect.sub(l.p2, midpoint);
            double angle = Math.acos(l1.dot(l2) / l1.norm() / l2.norm());
            if (angle > Math.PI / 2) {
                angle = Math.PI - angle;
            }
            if (Math.abs(angle) < angTh) {
                score += 0.3f * (1 - angle / angTh) + 0.7f * l.v.norm() / maxLength;
            }
        }

        return score;
    }

    /**************************************************************************/
}
