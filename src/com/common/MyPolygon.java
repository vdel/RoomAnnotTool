/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import java.awt.Point;
import java.awt.Polygon;
import java.util.LinkedList;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Polygons2D;

/**
 *
 * @author vdelaitr
 */
public class MyPolygon {
    Polygon2D poly;
    
    public MyPolygon() {        
    }
    
    public void union(Polygon2D p) {
        poly = Polygons2D.union(poly, p);
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
