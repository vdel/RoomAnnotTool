/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MainPanel.java
 *
 * Created on 31 janv. 2012, 23:58:50
 */

package com.annot.gui;


import com.annot.gui.MyCanvas3D.MyCanvas3DListener;
import com.annot.room.ObjectManager;
import com.annot.room.ObjectManager.ObjectException;
import com.common.J3DHelper;
import com.common.XML.XMLException;
import com.pose.Pose3D;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;


/**
 *
 * @author vdelaitr
 */

public class PanelMain extends javax.swing.JFrame implements MyCanvas3DListener {
    static final long serialVersionUID = 0L;

    /* Main frame instance */
    public static PanelMain mainPanel;

    /* Panels */
    PanelLayout panelLayout;
    PanelVisu3D panelVisu3D;
    PanelVerif panelVerif;

    /* room */
    Room3D room;

    /* Modified ? */
    boolean modified;    
    
    /* directory of open dialog */
    File directory;

    /** Creates new form MainPanel */
    public PanelMain() {        
        initComponents();
        setLocation(100, 100);
        pack();
        
        Pose3D p = new Pose3D();

        panelLayout = null;
        panelVisu3D = null;
        panelVerif = null;
        
        directory = new File(PanelMain.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        if (!directory.isDirectory()) {        
            directory = directory.getParentFile();
        }
        System.out.println("Root directory is: " + directory);

        try {
            room = new Room3D(new File(directory, "objectslibrary.xml"));
        }
        catch (ObjectException e1) {
            System.err.println(e1.getMessage());
            directory = new File(directory, "../../");
            try {
                room = new Room3D(new File(directory, "objectslibrary.xml"));
            }
            catch (ObjectException e2) {
                System.err.println(e2.getMessage());
                System.exit(1);
            }            
        }
        
        J3DHelper.setupJavaPath(directory);
        
        jList1.removeAll();
        DefaultListModel listModel = new DefaultListModel();
        LinkedList<String> objs = new LinkedList<String>();
        for (ObjectManager om : room.getObjects()) {
            if (!om.isPrivate()) {
                objs.add(om.getName());
            }
        }
        Collections.sort(objs);
        for (String obj : objs) {
            listModel.addElement(obj);
        }
        jList1.setModel(listModel);
        jButton1.setEnabled(true);

        modified = false;
        close();
    }

    private void modifState(boolean mod) {
        modified = mod;
        jButton3.setEnabled(mod);
    }

    final boolean close() {
        if(!modified || (new PanelOkCancelDialog(this, true,
                         "Annotation has been modified. Close without saving ?")).getReturnStatus()
                         == PanelOkCancelDialog.RET_OK) {
            setTitle("");
            jButton3.setEnabled(false);
            jButton4.setEnabled(false);
            jCheckBox1.setSelected(true);
            jCheckBox1.setEnabled(false);
            jSpinner1.setEnabled(false);
            jSpinner1.setValue(0);            

            hidePanelLayout();
            jButton2.setEnabled(false);
            jButton8.setEnabled(false);
            if (panelLayout != null) {
                panelLayout.dispose();
            }
            panelLayout = null;
            
            hidePanel3D();
            if (panelVisu3D != null){
                panelVisu3D.close();
            }
            panelVisu3D = null;

            hidePanelVerif();
            if (panelVerif != null) {
                panelVerif.dispose();
            }
            panelVerif = null;

            setTitle("3D Annotator");            
            modified = false;

            room.clear(); // clear after closing panelVisu3D

            return true;
        }
        else {
            return false;
        }
    }

    void setLayoutReady(boolean ready) {
        jButton2.setEnabled(ready);
        jButton8.setEnabled(ready);
    }

    @Override
    public void setModified(ObjectInstance3D object) {
        modifState(true);        
    }

    @Override
    public void objectSelected(ObjectInstance3D object) {
        jButton5.setText("Delete " + object.getName());
        jButton5.setEnabled(true);
    }

    @Override
    public void objectDeselected(ObjectInstance3D object) {
        jButton5.setText("Delete");
        jButton5.setEnabled(false);
    }

    void showPanelLayout() {
        hidePanel3D();
        jButton2.setText("I am done with room layout !");
        if (panelLayout == null) {
            panelLayout = new PanelLayout(this, room);
        }
        panelLayout.setVisible(true);
    }

    void hidePanelLayout() {
        jButton2.setText("I want to edit room layout !");
        jButton2.setEnabled(true);
        jButton8.setEnabled(true);
        if (panelLayout != null) {
            panelLayout.setVisible(false);
            panelLayout.updateRoomCorners(room);
        }
    }

    void showPanel3D() {
        hidePanelLayout();
        if (!room.isSetVP()) {
            jButton2.setEnabled(false);
            jCheckBox1.setEnabled(false);
            jLabel1.setEnabled(false);
            jSpinner1.setEnabled(false);

        }
        jList1.setEnabled(true);
        jButton5.setEnabled(false);
        jButton6.setEnabled(true);
        if(panelVisu3D == null) {
            if (room.getParams() == null) {
                room.computeRoomParameters(jCheckBox1.isSelected());
            }
            if (room.getFloor() == null) {
                room.build();
            }
            panelVisu3D = new PanelVisu3D(this, room);
        }
        else {
            room.computeRoomParameters(jCheckBox1.isSelected());
            panelVisu3D.refreshRoom();            
        }
        panelVisu3D.setVisible(true);
    }

    void hidePanel3D() {
        jList1.setEnabled(false);
        jButton5.setEnabled(false);
        jButton6.setEnabled(false);
        if (panelVisu3D != null) {
            panelVisu3D.setVisible(false);            
        }
    }

    void hidePanelVerif() {
        if (panelVerif != null) {
            panelVerif.dispose();            
        }
        jButton7.setEnabled(false);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        jPanel4 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jButton8 = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jSpinner1 = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel5 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jButton5 = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jPanel6 = new javax.swing.JPanel();
        jButton6 = new javax.swing.JButton();

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jButton2.setText("I am done with room layout !");
        jButton2.setEnabled(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel4.setText("Room Layout:");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, jPanel1, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), jLabel4, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jButton8.setText("Reset original room size");
        jButton8.setEnabled(false);
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Same focal");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jSpinner1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinner1StateChanged(evt);
            }
        });

        jLabel1.setText("Distortion:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton8, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE)
                    .addComponent(jLabel4)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinner1, javax.swing.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE))
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBox1)
                    .addComponent(jLabel1)
                    .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel3.setEnabled(false);

        jButton3.setText("Save");
        jButton3.setEnabled(false);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton7.setText("Check");
        jButton7.setEnabled(false);
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jButton3)
                .addComponent(jButton7))
        );

        jButton1.setText("Open...");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, jPanel2, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), jButton1, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton4.setText("Close");
        jButton4.setEnabled(false);
        jButton4.setMaximumSize(new java.awt.Dimension(62, 33));
        jButton4.setMinimumSize(new java.awt.Dimension(62, 33));
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jButton1)
                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel12.setText("Add new object (drag & drop):");

        jList1.setDragEnabled(true);
        jList1.setEnabled(false);
        jScrollPane1.setViewportView(jList1);

        jButton5.setText("Delete object");
        jButton5.setEnabled(false);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 249, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 372, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jButton6.setText("Reset original view");
        jButton6.setEnabled(false);
        jButton6.setMaximumSize(new java.awt.Dimension(101, 33));
        jButton6.setMinimumSize(new java.awt.Dimension(101, 33));
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jSeparator1)
                        .addGap(8, 8, 8))
                    .addComponent(jSeparator3)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addContainerGap()))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if(!close()) {
            return;
        }
        
        JFileChooser chooser = new JFileChooser(directory);
        PanelOpenDialog filter = new PanelOpenDialog();
        filter.addExtension("jpg");
        filter.addExtension("png");
        filter.addExtension("bmp");
        filter.setDescription("Images");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File img = new File(chooser.getSelectedFile().getPath());
            directory = img;

            File annot;
            String path = img.getPath();
            int lastPeriodPos = path.lastIndexOf(".");
            if (lastPeriodPos == -1) {
                annot = new File(img + ".xml");
            }
            else {
                annot = new File(path.substring(0, lastPeriodPos) + ".xml");
            }
            
            try {
                room.load(img, annot);
            }         
            catch (XMLException e) {
                new PanelError(this, e.getMessage());
            }
            catch (IOException e) {
                new PanelError(this, e.getMessage());
            }
            
            setTitle(img.getName());
            jButton4.setEnabled(true);
            jCheckBox1.setEnabled(true);
            jSpinner1.setEnabled(true);
            jSpinner1.setValue(Integer.valueOf((int)Math.round(room.getImage().getDistortion() * 100)));
            jCheckBox1.setSelected(room.getParams() == null || room.getParams().fx == room.getParams().fy);
            
            if(room.isSetParams() || room.isSetVP()) {                
                showPanel3D();
                if (room.getRefinedVP() != null) {
                    jButton7.setEnabled(true);
                }
            }
            else {
                showPanelLayout();
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        close();
    }
