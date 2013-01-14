/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.common;

import com.common.Expression.EvaluateException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import javax.vecmath.Point2d;

/**
 *
 * @author vdelaitr
 */
public class Expression {

    static final long serialVersionUID = 0L;
    
    static public class ParseException extends Exception {
        static final long serialVersionUID = 0L;
    }
    static public class EvaluateException extends Exception {
        static final long serialVersionUID = 0L;
    }

    /**************************************************************************/

    private static abstract class EvaluableExpression {
        abstract double evaluate(HashMap<String, Double> variables) throws EvaluateException;
        abstract boolean evaluateBoolean(HashMap<String, Double> variables) throws EvaluateException;
        abstract boolean isBoolean() throws ParseException;
        abstract boolean containsVariable() throws ParseException;
        abstract SimpleExpression simplify() throws ParseException;
        @Override
        abstract public String toString();
    }

    /**************************************************************************/

    private static class SimpleExpression extends EvaluableExpression {
        private double cst;
        private HashMap<String, Double> varcoeff;


        SimpleExpression() {
            cst = 0;
            varcoeff = new HashMap<String, Double>();
        }

        SimpleExpression(double c) {
            cst = c;
            varcoeff = new HashMap<String, Double>();
        }

        SimpleExpression(SimpleExpression e) {
            cst = e.cst;
            varcoeff = new HashMap<String, Double>(e.varcoeff);
        }

        double evaluate(HashMap<String, Double> variables) throws EvaluateException {
            double r = cst;
            for (Object v : varcoeff.keySet()) {
                r += varcoeff.get(v).doubleValue() *
                     variables.get(v).doubleValue();
            }
            return r;
        }

        boolean evaluateBoolean(HashMap<String, Double> variables) throws EvaluateException {
            throw new EvaluateException();
        }

        boolean isBoolean() throws ParseException {
            return false;
        }

        boolean containsVariable() throws ParseException {
            return !varcoeff.isEmpty();
        }

        SimpleExpression simplify() throws ParseException {
            return new SimpleExpression(this);
        }

        @Override
        public String toString() {
            String str = Double.toString(cst);
            for (String var : varcoeff.keySet()) {
                double d = varcoeff.get(var).doubleValue();
                str = str + " + " + (d == 1 ? "" : d  + " ") + var;
            }
            return str;
        }

        void add(double value) {
            cst += value;
        }

        void add(String var, double value) {
            if (varcoeff.containsKey(var)) {
                varcoeff.put(var, varcoeff.get(var).doubleValue() + value);
            }
            else {
                varcoeff.put(var, value);
            }
        }

        void add(SimpleExpression e) {
            cst += e.cst;
            for (String var : e.varcoeff.keySet()) {
                add(var, e.varcoeff.get(var).doubleValue());
            }
        }

        void mul(double value) {
            cst *= value;
            for (String var : varcoeff.keySet()) {
                varcoeff.put(var, varcoeff.get(var).doubleValue() * value);
            }
        }

        SimpleExpression getOpposite() {
            SimpleExpression e = new SimpleExpression(-cst);
            for (String var : varcoeff.keySet()) {
                e.add(var, -varcoeff.get(var).doubleValue());
            }

            return e;
        }
    }

    /**************************************************************************/

    private static class Constant extends EvaluableExpression {
        private double cst;

        Constant(double c) {
            cst = c;
        }
        
        double evaluate(HashMap variables) throws EvaluateException {
            return cst;
        }

        boolean evaluateBoolean(HashMap variables) throws EvaluateException {
            throw new EvaluateException();
        }

        boolean isBoolean() throws ParseException {
            return false;
        }

        boolean containsVariable() throws ParseException {
            return false;
        }

        SimpleExpression simplify() throws ParseException {
            return new SimpleExpression(cst);
        }

        @Override
        public String toString() {
            return Double.toString(cst);
        }
    }

    /**************************************************************************/
    
