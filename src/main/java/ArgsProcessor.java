import IR.syntaxtree.*;
import IR.visitor.DepthFirstVisitor;

import java.util.HashMap;
import java.util.Map;

public class ArgsProcessor extends DepthFirstVisitor {
    Map<String, Map<String, String>> args = new HashMap<>();

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
        String funcName = n.f1.f0.toString();

        // Set up "a" registers for current function
        args.put(funcName, new HashMap<>());

        int regNum = 2;        // Register number for "a" registers
        if (n.f3.present()) {
            while (n.f3.elements().hasMoreElements()) {
                String paramName = ((Identifier)n.f3.elements().nextElement()).f0.toString();
                String aReg = "a" + regNum;

                // Greedily add parameters into registers a2-a7
                if (regNum <= 7) {
                    args.get(funcName).put(paramName, aReg);
                }
                // Use next available "a" register
                regNum++;
            }
        }
    }
}
