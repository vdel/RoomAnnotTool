/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pose;

import Jama.Matrix;
import com.common.Descent.NewtonLogBarrierDescent;
import com.common.MyMatrix;
import com.common.MyVect;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author vdelaitr
 */
public class Pose3D extends NewtonLogBarrierDescent {
    // This ordering of the joints should be the same or you should modify the functions fromArray and toArray
    protected static enum JointType { R_ANKLE, R_KNEE, R_HIP, L_HIP, L_KNEE, L_ANKLE, R_WRIST, R_ELBOW, R_SHOULDER, L_SHOULDER, L_ELBOW, L_WRIST, NECK, TOP_HEAD, M_HIP, ABDOMEN, THORAX};
    // Source for length: 
    // Paolo de Leva (1996) Adjustments to 
    // Zatsiorsky-Seluyanov's Segment Inertia Parameters . Journal of 
    // Biomechanics 29 (9), pp. 1223-1230.   
    protected static enum SegmentType      { TRUNK, LOWER_TRUNK, MIDDLE_TRUNK, UPPER_TRUNK, HEAD, L_SHOULDER, L_UPPERARM, L_FOREARM, R_SHOULDER, R_UPPERARM, R_FOREARM, L_HIP, L_THIGH, L_SHANK, R_HIP, R_THIGH, R_SHANK};
    protected static double[] segmentLen = { -1,     146,         113,          242,         243,  150,        282,        269,       150,        282,        269,       118,   422,     434,     118,   422,     434};   
    
    //------------------------------------------------------------------------//
    
    protected static class Value {
        double val;
        
        Value(double val) {
            this.val = val;
        }
        
        double getVal() {
            return val;
        }
        
        boolean isVar() {
            return false;
        }
    }
    
    protected static class Cst extends Value {
        Cst() {
            super(0);
        }
        
        Cst(double val) {
            super(val);
        }
    }
    
    protected static class Var extends Value {
        VariableManager vm;
        int id;
        double defaultVal;
        double min, max;
        
        Var() {
            super(0);
            defaultVal = 0;
            min = Double.NEGATIVE_INFINITY;
            max = Double.POSITIVE_INFINITY;  
        }
        
        Var(double v) {
            super(v);
            defaultVal = v;
            min = Double.NEGATIVE_INFINITY;
            max = Double.POSITIVE_INFINITY;             
        }
        
        Var(double min, double max) {
            super(0);
            defaultVal = 0;
            this.min = min;
            this.max = max; 
            assert(this.min < this.max);
            assert(this.min <= 0);
            assert(0 <= this.max);            
        }
        
        Var(double v, double min, double max) {
            super(v);
            defaultVal = v;
            this.min = min;
            this.max = max;   
            assert(this.min < this.max);
            assert(this.min <= v);
            assert(v <= this.max);
        }
        
        @Override
        boolean isVar() {
            return true;
        }
        
        @Override
        double getVal() {
            return vm.getVal(id);
        }
        
        void setVal(double val) {                        
            vm.setVal(id, val);
        }
                
        double getMin() {
            return min;
        }
        
        double getMax() {
            return max;
        }        
        
        void reset() {
            val = defaultVal;
        }                
        
        double getEffort() {
            double e, v;
            v = getVal();
            e = 0;
            if (min != Double.NEGATIVE_INFINITY) {
                e -= Math.log(v - min);
            }
            if (max != Double.POSITIVE_INFINITY) {
                e -= Math.log(max - v);            
            }
            return e;
        }
        
        double getEffortDerivative() {
            double d, v;
            v = getVal();
            d = 0;
            if (min != Double.NEGATIVE_INFINITY) {
                d -= 1 / (v - min);
            }
            if (max != Double.POSITIVE_INFINITY) {
                d -= 1 / (v - max);            
            }
            return d;
        }
    }
    
    protected static class VariableManager {
        private List<Var> variables = new LinkedList<>();
        private boolean freezed = false;
        private Matrix x, min, max;
        
        void add(Var v) {
            if (!freezed) {
                v.vm = this;
                v.id = variables.size();
                variables.add(v);
            }
        }
        
        int getNum() {
            return variables.size();
        }
        
        void freezeVars() {
            freezed = true;
            int n = getNum();
            int i = 0;
            x = new Matrix(n, 1);
            min = new Matrix(n, 1);
            max = new Matrix(n, 1);
            for(Var v : variables) {
                x.set(i, 0, v.val);
                min.set(i, 0, v.min);
                max.set(i, 0, v.max);
                ++i;
            }            
        }
        
        void setVals(Matrix x) {
            this.x = x.copy();
        }
        