    private static class Variable extends EvaluableExpression {
        private String var;
        private double coef;

        Variable(String v) {
            this(v, 1);
        }

        Variable(String v, double c) {
            var = v;
            coef = c;
        }

        double evaluate(HashMap variables) throws EvaluateException {
            if (variables.containsKey(var)) {
                return ((Double)variables.get(var)).doubleValue() * coef;
            }
            else {
                throw new EvaluateException();
            }
        }

        boolean evaluateBoolean(HashMap variables) throws EvaluateException {
            throw new EvaluateException();
        }

        boolean isBoolean() throws ParseException {
            return false;
        }

        boolean containsVariable() throws ParseException {
            return true;
        }

        SimpleExpression simplify() throws ParseException {
            SimpleExpression e = new SimpleExpression();
            e.add(var, coef);
            return e;
        }

        @Override
        public String toString() {
            return coef == 1 ? var : Double.toString(coef) + " * " + var;
        }
    }

    /**************************************************************************/
   
    private static class UnaryOperator extends EvaluableExpression {
        static enum Type { ADD, SUB };

        private Type type;
        private EvaluableExpression expr;

        UnaryOperator(Type t, EvaluableExpression e) {
            type = t;
            expr = e;
        }

        double evaluate(HashMap<String, Double> variables) throws EvaluateException {
            switch (type) {
                case ADD: return expr.evaluate(variables);
                case SUB: return -expr.evaluate(variables);
                default:  throw new EvaluateException();
            }
        }

        boolean evaluateBoolean(HashMap<String, Double> variables) throws EvaluateException {
            throw new EvaluateException();
        }

        boolean isBoolean() throws ParseException {
            return false;
        }       

        boolean containsVariable() throws ParseException {
            return expr.containsVariable();
        }

        SimpleExpression simplify() throws ParseException {
            SimpleExpression e = expr.simplify();
            switch (type) {
                case ADD: return e;
                case SUB: return e.getOpposite();
                default: throw new ParseException();
            }
        }

        @Override
        public String toString() {
            switch (type) {
                case ADD: return expr.toString();
                case SUB:
                default:  return "-" + expr.toString();
            }
        }

        static boolean takesBoolean(Type type) throws ParseException {
            return false;
        }

        static Type getOperator(char op) throws ParseException {
            switch (op) {
                case '+': return Type.ADD;
                case '-': return Type.SUB;
                default:  throw new ParseException();
            }
        }

        static int precedence(Type type) throws ParseException {
            switch (type) {
                case ADD:
                case SUB: return 2;
                default:  throw new ParseException();
            }
        }
    }

    /**************************************************************************/

    private static class BinaryOperator extends EvaluableExpression {
        static enum Type { ADD, SUB, MUL, DIV, GT, LT, EQ };
        
        private Type type;
        private EvaluableExpression exprLeft, exprRight;

        BinaryOperator(Type t, EvaluableExpression eLeft, EvaluableExpression eRight) throws ParseException {
            if (eRight.containsVariable() && (t == Type.DIV || (t == Type.MUL && eLeft.containsVariable()))) {
                throw new ParseException();
            }
            type = t;
            exprLeft = eLeft;
            exprRight = eRight;
        }

        double evaluate(HashMap<String, Double> variables) throws EvaluateException {
            switch (type) {
                case ADD: return exprLeft.evaluate(variables) + exprRight.evaluate(variables);
                case SUB: return exprLeft.evaluate(variables) - exprRight.evaluate(variables);
                case MUL: return exprLeft.evaluate(variables) * exprRight.evaluate(variables);
                case DIV: return exprLeft.evaluate(variables) / exprRight.evaluate(variables);
                default: throw new EvaluateException();
            }
        }

        boolean evaluateBoolean(HashMap<String, Double> variables) throws EvaluateException {
            switch (type) {
                case GT: return exprLeft.evaluate(variables) >= exprRight.evaluate(variables);
                case LT: return exprLeft.evaluate(variables) <= exprRight.evaluate(variables);
                case EQ: return exprLeft.evaluate(variables) == exprRight.evaluate(variables);
                default: throw new EvaluateException();
            }
        }

