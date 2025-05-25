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
