/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.common;

import com.common.MyVect;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 *
 * @author vdelaitr
 */
public class XML {
    static final long serialVersionUID = 0L;
    
    static public class XMLException extends Exception {
        static final long serialVersionUID = 0L;
        
        public XMLException(String msg) {
            super(msg);
        }

        public XMLException(Exception e) {
            super(e);
        }
    }

    static public class XMLNode {
        private Element e;
        public XMLNode (Element el) {
            e = el;
        }

        public XMLNode(String str) {
            e = new Element(str);
        }

        public void setAttribute(String attrib, String value) {
            e.setAttribute(attrib, value);
        }

        public void setAttribute(String attrib, int value) {
            e.setAttribute(attrib, Integer.toString(value));
        }

        public void setAttribute(String attrib, double value) {
            e.setAttribute(attrib, Double.toString(value));
        }

        public void addContent(XMLNode n) {
            e.addContent(n.e);
        }

        public void addContent(String str) {
            e.addContent(str);
        }
        
        public boolean hasChild(String child) {
            return !getChildren(child).isEmpty();
        }

        public XMLNode getChild(String child) throws XMLException {
            List<Element> children = e.getChildren(child);
            if (children.size() > 1) {
                throw new XMLException("Child <"+ child + "> of <" + e.getName() + "> tag is not unique.");
            }
            else if (children.size() == 0) {
                throw new XMLException("No child  <"+ child + "> for tag <" + e.getName() + ">.");
            }
            return new XMLNode(children.get(0));
        }

        public List<XMLNode> getChildren(String child) {
            List<Element> children = e.getChildren(child);
            Vector<XMLNode> res = new Vector<XMLNode>(children.size());
            for (Element el : children) {
                res.add(new XMLNode(el));
            }
            return res;
        }

        public String getAttribute(String name, boolean allowsEmpty) throws XMLException {
            Attribute a = e.getAttribute(name);
            if(a == null) {
                throw new XMLException("No '"+ name + "' attribute in <" + e.getName() + "> tag.");
            }
            String s = a.getValue();
            if (!allowsEmpty && s.compareTo("") == 0) {
                throw new XMLException("Empty '"+ name + "' attribute in <" + e.getName() + "> tag.");
            }
            return s;
        }

        public String getAttribute(String name) throws XMLException {
            return getAttribute(name, false);
        }

        public int getIntegerAttribute(String name) throws XMLException {
            String str = getAttribute(name, false);
            try {
                return Integer.parseInt(str);
            }
            catch(NumberFormatException ex) {
                throw new XMLException("Bad integer '" + str + "' in '" + name + "' attribute of <" + e.getName() + "> tag.");
            }
        }

        public double getDoubleAttribute(String name) throws XMLException {
            String str = getAttribute(name, false);
            try {
                return Double.parseDouble(str);
            }
            catch(NumberFormatException ex) {
                throw new XMLException("Bad double '" + str + "' in '" + name + "' attribute of <" + e.getName() + "> tag.");
            }
        }

        public String getContent() throws XMLException {
            return e.getTextTrim();
        }

        public int getIntegerContent() throws XMLException {
            String str = e.getTextTrim();
            try {
                return Integer.parseInt(str);
            }
            catch(NumberFormatException ex) {
                throw new XMLException("Bad integer content '" + str + "' in <" + e.getName() + "> tag.");
            }
        }

        public double getDoubleContent() throws XMLException {
            String str = e.getTextTrim();
            try {
                return Double.parseDouble(str);
            }
            catch(NumberFormatException ex) {
                throw new XMLException("Bad double content '" + str + "' in <" + e.getName() + "> tag.");
            }
        }

        static public XMLNode fromVect(String name, MyVect v) {
            XMLNode n = new XMLNode(name);
            n.setAttribute("x", v.x);
            n.setAttribute("y", v.y);
            n.setAttribute("z", v.z);
            return n;
        }
        
        static public XMLNode fromMatrix(String name, MyMatrix m) {
            XMLNode n = new XMLNode(name);
            XMLNode row1 = fromVect("row1", m.getRow(0));
            XMLNode row2 = fromVect("row2", m.getRow(1));
            XMLNode row3 = fromVect("row3", m.getRow(2));
            n.addContent(row1);
            n.addContent(row2);
            n.addContent(row3);
            return n;
        }

        static public MyVect toVect(XMLNode n) throws XMLException {
            MyVect v = new MyVect();
            v.x = n.getDoubleAttribute("x");
            v.y = n.getDoubleAttribute("y");
            v.z = n.getDoubleAttribute("z");
            return v;
        }

        public MyVect toVect() throws XMLException {
            return toVect(this);
        }
        
        static public MyMatrix toMatrix(XMLNode n) throws XMLException {
            MyVect row1, row2, row3;
            row1 = n.getChild("row1").toVect();
            row2 = n.getChild("row2").toVect();
            row3 = n.getChild("row3").toVect();
            return new MyMatrix(row1, row2, row3);
        }

        public MyMatrix toMatrix() throws XMLException {
            return toMatrix(this);
        }
    }

    static public class XMLDocument {
        Document doc;

        public XMLDocument() {
            doc = new Document();
        }

        public void save(File file) {
            try {
                XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
                out.output(doc, new FileOutputStream(file));
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void save(String file) {
            save(new File(file));
        }

        public void setRootElement(XMLNode root) {
            doc.setRootElement(root.e);
        }
    }

    static public XMLNode open(String file) throws XMLException, IOException {
        return open(new File(file));
    }

    static public XMLNode open(File file) throws XMLException, IOException {
        SAXBuilder sxb = new SAXBuilder();
        try {
            Element root = sxb.build(file).getRootElement();
            if (root == null) {
                throw new XMLException("Empty XML document: " + file);
            }
            return new XMLNode(root);
        }       
        catch(org.jdom2.JDOMException e) {
            throw new XMLException(e);
        }
    }    
}