//GEN-LAST:event_jButton4ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        if(room.isSetCorners()) {
            if(panelLayout != null && panelLayout.isVisible()) {
                panelLayout.updateRoomCorners(room);
            }
            room.saveAnnots();
            modifState(false);
        }        
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        modifState(true); 
        if(panelLayout != null && panelLayout.isVisible())
        {
            jButton7.setEnabled(true);
            showPanel3D();
        }
        else {
            showPanelLayout();
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        modifState(true); 
        panelVisu3D.getCanvas().removeSelectedObject();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        panelVisu3D.getCanvas().setDefaultView(room.getParams());
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        if (panelVerif != null) {
            panelVerif.dispose();
        }
        if (room.getRefinedVP() != null) {
            panelVerif = new PanelVerif(this, room.getRefinedVP());
            panelVerif.setVisible(true);
        }
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        room.restoreSize();
        if (panelVisu3D != null) {
            panelVisu3D.refreshRoom();
        }
        modifState(true); 
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        if (panelVisu3D != null) {
            room.computeRoomParameters(jCheckBox1.isSelected());
            panelVisu3D.refreshRoom();
        }
        modifState(true);
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jSpinner1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinner1StateChanged
        if (room.getImage() != null) {
            room.getImage().setDistortion(((Integer)jSpinner1.getValue()).intValue() * 0.01);
            if (panelLayout != null) {
                panelLayout.refreshImg();
            }            
            if (panelVisu3D != null) {
                panelVisu3D.refreshImg();
            }
        }
    }//GEN-LAST:event_jSpinner1StateChanged

    /**
    * @param args the command line arguments
    */
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {            
            @Override
            public void run() {
                PanelMain panel = new PanelMain();
                PanelMain.mainPanel = panel;
                panel.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                panel.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JList jList1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSpinner jSpinner1;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

}
