import java.util.ArrayList;

import IR.token.Identifier;
import IR.token.Label;
import minijava.syntaxtree.AndExpression;
import minijava.syntaxtree.ArrayAllocationExpression;
import minijava.syntaxtree.ArrayLength;
import minijava.syntaxtree.AssignmentStatement;
import minijava.syntaxtree.CompareExpression;
import minijava.syntaxtree.FalseLiteral;
import minijava.syntaxtree.IfStatement;
import minijava.syntaxtree.IntegerLiteral;
import minijava.syntaxtree.MinusExpression;
import minijava.syntaxtree.NotExpression;
import minijava.syntaxtree.PlusExpression;
import minijava.syntaxtree.PrintStatement;
import minijava.syntaxtree.ThisExpression;
import minijava.syntaxtree.TimesExpression;
import minijava.syntaxtree.TrueLiteral;
import minijava.syntaxtree.WhileStatement;
import minijava.visitor.DepthFirstVisitor;
import sparrow.Add;
import sparrow.Alloc;
import sparrow.ErrorMessage;
import sparrow.Goto;
import sparrow.IfGoto;
import sparrow.Instruction;
import sparrow.LabelInstr;
import sparrow.LessThan;
import sparrow.Load;
import sparrow.Move_Id_Id;
import sparrow.Move_Id_Integer;
import sparrow.Multiply;
import sparrow.Print;
import sparrow.Program;
import sparrow.Store;
import sparrow.Subtract;

public class SparrowGenerator extends DepthFirstVisitor {
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
        return "v" + (tempCounter++);
    }

    // Get generated Sparrow program
    public String getGeneratedCode() {
        return code.toString();
    }

    // Get generated instructions
    public ArrayList<Instruction> getCurrentInstructions() {
        return currentInstructions;
    }

    // Get last result
    public Identifier getLastResult() {
        return lastResult;
    }

    // Check conflict with reserved register names
    private boolean isReservedRegister(String id) {
        // 'a' registers
        for (int i = 2; i <= 7; i++)
            if (id.equals("a" + i))
                return true;

        // 's' registers
        for (int i = 1; i <= 11; i++) {
            if (id.equals("s" + i))
                return true;
        }

        // 't' registers
        for (int i = 0; i <= 5; i++) {
            if (id.equals("t" + i))
                return true;
        }

        return false;
    }

    /**
     * Statements -> Sparrow instructions
     */
    @Override
    public void visit(AssignmentStatement n) {
        n.f0.accept(this);
        Identifier lhs = lastResult;
        n.f2.accept(this);
        Identifier rhs = lastResult;

        currentInstructions.add(new Move_Id_Id(lhs, rhs));
    }

    // TODO: ArrayAssignmentStatement

    @Override
    public void visit(IfStatement n) {
        String elseLabel = "else_" + (tempCounter++);
        String endLabel = "endif_" + (tempCounter++);

        n.f2.accept(this);
        Identifier condition = lastResult;
        currentInstructions.add(new IfGoto(condition, new Label(elseLabel)));

        n.f4.accept(this);
        currentInstructions.add(new Goto(new Label(endLabel)));

        currentInstructions.add(new LabelInstr(new Label(elseLabel)));
        n.f6.accept(this);

        currentInstructions.add(new LabelInstr(new Label(endLabel)));
    }

    @Override
    public void visit(WhileStatement n) {
        String startLabel = "while_" + tempCounter++;
        String endLabel = "endwhile_" + tempCounter++;

        currentInstructions.add(new LabelInstr(new Label(startLabel)));
        n.f2.accept(this);
        Identifier condition = lastResult;
        currentInstructions.add(new IfGoto(condition, new Label(endLabel)));

        n.f4.accept(this);
        currentInstructions.add(new Goto(new Label(startLabel)));
        currentInstructions.add(new LabelInstr(new Label(endLabel)));
    }

    @Override
    public void visit(PrintStatement n) {
        n.f2.accept(this);
        Identifier value = lastResult;

        currentInstructions.add(new Print(value));
    }

    /**
     * Expressions -> Sparrow instructions
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
    public void visit(TimesExpression n) {
        n.f0.accept(this);
        Identifier op1 = lastResult;
        n.f2.accept(this);
        Identifier op2 = lastResult;

        lastResult = new Identifier(getNewTemp());
        currentInstructions.add(new Multiply(lastResult, op1, op2));
    }
    
    // TODO: ArrayLookup
    
    @Override
    public void visit(ArrayLength n) {
        n.f0.accept(this);

        Identifier arr = lastResult;
        
        // Load array length from heap
        lastResult = new Identifier(getNewTemp());
        currentInstructions.add(new Load(lastResult, arr, 0));
    }

    // TODO: MessageSend
    // TODO: ExpressionList
    // TODO: ExpressionRest

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

    @Override
    public void visit(minijava.syntaxtree.Identifier n) {
        String id = n.f0.toString();

        if (isReservedRegister(id)) {
            // Mangle Identifier if there is a name conflict
            String mangledName = "var_" + id;
            lastResult = new Identifier(mangledName);
        } else {
            lastResult = new Identifier(id);
        }
    }

    @Override
    public void visit(ThisExpression n) {
        lastResult = new Identifier("this");
    }

    @Override
    public void visit(ArrayAllocationExpression n) {
        n.f3.accept(this);
        // Number of elements in array ... n
        Identifier numElements = lastResult;

        // Check if numElements > 0
        String errMsg = "\"Error: Array allocation with negative size\"";

        String elseLabel = "else_" + (tempCounter++);
        String endLabel = "endif_" + (tempCounter++);

        Identifier zero = new Identifier(getNewTemp());
        currentInstructions.add(new Move_Id_Integer(zero, 0));

        Identifier isNonNeg = new Identifier(getNewTemp());
        currentInstructions.add(new LessThan(isNonNeg, zero, numElements));

        currentInstructions.add(new IfGoto(isNonNeg, new Label(elseLabel)));
        
        // If numElements > 0
        // Byte size of array ... 4 * n + 4
        Identifier four = new Identifier(getNewTemp());
        currentInstructions.add(new Move_Id_Integer(four, 4));
        Identifier product = new Identifier(getNewTemp());
        currentInstructions.add(new Multiply(product, four, numElements));
        Identifier sz = new Identifier(getNewTemp());
        currentInstructions.add(new Add(sz, product, four));

        // Allocate array with byte size
        lastResult = new Identifier(getNewTemp());
        currentInstructions.add(new Alloc(lastResult, sz));

        // Store number of elements
        currentInstructions.add(new Store(lastResult, 0, numElements));

        // Goto endif
        currentInstructions.add(new Goto(new Label(endLabel)));

        // Else block
        currentInstructions.add(new LabelInstr(new Label(elseLabel)));
        currentInstructions.add(new ErrorMessage(errMsg));

        // Endif
        currentInstructions.add(new LabelInstr(new Label(endLabel)));
    }

    // TODO: AllocationExpression

    @Override
    public void visit(NotExpression n) {
        n.f1.accept(this);
        Identifier op = lastResult;

        lastResult = new Identifier(getNewTemp());

        Identifier one = new Identifier(getNewTemp());
        currentInstructions.add(new Move_Id_Integer(one, 1));

        currentInstructions.add(new Subtract(lastResult, one, op));
    }

}
