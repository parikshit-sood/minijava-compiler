
import minijava.syntaxtree.AllocationExpression;
import minijava.syntaxtree.AndExpression;
import minijava.syntaxtree.ArrayAllocationExpression;
import minijava.syntaxtree.ArrayLength;
import minijava.syntaxtree.ArrayLookup;
import minijava.syntaxtree.BracketExpression;
import minijava.syntaxtree.CompareExpression;
import minijava.syntaxtree.Expression;
import minijava.syntaxtree.FalseLiteral;
import minijava.syntaxtree.IntegerLiteral;
import minijava.syntaxtree.MinusExpression;
import minijava.syntaxtree.NotExpression;
import minijava.syntaxtree.PlusExpression;
import minijava.syntaxtree.PrimaryExpression;
import minijava.syntaxtree.ThisExpression;
import minijava.syntaxtree.TimesExpression;
import minijava.syntaxtree.TrueLiteral;
import minijava.visitor.GJDepthFirst;

public class Visitor extends GJDepthFirst<MJType, SymbolTable> {
    private ClassInfo currentClass;
    private MethodInfo currentMethod;

    /**
     * Expression visitors
     */
    @Override
    public MJType visit(Expression n, SymbolTable st) {
        return n.f0.accept(this, st);
    }

    @Override
    public MJType visit(AndExpression n, SymbolTable st) {
        MJType t0 = n.f0.accept(this, st);
        MJType t2 = n.f2.accept(this, st);

        if (!(t0.booleanType() && t2.booleanType())) {
            throw new TypeException("Boolean AND operation requires two boolean operands");
        }

        return new MJType("boolean");
    }

    @Override
    public MJType visit(CompareExpression n, SymbolTable st) {
        MJType t0 = n.f0.accept(this, st);
        MJType t2 = n.f2.accept(this, st);

        if (!(t0.intType() && t2.intType())) {
            throw new TypeException("Comparison operator requires two integer operands");
        }

        return new MJType("boolean");
    }
    
    @Override
    public MJType visit(PlusExpression n, SymbolTable st) {
        MJType t0 = n.f0.accept(this, st);
        MJType t2 = n.f2.accept(this, st);

        if (!(t0.intType() && t2.intType())) {
            throw new TypeException("Addition requires two int operands");
        }

        return new MJType("int");
    }

    @Override
    public MJType visit(MinusExpression n, SymbolTable st) {
        MJType t0 = n.f0.accept(this, st);
        MJType t2 = n.f2.accept(this, st);

        if (!(t0.intType() && t2.intType())) {
            throw new TypeException("Subtraction requires two int operands");
        }

        return new MJType("int");
    }

    @Override
    public MJType visit(TimesExpression n, SymbolTable st) {
        MJType t0 = n.f0.accept(this, st);
        MJType t2 = n.f2.accept(this, st);

        if (!(t0.intType() && t2.intType())) {
            throw new TypeException("Multiplication requires two int operands");
        }

        return new MJType("int");
    }

    @Override
    public MJType visit(ArrayLookup n, SymbolTable st) {
        MJType t0 = n.f0.accept(this, st);
        MJType t2 = n.f2.accept(this, st);

        if (!(t0.arrType() && t2.intType())) {
            throw new TypeException("Array lookup index requires int array and int index");
        }

        return new MJType("int");
    }

    @Override
    public MJType visit(ArrayLength n, SymbolTable st) {
        MJType t0 = n.f0.accept(this, st);

        if (!(t0.arrType())) {
            throw new TypeException("Length operation only defined for int arrays");
        }

        return new MJType("int");
    }

    // TODO: MessageSend visit()
    // TODO: ExpressionList visit()
    // TODO: ExpressionRest visit()

    @Override
    public MJType visit(PrimaryExpression n, SymbolTable st) {
        return n.f0.accept(this, st);
    }

    @Override
    public MJType visit(IntegerLiteral n, SymbolTable st) {
        return new MJType("int");
    }

    @Override
    public MJType visit(TrueLiteral n, SymbolTable st) {
        return new MJType("boolean");
    }

    @Override
    public MJType visit(FalseLiteral n, SymbolTable st) {
        return new MJType("boolean");
    }

    // TODO: Identifier visit()
    
    @Override
    public MJType visit(ThisExpression n, SymbolTable st) {
        return new MJType(currentClass.getName(), true);
    }

    @Override
    public MJType visit(ArrayAllocationExpression n, SymbolTable st) {
        MJType t3 = n.f3.accept(this, st);

        if (!(t3.intType())) {
            throw new TypeException("Array allocation size must be int");
        }

        return new MJType("arr");
    }

    @Override
    public MJType visit(AllocationExpression n, SymbolTable st) {
        return n.f1.accept(this, st);
    }

    @Override
    public MJType visit(NotExpression n, SymbolTable st) {
        MJType t1 = n.f1.accept(this, st);

        if (!(t1.booleanType())) {
            throw new TypeException("Boolean NOT expression requires boolean operand");
        }

        return new MJType("boolean");
    }

    @Override
    public MJType visit(BracketExpression n, SymbolTable st) {
        return n.f1.accept(this, st);
    }
}
