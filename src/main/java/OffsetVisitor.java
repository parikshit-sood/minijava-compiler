
import java.util.HashMap;
import java.util.Map;

import IR.token.Identifier;
import sparrowv.FunctionDecl;
import sparrowv.Move_Id_Reg;
import sparrowv.Move_Reg_Id;
import sparrowv.visitor.DepthFirst;

public class OffsetVisitor extends DepthFirst{
    Map<String, Map<String, Integer>> funcVarOffsets;       // function name -> identifier -> stack offset
    Map<String, Map<String, Integer>> funcArgOffsets;       // function name -> identifier -> stack offset
    Map<String, Integer> funcFrameSizes;
    String currentFunction;
    int varOffset;

    public OffsetVisitor() {
        funcVarOffsets = new HashMap<>();
        funcArgOffsets = new HashMap<>();
        funcFrameSizes = new HashMap<>();
    }

    private boolean isUnique(String var) {
        return !(funcArgOffsets.get(currentFunction).containsKey(var)) && !(funcVarOffsets.get(currentFunction).containsKey(var));
    }

    /*   Program parent;
    *   FunctionName functionName;
    *   List<Identifier> formalParameters;
    *   Block block; */
    @Override
    public void visit(FunctionDecl n) {
        // Process function name
        currentFunction = n.functionName.toString();
        funcArgOffsets.put(currentFunction, new HashMap<>());

        // Function arguments
        int offset = 0;
        for (Identifier fp: n.formalParameters) {
            funcArgOffsets.get(currentFunction).put(fp.toString(), offset);
            offset += 4;
        }

        // Visit instructions, searching for local parameters
        funcVarOffsets.put(currentFunction, new HashMap<>());
        varOffset = 0;
        n.block.accept(this);

        // Store stack frame size for this function
        // Add 2 slots for return address and old fp
        funcFrameSizes.put(currentFunction, (-1 * varOffset + 8));
    }

    /*   Identifier lhs;
    *   Register rhs; */
    @Override
    public void visit(Move_Id_Reg n) {
        String varName = n.lhs.toString();
        
        // If this is first instance of a variable, give it a stack offset
        if (isUnique(varName)) {
            funcVarOffsets.get(currentFunction).put(varName, varOffset);
            varOffset -= 4;
        }
    }

    /*   Register lhs;
    *   Identifier rhs; */
    @Override
    public void visit(Move_Reg_Id n) {
        String varName = n.rhs.toString();

        // If this is first instance of a variable, give it a stack offset
        if (isUnique(varName)) {
            funcVarOffsets.get(currentFunction).put(varName, varOffset);
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
