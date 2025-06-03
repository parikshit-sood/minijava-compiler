
import java.util.HashMap;
import java.util.Map;

import IR.token.Identifier;
import sparrowv.FunctionDecl;
import sparrowv.Move_Id_Reg;
import sparrowv.Move_Reg_Id;
import sparrowv.visitor.DepthFirst;

public class OffsetVisitor extends DepthFirst{
    Map<String, FunctionMetadata> fmd;
    String currentFunction;
    int varOffset;

    public OffsetVisitor() {
        fmd = new HashMap<>();
    }

    private boolean isUnique(String id) {
        FunctionMetadata currFunc = fmd.get(currentFunction);
        return !(currFunc.hasArg(id)) && !(currFunc.hasVar(id));
    }

    /*   Program parent;
    *   FunctionName functionName;
    *   List<Identifier> formalParameters;
    *   Block block; */
    @Override
    public void visit(FunctionDecl n) {
        // Initialize function metadata
        // Process function name
        currentFunction = n.functionName.toString();
        fmd.put(currentFunction, new FunctionMetadata());

        Map<String, Integer> funcArgOffsets = fmd.get(currentFunction).getArgOffsets();

        // Function arguments
        int offset = 0;
        for (Identifier fp: n.formalParameters) {
            funcArgOffsets.put(fp.toString(), offset);
            offset += 4;
        }

        // Visit instructions, searching for local parameters
        varOffset = 0;
        n.block.accept(this);

        // Store stack frame size for this function
        // Add 2 slots for return address and old fp
        fmd.get(currentFunction).setFrameSize(-1 * varOffset + 8);
    }

    /*   Identifier lhs;
    *   Register rhs; */
    @Override
    public void visit(Move_Id_Reg n) {
        String varName = n.lhs.toString();
        
        // If this is first instance of a variable, give it a stack offset
        Map<String, Integer> funcVarOffsets = fmd.get(currentFunction).getVarOffsets();
        if (isUnique(varName)) {
            funcVarOffsets.put(varName, varOffset);
            varOffset -= 4;
        }
    }

    /*   Register lhs;
    *   Identifier rhs; */
    @Override
    public void visit(Move_Reg_Id n) {
        String varName = n.rhs.toString();

        // If this is first instance of a variable, give it a stack offset
        Map<String, Integer> funcVarOffsets = fmd.get(currentFunction).getVarOffsets();
        if (isUnique(varName)) {
            funcVarOffsets.put(varName, varOffset);
            varOffset -= 4;
        }
    }

    // /*   Register lhs;
    // *   Register callee;
    // *   List<Identifier> args; */
    // @Override
    // public void visit(Call n) {
    //     Map<String, Integer> varOffsets = funcVarOffsets.get(currentFunction);

    //     for (Identifier a : n.args) {
    //         varOffsets.put(a.toString(), varOffset);
    //         varOffset -= 4;
    //     }
    // }
}
