import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import IR.token.*;
import sparrow.*;
import sparrow.visitor.DepthFirst;

public class TranslatorNoAlloc extends DepthFirst {
    List<sparrowv.Instruction> instructions;
    List<sparrowv.FunctionDecl> funcs;
    sparrowv.Program prog;

    // Register sets
    // private static final List<String> CALLER = Arrays.asList("t2","t3","t4","t5");
    // private static final List<String> CALLEE = Arrays.asList("s1","s2","s3","s4","s5","s6","s7","s8","s9","s10","s11");
    // private static final List<String> ARG = Arrays.asList("a2","a3","a4","a5","a6","a7");

    private int frameId = 0;

    // Helper: Save/restore a set of registers to stack slots
    private void saveRegisters(List<String> regs, int frame, boolean save) {
        for (String r : regs) {
            Identifier slot = new Identifier("stack_" + frame + "_save_" + r);
            if (save) {
                instructions.add(new sparrowv.Move_Id_Reg(slot, new Register(r)));
            } else {
                instructions.add(new sparrowv.Move_Reg_Id(new Register(r), slot));
            }
        }
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
    public void visit(LabelInstr n) {
        instructions.add(new sparrowv.LabelInstr(n.label));
    }

    @Override
    public void visit(Move_Id_Integer n) {
        instructions.add(new sparrowv.Move_Reg_Integer(new Register("t0"), n.rhs));
        instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register("t0")));
    }

    @Override
    public void visit(Move_Id_FuncName n) {
        instructions.add(new sparrowv.Move_Reg_FuncName(new Register("t0"), n.rhs));
        instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register("t0")));
    }

    @Override
    public void visit(Add n) {
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.arg1));
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t1"), n.arg2));
        instructions.add(new sparrowv.Add(new Register("t0"), new Register("t0"), new Register("t1")));
        instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register("t0")));
    }

    @Override
    public void visit(Subtract n) {
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.arg1));
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t1"), n.arg2));
        instructions.add(new sparrowv.Subtract(new Register("t0"), new Register("t0"), new Register("t1")));
        instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register("t0")));
    }

    @Override
    public void visit(Multiply n) {
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.arg1));
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t1"), n.arg2));
        instructions.add(new sparrowv.Multiply(new Register("t0"), new Register("t0"), new Register("t1")));
        instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register("t0")));
    }

    @Override
    public void visit(LessThan n) {
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.arg1));
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t1"), n.arg2));
        instructions.add(new sparrowv.LessThan(new Register("t0"), new Register("t0"), new Register("t1")));
        instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register("t0")));
    }

    @Override
    public void visit(Load n) {
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.base));
        instructions.add(new sparrowv.Load(new Register("t1"), new Register("t0"), n.offset));
        instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register("t1")));
    }

    @Override
    public void visit(Store n) {
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.rhs));
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t1"), n.base));
        instructions.add(new sparrowv.Store(new Register("t1"), n.offset, new Register("t0")));
    }

    @Override
    public void visit(Move_Id_Id n) {
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.rhs));
        instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register("t0")));
    }

    @Override
    public void visit(Alloc n) {
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.size));
        instructions.add(new sparrowv.Alloc(new Register("t1"), new Register("t0")));
        instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register("t1")));
    }

    @Override
    public void visit(Print n) {
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.content));
        instructions.add(new sparrowv.Print(new Register("t0")));
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
        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.condition));
        instructions.add(new sparrowv.IfGoto(new Register("t0"), n.label));
    }

    @Override
    public void visit(Call n) {
        // int myFrame = frameId; // use current frame for stack slots

        // Save all caller-saved and argument registers before call
        // saveRegisters(CALLER, myFrame, true);
        // saveRegisters(ARG, myFrame, true);

        instructions.add(new sparrowv.Move_Reg_Id(new Register("t0"), n.callee));
        instructions.add(new sparrowv.Call(new Register("t0"), new Register("t0"), n.args));
        instructions.add(new sparrowv.Move_Id_Reg(n.lhs, new Register("t0")));

        // Restore all caller-saved and argument registers after call
        // saveRegisters(CALLER, myFrame, false);
        // saveRegisters(ARG, myFrame, false);
    }
}