        Matrix getVals() {
            return x.copy();
        }
        
        void setVal(int i, double val) {
            double eps = 1e-6;
            val = Math.min(max.get(i, 0) - eps, Math.max(min.get(i, 0) + eps, val));
            if (!freezed) {
                variables.get(i).val = val;
            }
            else {
                x.set(i, 0, val);
            }            
        }
        
        double getVal(int i) {
            if (!freezed) {
                return variables.get(i).val;
            }
            else {
                return x.get(i, 0);
            }
        }
        
        List<Var> getVarList() {
            return variables;
        }
        
        void reset() {            
            for (Var v : variables) {
                v.reset();
            }
            freezeVars();
        }
    }
    
    //------------------------------------------------------------------------//
    
    protected static class Rotation {
        Value rx, ry, rz;
                
        Rotation(VariableManager variables, Value rx, Value ry, Value rz) {
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            
            if (rx.isVar()) {
                variables.add((Var)rx);
            }
            if (ry.isVar()) {
                variables.add((Var)ry);
            }
            if (rz.isVar()) {
                variables.add((Var)rz);
            }
        }
        
        MyMatrix getMatrix() {
            MyMatrix rotX = MyMatrix.rotationX(rx.getVal());
            MyMatrix rotY = MyMatrix.rotationY(ry.getVal());
            MyMatrix rotZ = MyMatrix.rotationZ(rz.getVal());
            rotX.mul(rotY);
            rotX.mul(rotZ);
            return rotX;
        }
        
        MyMatrix getDerivative(Var v) {
            MyMatrix rotX = (v == rx) ? MyMatrix.derivRotX(rx.getVal()) : MyMatrix.rotationX(rx.getVal());
            MyMatrix rotY = (v == ry) ? MyMatrix.derivRotY(ry.getVal()) : MyMatrix.rotationY(ry.getVal());
            MyMatrix rotZ = (v == rz) ? MyMatrix.derivRotZ(rz.getVal()) : MyMatrix.rotationZ(rz.getVal());
            rotX.mul(rotY);
            rotX.mul(rotZ);
            return rotX;
        }
    }
    
    //------------------------------------------------------------------------//
    
    protected static class Joint {
        JointType jt;
        Rotation rot;
        
        boolean hidden;
        MyVect pos = new MyVect();
        MyVect dpos = new MyVect();
        double projX, projY;
        double toRad = Math.PI / 180.;
        
        List<Segment> children = new LinkedList<>();
                
        Joint(JointType jt, VariableManager vars) {
            this.jt = jt;
            Var vx, vy, vz;
            switch (jt) {
                case R_ANKLE:
                    rot = null;
                    break;                
                case R_KNEE:
                    rot = new Rotation(vars, new Var(20 * toRad, 0, 170 * toRad), new Cst(), new Cst());
                    break;
                case R_HIP:                    
                    vx = new Var(-20 * toRad, -170 * toRad, 0);
                    vy = new Var(-20 * toRad, -45 * toRad, 10 * toRad);
                    vz = new Var(-10 * toRad, -45 * toRad, 20 * toRad);
                    rot = new Rotation(vars, vx, vy, vz);
                    break;    
                case L_ANKLE:
                    rot = null;
                    break;                
                case L_KNEE:
                    rot = new Rotation(vars, new Var(20 * toRad, 0, 170 * toRad), new Cst(), new Cst());
                    break;
                case L_HIP:
                    vx = new Var(-20 * toRad, -170 * toRad, 0);
                    vy = new Var(20 * toRad, -10 * toRad, 45 * toRad);
                    vz = new Var(10 * toRad, -20 * toRad, 45 * toRad);
                    rot = new Rotation(vars, vx, vy, vz);
                    break;
                case R_WRIST:
                    rot = null;
                    break;
                case R_ELBOW:
                    rot = new Rotation(vars, new Cst(), new Var(10 * toRad, 0, 170 * toRad), new Cst());
                    break;
                case R_SHOULDER:
                    vx = new Var(-10 * toRad, -100 * toRad, 10 * toRad);
                    vy = new Var(0 * toRad, -20 * toRad, 135 * toRad);
                    vz = new Var(80 * toRad, -90 * toRad, 110 * toRad);
                    rot = new Rotation(vars, vx, vy, vz);
                    break;
                case L_WRIST:
                    rot = null;
                    break;
                case L_ELBOW:
                    rot = new Rotation(vars, new Cst(), new Var(-10 * toRad, -170 * toRad, 0), new Cst());
                    break;
                case L_SHOULDER:
                    vx = new Var(-10 * toRad, -100 * toRad, 10 * toRad);
                    vy = new Var(0 * toRad, -135 * toRad, 20 * toRad);
                    vz = new Var(-80 * toRad, -110 * toRad, 90 * toRad);
                    rot = new Rotation(vars, vx, vy, vz);
                    break;    
                case NECK:
                    vx = new Var(10 * toRad, -30 * toRad, 30 * toRad);
                    vz = new Var(-30 * toRad, 30 * toRad);
                    rot = new Rotation(vars, vx, new Cst(), vz);
                    break;
                case TOP_HEAD:
                    rot = null;
                    break;
                case M_HIP:
                    rot = new Rotation(vars, new Var(), new Var(), new Var());
                    break;
                case THORAX:
                    vx = new Var(5 * toRad, -5 * toRad, 40 * toRad);
                    vy = new Var(-15 * toRad, 15 * toRad);
                    vz = new Var(-15 * toRad, 15 * toRad);
                    rot = new Rotation(vars, vx, vy, vz);
                    break;
                case ABDOMEN:
                    vx = new Var(5 * toRad, -5 * toRad, 40 * toRad);
                    vy = new Var(-15 * toRad, 15 * toRad);
                    vz = new Var(-15 * toRad, 15 * toRad);
                    rot = new Rotation(vars, vx, vy, vz);
                    break;
            }
        }
        
