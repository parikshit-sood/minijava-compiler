
import java.util.HashMap;
import java.util.Map;

import IR.token.Identifier;
import sparrowv.Call;
import sparrowv.FunctionDecl;
import sparrowv.Move_Id_Reg;
import sparrowv.Move_Reg_Id;
import sparrowv.visitor.DepthFirst;

public class OffsetVisitor extends DepthFirst{
    Map<String, Map<String, Integer>> funcVarOffsets;       // function name -> identifier -> stack offset
    Map<String, Integer> funcFrameSizes;
    String currentFunction;
    int varOffset;

    public OffsetVisitor() {
        funcVarOffsets = new HashMap<>();
        funcFrameSizes = new HashMap<>();
    }

    /*   Program parent;
    *   FunctionName functionName;
    *   List<Identifier> formalParameters;
    *   Block block; */
    @Override
    public void visit(FunctionDecl n) {
        
        // Process function name
        currentFunction = n.functionName.toString();
        HashMap<String, Integer> varOffsets = new HashMap<>();

        // Local variables start at offset -12
        int offset = 0;
        for (Identifier fp: n.formalParameters) {
            varOffsets.put(fp.toString(), offset);
            offset += 4;
        }

        // Store local variable offsets for current function
        funcVarOffsets.put(currentFunction, varOffsets);

        // Visit instructions, searching for local parameters
        varOffset = -12;
        n.block.accept(this);

        // Store return value
        String returnId = n.block.return_id.toString();
        funcVarOffsets.get(currentFunction).put(returnId, varOffset);

        // Store stack frame size for this function
        // Add one more slot for return value
        varOffset -= 4;
        funcFrameSizes.put(currentFunction, -1 * varOffset);
    }

    /*   Identifier lhs;
    *   Register rhs; */
    @Override
    public void visit(Move_Id_Reg n) {
        String varName = n.lhs.toString();

        Map<String, Integer> varOffsets = funcVarOffsets.get(currentFunction);
        
        // If this is first instance of a variable, give it a stack offset
        if (!varOffsets.containsKey(varName)) {
            varOffsets.put(varName, varOffset);
            varOffset -= 4;
        }
    }

    /*   Register lhs;
    *   Identifier rhs; */
    @Override
    public void visit(Move_Reg_Id n) {
        String varName = n.rhs.toString();

        Map<String, Integer> varOffsets = funcVarOffsets.get(currentFunction);

        // If this is first instance of a variable, give it a stack offset
        if (!varOffsets.containsKey(varName)) {
            varOffsets.put(varName, varOffset);
            varOffset -= 4;
        }
    }

    /*   Register lhs;
    *   Register callee;
    *   List<Identifier> args; */
    @Override
    public void visit(Call n) {
        Map<String, Integer> varOffsets = funcVarOffsets.get(currentFunction);

        for (Identifier a : n.args) {
            varOffsets.put(a.toString(), varOffset);
            varOffset -= 4;
        }
    }
}