        boolean isBoolean() throws ParseException {
            switch (type) {
                case ADD:
                case SUB:
                case MUL:
                case DIV: return false;
                case GT:
                case LT:
                case EQ: return true;
                default: throw new ParseException();
            }
        }

        boolean containsVariable() throws ParseException {
            return exprLeft.containsVariable() || exprRight.containsVariable();
        }

        SimpleExpression simplify() throws ParseException {
            SimpleExpression el = exprLeft.simplify();
            SimpleExpression er = exprRight.simplify();
            switch (type) {
                case ADD:
                    el.add(er);
                    return el;
                case SUB:
                    el.add(er.getOpposite());
                    return el;
                case MUL:
                    if (!el.containsVariable() &&
                        !er.containsVariable()) {
                        el.mul(er.cst);
                        return el;
                    }
                    else if (!el.containsVariable()) {
                        er.mul(el.cst);
                        return er;
                    }
                    else if (!er.containsVariable()) {
                        el.mul(er.cst);
                        return el;
                    }
                    else {
                        throw new ParseException();
                    }
                case DIV:
                    if (!el.containsVariable() &&
                        !er.containsVariable()) {
                        return new SimpleExpression(el.cst / er.cst);
                    }
                    else if (!er.containsVariable()) {
                        el.mul(1 / er.cst);
                        return el;
                    }
                    else {
                        throw new ParseException();
                    }
                case GT:
                case LT:
                case EQ:
                default: throw new ParseException();
            }
        }

        @Override
        public String toString() {
            switch (type) {
                case ADD: return "(" + exprLeft.toString() + ") + (" + exprRight.toString() + ")";
                case SUB: return "(" + exprLeft.toString() + ") - (" + exprRight.toString() + ")";
                case MUL: return "(" + exprLeft.toString() + ") * (" + exprRight.toString() + ")";
                case DIV: return "(" + exprLeft.toString() + ") / (" + exprRight.toString() + ")";
                case GT:  return "(" + exprLeft.toString() + ") > (" + exprRight.toString() + ")";
                case LT:  return "(" + exprLeft.toString() + ") < (" + exprRight.toString() + ")";
                case EQ:
                default:  return "(" + exprLeft.toString() + ") = (" + exprRight.toString() + ")";
            }
        }

        static boolean takesBoolean(Type type) throws ParseException {
            switch (type) {
                case ADD:
                case SUB:
                case MUL:
                case DIV:
                case GT:
                case LT:
                case EQ: return false;
                default: throw new ParseException();
            }
        }

        static Type getOperator(char op) throws ParseException {
            switch (op) {
                case '+': return Type.ADD;
                case '-': return Type.SUB;
                case '*': return Type.MUL;
                case '/': return Type.DIV;
                case '>': return Type.GT;
                case '<': return Type.LT;
                case '=': return Type.EQ;
                default: throw new ParseException();
            }
        }

        static int precedence(Type type) throws ParseException {
            switch (type) {
                case ADD:
                case SUB: return 4;
                case MUL:
                case DIV: return 3;
                case GT:
                case LT:  return 6;
                case EQ:  return 7;
                default: throw new ParseException();
            }
        }
    }

    /**************************************************************************/

    static private final int maximumPrecedence = 15; // max precedence is 14
    
    private static class ParseResult {
        EvaluableExpression expr;
        int i;
        int nParentheses;
        int maxPrec;

        ParseResult(EvaluableExpression e, int nexti, int n, int m) {
            expr = e;
            i = nexti;
            nParentheses = n;
            maxPrec = m;
        }
    }

    /**************************************************************************/

    private EvaluableExpression expr;

    private Expression(EvaluableExpression e) {
        expr = e;
    }

    /**************************************************************************/