        void propagatePos(double scale) {
            propagatePos(MyMatrix.Identity(), scale);
        }
        
        void propagatePos(MyMatrix r0, double scale) {
            if (rot == null) {
                return;
            }
            MyMatrix r = new MyMatrix(); 
            r.mul(r0, rot.getMatrix());            
            for (Segment s : children) {
                s.propagatePos(r, pos, scale);
            }
        }
        
        void computeGradient(double scale, double[] grad, Var v, double penHidden) {
            computeGradient(MyMatrix.Identity(), scale, grad, v, penHidden);
        }
        
        void computeGradient(MyMatrix r0, double scale, double[] grad, Var v, double penHidden) {
            dpos = new MyVect();
            if (rot == null) {
                return;
            }
            MyMatrix r = new MyMatrix();
            if (rot.rx == v || rot.ry == v || rot.rz == v) {
                r.mul(r0, rot.getDerivative(v));
                for (Segment s : children) {
                    s.computeGradient(r, scale, grad, penHidden);
                }
            }
            else {
                r.mul(r0, rot.getMatrix()); 
                for (Segment s : children) {
                    s.to.computeGradient(r, scale, grad, v, penHidden);
                }
            }
        }
    }
    
    //------------------------------------------------------------------------//
    
    protected static class Segment {
        SegmentType st;
        Joint from, to;
        double length;
        MyVect v;
        
