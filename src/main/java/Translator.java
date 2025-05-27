import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import IR.token.FunctionName;
import IR.token.Identifier;
import IR.token.Register;
import sparrow.visitor.DepthFirst;
import sparrow.*;
import sparrowv.*;

public class Translator extends DepthFirst{
    private Map<String, Map<String, String>> linearRegAlloc;
    private Map<String, Map<String, String>> aRegs;
    private Map<String, Map<String, Integer>> spilledOffsets;
    private String currentFunction;
    private List<sparrowv.Instruction> currentInstructions;
    private sparrowv.Program program;

    public Translator(
        Map<String, Map<String, String>> linear, 
        Map<String, Map<String, String>> aRegs
    ) {
        this.linearRegAlloc = linear;
        this.aRegs = aRegs;
        this.spilledOffsets = new HashMap<>();
        this.currentInstructions = new ArrayList<>();
        computeSpilledOffsets();
    }

    // ------------------
    // Helper functions
    // ------------------

    // Compute stack offsets for variables spilled into memory
    private void computeSpilledOffsets() {
        for (String funcName : linearRegAlloc.keySet())
            spilledOffsets.put(funcName, new HashMap<>());
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

    private int getStackOffset(String id) {
        if (!spilledOffsets.get(currentFunction).containsKey(id)) {
            int offset = spilledOffsets.get(currentFunction).size() * 4;
            spilledOffsets.get(currentFunction).put(id, offset);
        }

        return spilledOffsets.get(currentFunction).get(id);
    }

    // ------------------
    // Core functions
    // ------------------

    @Override
    public void visit(sparrow.Program n) {
        List<sparrowv.FunctionDecl> functions = new ArrayList<>();

        for (sparrow.FunctionDecl fd : n.funDecls) {
            sparrowv.FunctionDecl vFunc = visitFunction(fd);
            functions.add(vFunc);
        }

        program = new sparrowv.Program(functions);
    }

    private sparrowv.FunctionDecl visitFunction(sparrow.FunctionDecl n) {
        currentFunction = n.functionName.toString();
        currentInstructions = new ArrayList<>();

        // Handle parameters not allocated to a2-a7
        List<Identifier> params = new ArrayList<>();
        for (int i = 0; i < n.formalParameters.size(); i++) {
            if (i >= 6) {
                params.add(n.formalParameters.get(i));
            }
        }

        // Add callee-saved register saves
        addCalleeSaveInstructions();

        // Visit all instructions in block
        for (sparrow.Instruction instr : n.block.instructions) {
            instr.accept(this);
        }

        // Add callee-saved register restores
        addCalleeRestoreInstructions();

        // Handle return
        String returnVar = n.block.return_id.toString();
        String returnReg = handleReturnVariable(returnVar);

        sparrowv.Block block = new sparrowv.Block(currentInstructions, new Identifier(returnReg));
        
        return new sparrowv.FunctionDecl(new FunctionName(currentFunction), params, block);
    }
    
    @Override
    public void visit(sparrow.LabelInstr n) {
        currentInstructions.add(new sparrowv.LabelInstr(n.label));
    }

    @Override
    public void visit(sparrow.Move_Id_Integer n) {
        String lhs = n.lhs.toString();

        String lhsReg = isSpilled(lhs) ? "s9" : getRegisterOrSpill(lhs);

        currentInstructions.add(new sparrowv.Move_Reg_Integer(new Register(lhsReg), n.rhs));

        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    @Override
    public void visit(sparrow.Move_Id_FuncName n) {
        String lhs = n.lhs.toString();

        String lhsReg = isSpilled(lhs) ? "s9" : getRegisterOrSpill(lhs);

        currentInstructions.add(new sparrowv.Move_Reg_FuncName(new Register(lhsReg), n.rhs));

        if (isSpilled(lhs)) {
            currentInstructions.add(new sparrowv.Move_Id_Reg(new Identifier(lhs), new Register(lhsReg)));
        }
    }

    @Override
    public void visit(sparrow.Add n) {
        String lhsVar = n.lhs.toString();
        String arg1Var = n.arg1.toString();
        String arg2Var = n.arg2.toString();

        String arg1Reg = getRegisterOrSpill(arg1Var);
        String arg2Reg = getRegisterOrSpill(arg2Var);

        if (isSpilled(arg1Var)) {
            arg1Reg = "s9";
            currentInstructions.add(new Move_Reg_Id(new Register(arg1Reg), new Identifier(arg1Var)));
        }
        
        if (isSpilled(arg2Var)) {
            arg2Reg = "s10";
            currentInstructions.add(new Move_Reg_Id(new Register(arg2Reg), new Identifier(arg2Var)));
        }

        String lhsReg = isSpilled(lhsVar) ? "s11" : getRegisterOrSpill(lhsVar);

        currentInstructions.add(new sparrowv.Add(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        if (isSpilled(lhsVar)) {
            currentInstructions.add(new Move_Id_Reg(new Identifier(lhsVar), new Register(lhsReg)));
        }
    }

    @Override
    public void visit(sparrow.Subtract n) {
        String lhsVar = n.lhs.toString();
        String arg1Var = n.arg1.toString();
        String arg2Var = n.arg2.toString();

        String arg1Reg = getRegisterOrSpill(arg1Var);
        String arg2Reg = getRegisterOrSpill(arg2Var);

        if (isSpilled(arg1Var)) {
            arg1Reg = "s9";
            currentInstructions.add(new Move_Reg_Id(new Register(arg1Reg), new Identifier(arg1Var)));
        }
        
        if (isSpilled(arg2Var)) {
            arg2Reg = "s10";
            currentInstructions.add(new Move_Reg_Id(new Register(arg2Reg), new Identifier(arg2Var)));
        }

        String lhsReg = isSpilled(lhsVar) ? "s11" : getRegisterOrSpill(lhsVar);

        currentInstructions.add(new sparrowv.Subtract(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        if (isSpilled(lhsVar)) {
            currentInstructions.add(new Move_Id_Reg(new Identifier(lhsVar), new Register(lhsReg)));
        }
    }

    @Override
    public void visit(sparrow.Multiply n) {
        String lhsVar = n.lhs.toString();
        String arg1Var = n.arg1.toString();
        String arg2Var = n.arg2.toString();

        String arg1Reg = getRegisterOrSpill(arg1Var);
        String arg2Reg = getRegisterOrSpill(arg2Var);

        if (isSpilled(arg1Var)) {
            arg1Reg = "s9";
            currentInstructions.add(new Move_Reg_Id(new Register(arg1Reg), new Identifier(arg1Var)));
        }
        
        if (isSpilled(arg2Var)) {
            arg2Reg = "s10";
            currentInstructions.add(new Move_Reg_Id(new Register(arg2Reg), new Identifier(arg2Var)));
        }

        String lhsReg = isSpilled(lhsVar) ? "s11" : getRegisterOrSpill(lhsVar);

        currentInstructions.add(new sparrowv.Multiply(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        if (isSpilled(lhsVar)) {
            currentInstructions.add(new Move_Id_Reg(new Identifier(lhsVar), new Register(lhsReg)));
        }
    }

    @Override
    public void visit(sparrow.LessThan n) {
        String lhsVar = n.lhs.toString();
        String arg1Var = n.arg1.toString();
        String arg2Var = n.arg2.toString();

        String arg1Reg = getRegisterOrSpill(arg1Var);
        String arg2Reg = getRegisterOrSpill(arg2Var);

        if (isSpilled(arg1Var)) {
            arg1Reg = "s9";
            currentInstructions.add(new Move_Reg_Id(new Register(arg1Reg), new Identifier(arg1Var)));
        }
        
        if (isSpilled(arg2Var)) {
            arg2Reg = "s10";
            currentInstructions.add(new Move_Reg_Id(new Register(arg2Reg), new Identifier(arg2Var)));
        }

        String lhsReg = isSpilled(lhsVar) ? "s11" : getRegisterOrSpill(lhsVar);

        currentInstructions.add(new sparrowv.LessThan(new Register(lhsReg), new Register(arg1Reg), new Register(arg2Reg)));

        if (isSpilled(lhsVar)) {
            currentInstructions.add(new Move_Id_Reg(new Identifier(lhsVar), new Register(lhsReg)));
        }
    }

    @Override
    public void visit(sparrow.Load n) {
        String lhsVar = n.lhs.toString();
        String baseVar = n.base.toString();

        String baseReg = getRegisterOrSpill(baseVar);

        if (isSpilled(baseVar)) {
            baseReg = "s9";
            currentInstructions.add(new Move_Reg_Id(new Register(baseReg), new Identifier(baseVar)));
        }

        String lhsReg = isSpilled(lhsVar) ? "s10" : getRegisterOrSpill(lhsVar);

        currentInstructions.add(new sparrowv.Load(new Register(lhsReg), new Register(baseReg), n.offset));

        if (isSpilled(lhsVar)) {
            currentInstructions.add(new Move_Id_Reg(new Identifier(lhsVar), new Register(lhsReg)));
        }
    }

    @Override
    public void visit(sparrow.Store n) {
        String rhsVar = n.rhs.toString();
        String baseVar = n.base.toString();

        String rhsReg = getRegisterOrSpill(rhsVar);

        if (isSpilled(rhsReg)) {
            rhsReg = "s9";
            currentInstructions.add(new Move_Reg_Id(new Register(rhsReg), new Identifier(rhsVar)));
        }

        String baseReg = isSpilled(baseVar) ? "s10" : getRegisterOrSpill(baseVar);

        currentInstructions.add(new sparrowv.Store(new Register(baseReg), n.offset, new Register(rhsReg)));

        if (isSpilled(baseVar)) {
            currentInstructions.add(new Move_Id_Reg(new Identifier(baseVar), new Register(baseReg)));
        }
    }

    @Override
    public void visit(sparrow.Move_Id_Id n) {
        String lhsVar = n.lhs.toString();
        String rhsVar = n.rhs.toString();

        String lhsReg = getRegisterOrSpill(lhsVar);
        String rhsReg = getRegisterOrSpill(rhsVar);

        if (isSpilled(rhsReg)) {
            rhsReg = "s9";
            currentInstructions.add(new Move_Reg_Id(new Register(rhsReg), new Identifier(rhsVar)));
        }

        if (isSpilled(lhsReg)) {
            currentInstructions.add(new Move_Id_Reg(new Identifier(lhsVar), new Register(rhsReg)));
        } else {
            currentInstructions.add(new Move_Reg_Reg(new Register(lhsReg), new Register(rhsReg)));
        }
    }

    @Override
    public void visit(sparrow.Alloc n) {
        String lhsVar = n.lhs.toString();
        String rhsVar = n.size.toString();

        String rhsReg = getRegisterOrSpill(rhsVar);

        if (isSpilled(rhsVar)) {
            rhsReg = "s9";
            currentInstructions.add(new Move_Reg_Id(new Register(rhsReg), new Identifier(rhsVar)));
        }

        String lhsReg = isSpilled(lhsVar) ? "s10" : getRegisterOrSpill(lhsVar);

        currentInstructions.add(new sparrowv.Alloc(new Register(lhsReg), new Register(rhsReg)));

        if (isSpilled(lhsVar)) {
            currentInstructions.add(new Move_Id_Reg(new Identifier(lhsVar), new Register(lhsReg)));
        }
    }

    @Override
    public void visit(sparrow.Print n) {
        String id = n.content.toString();

        String idReg = isSpilled(id) ? "s9" : getRegisterOrSpill(id);

        currentInstructions.add(new sparrowv.Print(new Register(idReg)));
    }

    @Override
    public void visit(sparrow.ErrorMessage n) {
        currentInstructions.add(new sparrowv.ErrorMessage(n.msg));
    }

    @Override
    public void visit(sparrow.Goto n) {
        currentInstructions.add(new sparrowv.LabelInstr(n.label));
    }

    @Override
    public void visit(sparrow.IfGoto n) {
        String condVar = n.condition.toString();
        
        String condReg = isSpilled(condVar) ? "s9" : getRegisterOrSpill(condVar);

        currentInstructions.add(new sparrowv.IfGoto(new Register(condReg), n.label));
    }

    @Override
    public void visit(sparrow.Call n) {
        // TODO
    }
}
