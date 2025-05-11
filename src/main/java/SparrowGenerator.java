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
import minijava.syntaxtree.ExpressionList;
import minijava.syntaxtree.ExpressionRest;
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
import sparrow.Call;
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
    HashMap<Identifier, String> objTypeMap;
    String currentClass;
    ClassLayout currentLayout;

    public SparrowGenerator(HashMap<String, ClassLayout> layouts) {
        this.code = new Program(new ArrayList<>());
        this.tempCounter = 0;
        this.currentInstructions = new ArrayList<>();
        this.lastResult = null;
        this.classLayouts = layouts;
        this.objTypeMap = new HashMap<>();
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

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    @Override
    public void visit(MainClass n) {
        currentClass = n.f1.f0.toString();

        // Process statements
        n.f15.accept(this);
        
        // Set return value for main function block
        Identifier returnId = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(returnId, 0));

        Block block = new Block(currentInstructions, returnId);

        // Create a function with name "main"
        FunctionDecl mainFunc = new FunctionDecl(new FunctionName("main"), new ArrayList<>(), block);

        code.funDecls.add(mainFunc);
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    @Override
    public void visit(ClassDeclaration n) {
        this.currentClass = n.f1.f0.toString();
        this.currentLayout = classLayouts.get(currentClass);

        n.f4.accept(this);
    }

    /**
     * Methods -> Sparrow instructions
     */

    /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    @Override
    public void visit(MethodDeclaration n) {
        ArrayList<Instruction> savedInstructions = currentInstructions;
        currentInstructions = new ArrayList<>();
        params = new ArrayList<>();

        // Process function name
        n.f2.accept(this);

        // Process formal parameters
        Identifier thisId = new Identifier("this");
        params.add(thisId);
        if (n.f4.present()) {
            n.f4.accept(this);
        }

        // objTypeMap.put("this", currentClass);

        // Process statements
        n.f8.accept(this);
        n.f10.accept(this);
        Identifier returnId = lastResult;

        // Create block (variable declaration + statements + return)
        Block block = new Block(currentInstructions, returnId);

        // Create function declaration
        String mangledName = currentClass + "_" + n.f2.f0.toString();
        FunctionDecl methodDecl = new FunctionDecl(new FunctionName(mangledName), params, block);

        code.funDecls.add(methodDecl);
        currentInstructions = savedInstructions;

    }

    /**
    * f0 -> FormalParameter()
    * f1 -> ( FormalParameterRest() )*
    */
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

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    @Override
    public void visit(AssignmentStatement n) {
        n.f2.accept(this);
        Identifier rhs = lastResult;

        Identifier lhs = resolveName(n.f0.f0.toString());

        if (isField(lhs)) {
            int lhsOffset = currentLayout.fieldOffsets.get(lhs.toString());
            currentInstructions.add(new Store(new Identifier("this"), lhsOffset, rhs));     // [this + lhsOffset] = v0
        } else {
            currentInstructions.add(new Move_Id_Id(lhs, rhs));                              // v1 = v0
        }

        // Assign type of rhs to lhs
        // if (objTypeMap.containsKey(rhs.toString())) {
        //     objTypeMap.put(lhs.toString(), objTypeMap.get(rhs.toString()));
        // }

    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    @Override
    public void visit(ArrayAssignmentStatement n) {
        Label endIf = new Label("endif_" + (tempCounter++));
        Label errorLabel = new Label("error_" + (tempCounter++));
        String errMsg = "\"ArrayIndexOutOfBoundsException\"";

        // Get array address
        n.f0.accept(this);
        Identifier arr = lastResult;                                                    // v2
        Identifier size = getNewTemp();
        currentInstructions.add(new Load(size, arr, 0));                                // v3 = [v2 + 0]
        
        // Get index
        n.f2.accept(this);
        Identifier idx = lastResult;                                                    // v4

        Identifier four = getNewTemp();
        Identifier five = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(five, 5));                          // v5 = 5
        currentInstructions.add(new Move_Id_Integer(four, 4));                          // v6 = 4

        Identifier negOne = getNewTemp();
        currentInstructions.add(new Subtract(negOne, four, five));                      // v7 = v6 - v5

        // Lower bound -1 < idx
        Identifier validLower = getNewTemp();
        currentInstructions.add(new LessThan(validLower, negOne, idx));                 // v8 = v7 < v6

        // Upper bound idx < size
        Identifier validUpper = getNewTemp();
        currentInstructions.add(new LessThan(validUpper, idx, size));                   // v9 = v6 < v5
        Identifier validBounds = getNewTemp();
        currentInstructions.add(new Multiply(validBounds, validLower, validUpper));     // v10 = v8 * v9

        // Check -1 < idx < size
        currentInstructions.add(new IfGoto(validBounds, errorLabel));                   // if0 v10 goto error_1
        Identifier byteOffset = getNewTemp();

        // Create byteOffset
        currentInstructions.add(new Multiply(byteOffset, idx, four));                   // v11 = v4 * v6
        currentInstructions.add(new Add(byteOffset, byteOffset, four));                 // v11 = v11 + v6
        
        // Precompute base address + byteOffset
        Identifier address = getNewTemp();
        currentInstructions.add(new Add(address, arr, byteOffset));                     // v11 = v2 + v11

        // Get lookup index
        n.f5.accept(this);
        Identifier val = lastResult;

        currentInstructions.add(new Store(address, 0, val));                            // [v11 + 0] = val
        currentInstructions.add(new Goto(endIf));                                       // goto endif_0

        // Index error handling
        currentInstructions.add(new LabelInstr(errorLabel));                            // error_1:
        currentInstructions.add(new ErrorMessage(errMsg));                              // error("ArrayIndexOutOfBoundsException")

        // Endif
        currentInstructions.add(new LabelInstr(endIf));                                 // endif_0
    }

    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    @Override
    public void visit(IfStatement n) {
        String elseLabel = "else_" + (tempCounter++);
        String endLabel = "endif_" + (tempCounter++);

        // Process condition
        n.f2.accept(this);
        Identifier condition = lastResult;
        currentInstructions.add(new IfGoto(condition, new Label(elseLabel)));       // if0 v2 goto else_0

        // Process statements (condition = true)
        n.f4.accept(this);
        currentInstructions.add(new Goto(new Label(endLabel)));                     // goto endif_1

        // Else block (condition = false)
        currentInstructions.add(new LabelInstr(new Label(elseLabel)));              // else_0:
        n.f6.accept(this);

        // Endif
        currentInstructions.add(new LabelInstr(new Label(endLabel)));               // endif_1:
    }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    @Override
    public void visit(WhileStatement n) {
        String startLabel = "while_" + tempCounter++;
        String endLabel = "endwhile_" + tempCounter++;

        // Start label
        currentInstructions.add(new LabelInstr(new Label(startLabel)));         // while_0:

        // Process loop condition
        n.f2.accept(this);
        Identifier condition = lastResult;
        currentInstructions.add(new IfGoto(condition, new Label(endLabel)));    // if0 v2 goto endwhile_1

        // Process statements
        n.f4.accept(this);

        // Loop back to start label
        currentInstructions.add(new Goto(new Label(startLabel)));               // goto while_0

        // Terminate loop
        currentInstructions.add(new LabelInstr(new Label(endLabel)));           // endwhile_1:
    }

    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    @Override
    public void visit(PrintStatement n) {
        // Process print argument
        n.f2.accept(this);
        Identifier value = lastResult;

        currentInstructions.add(new Print(value));          // print(v0)
    }

    /**
     * Expressions -> Sparrow instructions
     */

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
    @Override
    public void visit(AndExpression n) {
        // Process operand #1
        n.f0.accept(this);
        Identifier op1 = lastResult;                                    // v1

        // Process operand #2
        n.f2.accept(this);
        Identifier op2 = lastResult;                                    // v2

        // Compute op1 && op2
        lastResult = getNewTemp();                                      // v3
        currentInstructions.add(new Multiply(lastResult, op1, op2));    // v3 = v1 * v2
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    @Override
    public void visit(CompareExpression n) {
        // Process operand #1
        n.f0.accept(this);
        Identifier op1 = lastResult;                                    // v1
        
        // Process operand #2
        n.f2.accept(this);
        Identifier op2 = lastResult;                                    // v2

        // Compute op1 < op2
        lastResult = getNewTemp();                                      // v3
        currentInstructions.add(new LessThan(lastResult, op1, op2));    // v3 = v1 < v2
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    @Override
    public void visit(PlusExpression n) {
        // Process operand #1
        n.f0.accept(this);
        Identifier op1 = lastResult;                                // v1

        // Process operand #2
        n.f2.accept(this);
        Identifier op2 = lastResult;                                // v2

        // Compute op1 + op2
        lastResult = getNewTemp();                                  // v3
        currentInstructions.add(new Add(lastResult, op1, op2));     // v3 = v1 + v2
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    @Override
    public void visit(MinusExpression n) {
        // Process operand #1
        n.f0.accept(this);
        Identifier op1 = lastResult;                                    // v1

        // Process operand #2
        n.f2.accept(this);
        Identifier op2 = lastResult;                                    // v2

        // Compute op1 - op2
        lastResult = getNewTemp();                                      // v3
        currentInstructions.add(new Subtract(lastResult, op1, op2));    // v3 = v1 - v2
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    @Override
    public void visit(TimesExpression n) {
        // Process operand #1
        n.f0.accept(this);
        Identifier op1 = lastResult;                                    // v1

        // Process operand #2
        n.f2.accept(this);
        Identifier op2 = lastResult;                                    // v2

        // Compute op1 * op2
        lastResult = getNewTemp();                                      // v3
        currentInstructions.add(new Multiply(lastResult, op1, op2));    // v3 = v1 * v2
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    @Override
    public void visit(ArrayLookup n) {
        Label endIf = new Label("endif_" + (tempCounter++));
        Label errorLabel = new Label("error_" + (tempCounter++));
        String errMsg = "\"ArrayIndexOutOfBoundsException\"";

        // Get array address
        n.f0.accept(this);
        Identifier arr = lastResult;                                                    // v2
        Identifier size = getNewTemp();
        currentInstructions.add(new Load(size, arr, 0));                                // v3 = [v2 + 0]
        
        // Get index
        n.f2.accept(this);
        Identifier idx = lastResult;                                                    // v4

        Identifier four = getNewTemp();
        Identifier five = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(five, 5));                          // v5 = 5
        currentInstructions.add(new Move_Id_Integer(four, 4));                          // v6 = 4

        Identifier negOne = getNewTemp();
        currentInstructions.add(new Subtract(negOne, four, five));                      // v7 = v6 - v5

        // Lower bound -1 < idx
        Identifier validLower = getNewTemp();
        currentInstructions.add(new LessThan(validLower, negOne, idx));                 // v8 = v7 < v6

        // Upper bound idx < size
        Identifier validUpper = getNewTemp();
        currentInstructions.add(new LessThan(validUpper, idx, size));                   // v9 = v6 < v5
        Identifier validBounds = getNewTemp();
        currentInstructions.add(new Multiply(validBounds, validLower, validUpper));     // v10 = v8 * v9

        // Check -1 < idx < size
        currentInstructions.add(new IfGoto(validBounds, errorLabel));                   // if0 v10 goto error_1
        Identifier byteOffset = getNewTemp();

        // Create byteOffset
        currentInstructions.add(new Multiply(byteOffset, idx, four));                   // v11 = v4 * v6
        currentInstructions.add(new Add(byteOffset, byteOffset, four));                 // v11 = v11 + v6
        
        // Precompute base address + byteOffset
        Identifier address = getNewTemp();
        currentInstructions.add(new Add(address, arr, byteOffset));                     // v11 = v2 + v11

        // Get lookup index
        lastResult = getNewTemp();

        currentInstructions.add(new Load(lastResult, address, 0));                      // [v11 + 0] = val
        currentInstructions.add(new Goto(endIf));                                       // goto endif_0

        // Index error handling
        currentInstructions.add(new LabelInstr(errorLabel));                            // error_1:
        currentInstructions.add(new ErrorMessage(errMsg));                              // error("ArrayIndexOutOfBoundsException")

        // Endif
        currentInstructions.add(new LabelInstr(endIf));                                 // endif_0
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    @Override
    public void visit(ArrayLength n) {
        // Process array
        n.f0.accept(this);
        Identifier arr = lastResult;                                // arr
        
        // Get array length
        lastResult = getNewTemp();                                  // v1
        currentInstructions.add(new Load(lastResult, arr, 0));      // load(v1, [arr + 0])
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    // TODO: MessageSend
    @Override
    public void visit(MessageSend n) {
        n.f0.accept(this);
        Identifier objRef = lastResult;

        Label errLabel = new Label("nullErr_" + (tempCounter++));
        Label endLabel = new Label("endIf_" + (tempCounter++));
        currentInstructions.add(new IfGoto(objRef, errLabel));              // if0 v0 goto nullErr_1

        String methodName = n.f2.f0.toString();

        Identifier vmt = getNewTemp();
        currentInstructions.add(new Load(vmt, objRef, 0));

        ArrayList<Identifier> args = new ArrayList<>();
        args.add(objRef);

        if (n.f4.present()) {
            ArrayList<Identifier> savedArgs = params;
            params = new ArrayList<>();
            n.f4.accept(this);
            args.addAll(params);

            params = savedArgs;
        }

        String objClassName = objTypeMap.get(objRef);
        ClassLayout objLayout = classLayouts.get(objClassName);
        int mOffset = objLayout.methodOffsets.get(objClassName + "_" + methodName);
        Identifier methodPtr = getNewTemp();
        currentInstructions.add(new Load(methodPtr, vmt, mOffset));

        lastResult = getNewTemp();
        currentInstructions.add(new Call(lastResult, methodPtr, args));
        currentInstructions.add(new Goto(endLabel));

        currentInstructions.add(new LabelInstr(errLabel));                  // nullErr_1:
        currentInstructions.add(new ErrorMessage("\"Null pointer\""));      // error("Null pointer")
        currentInstructions.add(new LabelInstr(endLabel));                  // endIf_2:

    }

    /**
    * f0 -> Expression()
    * f1 -> ( ExpressionRest() )*
    */
    // TODO: ExpressionList
    @Override
    public void visit(ExpressionList n) {
        n.f0.accept(this);
        params.add(lastResult);

        n.f1.accept(this);
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    @Override
    public void visit(ExpressionRest n) {
        n.f1.accept(this);
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    @Override
    public void visit(IntegerLiteral n) {
        // Store int value in Sparrow temp
        int val = Integer.parseInt(n.f0.toString());
        lastResult = getNewTemp();                                          // v0
        currentInstructions.add(new Move_Id_Integer(lastResult, val));      // v0 = val
    }

    /**
    * f0 -> "true"
    */
    @Override
    public void visit(TrueLiteral n) {
        // Store 1 in Sparrow temp
        lastResult = getNewTemp();                                          // v0
        currentInstructions.add(new Move_Id_Integer(lastResult, 1));        // v0 = 1
    }

    /**
    * f0 -> "false"
    */
    @Override
    public void visit(FalseLiteral n) {
        // Store 0 in Sparrow temp
        lastResult = getNewTemp();                                          // v0
        currentInstructions.add(new Move_Id_Integer(lastResult, 0));        // v0 = 0
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    @Override
    public void visit(minijava.syntaxtree.Identifier n) {
        String id = n.f0.toString();

        // Fields are in the heap... load using "this"
        if (isField(new Identifier(id))) {
            int offset = currentLayout.fieldOffsets.get(id);
            Identifier temp = getNewTemp();                                                 // v0
            currentInstructions.add(new Load(temp, new Identifier("this"), offset));        // load(v0, [this + offset])
            lastResult = temp;
        } else {
            // Mangle name if necessary
            lastResult = resolveName(id);                                                   // v0
        }
    }

    /**
    * f0 -> "this"
    */
    @Override
    public void visit(ThisExpression n) {
        Identifier thisId = new Identifier("this");
        lastResult = thisId;             // this
        objTypeMap.put(thisId, currentClass);
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    @Override
    public void visit(ArrayAllocationExpression n) {
        // Process requested element size
        n.f3.accept(this);
        Identifier numElements = lastResult;                                    

        String errMsg = "\"Array allocation with invalid size\"";
        Label elseLabel = new Label("else_" + (tempCounter++));
        Label endLabel = new Label("endif_" + (tempCounter++));

        // Number of elements must be positive (> 0)
        Identifier zero = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(zero, 0));                  // v3 = 0
        Identifier notNeg = getNewTemp();
        currentInstructions.add(new LessThan(notNeg, zero, numElements));       // v4 = 0 < v0
        currentInstructions.add(new IfGoto(notNeg, elseLabel));                 // if0 v4 goto else_1

        // Calculate number of bytes to allocate
        Identifier four = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(four, 4));                  // v5 = 4
        Identifier sz = getNewTemp();
        currentInstructions.add(new Multiply(sz, four, numElements));           // v6 = v5 * v0
        currentInstructions.add(new Add(sz, sz, four));                         // v6 = v6 + v5

        // Allocate memory space for array
        lastResult = getNewTemp();
        currentInstructions.add(new Alloc(lastResult, sz));                     // v7 = alloc(v6)

        // Check if allocation failed
        String errorLabel = "nullErr_" + (tempCounter++);
        currentInstructions.add(new IfGoto(lastResult, new Label(errorLabel))); // if0 v8 goto nullErr_7

        // Store number of elements in first 4 bytes of array allocation
        currentInstructions.add(new Store(lastResult, 0, numElements));         // [v7 + 0] = v0

        currentInstructions.add(new Goto(endLabel));                            // goto endif_2

        // Error handling for invalid number of elements
        currentInstructions.add(new LabelInstr(elseLabel));                     // else_1:
        currentInstructions.add(new ErrorMessage(errMsg));                      // error("Array allocation with invalid size")

        // Error handling for alloc failure
        currentInstructions.add(new LabelInstr(new Label(errorLabel)));         // nullErr_7:
        currentInstructions.add(new ErrorMessage("\"Null pointer\""));          // error("Null pointer")

        // Endif
        currentInstructions.add(new LabelInstr(endLabel));                      // endif_2:
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    @Override
    public void visit(AllocationExpression n) {
        String className = n.f1.f0.toString();
        ClassLayout layout = classLayouts.get(className);

        // Allocate space for fields table (vmt pointer + fields)
        Identifier size = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(size, layout.objSize));
        Identifier objPointer = getNewTemp();
        lastResult = objPointer;
        currentInstructions.add(new Alloc(objPointer, size));
        // Error check if alloc failed
        Label errorLabel = new Label("nullErr_" + (tempCounter++));
        Label endLabel = new Label("endIf_" + (tempCounter++));
        currentInstructions.add(new IfGoto(objPointer, errorLabel));

        // Store objPointer identifier -> className mapping
        objTypeMap.put(objPointer, className);

        // Allocate virtual method table (vmt)
        Identifier vmtSize = getNewTemp();
        currentInstructions.add(new Move_Id_Integer(vmtSize, layout.vmt.size() * 4));
        Identifier vmtPointer = getNewTemp();
        currentInstructions.add(new Alloc(vmtPointer, vmtSize));
        // Error check if alloc failed 
        currentInstructions.add(new IfGoto(vmtPointer, errorLabel));

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
        if (!layout.fields.isEmpty()) {
            Identifier zero = new Identifier("fInit");
            currentInstructions.add(new Move_Id_Integer(zero, 0));
            for (String fieldName : layout.fields) {
                int offset = layout.fieldOffsets.get(fieldName);
                currentInstructions.add(new Store(objPointer, offset, zero));
            }
        }

        currentInstructions.add(new Goto(endLabel));

        // Null check
        currentInstructions.add(new LabelInstr(errorLabel));
        currentInstructions.add(new ErrorMessage("\"Null pointer\""));
        
        // Endif
        currentInstructions.add(new LabelInstr(endLabel));
    }

    /**
    * f0 -> "!"
    * f1 -> Expression()
    */
    @Override
    public void visit(NotExpression n) {
        // Process operand
        n.f1.accept(this);
        Identifier op = lastResult;                                         // v1

        // Calculate negation of operand
        Identifier one = getNewTemp();                                      // v2
        lastResult = getNewTemp();                                          // v3
        currentInstructions.add(new Move_Id_Integer(one, 1));               // v2 = 1
        currentInstructions.add(new Subtract(lastResult, one, op));         // v3 = v1 - v0
    }

}