        Segment(SegmentType st, List<Joint> joints) {
            this.st = st;
            double len = segmentLen[st.ordinal()];
            switch (st) {
                case TRUNK:
                    from = findJoint(joints, JointType.M_HIP);
                    to   = findJoint(joints, JointType.NECK);
                    v = new MyVect(0, len, 0);
                    break;   
                case LOWER_TRUNK:
                    from = findJoint(joints, JointType.M_HIP);
                    to   = findJoint(joints, JointType.ABDOMEN);
                    v = new MyVect(0, len, 0);
                    break;   
                case MIDDLE_TRUNK:
                    from = findJoint(joints, JointType.ABDOMEN);
                    to   = findJoint(joints, JointType.THORAX);
                    v = new MyVect(0, len, 0);
                    break; 
                case UPPER_TRUNK:
                    from = findJoint(joints, JointType.THORAX);
                    to   = findJoint(joints, JointType.NECK);
                    v = new MyVect(0, len, 0);
                    break;
                case HEAD:
                    from = findJoint(joints, JointType.NECK);
                    to   = findJoint(joints, JointType.TOP_HEAD);
                    v = new MyVect(0, len, 0);
                    break;
                case L_SHOULDER:
                    from = findJoint(joints, JointType.THORAX);
                    to   = findJoint(joints, JointType.L_SHOULDER);
                    v = new MyVect(len, segmentLen[SegmentType.UPPER_TRUNK.ordinal()], 0);
                    break;  
                case L_UPPERARM:
                    from = findJoint(joints, JointType.L_SHOULDER);
                    to   = findJoint(joints, JointType.L_ELBOW);
                    v = new MyVect(len, 0, 0);
                    break;
                case L_FOREARM:
                    from = findJoint(joints, JointType.L_ELBOW);
                    to   = findJoint(joints, JointType.L_WRIST);
                    v = new MyVect(len, 0, 0);
                    break;
                case R_SHOULDER:
                    from = findJoint(joints, JointType.THORAX);
                    to   = findJoint(joints, JointType.R_SHOULDER);
                    v = new MyVect(-len, segmentLen[SegmentType.UPPER_TRUNK.ordinal()], 0);
                    break;
                case R_UPPERARM:
                    from = findJoint(joints, JointType.R_SHOULDER);
                    to   = findJoint(joints, JointType.R_ELBOW);
                    v = new MyVect(-len, 0, 0);
                    break;
                case R_FOREARM:
                    from = findJoint(joints, JointType.R_ELBOW);
                    to   = findJoint(joints, JointType.R_WRIST);
                    v = new MyVect(-len, 0, 0);
                    break;
                case L_HIP:
                    from = findJoint(joints, JointType.M_HIP);
                    to   = findJoint(joints, JointType.L_HIP);
                    v = new MyVect(len, 0, 0);
                    break; 
                case L_THIGH:
                    from = findJoint(joints, JointType.L_HIP);
                    to   = findJoint(joints, JointType.L_KNEE);
                    v = new MyVect(0, -len, 0);
                    break;
                case L_SHANK:
                    from = findJoint(joints, JointType.L_KNEE);
                    to   = findJoint(joints, JointType.L_ANKLE);
                    v = new MyVect(0, -len, 0);
                    break;
                case R_HIP:
                    from = findJoint(joints, JointType.M_HIP);
                    to   = findJoint(joints, JointType.R_HIP);
                    v = new MyVect(-len, 0, 0);
                    break; 
                case R_THIGH:
                    from = findJoint(joints, JointType.R_HIP);
                    to   = findJoint(joints, JointType.R_KNEE);
                    v = new MyVect(0, -len, 0);
                    break;
                case R_SHANK:
                    from = findJoint(joints, JointType.R_KNEE);
                    to   = findJoint(joints, JointType.R_ANKLE);
                    v = new MyVect(0, -len, 0);
                    break;
            }       
            length = v.norm();
            if (st != SegmentType.TRUNK) {  // TRUNK is fake: used in escapeLocalMin 
                from.children.add(this);
            }
        }
        
        void propagatePos(MyMatrix parentRot, MyVect parentPos, double scale) {
            to.pos = parentPos.add(parentRot.mul(v.mul(scale)));
            to.propagatePos(parentRot, scale);
        }
        
        void computeGradient(MyMatrix parentRot, double scale, double[] grad, double penHidden) {
            computeGradient(parentRot, scale, grad, new MyVect(), penHidden);
        }
        
        void computeGradient(MyMatrix parentRot, double scale, double[] grad, MyVect parentPos, double penHidden) {
            int i = to.jt.ordinal();
            double coeff = to.hidden ? penHidden : 1;
            to.dpos = parentPos.add(parentRot.mul(v.mul(scale))); 
            grad[i * 2 + 0] += to.dpos.x * coeff;
            grad[i * 2 + 1] += to.dpos.y * coeff;
                        
            if (to.rot != null) {
                MyMatrix r = new MyMatrix();
                r.mul(parentRot, to.rot.getMatrix());
                for (Segment s : to.children) {
                    s.computeGradient(r, scale, grad, to.dpos, penHidden);
                }
            }
        }
    }
    
    //------------------------------------------------------------------------//
    
    Joint root;
    
    List<Joint> joints = new LinkedList<>();
    List<Segment> segments = new LinkedList<>();
    VariableManager variables = new VariableManager();
    
    Var xHip = new Var();
    Var yHip = new Var();
    Var scale = new Var(1, 0, Double.POSITIVE_INFINITY);
    
    final double penalizationHidden = 0.5;
    final double penalizationFeetHeight = 1;
    final double penalizationNeckPos = 1;
   
    MyVect topDir;
    
    public Pose3D() {        
        variables.add(xHip);
        variables.add(yHip);          
        variables.add(scale);
        
        for (int i = 0; i < JointType.values().length; i++) {
            joints.add(new Joint(JointType.values()[i], variables));
        }
       
        for (int i = 0; i < SegmentType.values().length; i++) {
            segments.add(new Segment(SegmentType.values()[i], joints));
        }
        
        variables.freezeVars();
        
        root = findJoint(JointType.M_HIP);
    } 
    
    public Pose3D(double[] pose) {
        this();
        loadFrom3D(pose);
    }
    
    final Joint findJoint(JointType jt) {
        return findJoint(joints, jt);
    }
    
    static Joint findJoint(List<Joint> joints, JointType jt) {
        for (Joint j : joints) {
            if (j.jt == jt) {
                return j;
            }
        }
        return null;
    }
    
    final Segment findSegment(SegmentType jt) {
        return findSegment(segments, jt);
    }
    
