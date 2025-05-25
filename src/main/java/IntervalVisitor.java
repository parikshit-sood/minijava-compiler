import java.util.HashMap;
import java.util.Map;

import IR.syntaxtree.*;
import IR.visitor.GJVoidDepthFirst;

public class IntervalVisitor extends GJVoidDepthFirst<FunctionStruct>{
    Map<String, Map<String, Integer>> defs;   // function name -> variable name -> first def line number
    Map<String, Map<String, Integer>> uses;   // function name -> variable name -> last use line number
    int lineNum;

    public IntervalVisitor() {
        defs = new HashMap<>();
        uses = new HashMap<>();
        lineNum = 1;
    }

    // ---------------------
    // Helper functions
    // ---------------------

    /**
     * Check liveness information for variable in all registers, update as needed
     * @param id : Name of variable
     * @param funcName : Name of function
     * @param lineNum : Line number of variable definition or usage
     */
    private void upsertId(String id, String funcName, int lineNum) {
        // U`pdate the def and use maps for id
        upsertDef(id, funcName, lineNum);
        upsertUse(id, funcName, lineNum);
    }

    /** Update the earliest line number for variable definition in the function
     *
     * @param id : Name of variable
     * @param funcName : Name of function
     * @param lineNum : Line number of variable definition
     */
    private void upsertDef(String id, String funcName, int lineNum) {
        // Check if first instance of this function
        if (!defs.containsKey(funcName)) {
            defs.put(funcName, new HashMap<>());
        }

        // Upsert the earliest line number for this id definition (lhs)
        if (defs.get(funcName).containsKey(id)) {
            int earliest = Math.min(lineNum, defs.get(funcName).get(id));
            defs.get(funcName).put(id, earliest);
        } else {
            defs.get(funcName).put(id, lineNum);
        }
    }

    /** Update the latest line number for variable usage in the function
     *
     * @param id : Name of variable
     * @param funcName : Name of function
     * @param lineNum : Line number of variable use
     */
    private void upsertUse(String id, String funcName, int lineNum) {
        // Check if first instance of this function
        if (!uses.containsKey(funcName)) {
            uses.put(funcName, new HashMap<>());
        }

        // Upsert the latest line number where this id is used (rhs)
        if (uses.get(funcName).containsKey(id)) {
            int latest = Math.max(lineNum, uses.get(funcName).get(id));
            uses.get(funcName).put(id, latest);
        } else {
            uses.get(funcName).put(id, lineNum);
        }
    }

    // ---------------------
    // Core functions
    // ---------------------

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
        // TODO
    }

    /**
     * f0 -> LabelWithColon()
     *       | SetInteger()
     *       | SetFuncName()
     *       | Add()
     *       | Subtract()
     *       | Multiply()
     *       | LessThan()
     *       | Load()
     *       | Store()
     *       | Move()
     *       | Alloc()
     *       | Print()
     *       | ErrorMessage()
     *       | Goto()
     *       | IfGoto()
     *       | Call()
     */
    @Override
    public void visit(Instruction n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> IntegerLiteral()
     */
    @Override
    public void visit(SetInteger n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "@"
     * f3 -> FunctionName()
     */
    @Override
    public void visit(SetFuncName n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "+"
     * f4 -> Identifier()
     */
    @Override
    public void visit(Add n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "-"
     * f4 -> Identifier()
     */
    @Override
    public void visit(Subtract n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "*"
     * f4 -> Identifier()
     */
    @Override
    public void visit(Multiply n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "<"
     * f4 -> Identifier()
     */
    @Override
    public void visit(LessThan n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "["
     * f3 -> Identifier()
     * f4 -> "+"
     * f5 -> IntegerLiteral()
     * f6 -> "]"
     */
    @Override
    public void visit(Load n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> "["
     * f1 -> Identifier()
     * f2 -> "+"
     * f3 -> IntegerLiteral()
     * f4 -> "]"
     * f5 -> "="
     * f6 -> Identifier()
     */
    @Override
    public void visit(Store n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     */
    @Override
    public void visit(Move n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "alloc"
     * f3 -> "("
     * f4 -> Identifier()
     * f5 -> ")"
     */
    @Override
    public void visit(Alloc n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> "print"
     * f1 -> "("
     * f2 -> Identifier()
     * f3 -> ")"
     */
    @Override
    public void visit(Print n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> "goto"
     * f1 -> Label()
     */
    @Override
    public void visit(Goto n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> "if0"
     * f1 -> Identifier()
     * f2 -> "goto"
     * f3 -> Label()
     */
    @Override
    public void visit(IfGoto n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "call"
     * f3 -> Identifier()
     * f4 -> "("
     * f5 -> ( Identifier() )*
     * f6 -> ")"
     */
    @Override
    public void visit(Call n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    @Override
    public void visit(Label n, FunctionStruct f) {
        // TODO
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    @Override
    public void visit(Identifier n, FunctionStruct f) {
        // TODO
    }
}
