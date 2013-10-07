/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Polygons2D;
import math.geom2d.polygon.SimplePolygon2D;

/**
 *
 * @author vdelaitr
 */
public class MyPolygon {
    Polygon2D poly;
    
    public MyPolygon() {        
       poly = new SimplePolygon2D();
    }
    
    // From any vertices
    public MyPolygon(double[] xcoords, double[] ycoords) {
        poly = new SimplePolygon2D(xcoords, ycoords);
    }
    
    // From rectangle
    public MyPolygon(double[] center, double[] dims, double angle) {
        double[] xcoords = new double[4];
        double[] ycoords = new double[4];
        
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double w = dims[0] / 2;
        double h = dims[1] / 2;
        
        double[] du = {1., -1., -1., 1.}; 
        double[] dv = {1., 1., -1., -1.};
        
        for (int i = 0; i < 4; ++i) {
            double dx = du[i] * w;
            double dy = dv[i] * h;
            xcoords[i] = center[0] + dx * c - dy * s;
            ycoords[i] = center[1] + dx * s + dy * c;
        }
        
        poly = new SimplePolygon2D(xcoords, ycoords);
    }
    
    public double area() {
        return Math.abs(poly.area());
    }
    
    public MyPolygon intersection(Polygon2D p) {
        poly = Polygons2D.intersection(poly, p);
        return this;
    }
    
    public MyPolygon union(Polygon2D p) {
        poly = Polygons2D.union(poly, p);
        return this;
    }
    
   public MyPolygon intersection(MyPolygon p) {
        return intersection(p.poly);
    }
    
    public MyPolygon union(MyPolygon p) {
        return union(p.poly);        
    }
    
    public double[] toArray() {
        double[] p = new double[2 * poly.vertexNumber()];
        for (int i = 0; i < poly.vertexNumber(); i++) {
            p[2 * i]     = poly.vertex(i).x();
            p[2 * i + 1] = poly.vertex(i).y();
        }
        return p;
    }
}