    static Segment findSegment(List<Segment> segments, SegmentType st) {
        for (Segment s : segments) {
            if (s.st == st) {
                return s;
            }
        }
        return null;
    }
    
    public final void loadFrom2D(double[] pose) {
        variables.reset();
        
        for (int i = 0; i < 14; ++i) {
            Joint j = findJoint(JointType.values()[i]);
            j.projX = pose[3 * i + 0];
            j.projY = pose[3 * i + 1];
            j.hidden = pose[3 * i + 2] == 0;
            j.pos = new MyVect(j.projX, j.projY, 0);
        }
        
        setAdditionalJoints();
        setScale(true);
    }
    
    public final void loadFrom3D(double[] pose) {
        variables.reset();
        
        for (int i = 0; i < 14; ++i) {
            Joint j = findJoint(JointType.values()[i]);
            j.pos = new MyVect(pose[3 * i + 0], pose[3 * i + 1], pose[3 * i + 2]);
            j.projX = j.pos.x;
            j.projY = j.pos.y;
            j.hidden = false;
        }
        
        setAdditionalJoints();
        setScale(false);
    }
    
    private void setAdditionalJoints() {
        Joint lh = findJoint(JointType.L_HIP);
        Joint rh = findJoint(JointType.R_HIP);
        Joint mh = findJoint(JointType.M_HIP);
        Joint n  = findJoint(JointType.NECK);
        Joint t = findJoint(JointType.THORAX);
        Joint a = findJoint(JointType.ABDOMEN);
        mh.pos = lh.pos.add(rh.pos).mul(0.5);
        mh.projX = mh.pos.x;
        mh.projY = mh.pos.y;
        mh.hidden = true;
        double upperTrunkLen  = segmentLen[SegmentType.UPPER_TRUNK.ordinal()];
        double middleTrunkLen = segmentLen[SegmentType.MIDDLE_TRUNK.ordinal()];
        double lowerTrunkLen  = segmentLen[SegmentType.LOWER_TRUNK.ordinal()];        
        double totalTrunkLen = upperTrunkLen + middleTrunkLen + lowerTrunkLen;
        t.pos = n.pos.add(mh.pos.sub(n.pos).mul(upperTrunkLen / totalTrunkLen));
        t.projX = t.pos.x;
        t.projY = t.pos.y;
        t.hidden = true;
        a.pos = mh.pos.add(n.pos.sub(mh.pos).mul(lowerTrunkLen / totalTrunkLen));
        a.projX = a.pos.x;
        a.projY = a.pos.y;
        a.hidden = true;
    }
    
    private void setScale(boolean useProj) {
        double avgProjLen = 0;
        double avgLen = 0;
        for (Segment s : segments) {
            if (useProj) {
                avgProjLen += Math.sqrt(Math.pow(s.from.projX - s.to.projX, 2) + 
                              Math.pow(s.from.projY - s.to.projY, 2));
            }
            else {
                avgProjLen += s.to.pos.sub(s.from.pos).norm();
            }
            avgLen += s.v.norm();
        }
        
        Joint mh = findJoint(JointType.M_HIP);
        scale.setVal(avgProjLen / avgLen);
        
        xHip.setVal(mh.projX);
        yHip.setVal(mh.projY);  
    }
    
    private double[] toArray() {
        double[] pose = new double[14 * 3];
        for (int i = 0; i < 14; ++i) {
            MyVect pos = findJoint(JointType.values()[i]).pos;
            pose[3 * i + 0] = pos.x;
            pose[3 * i + 1] = pos.y;
            pose[3 * i + 2] = pos.z;
        }
        return pose;
    }
    
    public void propagatePos() {
        root.pos.x = xHip.getVal();
        root.pos.y = yHip.getVal();
        root.propagatePos(scale.getVal());
    }
    
    @Override
    public Matrix evaluateInitialX(Object params) {
        return variables.getVals();
    }
    
    @Override
    public Matrix evaluateFunction(Matrix x, Object params) {
        variables.setVals(x);
        propagatePos();
        
        Matrix f = new Matrix(joints.size() * 2 + 1, 1);
        
        int i = 0;
        for (Joint j : joints) {
            double dx = j.pos.x - j.projX;
            double dy = j.pos.y - j.projY;
            double coeff = j.hidden ? penalizationHidden : 1;
            f.set(i * 2 + 0, 0, dx * coeff);
            f.set(i * 2 + 1, 0, dy * coeff);    
            ++i;
        }
        // last dim is a hack for preventing overfitting:
        Joint neck = findJoint(JointType.NECK);  
        Joint la = findJoint(JointType.L_ANKLE); 
        Joint ra = findJoint(JointType.R_ANKLE); 
        f.set(i * 2, 0, neck.pos.z * penalizationNeckPos + // neck should be at the same depth as middle of hips
                        Math.pow(la.pos.sub(ra.pos).dot(topDir), 2) * // feet should be at the same height
                        penalizationFeetHeight);
        
        return f;
    }
    
