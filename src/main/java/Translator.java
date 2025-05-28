import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import IR.syntaxtree.*;
import IR.token.Register;
import IR.token.FunctionName;
import IR.token.Identifier;
import IR.visitor.DepthFirstVisitor;

public class Translator extends DepthFirstVisitor {
    // Liveness information
    private Map<String, Map<String, String>> linearRegAlloc;        // linear register allocation
    private Map<String, Map<String, String>> aRegs;                 // argument "a" registers
    private Map<String, Map<String, Interval>> liveRanges;          // live ranges of local variables
    private Map<String, Map<String, Interval>> aLiveRanges;         // argument live ranges

    // Sparrow-V translation states
    private String currentFunction;
    private List<sparrowv.Instruction> currentInstructions;
    private List<sparrowv.FunctionDecl> functions;
    private Identifier blockReturnID;
    private static final Set<String> CALLER_SET = new HashSet<>(Arrays.asList("t0", "t1", "t2", "t3", "t4", "t5"));
    private static final Set<String> CALLEE_SET = new HashSet<>(Arrays.asList("s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8"));     // save s9, s10, s11
    private static final String[] ARG_REGS = {"a2", "a3", "a4", "a5", "a6", "a7"};
    private int frameId;
    private int currLineNum;


    public Translator(
        Map<String, Map<String, String>> linear, 
        Map<String, Map<String, String>> aRegs,
        Map<String, Map<String, Interval>> liveRanges,
        Map<String, Map<String, Interval>> aRanges
    ) {
        this.linearRegAlloc = linear;
        this.aRegs = aRegs;
        this.currentInstructions = new ArrayList<>();
        this.liveRanges = liveRanges;
        this.aLiveRanges = aRanges;
        frameId = 0;
        currLineNum = 0;
    }

    public String translate() {
        return new sparrowv.Program(functions).toString();
    }

    // ------------------
    // Helper functions
    // ------------------

    private String getRegisterOrSpill(String id) {
        // Check "a" registers first
        if (aRegs.get(currentFunction) != null && aRegs.get(currentFunction).containsKey(id)) {
            return aRegs.get(currentFunction).get(id);
        }

        // Check "t" and "s" registers
        if (linearRegAlloc.get(currentFunction) != null && linearRegAlloc.get(currentFunction).containsKey(id)) {
            return linearRegAlloc.get(currentFunction).get(id);
        }

        // Variable is spilled
        return id;
    }

    private boolean isSpilled(String id) {
        return getRegisterOrSpill(id).equals(id);
    }

