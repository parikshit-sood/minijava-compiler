
import java.util.HashMap;
import java.util.Map;

import IR.syntaxtree.FunctionDeclaration;
import IR.syntaxtree.Identifier;
import IR.visitor.DepthFirstVisitor;

public class ArgsVisitor extends DepthFirstVisitor {
    public Map<String, VarAlloc> funcVarAllocs = new HashMap<>();       // function name -> variable allocations in function

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
        // Get function name
        String funcName = n.f1.f0.toString();

        VarAlloc varAlloc = new VarAlloc();
        varAlloc.varRegMap = new HashMap<>();
        varAlloc.varOffsetMap = new HashMap<>();

        // Use registers a2-a7 for allocating arguments
        int regNum = 2;

        if (n.f3.present()) {
            for (int i = 0; i < n.f3.size(); i++) {
                Identifier param = (Identifier) n.f3.elementAt(i);
                String paramName = param.f0.toString();
                if (regNum <= 7) {
                    varAlloc.varRegMap.put(paramName, "a" + regNum);
                }
                regNum++;
            }
        }

        // Store argument allocations for this function
        funcVarAllocs.put(funcName, varAlloc);
    }
}