    public boolean isBoolean() {
        try {
            return expr.isBoolean();
        }
        catch (ParseException e) {
            return false;
        }
    }

    public double evaluate(HashMap<String, Double> variables) throws EvaluateException {
        return expr.evaluate(variables);
    }

    public boolean evaluateBoolean(HashMap<String, Double> variables) throws EvaluateException {
        return expr.evaluateBoolean(variables);
    }

    public Expression simplify() {
        try {
            return new Expression(expr.simplify());
        }
        catch (ParseException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return expr.toString();
    }

    /**************************************************************************/

    static public Expression parse(String s) throws ParseException {
        ParseResult res = parse(s, 0, maximumPrecedence, 0);
        if (res.nParentheses > 0) {
            throw new ParseException();
        }
        else {
            return new Expression(res.expr);
        }
    }

    static public Expression parseNumericExpression(String s) throws ParseException {
        ParseResult res = parse(s, 0, maximumPrecedence, 0);
        if (res.nParentheses > 0 || res.expr.isBoolean()) {
            throw new ParseException();
        }
        else {
            return new Expression(res.expr);
        }
    }

    static public Expression parseBooleanExpression(String s) throws ParseException {
        ParseResult res = parse(s, 0, maximumPrecedence, 0);
        if (res.nParentheses > 0 || !res.expr.isBoolean()) {
            throw new ParseException();
        }
        else {
            return new Expression(res.expr);
        }
    }

    /**************************************************************************/

    static public Expression Zero() {
        return new Expression(new Constant(0));
    }

    static public Expression Add(Expression a, Expression b) throws ParseException {
        if (!a.isBoolean() && !b.isBoolean()) {
            return new Expression(new BinaryOperator(BinaryOperator.Type.ADD, a.expr, b.expr));
        }
        else {
            throw new ParseException();
        }
    }

    public Expression Add(Expression b) throws ParseException {
        return Add(this, b);
    }

    static public Expression Sub(Expression a, Expression b) throws ParseException {
        if (!a.isBoolean() && !b.isBoolean()) {
            return new Expression(new BinaryOperator(BinaryOperator.Type.SUB, a.expr, b.expr));
        }
        else {
            throw new ParseException();
        }
    }

    public Expression Sub(Expression b) throws ParseException {
        return Sub(this, b);
    }

    static public Expression Times(Expression a, double s) throws ParseException {
        if (!a.isBoolean()) {
            return new Expression(new BinaryOperator(BinaryOperator.Type.MUL, a.expr, new Constant(s)));
        }
        else {
            throw new ParseException();
        }
    }

    public Expression Times(double s) throws ParseException {
        return Times(this, s);
    }


    /**************************************************************************/

    public HashMap<String, Double> getVariables() throws ParseException {
        SimpleExpression e = expr.simplify();
        return e.varcoeff;
    }

    /**************************************************************************/

    private static ParseResult parse(String s, int i,
                                     int maxPrec, int nParentheses)
                                     throws ParseException {
        EvaluableExpression expr = null;        
        while (i < s.length()) {           
            switch (s.charAt(i)) {
                case ' ':
                    i++;
                    continue;
                case '(':
                    return parse(s, i + 1, maximumPrecedence, nParentheses + 1);
                case ')':
                    if (expr == null || nParentheses == 0) {
                        throw new ParseException();
                    }
                    else {
                        return new ParseResult(expr, i + 1, nParentheses - 1, 0);
                    }
                default: {
                    if (expr == null) {
                        if (isConstant(s, i)) {
                            ParseResult res = parseConstant(s, i, nParentheses, maxPrec);
                            expr = res.expr;
                            i = res.i;
                            nParentheses = res.nParentheses;
                            maxPrec = res.maxPrec;
                        }
                        else if (isVariable(s, i)) {
                            ParseResult res = parseVariable(s, i, nParentheses, maxPrec);
                            expr = res.expr;
                            i = res.i;
                            nParentheses = res.nParentheses;
                            maxPrec = res.maxPrec;
                        }
                        else {
                            UnaryOperator.Type t = UnaryOperator.getOperator(s.charAt(i));
                            int prec = UnaryOperator.precedence(t);
                            if (prec < maxPrec) {
                                ParseResult res = parse(s, i + 1, prec, nParentheses);
                                if (!(UnaryOperator.takesBoolean(t) ^ res.expr.isBoolean())) {
                                    expr = new UnaryOperator(t, res.expr);
                                    i = res.i;
                                    nParentheses = res.nParentheses;
                                    maxPrec = Math.min(maxPrec, res.maxPrec);
                                }
                                else {
                                    throw new ParseException();
                                }
                            }
                            else {
                                return new ParseResult(expr, i, nParentheses, maximumPrecedence);
                            }
                        }
                    }
                    else {
                        BinaryOperator.Type t = BinaryOperator.getOperator(s.charAt(i));
                        int prec = BinaryOperator.precedence(t);
                        if (prec < maxPrec) {
                            ParseResult res = parse(s, i + 1, prec, nParentheses);                            
                            if (!(BinaryOperator.takesBoolean(t) ^ res.expr.isBoolean())) {
                                expr = new BinaryOperator(t, expr, res.expr);
                                i = res.i;
                                nParentheses = res.nParentheses;
                                maxPrec = Math.min(maxPrec, res.maxPrec);
                            }
                            else {
                                throw new ParseException();
                            }    
                        }
                        else {
                            return new ParseResult(expr, i, nParentheses, maximumPrecedence);
                        }
                    }
                }
            }
        }
        if (expr == null) {
            throw new ParseException();
        }
        else {
            return new ParseResult(expr, i, nParentheses, maximumPrecedence);
        }
    }

    private static ParseResult parseConstant(String s, int i, int nParentheses, int maxPrec) throws ParseException {
        int begin = i++;
        while (i < s.length() && isConstant(s, i)) {
            i++;
        }
        try {
            double v = Float.parseFloat(s.substring(begin, i));
            return new ParseResult(new Constant(v), i, nParentheses, maxPrec);
        }
        catch (NumberFormatException e) {
            throw new ParseException();
        }
    }

    private static ParseResult parseVariable(String s, int i, int nParentheses, int maxPrec) throws ParseException {
        int begin = i++;
        while (i < s.length() && isVariable(s, i)) {
            i++;
        }
        return new ParseResult(new Variable(s.substring(begin, i)), i, nParentheses, maxPrec);
    }

    private static boolean isConstant(String s, int i) {
         char c = s.charAt(i);
         return ('0' <= c && c <= '9') || c == '.';
    }

    private static boolean isVariable(String s, int i) {
         char c = s.charAt(i);
         return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
    }

    /**************************************************************************/

    @SuppressWarnings("unchecked")
    static public void test() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        HashMap variables = new HashMap();
        Expression expr;
        while(true) {
            System.out.print(">> ");
            String str = null;
            try {
                str = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            int index = str.indexOf("<-");
            if (index == -1) {
                try {
                    expr = Expression.parse(str);
                    System.out.println("Expr: " + expr.toString());
                    if (expr.isBoolean()) {
                        boolean b = expr.evaluateBoolean(variables);
                        System.out.println(b);
                    }
                    else {
                        double v = expr.evaluate(variables);
                        System.out.println(v);
                    }
                }
                catch (ParseException e) {
                    System.out.println("Error while parsing!");
                }
                catch (EvaluateException e) {
                    System.out.println("Error while evaluating!");
                }
            }
            else {
                String var = str.substring(0, index).trim();
                try {
                    double val = Float.parseFloat(str.substring(index+2).trim());
                    variables.put(var, val);
                    System.out.println(var + " = " + val + " !");
                }
                catch (NumberFormatException e) {
                    System.out.println("Error while parsing double!");
                    e.printStackTrace();
                }
            }
         }
     }
}
