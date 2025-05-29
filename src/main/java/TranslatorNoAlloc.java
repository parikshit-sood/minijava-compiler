import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import IR.token.Identifier;
import IR.token.Register;
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
import sparrow.visitor.DepthFirst;

public class TranslatorNoAlloc extends DepthFirst {
    // Liveness information
    final private Map<String, Map<String, String>> linearRegAlloc;        // linear register allocation
    final private Map<String, Map<String, String>> aRegs;                 // argument "a" registers
    private Map<String, Map<String, Interval>> liveRanges;          // live ranges of local variables
    private Map<String, Map<String, Interval>> aLiveRanges;         // argument live ranges

    // Sparrow-V states
    List<sparrowv.Instruction> instructions;
    List<sparrowv.FunctionDecl> funcs;
    sparrowv.Program prog;
    String currentFunction;
    int currLineNum;

    // Register sets
    private static final List<String> CALLER_SET = Arrays.asList("t0", "t1", "t2","t3","t4","t5");
    private static final List<String> CALLEE_SET = Arrays.asList("s1","s2","s3","s4","s5","s6","s7","s8");
    private static final List<String> ARG_REGS = Arrays.asList("a2","a3","a4","a5","a6","a7");

    private int frameId = 0;

    public TranslatorNoAlloc(
        Map<String, Map<String, String>> linearRegAlloc,
        Map<String, Map<String, String>> aRegs,
        Map<String, Map<String, Interval>> tsIntervals,
        Map<String, Map<String, Interval>> aRanges
    ) {
        this.linearRegAlloc = linearRegAlloc;
        this.aRegs = aRegs;
        this.liveRanges = new HashMap<>(tsIntervals);
        this.aLiveRanges = new HashMap<>(aRanges);
        this.currLineNum = 0;
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

    private void saveRestore(Set<String> regs, int frame, boolean save) {
        System.err.println((save?"SAVE ":"REST ")+currentFunction+
                       " @frame"+frame+" -> "+regs);
        for (String r : regs) {
            Identifier slot = new Identifier("stack_" + frame + "_save_" + r);
            if (save) {
                instructions.add(new sparrowv.Move_Id_Reg(slot, new Register(r)));
            } else {
                instructions.add(new sparrowv.Move_Reg_Id(new Register(r), slot));
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

    @Override
    public void visit(Program n) { 
        funcs = new ArrayList<>();
        for (FunctionDecl fd : n.funDecls) {
            fd.accept(this);
        }
        prog = new sparrowv.Program(funcs);
    }

    @Override
    public void visit(FunctionDecl n) {
        int myFrame = frameId++;
        List<Identifier> params = new ArrayList<>();
        for (Identifier fp : n.formalParameters) {
            params.add(fp);
        }
        instructions = new ArrayList<>();

        // Function prologue: save all callee-saved registers
        // saveRegisters(CALLEE, myFrame, true);

        n.block.accept(this);

        // Function epilogue: restore all callee-saved registers
        // saveRegisters(CALLEE, myFrame, false);

        sparrowv.Block block = new sparrowv.Block(instructions, n.block.return_id);
        funcs.add(new sparrowv.FunctionDecl(n.functionName, params, block));
    }

    @Override
    public void visit(Block n) {
        for (Instruction i: n.instructions) {
            currLineNum++;
            i.accept(this);
        }
    }

    @Override
    public void visit(LabelInstr n) {
        instructions.add(new sparrowv.LabelInstr(n.label));
    }

    @Override
    public void visit(Move_Id_Integer n) {
        String lhs = n.lhs.toString();
        String lhsReg = isSpilled(lhs) ? "s9" : getRegisterOrSpill(lhs);

        instructions.add(new sparrowv.Move_Reg_Integer(new Register(lhsReg), n.rhs));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));

    }

    @Override
    public void visit(Move_Id_FuncName n) {
        String lhs = n.lhs.toString();
        String lhsReg = isSpilled(lhs) ? "s9" : getRegisterOrSpill(lhs);

        instructions.add(new sparrowv.Move_Reg_FuncName(new Register(lhsReg), n.rhs));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Add n) {
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String lhs = n.lhs.toString();

        String arg1Reg = isSpilled(arg1) ? "s9" : getRegisterOrSpill(arg1);
        String arg2Reg = isSpilled(arg2) ? "s10" : getRegisterOrSpill(arg2);
        String lhsReg = isSpilled(lhs) ? "s11" : getRegisterOrSpill(lhs);
        
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
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String lhs = n.lhs.toString();

        String arg1Reg = isSpilled(arg1) ? "s9" : getRegisterOrSpill(arg1);
        String arg2Reg = isSpilled(arg2) ? "s10" : getRegisterOrSpill(arg2);
        String lhsReg = isSpilled(lhs) ? "s11" : getRegisterOrSpill(lhs);
        
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
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String lhs = n.lhs.toString();

        String arg1Reg = isSpilled(arg1) ? "s9" : getRegisterOrSpill(arg1);
        String arg2Reg = isSpilled(arg2) ? "s10" : getRegisterOrSpill(arg2);
        String lhsReg = isSpilled(lhs) ? "s11" : getRegisterOrSpill(lhs);
        
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
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String lhs = n.lhs.toString();

        String arg1Reg = isSpilled(arg1) ? "s9" : getRegisterOrSpill(arg1);
        String arg2Reg = isSpilled(arg2) ? "s10" : getRegisterOrSpill(arg2);
        String lhsReg = isSpilled(lhs) ? "s11" : getRegisterOrSpill(lhs);
        
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
        String lhs = n.lhs.toString();
        String base = n.base.toString();

        String baseReg = isSpilled(base) ? "s9" : getRegisterOrSpill(base);
        String lhsReg = isSpilled(lhs) ? "s10" : getRegisterOrSpill(lhs);

        if (isSpilled(base))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(baseReg), n.base));

        instructions.add(new sparrowv.Load(new Register(lhsReg), new Register(baseReg), n.offset));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Store n) {
        String base = n.base.toString();
        String rhs = n.rhs.toString();

        String rhsReg = isSpilled(rhs) ? "s9" : getRegisterOrSpill(rhs);
        String baseReg = isSpilled(base) ? "s10" : getRegisterOrSpill(base);

        if (isSpilled(rhs))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(rhsReg), n.rhs));

        if (isSpilled(base))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(baseReg), n.base));

        instructions.add(new sparrowv.Store(new Register(baseReg), n.offset, new Register(rhsReg)));
    }

    @Override
    public void visit(Move_Id_Id n) {
        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();

        String lhsReg = isSpilled(lhs) ? "s9" : getRegisterOrSpill(lhs);
        String rhsReg = isSpilled(rhs) ? "s10" : getRegisterOrSpill(rhs);

        if (isSpilled(rhs))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(rhsReg), n.rhs));

        instructions.add(new sparrowv.Move_Reg_Reg(new Register(lhsReg), new Register(rhsReg)));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Alloc n) {
        String size = n.size.toString();
        String lhs = n.lhs.toString();

        String sizeReg = isSpilled(size) ? "s9" : getRegisterOrSpill(size);
        String lhsReg = isSpilled(lhs) ? "s9" : getRegisterOrSpill(lhs);

        if (isSpilled(size))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(sizeReg), n.size));

        instructions.add(new sparrowv.Alloc(new Register(lhsReg), new Register(sizeReg)));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));
    }

    @Override
    public void visit(Print n) {
        String content = n.content.toString();
        String contentReg = isSpilled(content) ? "s9" : getRegisterOrSpill(content);

        if (isSpilled(content))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(contentReg), n.content));
        instructions.add(new sparrowv.Print(new Register(contentReg)));
    }

    @Override
    public void visit(ErrorMessage n) {
        instructions.add(new sparrowv.ErrorMessage(n.msg));
    }

    @Override
    public void visit(Goto n) {
        instructions.add(new sparrowv.Goto(n.label));
    }

    @Override
    public void visit(IfGoto n) {
        String cond = n.condition.toString();
        String condReg = isSpilled(cond) ? "s9" : getRegisterOrSpill(cond);

        if (isSpilled(cond))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(condReg), n.condition));
        instructions.add(new sparrowv.IfGoto(new Register(condReg), n.label));
    }

    @Override
    public void visit(Call n) {
        int callFrame = frameId;

        // Save all caller-saved and argument registers before call
        // saveRegisters(CALLER, myFrame, true);
        // saveRegisters(ARG, myFrame, true);

        String callee = n.callee.toString();
        String lhs = n.lhs.toString();

        String calleeReg = isSpilled(callee) ? "s9" : getRegisterOrSpill(callee);
        String lhsReg = isSpilled(lhs) ? "s10" : getRegisterOrSpill(lhs);

        if (isSpilled(callee))
            instructions.add(new sparrowv.Move_Reg_Id(new Register(calleeReg), n.callee));

        instructions.add(new sparrowv.Call(new Register(lhsReg), new Register(calleeReg), n.args));

        if (isSpilled(lhs))
            instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register(lhsReg)));

        // Restore all caller-saved and argument registers after call
        // saveRegisters(CALLER, myFrame, false);
        // saveRegisters(ARG, myFrame, false);
    }
}