    private void saveRestore(Set<String> regs, int frame, boolean save) {
        System.err.println((save?"SAVE ":"REST ")+currentFunction+
                       " @frame"+frame+" -> "+regs);
        for (String r : regs) {
            Identifier slot = new Identifier("stack_" + frame + "_save_" + r);
            if (save) {
                currentInstructions.add(new sparrowv.Move_Id_Reg(slot, new Register(r)));
            } else {
                currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(r), slot));
            }
        }
    }

    private boolean livesPast(String var, int pos) {
        Interval iv = liveRanges.get(currentFunction).get(var);

        if (iv == null) {
            iv = aLiveRanges.get(currentFunction).get(var);
        }
        return iv != null && iv.getLast() > pos;
    }

    // ------------------
    // Core functions
    // ------------------

    /**
     * f0 -> ( FunctionDeclaration() )*
     * f1 -> <EOF>
     */
    @Override
    public void visit(Program n) {
        // Build program with list of functions
        functions = new ArrayList<>();
        n.f0.accept(this);
    }

    /**
     * f0 -> "func"
     * f1 -> FunctionName()
     * f2 -> "("
     * f3 -> ( Identifier() )*
     * f4 -> ")"
     * f5 -> Block()
     */
    @Override
    public void visit(FunctionDeclaration n) {
        // Store current function name
        currentFunction = n.f1.f0.toString();
        int myFrame = frameId++;

        currentInstructions = new ArrayList<>();

        Set<String> usedCallee = linearRegAlloc
            .get(currentFunction)
            .values()
            .stream()
            .filter(CALLEE_SET::contains)
            .collect(Collectors.toSet());

        // Save callee-saved registers
        saveRestore(usedCallee, myFrame, true);

        // Process function block
        n.f5.accept(this);

        // Restore callee-saved registers
        saveRestore(usedCallee, myFrame, false);

        // Process function parameters
        List<Identifier> params = new ArrayList<>();
        if (n.f3.present()) {
            for (int i = 6; i < n.f3.size(); i++) {
                String paramName = ((IR.syntaxtree.Identifier) n.f3.elementAt(i)).f0.toString();
                params.add(new Identifier(paramName));
            }
        }

        sparrowv.Block block = new sparrowv.Block(currentInstructions, blockReturnID);

        functions.add(new sparrowv.FunctionDecl(
            new FunctionName(currentFunction),
            params,
            block
        ));
    }

    /**
     * f0 -> ( Instruction() )*
     * f1 -> "return"
     * f2 -> Identifier()
     */
    @Override
    public void visit(Block n) {
        // Visit instructions
        if (n.f0.present()) {
            for (Node node : n.f0.nodes) {
                node.accept(this);
            }
        }

        // Process return identifier
        String retId = getRegisterOrSpill(n.f2.f0.toString());
        currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier("v0"), new Register(retId)));
        blockReturnID = new Identifier("v0");
    }

    /**
     * f0 -> LabelWithColon()
     *       | SetInteger()
     *       | SetFuncName()
     *       | Add()
     *       | Subtract()
     *       | Multiply()
     *       | LessThan()
     *       | Load()
     *       | Store()
     *       | Move()
     *       | Alloc()
     *       | Print()
     *       | ErrorMessage()
     *       | Goto()
     *       | IfGoto()
     *       | Call()
     */
    @Override
    public void visit(Instruction n) {
        currLineNum++;
        n.f0.accept(this);
    }


    /**
     * f0 -> Label()
     * f1 -> ":"
     */
    @Override
    public void visit(LabelWithColon n) {
        IR.token.Label label = new IR.token.Label(n.f0.f0.toString());

        currentInstructions.add(new sparrowv.LabelInstr(label));
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> IntegerLiteral()
     */
    @Override
    public void visit(SetInteger n) {
        // Get identifiers
        String lhs = n.f0.f0.toString();
        
        // Get register allocation
        String lhsReg = isSpilled(lhs) ? "s9" : getRegisterOrSpill(lhs);

        int val = Integer.parseInt(n.f2.f0.toString());

        // Sparrow-V move integer into register
        currentInstructions.add(new sparrowv.Move_Reg_Integer(new Register(lhsReg), val));

        // Spill register value into identifier in memory (if id was not allocated to a non-temp register)
        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "@"
     * f3 -> FunctionName()
     */
    @Override
    public void visit(SetFuncName n) {
        // Get identifier
        String lhs = n.f0.f0.toString();

        // Get register allocation
        String lhsReg = isSpilled(lhs) ? "s9" : getRegisterOrSpill(lhs);

        // Sparrow-V Move_Reg_FuncName instruction
        currentInstructions.add(new sparrowv.Move_Reg_FuncName(new Register(lhsReg), new FunctionName(n.f3.f0.toString())));

        // Spill into memory if no register allocated
        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "+"
     * f4 -> Identifier()
     */
    @Override
    public void visit(Add n) {
        // Get identifiers
        String lhs = n.f0.f0.toString();
        String arg1 = n.f2.f0.toString();
        String arg2 = n.f4.f0.toString();

        // Get register allocations if they exist
        String arg1Reg = getRegisterOrSpill(arg1);
        String arg2Reg = getRegisterOrSpill(arg2);

        // Use temp register s9
        if (isSpilled(arg1)) {
            arg1Reg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(arg1Reg), new Identifier(arg1)));
        }

        // Use temp register s10
        if (isSpilled(arg2)) {
            arg2Reg = "s10";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(arg2Reg), new Identifier(arg2)));
        }

        // Use temp register s11
        String lhsReg = isSpilled(lhs) ? "s11" : getRegisterOrSpill(lhs);

        // Sparrow-V Add instruction
        currentInstructions.add(new sparrowv.Add(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        // Move back to identifier is lhs spilled
        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "-"
     * f4 -> Identifier()
     */
    @Override
    public void visit(Subtract n) {
        // Get identifiers
        String lhs = n.f0.f0.toString();
        String arg1 = n.f2.f0.toString();
        String arg2 = n.f4.f0.toString();

        // Get register allocations if they exist
        String arg1Reg = getRegisterOrSpill(arg1);
        String arg2Reg = getRegisterOrSpill(arg2);

        // Use temp register s9
        if (isSpilled(arg1)) {
            arg1Reg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(arg1Reg), new Identifier(arg1)));
        }

        // Use temp register s10
        if (isSpilled(arg2)) {
            arg2Reg = "s10";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(arg2Reg), new Identifier(arg2)));
        }

        // Use temp register s11
        String lhsReg = isSpilled(lhs) ? "s11" : getRegisterOrSpill(lhs);

        // Sparrow-V Subtract instruction
        currentInstructions.add(new sparrowv.Subtract(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        // Move back to identifier is lhs spilled
        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "*"
     * f4 -> Identifier()
     */
    @Override
    public void visit(Multiply n) {
        // Get identifiers
        String lhs = n.f0.f0.toString();
        String arg1 = n.f2.f0.toString();
        String arg2 = n.f4.f0.toString();

        // Get register allocations if they exist
        String arg1Reg = getRegisterOrSpill(arg1);
        String arg2Reg = getRegisterOrSpill(arg2);

        // Use temp register s9
        if (isSpilled(arg1)) {
            arg1Reg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(arg1Reg), new Identifier(arg1)));
        }

        // Use temp register s10
        if (isSpilled(arg2)) {
            arg2Reg = "s10";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(arg2Reg), new Identifier(arg2)));
        }

        // Use temp register s11
        String lhsReg = isSpilled(lhs) ? "s11" : getRegisterOrSpill(lhs);

        // Sparrow-V Multiply instruction
        currentInstructions.add(new sparrowv.Multiply(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        // Move back to identifier is lhs spilled
        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "<"
     * f4 -> Identifier()
     */
    @Override
    public void visit(LessThan n) {
        // Get identifiers
        String lhs = n.f0.f0.toString();
        String arg1 = n.f2.f0.toString();
        String arg2 = n.f4.f0.toString();

        // Get register allocations if they exist
        String arg1Reg = getRegisterOrSpill(arg1);
        String arg2Reg = getRegisterOrSpill(arg2);

        // Use temp register s9
        if (isSpilled(arg1)) {
            arg1Reg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(arg1Reg), new Identifier(arg1)));
        }

        // Use temp register s10
        if (isSpilled(arg2)) {
            arg2Reg = "s10";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(arg2Reg), new Identifier(arg2)));
        }

        // Use temp register s11
        String lhsReg = isSpilled(lhs) ? "s11" : getRegisterOrSpill(lhs);

        // Sparrow-V LessThan instruction
        currentInstructions.add(new sparrowv.LessThan(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        // Move back to identifier is lhs spilled
        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "["
     * f3 -> Identifier()
     * f4 -> "+"
     * f5 -> IntegerLiteral()
     * f6 -> "]"
     */
    @Override
    public void visit(Load n) {
        // Get identifiers
        String lhs = n.f0.f0.toString();
        String base = n.f3.f0.toString();

        // Get register allocations
        String baseReg = getRegisterOrSpill(base);

        // Load spills from stack id into register
        if (isSpilled(base)) {
            baseReg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(baseReg), new Identifier(base)));
        }

        String lhsReg = isSpilled(lhs) ? "s10" : getRegisterOrSpill(lhs);

        // Get load offset
        int offset = Integer.parseInt(n.f5.f0.toString());

        // Sparrow-V Load instruction
        currentInstructions.add(new sparrowv.Load(new Register(lhsReg), new Register(baseReg), offset));

        // Store result back into spilled variable
        if (isSpilled(base)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    /**
     * f0 -> "["
     * f1 -> Identifier()
     * f2 -> "+"
     * f3 -> IntegerLiteral()
     * f4 -> "]"
     * f5 -> "="
     * f6 -> Identifier()
     */
    // TODO : Doesn't make sense that reg + 4 is being used, and then I just do id = reg? how did the value end up in id?
    @Override
    public void visit(Store n) {
        // Get identifiers
        String base = n.f1.f0.toString();
        String rhs = n.f6.f0.toString();

        // Get register allocations
        String rhsReg = getRegisterOrSpill(rhs);

        if (isSpilled(rhs)) {
            rhsReg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(rhsReg), new Identifier(rhs)));
        }

        String baseReg = isSpilled(base) ? "s10" : getRegisterOrSpill(base);

        // Get store offset
        int offset = Integer.parseInt(n.f3.f0.toString());

        // Sparrow-V Store instruction
        currentInstructions.add(new sparrowv.Store(new Register(baseReg), offset, new Register(rhsReg)));

        // Handle base address spill
        if (isSpilled(base)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(base), new Register(baseReg)));
        }
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     */
    @Override
    public void visit(Move n) {
        // Get identifiers
        String lhs = n.f0.f0.toString();
        String rhs = n.f2.f0.toString();

        // Get register allocation
        String rhsReg = getRegisterOrSpill(rhs);

        // Load spilled variable into temp register
        if (isSpilled(rhs)) {
            rhsReg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(rhsReg), new Identifier(rhs)));
        }

        String lhsReg = isSpilled(lhs) ? "s10" : getRegisterOrSpill(lhs);

        // Sparrow-V Move instruction
        currentInstructions.add(new sparrowv.Move_Reg_Reg(new Register(lhsReg), new Register(rhsReg)));

        // Load temp register into spilled lhs variable
        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "alloc"
     * f3 -> "("
     * f4 -> Identifier()
     * f5 -> ")"
     */
    @Override
    public void visit(Alloc n) {
        // Get identifiers
        String lhs = n.f0.f0.toString();
        String size = n.f4.f0.toString();

        // Get register allocation
        String sizeReg = getRegisterOrSpill(size);

        // Handle spills
        if (isSpilled(size)) {
            sizeReg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(sizeReg), new Identifier(size)));
        }

        String lhsReg = isSpilled(lhs) ? "s10" : getRegisterOrSpill(lhs);

        // Sparrow-V Alloc instruction
        currentInstructions.add(new sparrowv.Alloc(new Register(lhsReg), new Register(sizeReg)));

        // Handle lhs spill
        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    /**
     * f0 -> "print"
     * f1 -> "("
     * f2 -> Identifier()
     * f3 -> ")"
     */
    @Override
    public void visit(Print n) {
        // Get identifiers
        String id = n.f2.f0.toString();

        // Get register allocation
        String idReg = getRegisterOrSpill(id);

        // Handle spilled identifier
        if (isSpilled(id)) {
            idReg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(idReg), new Identifier(id)));
        }

        // Sparrow-V Print instruction
        currentInstructions.add(new sparrowv.Print(new Register(idReg)));
    }

    /**
     * f0 -> "error"
     * f1 -> "("
     * f2 -> StringLiteral()
     * f3 -> ")"
     */
    @Override
    public void visit(ErrorMessage n) {
        // Sparrow-V ErrorMessage instruction
        currentInstructions.add(new sparrowv.ErrorMessage(n.f2.f0.toString()));
    }

    /**
     * f0 -> "goto"
     * f1 -> Label()
     */
    @Override
    public void visit(Goto n) {
        // Create new label
        IR.token.Label label = new IR.token.Label(n.f1.f0.toString());

        // Sparrow-V Goto instruction
        currentInstructions.add(new sparrowv.Goto(label));
    }

    /**
     * f0 -> "if0"
     * f1 -> Identifier()
     * f2 -> "goto"
     * f3 -> Label()
     */
    @Override
    public void visit(IfGoto n) {
        // Get condition identifier
        String cond = n.f1.f0.toString();

        // Get register allocation
        String condReg = getRegisterOrSpill(cond);

        if (isSpilled(cond)) {
            condReg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(condReg), new Identifier(cond)));
        }

        // Create new label
        IR.token.Label label = new IR.token.Label(n.f3.f0.toString());

        // Sparrow-V IfGoto instruction
        currentInstructions.add(new sparrowv.IfGoto(new Register(condReg), label));
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "call"
     * f3 -> Identifier()
     * f4 -> "("
     * f5 -> ( Identifier() )*
     * f6 -> ")"
     */
    // TODO
    @Override
    public void visit(Call n) {
        // Get result identifier
        String lhs = n.f0.f0.toString();
        String callee = n.f3.f0.toString();

        // Get lhs register
        String lhsReg = getRegisterOrSpill(lhs);

        if (isSpilled(lhs)) {
            lhsReg = "s10";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(lhsReg), new Identifier(lhs)));
        }

        int beforeCallLine = currLineNum;

        // Find live caller registers
        Map<String, String> locals = linearRegAlloc.get(currentFunction);
        Set<String> liveCaller = new HashSet<>();

        for (Map.Entry<String, String> e : locals.entrySet()) {
            String var = e.getKey(), reg = e.getValue();

            // Only consider t0-t5
            if (!CALLER_SET.contains(reg)) 
                continue;

            // Ignore lhs register
            if (reg.equals(lhsReg))
                continue;

            if (livesPast(var, beforeCallLine)) {
                liveCaller.add(reg);
            }
        }

        // Find live arg registers
        Set<String> liveArgs = new HashSet<>();

        for (Map.Entry<String, String> e : aRegs.get(currentFunction).entrySet()) {
            String var = e.getKey(), reg = e.getValue();

            // Ignore lhs register
            if (reg.equals(lhsReg))
                continue;

            if (livesPast(var, beforeCallLine)) {
                liveArgs.add(reg);
            }
        }

        int callFrame = frameId++;

        // Save caller and arg registers
        saveRestore(liveCaller, callFrame, true);
        saveRestore(liveArgs, callFrame, true);

        // Process call arguments, handle spills
        List<Identifier> stackArgs = new ArrayList<>();
        int argIdx = 2;

        if (n.f5.present()) {
            for (Node node : n.f5.nodes) {
                String arg = ((IR.syntaxtree.Identifier) node).f0.toString();

                if (argIdx <= 7) {
                    String argReg = "a" + argIdx;
                    String srcReg = getRegisterOrSpill(arg);

                    if (isSpilled(arg)) {
                        srcReg = "s9";
                        currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(srcReg), new Identifier(arg)));
                    }
                    currentInstructions.add(new sparrowv.Move_Reg_Reg(new Register(argReg), new Register(srcReg)));
                } else {
                    stackArgs.add(new Identifier(arg));
                }

                argIdx++;
            }
        }

        // Move callee to register is spilled
        String calleeReg = getRegisterOrSpill(callee);
        if (isSpilled(callee)) {
            calleeReg = "s9";
            currentInstructions.add(new sparrowv.Move_Reg_Id(new Register(calleeReg), new Identifier(callee)));
        }

        // Sparrow-V Call instruction
        currentInstructions.add(new sparrowv.Call(new Register(lhsReg), new Register(calleeReg), stackArgs));

        // Move result to lhs in case of spill
        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }

        // Restore caller registers and arg registers
        saveRestore(liveCaller, callFrame, false);
        saveRestore(liveArgs, callFrame, false);
    }
}