    @Override
    public Matrix evaluateJacobian(Matrix x, Object params) {
        variables.setVals(x);
        propagatePos();                

        int nvars = x.getRowDimension();
        int ndims = joints.size() * 2 + 1;
        double[][] jacob = new double[nvars][];
        
        Joint neck = findJoint(JointType.NECK);
        Joint la = findJoint(JointType.L_ANKLE); 
        Joint ra = findJoint(JointType.R_ANKLE); 

        int i = 0;
        for (Var v : variables.getVarList()) {
            if (v == xHip) {
                jacob[i] = new double[ndims];     
                for (Joint j : joints) {
                    jacob[i][j.jt.ordinal() * 2] = j.hidden ? penalizationHidden : 1;
                }
            }
            else if (v == yHip) {
                jacob[i] = new double[ndims];
                for (Joint j : joints) {
                    jacob[i][j.jt.ordinal() * 2 + 1] = j.hidden ? penalizationHidden : 1;
                }
            }
            else if (v == scale) {
                jacob[i] = new double[ndims];
                for (Joint j : joints) {
                    double coeff = j.hidden ? penalizationHidden : 1;
                    jacob[i][j.jt.ordinal() * 2 + 0] = coeff * (j.pos.x - xHip.getVal()) / scale.getVal();
                    jacob[i][j.jt.ordinal() * 2 + 1] = coeff * (j.pos.y - yHip.getVal()) / scale.getVal();
                }
                jacob[i][joints.size() * 2] = (neck.pos.z * penalizationNeckPos +
                                               2 * Math.pow(la.pos.sub(ra.pos).dot(topDir), 2) 
                                                 * penalizationFeetHeight) / scale.getVal();
            }
            else {
                jacob[i] = new double[ndims];
                root.computeGradient(scale.getVal(), jacob[i], v, penalizationHidden);
                jacob[i][joints.size() * 2] += neck.dpos.z * penalizationNeckPos +
                                               2 * (la.pos.sub(ra.pos).dot(topDir)) *
                                                   (la.dpos.sub(ra.dpos).dot(topDir)) * 
                                                    penalizationFeetHeight;  
            }
            
            ++i;
        }
        
        return new Matrix(jacob).transpose();
    }
    
    @Override
    public double evaluateBarriers(Matrix x, Object params) {
        double barrier = 0;
        
        for (Var v : variables.getVarList()) {
            barrier += v.getEffort();
        }
        
        return Double.isNaN(barrier) ? Double.POSITIVE_INFINITY : barrier;
    }

    @Override
    public Matrix evaluateBarsGrad(Matrix x, Object params) {
        int nvars = variables.getNum();
        Matrix grad = new Matrix(nvars, 1);
            
        int i = 0;
        for (Var v : variables.getVarList()) {
            grad.set(i, 0, v.getEffortDerivative());
            ++i;
        }

        return grad;
    }
    
    public double[] lift3D() {  
        return lift3D(new MyVect());
    }
    
