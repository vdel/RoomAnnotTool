/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import java.awt.Point;
import java.awt.Polygon;
import java.util.LinkedList;

/**
 *
 * @author vdelaitr
 */
public class MyPolygon {
    LinkedList<Polygon> polys;
    
    class EdgeCut {
        double dist;
        int edgeID;
        
        EdgeCut() {
            edgeID = -1;
        }
        
        EdgeCut(double dist, int edgeID) {
            this.dist = dist;
            this.edgeID = edgeID;
        }
    }
            
    public MyPolygon() {        
    }
    
    public void addPolygon(Polygon poly) {
        if (getSignedArea(poly) < 0) { // we make it counter clockwise
            Polygon ccw = new Polygon();
            for (int i = poly.npoints - 1; i >= 0; i--) {
                ccw.addPoint(poly.xpoints[i], poly.ypoints[i]);
            }
            poly = ccw;
        }
        
        polys.add(poly);
    }
    
    private double getSignedArea(Polygon poly) { // positive if counter clockwise
        double a = 0;
        for (int i = 0; i < poly.npoints; i++) {
            a += poly.xpoints[i] * poly.ypoints[(i + 1) % poly.npoints] - 
                 poly.xpoints[(i + 1) % poly.npoints] * poly.ypoints[i];            
        }
        return a / 2.;
    }
    
    public void union(MyPolygon p) {
        for (Polygon poly : p.polys) {
            union(poly);
        }
    }
    
    private void union(Polygon a) {  // holes are not handled
        for(int i = 0; i < polys.size(); i++) {
            Polygon u = new Polygon();
            Polygon b = polys.get(i);
            int status = union(a, b, u);
            // status == 0: no intersection
            if (status == 1) {  // a inside b
                return;
            }
            else if (status == 2) {  // b inside a
                polys.remove(i);
                i--;
            }
            else if (status == 3) { // u gets union of a and b
                polys.remove(i);
                i--;
                a = u;
            }
        }
        polys.add(a);
    }
    
    private int union(Polygon a, Polygon b, Polygon u) {
        Polygon[] p = new Polygon[2];
        p[0] = a;
        p[1] = b;
        
        boolean[][] inside = new boolean[2][];
        inside[0] = findInside(a, b);
        inside[1] = findInside(b, a);
        
        int nInsideB = countInside(inside[0]);
        int nInsideA = countInside(inside[1]);
        if (nInsideA == 0 && nInsideB == 0) {
            return 0;            
        }
        else if(nInsideB == a.npoints) {
            return 1;
        }
        else if(nInsideA == b.npoints) {
            return 2;
        }
        
        EdgeCut[][] edges = new EdgeCut[2][];
        edges[0] = new EdgeCut[a.npoints];
        edges[1] = new EdgeCut[b.npoints];
        setCut(a, b, inside[0], edges[0], edges[1]);
        setCut(b, a, inside[1], edges[1], edges[0]);
               
        int currpoly0, currpoint0;
        int leftA = leftMostPoint(a);
        int leftB = leftMostPoint(b);
        if (a.xpoints[leftA] < b.xpoints[leftB]) {
            currpoly0 = 0;
            currpoint0 = leftA;            
        }
        else {
            currpoly0 = 1;
            currpoint0 = leftB;
        }
        
        int currpoly = currpoly0;
        int currpoint = currpoint0;                
        while (!inside[currpoly][currpoint]) {
            u.addPoint(p[currpoly].xpoints[currpoint], p[currpoly].ypoints[currpoint]);
            currpoint = (currpoint + 1) % p[currpoly].npoints;
            if (currpoly == currpoly0 && currpoint == currpoint0) {
                break;
            }
        }
        int prev = (currpoint - 1 + a.npoints) % a.npoints;
        Point p = intersect(a.xpoints[prev], 
                            a.ypoints[prev], 
                            a.xpoints[currpoint] - a.xpoints[prev],
                            a.ypoints[currpoint] - a.ypoints[prev])
        
        
        return 3;
    }
    
    private boolean[] findInside(Polygon a, Polygon b) {
        boolean[] isInside = new boolean[a.npoints];
        
        for (int i = 0; i < a.npoints; i++) {
            isInside[i] = b.contains(a.xpoints[i], a.ypoints[i]);
        }
        
        return isInside;
    }
    
    private int countInside(boolean[] isInside) {
        int count = 0;
        for (int i = 0; i < isInside.length; i++) {
            if (isInside[i]) {
                count++;
            }
        }
        return count;
    }
    
    private int leftMostPoint(Polygon a) {
        int i = 0;
        for (int j = 1; j < a.npoints; j++) {
            if (a.xpoints[j] < a.xpoints[i]) {
                i = j;
            }
        }
        return i;
    }
    
    private void setCut(Polygon a, Polygon b, boolean[] insideB, EdgeCut[] edgeA, EdgeCut[] edgeB) {
        for (int i = 0; i < a.npoints; i++) {
            int next = (i + 1) % a.npoints;
            if (!insideB[i] & insideB[next]) {
                
            }
            else if(insideB[i] & !insideB[next]) {
                
            }
        }
    }
}
