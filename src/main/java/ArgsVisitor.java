
import java.util.HashMap;
import java.util.Map;

import IR.syntaxtree.FunctionDeclaration;
import IR.syntaxtree.Identifier;
import IR.visitor.GJVoidDepthFirst;

public class ArgsVisitor extends GJVoidDepthFirst<FunctionStruct>{
    Map<String, Map<String, String>> argRegs;

    public ArgsVisitor() {
        argRegs = new HashMap<>();
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
    public void visit(FunctionDeclaration n, FunctionStruct f) {
        f.name = n.f1.f0.toString();
        
        argRegs.put(f.name, new HashMap<>());
        int regNum = 2;

        if (n.f3.present()) {
            for (int i = 0; i < n.f3.size(); i++) {
                String paramName = ((Identifier) n.f3.elementAt(i)).f0.toString();
                
                if (regNum <= 7)
                    argRegs.get(f.name).put(paramName, "a" + regNum);

                regNum++;
            }
        }
    }
}