    public double[] lift3D(MyVect topDir) {
        this.topDir = topDir;
        double initeps = 1e-4;
        double refiteps = 1e-3;
        double finaleps = 1e-6;
        
        Joint lh = findJoint(JointType.L_HIP);        
        Joint mh = findJoint(JointType.M_HIP);
        Joint ls = findJoint(JointType.L_SHOULDER);
        Joint rs = findJoint(JointType.R_SHOULDER);
        Joint n  = findJoint(JointType.NECK);
        
        propagatePos();
        MyVect trunk = new MyVect(n.projX - mh.projX, n.projY - mh.projY, 0);
        double zAngle = Math.atan2(-trunk.x, trunk.y);
        MyVect hips = new MyVect(lh.projX - mh.projX, lh.projY - mh.projY, 0);
        double hipsLenProj = Math.sqrt(hips.x * hips.x + hips.y * hips.y);
        double hipsLen = segmentLen[SegmentType.L_HIP.ordinal()] * scale.getVal();
        double hipsDepth = hipsLenProj > hipsLen ? 0 : 
                           Math.sqrt(hipsLen * hipsLen - hipsLenProj * hipsLenProj);
        double hipsAngle = Math.atan2(hipsDepth, hips.det(trunk).z > 0 ? hipsLen : -hipsLen);
        MyVect shld = new MyVect(ls.projX - rs.projX, ls.projY - rs.projY, 0);
        double shldLenProj = Math.sqrt(shld.x * shld.x + shld.y * shld.y);
        double shldLen = segmentLen[SegmentType.L_SHOULDER.ordinal()] * 2 * scale.getVal();
        double shldDepth = shldLenProj > shldLen ? 0 : 
                           Math.sqrt(shldLen * shldLen - shldLenProj * shldLenProj);
        double shldAngle = Math.atan2(shldDepth, shld.det(trunk).z > 0 ? shldLen : -shldLen);
        
        if (shldAngle - hipsAngle > Math.PI) {
            shldAngle -= 2 * Math.PI;
        }
        if (shldAngle - hipsAngle < -Math.PI) {
            shldAngle += 2 * Math.PI;
        }        
        double yAngle = normalizeAngle((hipsAngle + shldAngle) / 2);
        
        MyMatrix rot = MyMatrix.rotationZ(zAngle);
        rot.mul(MyMatrix.rotationY(yAngle));
        
        MyVect x = rot.mul(new MyVect(1, 0, 0));
        MyVect y = rot.mul(new MyVect(0, 1, 0));
        
        ((Var)root.rot.rz).setVal(Math.atan2(x.y, x.x));
        rot = MyMatrix.rotationZ(-root.rot.rz.getVal());
        x = rot.mul(x);
        y = rot.mul(y);
        
        ((Var)root.rot.ry).setVal(Math.atan2(x.z, x.x));
        rot = MyMatrix.rotationY(-root.rot.ry.getVal());
        y = rot.mul(y);
        
        ((Var)root.rot.rx).setVal(Math.atan2(y.z, y.y));
        rot = MyMatrix.rotationX(-root.rot.rx.getVal());
        
        variables.setVals(solve(null, initeps, 1, false));

        for (Segment s : segments) {
            escapeLocalMin(s, rot.mul(new MyVect(0, 1, 0)), 
                           rot.mul(new MyVect(0, 0, 1)), refiteps);    
        }
        
        variables.setVals(solve(null, finaleps, 1, false));

        propagatePos();
        return toArray();
    }
    
    public double[] getParameters() {
        return variables.getVals().getColumnPackedCopy();
    }
    
