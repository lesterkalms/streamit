/*
 * DoComplexProp.java: perform constant propagation on function bodies
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: DoComplexProp.java,v 1.4 2002-09-18 15:59:55 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.ArrayList;

/**
 * DoComplexProp does all of the work required to convert complex
 * expressions to real ones in front-end code.  It builds up symbol
 * tables for variables, and then runs VarToComplex to replace
 * references to complex variables with complex expressions referencing
 * the fields of the variables.  ComplexProp is then run to cause an
 * expression to be either purely real or be an ExprComplex at the
 * top level.  Finally, this pass inserts statements as necessary to
 * cause all statements to deal with purely real values.
 *
 * Things this is known to punt on:
 * -- Some things (parameters to function calls, enqueue(), push(), 
 *    return) need to be actual variables rather than complex
 *    expressions.  Generate temporary variables for these if needed.
 * -- Semantics of for loops (for(complex c = 1+1i; abs(c) < 5; c += 1i))
 */
public class DoComplexProp extends FEReplacer
{
    private StreamType streamType;
    private LinkedList oldStreamTypes;
    private SymbolTable symTab;
    private GetExprType getExprType;
    private VarToComplex varToComplex;
    private ComplexProp cplxProp;
    
    public DoComplexProp()
    {
        streamType = null;
        oldStreamTypes = new LinkedList();
        symTab = null;
        varToComplex = null;
        cplxProp = new ComplexProp();
    }

    private void pushSymTab()
    {
        symTab = new SymbolTable(symTab);
        varToComplex = new VarToComplex(symTab, streamType);
        getExprType = new GetExprType(symTab, streamType);
    }
    
    private void popSymTab()
    {
        symTab = symTab.getParent();
        varToComplex = new VarToComplex(symTab, streamType);
        getExprType = new GetExprType(symTab, streamType);
    }

    private void paramListToSymTab(List params)
    {
        for (Iterator iter = params.iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            symTab.register(param.getName(), param.getType());
        }
    }

    private void pushStreamType(StreamType st)
    {
        oldStreamTypes.addFirst(streamType);
        streamType = st;
        varToComplex = new VarToComplex(symTab, streamType);
        getExprType = new GetExprType(symTab, streamType);
    }
    
    private void popStreamType()
    {
        streamType = (StreamType)oldStreamTypes.removeFirst();
        varToComplex = new VarToComplex(symTab, streamType);
        getExprType = new GetExprType(symTab, streamType);
    }

    private Expression doExprProp(Expression expr)
    {
        expr = (Expression)expr.accept(varToComplex);
        expr = (Expression)expr.accept(cplxProp);
        return expr;
    }

    protected Expression doExpression(Expression expr)
    {
        return doExprProp(expr);
    }

    public Object visitFunction(Function func)
    {
        pushSymTab();
        paramListToSymTab(func.getParams());
        Object result = super.visitFunction(func);
        popSymTab();
        return result;
    }

    public Object visitFuncWork(FuncWork func)
    {
        pushSymTab();
        paramListToSymTab(func.getParams());
        Object result = super.visitFuncWork(func);
        popSymTab();
        return result;
    }

    public Object visitStmtAssign(StmtAssign stmt)
    {
        Expression lhs = stmt.getLHS();
        Expression rhs = doExprProp(stmt.getRHS());
        if (rhs instanceof ExprComplex)
        {
            ExprComplex cplx = (ExprComplex)rhs;
            addStatement(new StmtAssign(stmt.getContext(),
                                        new ExprField(lhs.getContext(),
                                                      lhs, "real"),
                                        cplx.getReal()));
            addStatement(new StmtAssign(stmt.getContext(),
                                        new ExprField(lhs.getContext(),
                                                      lhs, "imag"),
                                        cplx.getImag()));
            return null;
        }
        else if (((Type)lhs.accept(getExprType)).isComplex() &&
                 !((Type)rhs.accept(getExprType)).isComplex())
        {
            addStatement(new StmtAssign(stmt.getContext(),
                                        new ExprField(lhs.getContext(),
                                                     lhs, "real"),
                                        rhs));
            addStatement(new StmtAssign(stmt.getContext(),
                                        new ExprField(lhs.getContext(),
                                                      lhs, "imag"),
                                        new ExprConstInt(lhs.getContext(),
                                                         0)));
            return null;
        }
        else if (rhs != stmt.getRHS())
            return new StmtAssign(stmt.getContext(), lhs, rhs);
        else
            return stmt;
    }

    public Object visitStmtBlock(StmtBlock block)
    {
        pushSymTab();
        Statement result = (Statement)super.visitStmtBlock(block);
        popSymTab();
        return result;
    }

    public Object visitStmtExpr(StmtExpr stmt)
    {
        Expression newExpr = doExprProp(stmt.getExpression());
        if (newExpr instanceof ExprComplex)
        {
            ExprComplex cplx = (ExprComplex)newExpr;
            addStatement(new StmtExpr(stmt.getContext(), cplx.getReal()));
            addStatement(new StmtExpr(stmt.getContext(), cplx.getImag()));
            return null;
        }
        if (newExpr == stmt.getExpression())
            return stmt;
        return new StmtExpr(stmt.getContext(), newExpr);
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        // Assume there's no init part for now.
        symTab.register(stmt.getName(), stmt.getType());
        return stmt;
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        pushSymTab();
        pushStreamType(spec.getStreamType());
        paramListToSymTab(spec.getParams());
        
        Object result = super.visitStreamSpec(spec);

        popStreamType();
        popSymTab();

        return result;
    }
}
