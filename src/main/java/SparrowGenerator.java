import java.util.ArrayList;

import IR.token.Identifier;
import minijava.syntaxtree.AndExpression;
import minijava.syntaxtree.CompareExpression;
import minijava.syntaxtree.FalseLiteral;
import minijava.syntaxtree.IntegerLiteral;
import minijava.syntaxtree.MinusExpression;
import minijava.syntaxtree.PlusExpression;
import minijava.syntaxtree.TrueLiteral;
import minijava.visitor.DepthFirstVisitor;
import sparrow.Add;
import sparrow.Instruction;
import sparrow.LessThan;
import sparrow.Move_Id_Integer;
import sparrow.Multiply;
import sparrow.Program;
import sparrow.Subtract;

public class SparrowGenerator extends DepthFirstVisitor{
    private ArrayList<Instruction> currentInstructions;
    private int tempCounter;
    private Program code;
    private Identifier lastResult;

    public SparrowGenerator() {
        this.code = new Program();
        this.tempCounter = 0;
        this.currentInstructions = new ArrayList<>();
        this.lastResult = null;
    }

    // Get next available temp variable
    private String getNewTemp() {
        return "t" + (tempCounter++);
    }

    /**
     * Expression translations
     */
    @Override
    public void visit(AndExpression n) {
        n.f0.accept(this);
        Identifier op1 = lastResult;
        n.f2.accept(this);
        Identifier op2 = lastResult;

        lastResult = new Identifier(getNewTemp());
        currentInstructions.add(new Multiply(lastResult, op1, op2));
    }

    @Override
    public void visit(CompareExpression n) {
        n.f0.accept(this);
        Identifier op1 = lastResult;
        n.f2.accept(this);
        Identifier op2 = lastResult;

        lastResult = new Identifier(getNewTemp());
        currentInstructions.add(new LessThan(lastResult, op1, op2));
    }

    @Override
    public void visit(PlusExpression n) {
        n.f0.accept(this);
        Identifier op1 = lastResult;
        n.f2.accept(this);
        Identifier op2 = lastResult;

        lastResult = new Identifier(getNewTemp());
        currentInstructions.add(new Add(lastResult, op1, op2));
    }

    @Override
    public void visit(MinusExpression n) {
        n.f0.accept(this);
        Identifier op1 = lastResult;
        n.f2.accept(this);
        Identifier op2 = lastResult;

        lastResult = new Identifier(getNewTemp());
        currentInstructions.add(new Subtract(lastResult, op1, op2));
    }

    @Override
    public void visit(IntegerLiteral n) {
        int val = Integer.parseInt(n.f0.toString());
        lastResult = new Identifier(getNewTemp());
        currentInstructions.add(new Move_Id_Integer(lastResult, val));
    }

    @Override
    public void visit(TrueLiteral n) {
        lastResult = new Identifier(getNewTemp());
        currentInstructions.add(new Move_Id_Integer(lastResult, 1));
    }

    @Override
    public void visit(FalseLiteral n) {
        lastResult = new Identifier(getNewTemp());
        currentInstructions.add(new Move_Id_Integer(lastResult, 0));
    }

}
