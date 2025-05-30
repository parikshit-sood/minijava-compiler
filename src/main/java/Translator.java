import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import IR.token.Identifier;
import IR.token.Register;
import sparrow.*;
import sparrow.visitor.DepthFirst;

public class Translator extends DepthFirst {
    // Liveness information
    final private Map<String, Map<String, String>> linearRegAlloc;        // linear register allocation
    final private Map<String, Map<String, String>> aRegs;                 // argument "a" registers
    final private Map<String, Map<String, Interval>> liveRanges;          // live ranges of local variables
    final private Map<String, Map<String, Interval>> aLiveRanges;         // argument live ranges

    // Sparrow-V states
    List<sparrowv.Instruction> instructions;
    List<sparrowv.FunctionDecl> funcs;
    sparrowv.Program prog;
    String currentFunction;

    // Register sets
    private static final Set<String> CALLER_SET = new HashSet<>(Arrays.asList( "t3","t4","t5"));
    private static final Set<String> CALLEE_SET = new HashSet<>(Arrays.asList("s1","s2","s3","s4","s5","s6","s7","s8", "s9", "s10", "s11"));
    private static final Set<String> ARG_REGS = new HashSet<>(Arrays.asList("a2","a3","a4","a5","a6","a7"));

    boolean isMain;
    private int currLine;

    public Translator(
        Map<String, Map<String, String>> linearRegAlloc,
        Map<String, Map<String, String>> aRegs,
        Map<String, Map<String, Interval>> tsIntervals,
        Map<String, Map<String, Interval>> aRanges
    ) {
        this.linearRegAlloc = linearRegAlloc;
        this.aRegs = aRegs;
        this.liveRanges = new HashMap<>(tsIntervals);
        this.aLiveRanges = new HashMap<>(aRanges);
    }

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

    private void saveRestore(Set<String> regs, boolean save) {
        if (regs.isEmpty()) return;

        for (String r : regs) {
            Identifier slot = new Identifier("stack_save_" + r);
            if (save) {
                instructions.add(new sparrowv.Move_Id_Reg(slot, new Register(r)));
            } else {
                instructions.add(new sparrowv.Move_Reg_Id(new Register(r), slot));
            }
        }
    }

    private boolean livePastNow(String reg) {
        Map<String, Interval> l  = liveRanges.get(currentFunction);
        Map<String, Interval> al = aLiveRanges.get(currentFunction);

        return Stream.concat(l.entrySet().stream(), al.entrySet().stream())
                    .anyMatch(e -> {
                        String home = getRegisterOrSpill(e.getKey());
                        return home.equals(reg) && e.getValue().getLast() > currLine;
                    });
    }

    @Override
    public void visit(Program n) { 
        funcs = new ArrayList<>();
        int idx = 0;    // Main function is always the first

        for (FunctionDecl fd : n.funDecls) {
            isMain = (idx == 0);
            fd.accept(this);
            idx++;
        }
        prog = new sparrowv.Program(funcs);
    }

    @Override
    public void visit(FunctionDecl n) {                 
        currentFunction = n.functionName.toString();
        currLine = 0;

        // Load formal parameters
        List<Identifier> params = new ArrayList<>();
        for (int i = 6; i < n.formalParameters.size(); i++) {
            Identifier fp = n.formalParameters.get(i);
            params.add(fp);
        }

        instructions = new ArrayList<>();

        // Get used callees for the current function
        Set<String> usedCallees = linearRegAlloc.get(currentFunction)
            .values()
            .stream()
            .filter(CALLEE_SET::contains)
            .collect(Collectors.toSet());

        // Save callee-saved registers
        if (!isMain)
            saveRestore(usedCallees, true);
        
        // Load formal parameters into registers if needed
        for (int i = 0; i < params.size(); i++) {
            Identifier param = params.get(i);
            String paramName = param.toString();
            String paramReg = getRegisterOrSpill(paramName);

            if (!isSpilled(paramName)) {
                instructions.add(new sparrowv.Move_Reg_Id(new Register(paramReg), param));
            }
        }

        n.block.accept(this);

        Identifier returnId = n.block.return_id;
        String returnReg = getRegisterOrSpill(returnId.toString());

        if (!isSpilled(returnId.toString()))
            instructions.add(new sparrowv.Move_Id_Reg(returnId, new Register(returnReg)));

        // Restore used callee registers
        if (!isMain)
            saveRestore(usedCallees, false);

        sparrowv.Block block = new sparrowv.Block(instructions, returnId);
        funcs.add(new sparrowv.FunctionDecl(n.functionName, params, block));
    }

