/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.annot.gui;


import java.awt.dnd.DropTarget;

/**
 *
 * @author vdelaitr
 */

public class PanelVisu3D extends VisuFrame {

    private PanelMain main;
        
    public PanelVisu3D(PanelMain m, Room3D r) {
        super(m.getLocation().x + m.getWidth(), m.getLocation().y, r);
        main = m;

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                main.close();
            }
        }); 

        canvas.addMyCanvas3DListener(main);

        // Create drop target
        new DropTarget(canvas, canvas);
    }
}