    private void escapeLocalMin(Segment s, MyVect yDir, MyVect zDir, double initeps) {
        if (s.from.rot != null && 
           (s.from.rot.rx.isVar() || 
            s.from.rot.ry.isVar())) {
            Matrix origPose = variables.getVals(); 
            Matrix best = origPose;
            double minEffort = evaluateEffort();
            double effort;
            
            //System.out.println(s.st.toString() + ": effort = " + minEffort);
            
            MyVect v = s.to.pos.sub(s.from.pos);
            
            if (s.st == SegmentType.TRUNK) {
                if (s.from.rot.rx.isVar()) {
                    Var var = (Var)s.from.rot.rx;
                    double dangle = 2 * Math.atan2(v.z, v.y);
                    var.setVal(var.getVal() - dangle);
                    
                    if (checkHipOK(s.from, yDir, zDir)) {
                        Joint ls = findJoint(JointType.L_SHOULDER);
                        ((Var)ls.rot.rx).setVal(ls.rot.rx.getVal() + dangle);
                        Joint rs = findJoint(JointType.R_SHOULDER);
                        ((Var)rs.rot.rx).setVal(rs.rot.rx.getVal() + dangle);
                        Joint lh = findJoint(JointType.L_HIP);
                        ((Var)lh.rot.rx).setVal(lh.rot.rx.getVal() + dangle);
                        Joint rh = findJoint(JointType.R_HIP);
                        ((Var)rh.rot.rx).setVal(rh.rot.rx.getVal() + dangle);            

                        variables.setVals(solve(null, initeps, 1, false));

                        if (evaluateEffort() < minEffort) {
                            best = variables.getVals(); 
                        }
                    }
                    variables.setVals(origPose);
                }
            }
            else {
                if (s.from.rot.rx.isVar()) {
                    Var var = (Var)s.from.rot.rx;
                    double angle = -Math.atan2(v.z, v.y);
                    if (var.min < angle && angle < var.max) {
                        var.setVal(angle);  
                        
                        if (checkHipOK(s.from, yDir, zDir)) {
                            variables.setVals(solve(null, initeps, 1, false));

                            effort = evaluateEffort();
                            //System.out.println("RX: effort = " + effort);
                            if (effort < minEffort) {
                                minEffort = effort;
                                best = variables.getVals(); 
                            }
                        }
                        variables.setVals(origPose);
                    }
                }

                if (s.from.rot.ry.isVar() && !s.from.rot.rx.isVar() && s.from.rot.rx.getVal() == 0) {
                    Var var = (Var)s.from.rot.ry;
                    double angle = -Math.atan2(v.x, v.z);
                    if (var.min < angle && angle < var.max) {
                        var.setVal(angle);   
                        
                        if (checkHipOK(s.from, yDir, zDir)) {
                            variables.setVals(solve(null, initeps, 1, false));

                            effort = evaluateEffort();
                            //System.out.println("RY: effort = " + effort);
                            if (effort < minEffort) {
                                minEffort = effort;
                                best = variables.getVals(); 
                            }
                        }
                        variables.setVals(origPose);
                    }
                }

                if (s.from.rot.ry.isVar() && s.from.rot.rx.isVar()) {
                    Var varx = (Var)s.from.rot.rx;
                    Var vary = (Var)s.from.rot.ry;

                    MyMatrix invX = MyMatrix.rotationX(-varx.getVal());
                    MyMatrix invY = MyMatrix.rotationY(-vary.getVal());
                    MyVect vDst = invX.mul(new MyVect(v.x, v.y, -v.z));
                    MyVect vSrc = invY.mul(invX.mul(v));

                    double angley = normalizeAngle(vary.getVal());
                    angley = angley > 0 ? Math.PI - angley : -Math.PI - angley;

                    vSrc = MyMatrix.rotationY(angley).mul(vSrc);

                    double dangle = Math.atan2(vDst.z, vDst.y) - 
                                    Math.atan2(vSrc.z, vSrc.y);
                    double anglex = normalizeAngle(varx.getVal() + dangle);

                    if (varx.min < anglex && anglex < varx.max &&
                        vary.min < angley && angley < vary.max) {
                        varx.setVal(anglex);
                        vary.setVal(angley);

                        if (checkHipOK(s.from, yDir, zDir)) {
                            variables.setVals(solve(null, initeps, 1, false));

                            effort = evaluateEffort();
                            //System.out.println("RX + RY 1: effort = " + effort);
                            if (effort < minEffort) {
                                minEffort = effort;
                                best = variables.getVals(); 
                            }
                        }
                        variables.setVals(origPose);
                    }
                }  

                if (s.from.rot.ry.isVar() && s.from.rot.rx.isVar()) {
                    Var varx = (Var)s.from.rot.rx;
                    Var vary = (Var)s.from.rot.ry;

                    MyMatrix invX = MyMatrix.rotationX(-varx.getVal());
                    MyMatrix invY = MyMatrix.rotationY(-vary.getVal());
                    MyVect vDst = invX.mul(new MyVect(v.x, v.y, v.z));
                    MyVect vSrc = invY.mul(invX.mul(v));

                    double angley = normalizeAngle(vary.getVal());
                    angley = angley > 0 ? Math.PI - angley : -Math.PI - angley;

                    vSrc = MyMatrix.rotationY(angley).mul(vSrc);

                    double dangle = Math.atan2(vDst.z, vDst.y) - 
                                    Math.atan2(vSrc.z, vSrc.y);
                    double anglex = normalizeAngle(varx.getVal() + dangle);

                    if (varx.min < anglex && anglex < varx.max &&
                        vary.min < angley && angley < vary.max) {
                        varx.setVal(anglex);
                        vary.setVal(angley);

                        if (checkHipOK(s.from, yDir, zDir)) {
                            variables.setVals(solve(null, initeps, 1, false));

                            effort = evaluateEffort();
                            //System.out.println("RX + RY 2: effort = " + effort);
                            if (effort < minEffort) {
                                best = variables.getVals(); 
                            }
                        }
                    }
                }            
            }
            
            variables.setVals(best);  
        }
    }
    
    boolean checkHipOK(Joint j, MyVect yDir, MyVect zDir) {
        if (j.jt == JointType.M_HIP) {
            MyMatrix rot = j.rot.getMatrix();
            MyVect y = rot.mul(new MyVect(0, 1, 0));
            MyVect z = rot.mul(new MyVect(0, 0, 1));
            return yDir.dot(y) >= 0 && zDir.dot(z) >= 0;
        }
        else {
            return true;
        }
    }
    
    double normalizeAngle(double angle) {
        int i = (int)Math.floor((angle + Math.PI) / (2 * Math.PI));
        if (i == 0) {
            return angle;
        }
        else {
            return angle - 2 * i * Math.PI;
        }
    }

    private double evaluateEffort() {
        double effort = 0;        
        for (Var v : variables.getVarList()) {
            if (v != xHip && v != yHip && v != scale) {
                effort += v.getEffort();
            }
        }        
        return effort;
    }
}