    @Override
    public void visit(LabelInstr n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        instructions.add(new sparrowv.LabelInstr(n.label));
    }

    @Override
    public void visit(Move_Id_Integer n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String lhs = n.lhs.toString();
        String lhsReg = isSpilled(lhs) ? "t0" : getRegisterOrSpill(lhs);

        instructions.add(new sparrowv.Move_Reg_Integer(new Register(lhsReg), n.rhs));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));

    }

    @Override
    public void visit(Move_Id_FuncName n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String lhs = n.lhs.toString();
        String lhsReg = isSpilled(lhs) ? "t0" : getRegisterOrSpill(lhs);

        instructions.add(new sparrowv.Move_Reg_FuncName(new Register(lhsReg), n.rhs));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Add n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String lhs = n.lhs.toString();

        String arg1Reg = isSpilled(arg1) ? "t0" : getRegisterOrSpill(arg1);
        String arg2Reg = isSpilled(arg2) ? "t1" : getRegisterOrSpill(arg2);
        String lhsReg = isSpilled(lhs) ? "t1" : getRegisterOrSpill(lhs);
        
        if (isSpilled(arg1))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(arg1Reg), n.arg1));
        
        if (isSpilled(arg2))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(arg2Reg), n.arg2));

        instructions.add(new sparrowv.Add(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Subtract n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String lhs = n.lhs.toString();

        String arg1Reg = isSpilled(arg1) ? "t0" : getRegisterOrSpill(arg1);
        String arg2Reg = isSpilled(arg2) ? "t1" : getRegisterOrSpill(arg2);
        String lhsReg = isSpilled(lhs) ? "t1" : getRegisterOrSpill(lhs);
        
        if (isSpilled(arg1))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(arg1Reg), n.arg1));
        
        if (isSpilled(arg2))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(arg2Reg), n.arg2));

        instructions.add(new sparrowv.Subtract(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Multiply n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String lhs = n.lhs.toString();

        String arg1Reg = isSpilled(arg1) ? "t0" : getRegisterOrSpill(arg1);
        String arg2Reg = isSpilled(arg2) ? "t1" : getRegisterOrSpill(arg2);
        String lhsReg = isSpilled(lhs) ? "t1" : getRegisterOrSpill(lhs);
        
        if (isSpilled(arg1))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(arg1Reg), n.arg1));
        
        if (isSpilled(arg2))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(arg2Reg), n.arg2));

        instructions.add(new sparrowv.Multiply(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(LessThan n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String lhs = n.lhs.toString();

        String arg1Reg = isSpilled(arg1) ? "t0" : getRegisterOrSpill(arg1);
        String arg2Reg = isSpilled(arg2) ? "t1" : getRegisterOrSpill(arg2);
        String lhsReg = isSpilled(lhs) ? "t1" : getRegisterOrSpill(lhs);
        
        if (isSpilled(arg1))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(arg1Reg), n.arg1));
        
        if (isSpilled(arg2))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(arg2Reg), n.arg2));

        instructions.add(new sparrowv.LessThan(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Load n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String lhs = n.lhs.toString();
        String base = n.base.toString();

        String baseReg = isSpilled(base) ? "t0" : getRegisterOrSpill(base);
        String lhsReg = isSpilled(lhs) ? "t1" : getRegisterOrSpill(lhs);

        if (isSpilled(base))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(baseReg), n.base));

        instructions.add(new sparrowv.Load(new Register(lhsReg), new Register(baseReg), n.offset));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Store n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String base = n.base.toString();
        String rhs = n.rhs.toString();

        String rhsReg = isSpilled(rhs) ? "t0" : getRegisterOrSpill(rhs);
        String baseReg = isSpilled(base) ? "t1" : getRegisterOrSpill(base);

        if (isSpilled(rhs))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(rhsReg), n.rhs));

        if (isSpilled(base))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(baseReg), n.base));

        instructions.add(new sparrowv.Store(new Register(baseReg), n.offset, new Register(rhsReg)));
    }

    @Override
    public void visit(Move_Id_Id n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();

        String lhsReg = isSpilled(lhs) ? "t0" : getRegisterOrSpill(lhs);
        String rhsReg = isSpilled(rhs) ? "t1" : getRegisterOrSpill(rhs);

        if (isSpilled(rhs))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(rhsReg), n.rhs));

        instructions.add(new sparrowv.Move_Reg_Reg(new Register(lhsReg), new Register(rhsReg)));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Alloc n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String size = n.size.toString();
        String lhs = n.lhs.toString();

        String sizeReg = isSpilled(size) ? "t0" : getRegisterOrSpill(size);
        String lhsReg = isSpilled(lhs) ? "t1" : getRegisterOrSpill(lhs);

        if (isSpilled(size))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(sizeReg), n.size));

        instructions.add(new sparrowv.Alloc(new Register(lhsReg), new Register(sizeReg)));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Print n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String content = n.content.toString();
        String contentReg = isSpilled(content) ? "t0" : getRegisterOrSpill(content);

        if (isSpilled(content))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(contentReg), n.content));
        instructions.add(new sparrowv.Print(new Register(contentReg)));
    }

    @Override
    public void visit(ErrorMessage n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        instructions.add(new sparrowv.ErrorMessage(n.msg));
    }

    @Override
    public void visit(Goto n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);
        
        instructions.add(new sparrowv.Goto(n.label));
    }

    @Override
    public void visit(IfGoto n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        String cond = n.condition.toString();
        String condReg = isSpilled(cond) ? "t0" : getRegisterOrSpill(cond);

        if (isSpilled(cond))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(condReg), n.condition));
        instructions.add(new sparrowv.IfGoto(new Register(condReg), n.label));
    }

    @Override
    public void visit(Call n) {
        currLine++;

        System.err.println("Translating function: " + currentFunction + ", at Sparrow line: " + currLine);

        // Save all caller-saved and argument registers before call
        String callee = n.callee.toString();
        String lhs = n.lhs.toString();

        String calleeReg = isSpilled(callee) ? "t2" : getRegisterOrSpill(callee);
        String lhsReg = isSpilled(lhs) ? "t1" : getRegisterOrSpill(lhs);

        // Get live caller and args registers after the call
        Set<String> liveCaller = CALLER_SET.stream()
                     .filter(this::livePastNow)
                     .collect(Collectors.toSet());

        Set<String> liveArgs = new HashSet<>();
        if (aRegs.get(currentFunction) != null) {
            liveArgs.addAll(aRegs.get(currentFunction).values());
        }

        saveRestore(liveCaller, true);
        saveRestore(liveArgs, true);

        if (isSpilled(callee))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(calleeReg), n.callee));

        // Load arguments into "a" registers from source register
        for (int i = 0; i < Math.min(6, n.args.size()); i++) {
            Identifier arg = n.args.get(i);
            String srcReg = getRegisterOrSpill(arg.toString());

            if (isSpilled(arg.toString())) {
                instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), arg));
                instructions.add(new sparrowv.Move_Reg_Reg(new Register("a" + (i + 2)), new Register("t0")));
            } else {
                if (ARG_REGS.contains(srcReg)) {
                    instructions.add(new sparrowv.Move_Reg_Id(new Register("a" + (i + 2)), new Identifier("stack_save_" + srcReg)));
                } else {
                    instructions.add(new sparrowv.Move_Reg_Reg(new Register("a" + (i + 2)), new Register(srcReg)));
                }
            }
        }

        // Spill additional arguments into "t" registers
        List<Identifier> overflowArgs = new ArrayList<>();
        for (int i = 6; i < n.args.size(); i++) {
            Identifier arg = n.args.get(i);
            String srcReg = getRegisterOrSpill(arg.toString());

            if (!isSpilled(arg.toString())) {
                if (!ARG_REGS.contains(srcReg))
                    instructions.add(new sparrowv.Move_Id_Reg(arg, new Register(srcReg)));
                else {
                    instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), new Identifier("stack_save_" + srcReg)));
                    instructions.add(new sparrowv.Move_Id_Reg(arg, new Register("t0")));
                }
            }

            overflowArgs.add(arg);
        }

        instructions.add(new sparrowv.Call(new Register(lhsReg), new Register(calleeReg), overflowArgs));

        if (isSpilled(lhs)) {
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
        } else {
            instructions.add(new sparrowv.Move_Reg_Reg(new Register("t1"), new Register(lhsReg)));
        }

        // Restore all caller-saved and argument registers after call
        saveRestore(liveCaller, false);
        saveRestore(liveArgs, false);

        if (!isSpilled(lhs)) {
            instructions.add(new sparrowv.Move_Reg_Reg(new Register(lhsReg), new Register("t1")));
        }
    }
}