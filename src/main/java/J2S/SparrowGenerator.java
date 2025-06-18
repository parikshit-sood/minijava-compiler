package J2S;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import IR.token.FunctionName;
import IR.token.Label;
import minijava.syntaxtree.AllocationExpression;
import minijava.syntaxtree.AndExpression;
import minijava.syntaxtree.ArrayAllocationExpression;
import minijava.syntaxtree.ArrayAssignmentStatement;
import minijava.syntaxtree.ArrayLength;
import minijava.syntaxtree.ArrayLookup;
import minijava.syntaxtree.ArrayType;
import minijava.syntaxtree.AssignmentStatement;
import minijava.syntaxtree.BooleanType;
import minijava.syntaxtree.ClassDeclaration;
import minijava.syntaxtree.ClassExtendsDeclaration;
import minijava.syntaxtree.CompareExpression;
import minijava.syntaxtree.ExpressionList;
import minijava.syntaxtree.FalseLiteral;
import minijava.syntaxtree.FormalParameter;
import minijava.syntaxtree.FormalParameterList;
import minijava.syntaxtree.FormalParameterRest;
import minijava.syntaxtree.Identifier;
import minijava.syntaxtree.IfStatement;
import minijava.syntaxtree.IntegerLiteral;
import minijava.syntaxtree.IntegerType;
import minijava.syntaxtree.MainClass;
import minijava.syntaxtree.MessageSend;
import minijava.syntaxtree.MethodDeclaration;
import minijava.syntaxtree.MinusExpression;
import minijava.syntaxtree.Node;
import minijava.syntaxtree.NodeSequence;
import minijava.syntaxtree.NotExpression;
import minijava.syntaxtree.PlusExpression;
import minijava.syntaxtree.PrintStatement;
import minijava.syntaxtree.ThisExpression;
import minijava.syntaxtree.TimesExpression;
import minijava.syntaxtree.TrueLiteral;
import minijava.syntaxtree.VarDeclaration;
import minijava.syntaxtree.WhileStatement;
import minijava.visitor.DepthFirstVisitor;
import sparrow.Add;
import sparrow.Alloc;
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
    private int tempCounter = 0;
    private int labelCounter = 0;
    private IR.token.Identifier lastResult;
    private List<Instruction> currentInstructions;
    private Map<String, IR.token.Identifier> varMap;
    private Map<String, String> varTypeMap;
    private String currentClass;
    private Map<String, ClassLayout> classLayouts;
    private Program code;
    private Set<String> reservedRegs;

    public SparrowGenerator(HashMap<String, ClassLayout> layoutMap) {
        this.tempCounter = 0;
        this.labelCounter = 0;
        this.currentInstructions = new ArrayList<>();
        this.varMap = new HashMap<>();
        this.varTypeMap = new HashMap<>();
        this.currentClass = "";
        this.classLayouts = new HashMap<>(layoutMap);
        this.code = new Program(new ArrayList<>());
        this.reservedRegs = new HashSet<>();

        for (int i = 2; i <= 7; i++)
            reservedRegs.add("a" + i);

        for (int i = 0; i <= 5; i++)
            reservedRegs.add("t" + i);

        for (int i = 1; i <= 11; i++)
            reservedRegs.add("s" + i);
    }

    public String getGeneratedCode() {
        return code.toString();
    }

    /**
     * Helper functions
     */
    private IR.token.Identifier getNewTemp(String type) {
        IR.token.Identifier temp = new IR.token.Identifier("v" + (tempCounter++));

        if (type != null) {
            varTypeMap.put(temp.toString(), type);
        }

        return temp;
    }

    private String getClassNameForObject(IR.token.Identifier obj) {
        String objId = obj.toString();
        if (objId.equals("this")) {
            return currentClass;
        }
        return varTypeMap.get(objId);
    }

    private String typeString(minijava.syntaxtree.Type tp) {
        Node n = tp.f0.choice;

        if (n instanceof BooleanType) return "boolean";
        if (n instanceof IntegerType) return "int";
        if ((n instanceof ArrayType) || (n instanceof NodeSequence)) return "arr";

        return ((minijava.syntaxtree.Identifier) n).f0.toString();
    }

    private void addParam(FormalParameter p, List<IR.token.Identifier> params) {
        String type = typeString(p.f0);
        String varName = p.f1.f0.toString();

        IR.token.Identifier varId = getNewTemp(type);
        varMap.put(varName, varId);
        params.add(varId);
    }

    private String findMethod(String className, String methodName) {
        if (className == null)
            return null;

        ClassLayout layout = classLayouts.get(className);

        if (layout == null)
            return null;

        for (String name : layout.getMethodOffsets().keySet()) {
            if (name.endsWith("_" + methodName)) {
                return name;
            }
        }

        return findMethod(layout.getParent(), methodName);
    }

    // -----------------
    // Classes
    // -----------------
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
        currentClass = "Main";
        currentInstructions = new ArrayList<>();

        n.f14.accept(this);
        n.f15.accept(this);

        IR.token.Identifier returnId = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(returnId, 0));

        sparrow.Block block = new sparrow.Block(currentInstructions, returnId);

        code.funDecls.add(new FunctionDecl(new FunctionName("main"), new ArrayList<>(), block));
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
        currentClass = n.f1.f0.toString();

        n.f3.accept(this);
        n.f4.accept(this);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public void visit(ClassExtendsDeclaration n) {
        currentClass = n.f1.f0.toString();
        
        n.f5.accept(this);
        n.f6.accept(this);
    }

    // -----------------
    // Methods
    // -----------------
    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public void visit(VarDeclaration n) {
        String type = typeString(n.f0);

        IR.token.Identifier varId = getNewTemp(type);
        varMap.put(n.f1.f0.toString(), varId);
    }

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
        String name = currentClass + "_" + n.f2.f0.toString();

        Map<String, IR.token.Identifier> savedVarMap = new HashMap<>(varMap);
        Map<String, String> savedVarTypeMap = new HashMap<>(varTypeMap);

        varMap.clear();
        varTypeMap.clear();

        List<IR.token.Identifier> params = new ArrayList<>();

        IR.token.Identifier thisParam = getNewTemp(currentClass);
        varTypeMap.put(thisParam.toString(), currentClass);
        params.add(thisParam);
        varMap.put("this", thisParam);

        if (n.f4.present()) {
            FormalParameterList fps = (FormalParameterList) n.f4.node;

            addParam(fps.f0, params);

            for (int i = 0; i < fps.f1.size(); i++) {
                FormalParameterRest rest = (FormalParameterRest) fps.f1.elementAt(i);
                addParam(rest.f1, params);
            }
        }

        List<Instruction> savedInstructions = currentInstructions;
        currentInstructions = new ArrayList<>();

        n.f7.accept(this);
        n.f8.accept(this);
        n.f10.accept(this);
        sparrow.Block body = new sparrow.Block(currentInstructions, lastResult);

        code.funDecls.add(new FunctionDecl(new FunctionName(name), params, body));

        currentInstructions = savedInstructions;
        varMap = savedVarMap;
        varTypeMap = savedVarTypeMap;
    }

    // -----------------
    // Statements
    // -----------------

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public void visit(AssignmentStatement n) {
        String varName = n.f0.f0.toString();

        n.f2.accept(this);
        IR.token.Identifier rhs = lastResult;

        IR.token.Identifier lhs = varMap.get(varName);

        if (lhs != null) {
            currentInstructions.add(new Move_Id_Id(lhs, rhs));
            return;
        }

        if (!currentClass.equals("Main")) {
            int offset = classLayouts.get(currentClass).getFieldOffset(varName);
            IR.token.Identifier thisId = varMap.get("this");
            if (thisId == null) {
                thisId = new IR.token.Identifier("this");
            }

            currentInstructions.add(new Store(thisId, offset, rhs));
        }
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
        Label outOfBounds = new Label("boundsErr_" + (labelCounter++));
        Label success = new Label("success_" + (labelCounter++));

        String name = n.f0.f0.toString();
        IR.token.Identifier arr = varMap.get(name);

        if (arr == null) {
                int offset = classLayouts.get(currentClass).getFieldOffset(name);
                arr = getNewTemp("arr");
                currentInstructions.add(new Load(arr, varMap.get("this"), offset));
        }

        // checkNull(arr);

        n.f2.accept(this);
        IR.token.Identifier idx = lastResult;

        IR.token.Identifier length = getNewTemp("int");
        currentInstructions.add(new Load(length, arr, 0));

        IR.token.Identifier five = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(five, 5));

        IR.token.Identifier four = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(four, 4));

        IR.token.Identifier negOne = getNewTemp("int");
        currentInstructions.add(new Subtract(negOne, four, five));

        IR.token.Identifier lowerBound = getNewTemp("boolean");
        currentInstructions.add(new LessThan(lowerBound, negOne, idx));

        IR.token.Identifier upperBound = getNewTemp("boolean");
        currentInstructions.add(new LessThan(upperBound, idx, length));

        IR.token.Identifier validBounds = getNewTemp("boolean");
        currentInstructions.add(new Multiply(validBounds, lowerBound, upperBound));

        currentInstructions.add(new IfGoto(validBounds, outOfBounds));

        IR.token.Identifier offset = getNewTemp("int");
        currentInstructions.add(new Multiply(offset, idx, four));
        currentInstructions.add(new Add(offset, offset, four));

        IR.token.Identifier addr = getNewTemp("int");
        currentInstructions.add(new Add(addr, arr, offset));

        n.f5.accept(this);
        IR.token.Identifier value = lastResult;
        currentInstructions.add(new Store(addr, 0, value));

        currentInstructions.add(new Goto(success));

        currentInstructions.add(new LabelInstr(outOfBounds));
        currentInstructions.add(new ErrorMessage("\"array index out of bounds\""));

        currentInstructions.add(new LabelInstr(success));
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
        Label elseIf = new Label("else_" + (labelCounter++));
        Label endIf = new Label("endif_" + (labelCounter++));

        n.f2.accept(this);
        IR.token.Identifier cond = lastResult;

        currentInstructions.add(new IfGoto(cond, elseIf));

        n.f4.accept(this);

        currentInstructions.add(new Goto(endIf));
        currentInstructions.add(new LabelInstr(elseIf));

        n.f6.accept(this);

        currentInstructions.add(new LabelInstr(endIf));
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
        Label start = new Label("while_" + (labelCounter++));
        Label end = new Label("endWhile_" + (labelCounter++));

        currentInstructions.add(new LabelInstr(start));

        n.f2.accept(this);
        IR.token.Identifier cond = lastResult;

        currentInstructions.add(new IfGoto(cond, end));

        n.f4.accept(this);

        currentInstructions.add(new Goto(start));

        currentInstructions.add(new LabelInstr(end));
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
        n.f2.accept(this);
        IR.token.Identifier expr = lastResult;

        currentInstructions.add(new Print(expr));
    }

    // -----------------
    // Expressions
    // -----------------

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "&&"
     * f2 -> PrimaryExpression()
     */
    @Override
    public void visit(AndExpression n) {
        Label sc = new Label("else_" + (labelCounter++));
        Label endSC = new Label("end_" + (labelCounter++));

        n.f0.accept(this);
        IR.token.Identifier op1 = lastResult;

        currentInstructions.add(new IfGoto(op1, sc));

        n.f2.accept(this);
        IR.token.Identifier op2 = lastResult;

        IR.token.Identifier result = getNewTemp("boolean");
        currentInstructions.add(new Move_Id_Id(result, op2));
        currentInstructions.add(new Goto(endSC));

        currentInstructions.add(new LabelInstr(sc));
        currentInstructions.add(new Move_Id_Integer(result, 0));

        currentInstructions.add(new LabelInstr(endSC));

        lastResult = result;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public void visit(CompareExpression n) {
        n.f0.accept(this);
        IR.token.Identifier op1 = lastResult;

        n.f2.accept(this);
        IR.token.Identifier op2 = lastResult;

        IR.token.Identifier result = getNewTemp("boolean");
        currentInstructions.add(new LessThan(result, op1, op2));

        lastResult = result;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public void visit(PlusExpression n) {
        n.f0.accept(this);
        IR.token.Identifier op1 = lastResult;

        n.f2.accept(this);
        IR.token.Identifier op2 = lastResult;

        IR.token.Identifier result = getNewTemp("int");
        currentInstructions.add(new Add(result, op1, op2));

        lastResult = result;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public void visit(MinusExpression n) {
        n.f0.accept(this);
        IR.token.Identifier op1 = lastResult;

        n.f2.accept(this);
        IR.token.Identifier op2 = lastResult;

        IR.token.Identifier result = getNewTemp("int");
        currentInstructions.add(new Subtract(result, op1, op2));

        lastResult = result;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public void visit(TimesExpression n) {
        n.f0.accept(this);
        IR.token.Identifier op1 = lastResult;

        n.f2.accept(this);
        IR.token.Identifier op2 = lastResult;

        IR.token.Identifier result = getNewTemp("int");
        currentInstructions.add(new Multiply(result, op1, op2));

        lastResult = result;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public void visit(ArrayLookup n) {
        Label outOfBounds = new Label("boundsErr_" + (labelCounter++));
        Label success = new Label("success_" + (labelCounter++));

        n.f0.accept(this);
        IR.token.Identifier arr = lastResult;

        // checkNull(arr);

        n.f2.accept(this);
        IR.token.Identifier idx = lastResult;


        IR.token.Identifier length = getNewTemp("int");
        currentInstructions.add(new Load(length, arr, 0));

        IR.token.Identifier five = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(five, 5));

        IR.token.Identifier four = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(four, 4));

        IR.token.Identifier negOne = getNewTemp("int");
        currentInstructions.add(new Subtract(negOne, four, five));

        IR.token.Identifier lowerBound = getNewTemp("boolean");
        currentInstructions.add(new LessThan(lowerBound, negOne, idx));

        IR.token.Identifier upperBound = getNewTemp("boolean");
        currentInstructions.add(new LessThan(upperBound, idx, length));

        IR.token.Identifier validBounds = getNewTemp("boolean");
        currentInstructions.add(new Multiply(validBounds, lowerBound, upperBound));

        currentInstructions.add(new IfGoto(validBounds, outOfBounds));

        IR.token.Identifier offset = getNewTemp("int");
        currentInstructions.add(new Multiply(offset, idx, four));
        currentInstructions.add(new Add(offset, offset, four));

        IR.token.Identifier addr = getNewTemp("int");
        currentInstructions.add(new Add(addr, arr, offset));

        IR.token.Identifier result = getNewTemp("int");
        currentInstructions.add(new Load(result, addr, 0));

        currentInstructions.add(new Goto(success));

        currentInstructions.add(new LabelInstr(outOfBounds));
        currentInstructions.add(new ErrorMessage("\"array index out of bounds\""));

        currentInstructions.add(new LabelInstr(success));

        lastResult = result;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public void visit(ArrayLength n) {
        n.f0.accept(this);
        IR.token.Identifier arr = lastResult;

        // checkNull(arr);

        IR.token.Identifier length = getNewTemp("int");
        currentInstructions.add(new Load(length, arr, 0));

        lastResult = length;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    @Override
    public void visit(MessageSend n) {
        n.f0.accept(this);
        IR.token.Identifier classObj = lastResult;

        Label nullError = new Label("nullErr_" + (labelCounter++));
        Label success = new Label("success_" + (labelCounter++));
        
        currentInstructions.add(new IfGoto(classObj, nullError));
        currentInstructions.add(new Goto(success));
        
        currentInstructions.add(new LabelInstr(nullError));
        currentInstructions.add(new ErrorMessage("\"null pointer\""));
        
        currentInstructions.add(new LabelInstr(success));

        String className = getClassNameForObject(classObj);

        List<IR.token.Identifier> args = new ArrayList<>();
        args.add(classObj);

        if (n.f4.present()) {
            ExpressionList exprList = (ExpressionList) n.f4.node;

            exprList.f0.accept(this);
            args.add(lastResult);

            for (int i = 0; i < exprList.f1.size(); i++) {
                exprList.f1.elementAt(i).accept(this);
                args.add(lastResult);
            }
        }

        String methodName = findMethod(className, n.f2.f0.toString());
        int offset = classLayouts.get(className).getMethodOffset(methodName);

        IR.token.Identifier vmt = getNewTemp("int");
        currentInstructions.add(new Load(vmt, classObj, 0));

        IR.token.Identifier funcId = getNewTemp(null);
        currentInstructions.add(new Load(funcId, vmt, offset));

        IR.token.Identifier callRes = getNewTemp(className);
        currentInstructions.add(new Call(callRes, funcId, args));

        lastResult = callRes;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    @Override
    public void visit(IntegerLiteral n) {
        int value = Integer.parseInt(n.f0.toString());

        IR.token.Identifier result = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(result, value));

        lastResult = result;
    }

    /**
     * f0 -> "true"
     */
    @Override
    public void visit(TrueLiteral n) {
        IR.token.Identifier result = getNewTemp("boolean");
        currentInstructions.add(new Move_Id_Integer(result, 1));

        lastResult = result;
    }

    /**
     * f0 -> "false"
     */
    @Override
    public void visit(FalseLiteral n) {
        IR.token.Identifier result = getNewTemp("boolean");
        currentInstructions.add(new Move_Id_Integer(result, 0));

        lastResult = result;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    @Override
    public void visit(Identifier n) {
        String name = n.f0.toString();

        IR.token.Identifier localVar = varMap.get(name);

        if (localVar != null) {
            lastResult = localVar;
            return;
        }

        int offset = classLayouts.get(currentClass).getFieldOffset(name);
        String fieldType = classLayouts.get(currentClass).getFieldType(name);
        IR.token.Identifier fieldVar = getNewTemp(fieldType);
        IR.token.Identifier base = varMap.get("this");

        currentInstructions.add(new Load(fieldVar, base, offset));

        // checkNull(fieldVar);

        lastResult = fieldVar;
    }

    /**
     * f0 -> "this"
     */
    @Override
    public void visit(ThisExpression n) {
        lastResult = varMap.get("this");
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
        Label lenErr = new Label("badLength_" + (labelCounter++));
        Label nullErr = new Label("nullErr_" + (labelCounter++));
        Label success = new Label("success_" + (labelCounter++));

        n.f3.accept(this);
        IR.token.Identifier length = lastResult;

        IR.token.Identifier zero = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(zero, 0));

        IR.token.Identifier one = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(one, 1));

        IR.token.Identifier negOne = getNewTemp("int");
        currentInstructions.add(new Subtract(negOne, zero, one));

        IR.token.Identifier validLength = getNewTemp("int");
        currentInstructions.add(new LessThan(validLength, negOne, length));

        currentInstructions.add(new IfGoto(validLength, lenErr));

        IR.token.Identifier four = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(four, 4));

        IR.token.Identifier bytes = getNewTemp("int");
        currentInstructions.add(new Multiply(bytes, length, four));
        currentInstructions.add(new Add(bytes, bytes, four));

        IR.token.Identifier arr = getNewTemp("arr");
        currentInstructions.add(new Alloc(arr, bytes));

        currentInstructions.add(new IfGoto(arr, nullErr));
        currentInstructions.add(new Store(arr, 0, length));
        currentInstructions.add(new Goto(success));

        currentInstructions.add(new LabelInstr(lenErr));
        currentInstructions.add(new ErrorMessage("\"bad array length\""));

        currentInstructions.add(new LabelInstr(nullErr));
        currentInstructions.add(new ErrorMessage("\"null pointer\""));

        currentInstructions.add(new LabelInstr(success));

        lastResult = arr;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public void visit(AllocationExpression n) {
        Label nullErr = new Label("nullErr_" + (labelCounter++));
        Label success = new Label("success_" + (labelCounter++));

        String className = n.f1.f0.toString();
        ClassLayout classObj = classLayouts.get(className);

        int objBytes = classObj.getObjSize();

        IR.token.Identifier size = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(size, objBytes));

        IR.token.Identifier result = getNewTemp(className);
        currentInstructions.add(new Alloc(result, size));

        currentInstructions.add(new IfGoto(result, nullErr));

        IR.token.Identifier zero = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(zero, 0));

        Map<String, Integer> fieldOffsets = classObj.getFieldOffsets();
        List<Map.Entry<String, Integer>> sortedFields = new ArrayList<>(fieldOffsets.entrySet());
        sortedFields.sort(Map.Entry.comparingByValue());

        for (Map.Entry<String, Integer> entry : sortedFields) {
            // String fieldName = entry.getKey();
            int offset = entry.getValue();
            currentInstructions.add(new Store(result, offset, zero));
        }

        IR.token.Identifier vmt = getNewTemp("int");
        int vmtBytes = classObj.getVmtSize();

        currentInstructions.add(new Move_Id_Integer(size, vmtBytes));
        currentInstructions.add(new Alloc(vmt, size));

        currentInstructions.add(new IfGoto(vmt, nullErr));

        Map<String, Integer> methodOffsets = classObj.getMethodOffsets();

        for (Map.Entry<String, Integer> entry : methodOffsets.entrySet()) {
            String methodName = entry.getKey();
            int offset = entry.getValue();
            IR.token.Identifier methodLabel = getNewTemp(null);
            currentInstructions.add(new Move_Id_FuncName(methodLabel, new FunctionName(methodName)));
            currentInstructions.add(new Store(vmt, offset, methodLabel));
        }

        currentInstructions.add(new Store(result, 0, vmt));
        currentInstructions.add(new Goto(success));

        currentInstructions.add(new LabelInstr(nullErr));
        currentInstructions.add(new ErrorMessage("\"null pointer\""));

        currentInstructions.add(new LabelInstr(success));

        lastResult = result;
    }

    /**
     * f0 -> "!"
     * f1 -> Expression()
     */
    @Override
    public void visit(NotExpression n) {
        n.f1.accept(this);
        IR.token.Identifier expr = lastResult;

        IR.token.Identifier one = getNewTemp("int");
        currentInstructions.add(new Move_Id_Integer(one, 1));

        IR.token.Identifier result = getNewTemp("boolean");
        currentInstructions.add(new Subtract(result, one, expr));

        lastResult = result;
    }
}