
import minijava.syntaxtree.AllocationExpression;
import minijava.syntaxtree.AndExpression;
import minijava.syntaxtree.ArrayAllocationExpression;
import minijava.syntaxtree.ArrayAssignmentStatement;
import minijava.syntaxtree.ArrayLength;
import minijava.syntaxtree.ArrayLookup;
import minijava.syntaxtree.AssignmentStatement;
import minijava.syntaxtree.BracketExpression;
import minijava.syntaxtree.CompareExpression;
import minijava.syntaxtree.Expression;
import minijava.syntaxtree.FalseLiteral;
import minijava.syntaxtree.Identifier;
import minijava.syntaxtree.IfStatement;
import minijava.syntaxtree.IntegerLiteral;
import minijava.syntaxtree.MinusExpression;
import minijava.syntaxtree.NotExpression;
import minijava.syntaxtree.PlusExpression;
import minijava.syntaxtree.PrimaryExpression;
import minijava.syntaxtree.PrintStatement;
import minijava.syntaxtree.Statement;
import minijava.syntaxtree.ThisExpression;
import minijava.syntaxtree.TimesExpression;
import minijava.syntaxtree.TrueLiteral;
import minijava.syntaxtree.WhileStatement;
import minijava.visitor.GJDepthFirst;

public class Visitor extends GJDepthFirst<MJType, SymbolTable> {
    private ClassInfo currentClass;
    private MethodInfo currentMethod;

    /**
     * Statement visitors
     */
    @Override
    public MJType visit(Statement n, SymbolTable st) {
        n.f0.accept(this, st);

        return new MJType("void");
    }

    @Override
    public MJType visit(AssignmentStatement n, SymbolTable st) {
        MJType t0 = n.f0.accept(this, st);
        MJType t2 = n.f2.accept(this, st);

        if (t0 == null || t2 == null || !(t0.getType().equals(t2.getType()) || st.isSubtype(t0, t2))) {
            throw new TypeException("Type mismatch in assignment statement");
        } 
        
        return new MJType("void");
    }

    @Override
    public MJType visit(ArrayAssignmentStatement n, SymbolTable st) {
        MJType t0 = n.f0.accept(this, st);
        MJType t2 = n.f2.accept(this, st);
        MJType t5 = n.f5.accept(this, st);

        if (!(t0.arrType() && t2.intType() && t5.intType())) {
            throw new TypeException("Type mismatch in array assignment statement");
        }

        return new MJType("void");
    }

    @Override
    public MJType visit(IfStatement n, SymbolTable st) {
        MJType t2 = n.f2.accept(this, st);

        if (!(t2.booleanType())) {
            throw new TypeException("If statement condition must be boolean");
        }

        n.f4.accept(this, st);
        n.f6.accept(this, st);

        return new MJType("void");
    }

    @Override
    public MJType visit(WhileStatement n, SymbolTable st) {
        MJType t2 = n.f2.accept(this, st);
        
        if (!(t2.booleanType())) {
            throw new TypeException("While loop condition must be boolean");
        }

        n.f4.accept(this, st);

        return new MJType("void");
    }

    @Override
    public MJType visit(PrintStatement n, SymbolTable st) {
        MJType t2 = n.f2.accept(this, st);

        if (!(t2.intType())) {
            throw new TypeException("System.out.println can only print integers");
        }

        return new MJType("void");
    }

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

    @Override
    public MJType visit(Identifier n, SymbolTable st) {
        MJType type = null;
        String id = n.f0.toString();

        if (st.hasClass(id)) {
            // class instance
            type = new MJType(id, true);
            return type;
        }

        if (currentClass != null) {
            // field variable
            type = currentClass.getFields().get(id);
            if (type != null) {
                return type;
            }
        }

        if (currentMethod != null) {
            // local variable
            type = currentMethod.getLocalVariables().get(id);
            if (type != null) {
                return type;
            }
        }

        throw new TypeException("Identifier " + id + " does not exist in the current scope");
    }

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
