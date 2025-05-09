import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import IR.token.FunctionName;
import IR.token.Identifier;
import IR.token.Label;
import minijava.syntaxtree.AllocationExpression;
import minijava.syntaxtree.AndExpression;
import minijava.syntaxtree.ArrayAllocationExpression;
import minijava.syntaxtree.ArrayAssignmentStatement;
import minijava.syntaxtree.ArrayLength;
import minijava.syntaxtree.ArrayLookup;
import minijava.syntaxtree.AssignmentStatement;
import minijava.syntaxtree.ClassDeclaration;
import minijava.syntaxtree.CompareExpression;
import minijava.syntaxtree.FalseLiteral;
import minijava.syntaxtree.FormalParameterList;
import minijava.syntaxtree.FormalParameterRest;
import minijava.syntaxtree.IfStatement;
import minijava.syntaxtree.IntegerLiteral;
import minijava.syntaxtree.MainClass;
import minijava.syntaxtree.MessageSend;
import minijava.syntaxtree.MethodDeclaration;
import minijava.syntaxtree.MinusExpression;
import minijava.syntaxtree.Node;
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
import sparrow.Block;
import sparrow.ErrorMessage;
import sparrow.FunctionDecl;
import sparrow.Goto;
import sparrow.IfGoto;
import sparrow.Instruction;
import sparrow.LabelInstr;
import sparrow.LessThan;
import sparrow.Load;
import sparrow.Move_Id_FuncName;
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
    private ArrayList<Identifier> params;
    private final HashSet<String> reservedRegisters;
    private final HashMap<String, ClassLayout> classLayouts;
    // HashMap<String, String> objTypeMap;
    String currentClass;
    ClassLayout currentLayout;

    public SparrowGenerator(HashMap<String, ClassLayout> layouts) {
        this.code = new Program(new ArrayList<>());
        this.tempCounter = 0;
        this.currentInstructions = new ArrayList<>();
        this.lastResult = null;
        this.classLayouts = layouts;
        this.reservedRegisters = new HashSet<>();

        // Populate reserved registers hashset
        // 'a' registers
        for (int i = 2; i <= 7; i++)
            reservedRegisters.add("a" + i);

        // 's' registers
        for (int i = 1; i <= 11; i++)
            reservedRegisters.add("s" + i);

        // 't' registers
        for (int i = 0; i <= 5; i++)
            reservedRegisters.add("t" + i);
    }

    /**
     * Helper functions
     */
    private Identifier getNewTemp() {
        return new Identifier("v" + (tempCounter++));
    }

    public Identifier resolveName(String name) {
        // Mangle if name conflict
        return reservedRegisters.contains(name) ? new Identifier("var_" + name) : new Identifier(name);
    }

    private boolean isField(Identifier id) {
        return currentLayout != null && currentLayout.fieldOffsets.containsKey(id.toString());
    }

    public String getGeneratedCode() {
        return code.toString();
    }

    public ArrayList<Instruction> getCurrentInstructions() {
        return currentInstructions;
    }

    public Identifier getLastResult() {
        return lastResult;
    }

    /**
     * Classes -> Sparrow instructions
     */
    @Override
    public void visit(MainClass n) {
        currentClass = n.f1.f0.toString();

        // Create block of current instuctions
        ArrayList<Instruction> savedInstructions = currentInstructions;
        currentInstructions = new ArrayList<>();

        // Accept statements
        n.f15.accept(this);
        
        // Set return value for main function block
        Identifier returnId = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(returnId, 0));

        Block block = new Block(currentInstructions, returnId);

        // Create a function with name "main"
        FunctionDecl mainFunc = new FunctionDecl(new FunctionName("main"), new ArrayList<>(), block);

        code.funDecls.add(mainFunc);
        currentInstructions = savedInstructions;
    }

    @Override
    public void visit(ClassDeclaration n) {
        this.currentClass = n.f1.f0.toString();
        this.currentLayout = classLayouts.get(currentClass);

        n.f3.accept(this);
        n.f4.accept(this);
    }

    /**
     * Methods -> Sparrow instructions
     */
    @Override
    public void visit(MethodDeclaration n) {
        ArrayList<Instruction> savedInstructions = currentInstructions;
        currentInstructions = new ArrayList<>();
        params = new ArrayList<>();
        tempCounter = 0;

        // Process function name
        n.f2.accept(this);

        // Process formal parameters
        if (n.f4.present()) {
            n.f4.accept(this);
        }

        // Process variable declaration
        for (Node varDecl : n.f7.nodes) {
            varDecl.accept(this);
        }

        // Process statements
        n.f8.accept(this);
        n.f10.accept(this);
        Identifier returnId = lastResult;

        // Create block (variable declaration + statements + return)
        Block block = new Block(currentInstructions, returnId);

        // Create function declaration
        FunctionDecl methodDecl = new FunctionDecl(new FunctionName(n.f2.f0.toString()), params, block);

        code.funDecls.add(methodDecl);
        currentInstructions = savedInstructions;

    }

    @Override
    public void visit(FormalParameterList n) {
        n.f0.accept(this);
        params.add(lastResult);

        for (Node paramRest: n.f1.nodes) {
            ((FormalParameterRest) paramRest).f1.accept(this);
            params.add(lastResult);
        }
    }

    /**
     * Statements -> Sparrow instructions
     */
    @Override
    public void visit(AssignmentStatement n) {
        n.f2.accept(this);
        Identifier rhs = lastResult;

        Identifier lhs = resolveName(n.f0.f0.toString());

        if (isField(lhs)) {
            int lhsOffset = currentLayout.fieldOffsets.get(lhs.toString());
            currentInstructions.add(new Store(new Identifier("this"), lhsOffset, rhs));
        } else {
            currentInstructions.add(new Move_Id_Id(lhs, rhs));
        }
    }

    @Override
    public void visit(ArrayAssignmentStatement n) {
        Label elseLabel = new Label("else_" + (tempCounter++));
        Label checkUpper = new Label("check_upper" + (tempCounter++));
        Label endIf = new Label("endif_" + (tempCounter++));
        Label error = new Label("error_" + (tempCounter++));
        String errMsg = "\"ArrayIndexOutOfBoundsException\"";

        // Get array address
        n.f0.accept(this);
        Identifier arr = lastResult;
        Identifier size = getNewTemp();
        currentInstructions.add(new Load(size, arr, 0)); // Load array size from offset 0
        
        // Get index
        n.f2.accept(this);
        Identifier idx = lastResult;
        
        // Check idx < 0. Error if true
        Identifier zero = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(zero, 0));
        Identifier tooSmall = getNewTemp();
        currentInstructions.add(new LessThan(tooSmall, idx, zero));
        currentInstructions.add(new IfGoto(tooSmall, checkUpper));
        currentInstructions.add(new Goto(error));

        // Check size - 1 < idx. Error if false
        currentInstructions.add(new LabelInstr(checkUpper));
        Identifier tooBig = getNewTemp();
        Identifier one = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(one, 1));
        Identifier upperBound = getNewTemp();
        currentInstructions.add(new Subtract(upperBound, size, one));
        currentInstructions.add(new LessThan(tooBig, upperBound, idx));
        currentInstructions.add(new IfGoto(tooBig, elseLabel));
        currentInstructions.add(new Goto(error));

        // Else block: Okay if -1 < idx < size. Compute offset
        currentInstructions.add(new LabelInstr(elseLabel));
        Identifier temp = getNewTemp();
        currentInstructions.add(new Add(temp, one, idx));
        Identifier four = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(four, 4));
        Identifier byteOffset = getNewTemp();
        currentInstructions.add(new Multiply(byteOffset, four, temp));

        // Store from temp into arr[idx]
        n.f5.accept(this);
        Identifier val = lastResult;
        currentInstructions.add(new Store(byteOffset, 0, val));
        currentInstructions.add(new Goto(endIf));

        // Error block
        currentInstructions.add(new LabelInstr(error));
        currentInstructions.add(new ErrorMessage(errMsg));

        // Endif
        currentInstructions.add(new LabelInstr(endIf));
    }

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

        lastResult = getNewTemp();
        currentInstructions.add(new Multiply(lastResult, op1, op2));
    }

    @Override
    public void visit(CompareExpression n) {
        n.f0.accept(this);
        Identifier op1 = lastResult;
        n.f2.accept(this);
        Identifier op2 = lastResult;

        lastResult = getNewTemp();
        currentInstructions.add(new LessThan(lastResult, op1, op2));
    }

    @Override
    public void visit(PlusExpression n) {
        n.f0.accept(this);
        Identifier op1 = lastResult;
        n.f2.accept(this);
        Identifier op2 = lastResult;

        lastResult = getNewTemp();
        currentInstructions.add(new Add(lastResult, op1, op2));
    }

    @Override
    public void visit(MinusExpression n) {
        n.f0.accept(this);
        Identifier op1 = lastResult;
        n.f2.accept(this);
        Identifier op2 = lastResult;

        lastResult = getNewTemp();
        currentInstructions.add(new Subtract(lastResult, op1, op2));
    }

    @Override
    public void visit(TimesExpression n) {
        n.f0.accept(this);
        Identifier op1 = lastResult;
        n.f2.accept(this);
        Identifier op2 = lastResult;

        lastResult = getNewTemp();
        currentInstructions.add(new Multiply(lastResult, op1, op2));
    }

    @Override
    public void visit(ArrayLookup n) {
        Label elseLabel = new Label("else_" + (tempCounter++));
        Label checkUpper = new Label("check_upper" + (tempCounter++));
        Label endIf = new Label("endif_" + (tempCounter++));
        Label error = new Label("error_" + (tempCounter++));
        String errMsg = "\"ArrayIndexOutOfBoundsException\"";

        // Get array address
        n.f0.accept(this);
        Identifier arr = lastResult;
        Identifier size = getNewTemp();
        currentInstructions.add(new Load(size, arr, 0)); // Load array size from offset 0
        
        // Get index
        n.f2.accept(this);
        Identifier idx = lastResult;
        
        // Check idx < 0. Error if true
        Identifier zero = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(zero, 0));
        Identifier tooSmall = getNewTemp();
        currentInstructions.add(new LessThan(tooSmall, idx, zero));
        currentInstructions.add(new IfGoto(tooSmall, checkUpper));
        currentInstructions.add(new Goto(error));

        // Check size - 1 < idx. Error if false
        currentInstructions.add(new LabelInstr(checkUpper));
        Identifier tooBig = getNewTemp();
        Identifier one = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(one, 1));
        Identifier upperBound = getNewTemp();
        currentInstructions.add(new Subtract(upperBound, size, one));
        currentInstructions.add(new LessThan(tooBig, upperBound, idx));
        currentInstructions.add(new IfGoto(tooBig, elseLabel));
        currentInstructions.add(new Goto(error));

        // Else block: Okay if -1 < idx < size. Compute offset
        currentInstructions.add(new LabelInstr(elseLabel));
        Identifier temp = getNewTemp();
        currentInstructions.add(new Add(temp, one, idx));
        Identifier four = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(four, 4));
        Identifier byteOffset = getNewTemp();
        currentInstructions.add(new Multiply(byteOffset, four, temp));

        // Load arr[idx] into new temp
        lastResult = getNewTemp();
        currentInstructions.add(new Load(lastResult, byteOffset, 0));
        currentInstructions.add(new Goto(endIf));

        // Error block
        currentInstructions.add(new LabelInstr(error));
        currentInstructions.add(new ErrorMessage(errMsg));

        // Endif
        currentInstructions.add(new LabelInstr(endIf));
    }

    @Override
    public void visit(ArrayLength n) {
        n.f0.accept(this);

        Identifier arr = lastResult;
        
        // Load array length from heap
        lastResult = getNewTemp();
        currentInstructions.add(new Load(lastResult, arr, 0));
    }

    // TODO: MessageSend
    @Override
    public void visit(MessageSend n) {

    }
    // TODO: ExpressionList
    // TODO: ExpressionRest

    @Override
    public void visit(IntegerLiteral n) {
        int val = Integer.parseInt(n.f0.toString());
        lastResult = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(lastResult, val));
    }

    @Override
    public void visit(TrueLiteral n) {
        lastResult = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(lastResult, 1));
    }

    @Override
    public void visit(FalseLiteral n) {
        lastResult = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(lastResult, 0));
    }

    @Override
    public void visit(minijava.syntaxtree.Identifier n) {
        String id = n.f0.toString();

        if (isField(new Identifier(id))) {
            int offset = currentLayout.fieldOffsets.get(id);
            Identifier temp = getNewTemp();
            currentInstructions.add(new Load(temp, new Identifier("this"), offset));
            lastResult = temp;
        } else {
            lastResult = resolveName(id);
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

        Identifier zero = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(zero, 0));

        Identifier isNonNeg = getNewTemp();
        currentInstructions.add(new LessThan(isNonNeg, zero, numElements));

        currentInstructions.add(new IfGoto(isNonNeg, new Label(elseLabel)));
        
        // If numElements > 0
        // Byte size of array ... 4 * n + 4
        Identifier four = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(four, 4));
        Identifier product = getNewTemp();
        currentInstructions.add(new Multiply(product, four, numElements));
        Identifier sz = getNewTemp();
        currentInstructions.add(new Add(sz, product, four));

        // Allocate array with byte size
        lastResult = getNewTemp();
        currentInstructions.add(new Alloc(lastResult, sz));
        // Error check if alloc failed
        String errorLabel = "nullCheck_" + tempCounter++;
        currentInstructions.add(new IfGoto(lastResult, new Label(errorLabel)));
        currentInstructions.add(new Goto(new Label(endLabel)));

        // Store number of elements
        currentInstructions.add(new Store(lastResult, 0, numElements));

        // Goto endif
        currentInstructions.add(new Goto(new Label(endLabel)));

        // Else block
        currentInstructions.add(new LabelInstr(new Label(elseLabel)));
        currentInstructions.add(new ErrorMessage(errMsg));

        // Null check
        currentInstructions.add(new LabelInstr(new Label(errorLabel)));
        currentInstructions.add(new ErrorMessage("\"Null pointer\""));

        // Endif
        currentInstructions.add(new LabelInstr(new Label(endLabel)));
    }

    @Override
    public void visit(AllocationExpression n) {
        String className = n.f1.f0.toString();
        ClassLayout layout = classLayouts.get(className);

        // Allocate space for fields table (vmt pointer + fields)
        Identifier size = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(size, layout.objSize));
        Identifier objPointer = new Identifier("ft");
        currentInstructions.add(new Alloc(objPointer, size));
        // Error check if alloc failed
        String errorLabel = "nullCheck_" + tempCounter++;
        String endLabel = "endNull_" + tempCounter++;
        currentInstructions.add(new IfGoto(objPointer, new Label(errorLabel)));
        currentInstructions.add(new Goto(new Label(endLabel)));

        // Allocate virtual method table (vmt)
        Identifier vmtSize = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(vmtSize, layout.vmt.size() * 4));
        Identifier vmtPointer = new Identifier("vmt");
        currentInstructions.add(new Alloc(vmtPointer, vmtSize));
        // Error check if alloc failed 
        currentInstructions.add(new IfGoto(vmtPointer, new Label(errorLabel)));
        currentInstructions.add(new Goto(new Label(endLabel)));

        // Initialize VMT entries
        for (String name: layout.vmt) {
            int offset = layout.methodOffsets.get(name);
            Identifier ptr = getNewTemp();
            currentInstructions.add(new Move_Id_FuncName(ptr, new FunctionName(name)));
            currentInstructions.add(new Store(vmtPointer, offset, ptr));
        }

        // Store VMT pointer at offset 0 of fields table
        currentInstructions.add(new Store(objPointer, 0, vmtPointer));

        // Initialize all fields to 0
        Identifier zero = new Identifier("fInit");
        currentInstructions.add(new Move_Id_Integer(zero, 0));
        for (String fieldName : layout.fields) {
            int offset = layout.fieldOffsets.get(fieldName);
            currentInstructions.add(new Store(objPointer, offset, zero));
        }

        // Null check
        currentInstructions.add(new LabelInstr(new Label(errorLabel)));
        currentInstructions.add(new ErrorMessage("\"Null pointer\""));
        
        // Endif
        currentInstructions.add(new LabelInstr(new Label(endLabel)));

        lastResult = objPointer;
    }

    @Override
    public void visit(NotExpression n) {
        n.f1.accept(this);
        Identifier op = lastResult;

        lastResult = getNewTemp();

        Identifier one = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(one, 1));

        currentInstructions.add(new Subtract(lastResult, one, op));
    }

}
