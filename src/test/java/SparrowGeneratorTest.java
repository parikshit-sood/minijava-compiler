import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import IR.token.Identifier;
import minijava.syntaxtree.AllocationExpression;
import minijava.syntaxtree.AndExpression;
import minijava.syntaxtree.ArrayAllocationExpression;
import minijava.syntaxtree.ArrayAssignmentStatement;
import minijava.syntaxtree.ArrayLength;
import minijava.syntaxtree.ArrayLookup;
import minijava.syntaxtree.AssignmentStatement;
import minijava.syntaxtree.CompareExpression;
import minijava.syntaxtree.Expression;
import minijava.syntaxtree.FalseLiteral;
import minijava.syntaxtree.FormalParameter;
import minijava.syntaxtree.FormalParameterList;
import minijava.syntaxtree.FormalParameterRest;
import minijava.syntaxtree.IfStatement;
import minijava.syntaxtree.IntegerLiteral;
import minijava.syntaxtree.IntegerType;
import minijava.syntaxtree.MethodDeclaration;
import minijava.syntaxtree.MinusExpression;
import minijava.syntaxtree.NodeChoice;
import minijava.syntaxtree.NodeListOptional;
import minijava.syntaxtree.NodeOptional;
import minijava.syntaxtree.NodeToken;
import minijava.syntaxtree.NotExpression;
import minijava.syntaxtree.PlusExpression;
import minijava.syntaxtree.PrimaryExpression;
import minijava.syntaxtree.PrintStatement;
import minijava.syntaxtree.Statement;
import minijava.syntaxtree.ThisExpression;
import minijava.syntaxtree.TimesExpression;
import minijava.syntaxtree.TrueLiteral;
import minijava.syntaxtree.Type;
import minijava.syntaxtree.WhileStatement;
import sparrow.Instruction;

public class SparrowGeneratorTest {
    private SparrowGenerator generator;

    private void print(ArrayList<Instruction> instructions) {
        for (Instruction instr: instructions) {
            System.out.println(instr.toString());
        }
    }

    @Before
    public void setUp() {
        HashMap<String, ClassLayout> testLayout = new HashMap<>();
        ClassLayout a = new ClassLayout();
        a.className = "A";
        a.fields.add("x");
        a.fields.add("y");
        a.fieldOffsets.put("x", 4);
        a.fieldOffsets.put("y", 8);
        a.vmt.add("A_foo");
        a.vmt.add("A_bar");
        a.methodOffsets.put("A_foo", 0);
        a.methodOffsets.put("A_bar", 4);
        a.objSize = 12;
        testLayout.put("A", a);
        generator = new SparrowGenerator(testLayout);
        generator.currentClass = "A";
        generator.currentLayout = a;
    }

    @Test
    public void testVisitNotExpression() {
        // Create a NotExpression
        NotExpression notExpr = new NotExpression(
            new NodeToken("!"),  // f0: NOT operator token
            new Expression(      // f1: Expression being negated
                new NodeChoice(
                    new PrimaryExpression(
                        new NodeChoice(
                            new FalseLiteral(new NodeToken("false"))
                        )
                    )
                )
            )
        );

        System.out.println("\n\nTEST: NotExpression");
        System.out.println("--------------------------------");
        
        // Visit the NOT expression
        notExpr.accept(generator);
        
        // Get generated instructions
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();
        
        // Print generated Sparrow instructions
        print(instructions);
    }

    @Test
    public void testVisitThisExpression() {
        // Create a ThisExpression using the no-arg constructor
        ThisExpression thisExpr = new ThisExpression();

        System.out.println("\n\nTEST: ThisExpression");
        System.out.println("--------------------------------");
        
        // Visit the THIS expression
        thisExpr.accept(generator);
        
        // Get lastResult
        Identifier result = generator.getLastResult();
        
        // Verify that lastResult is set to "this"
        assertEquals("this", result.toString());
        
        // Verify no instructions were generated (just sets lastResult)
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();
        assertEquals(0, instructions.size());

    }

    @Test
    public void testVisitIdentifier() {

        // Testing no-conflict Identifier
        minijava.syntaxtree.Identifier idExpr = new minijava.syntaxtree.Identifier(new NodeToken("testVar"));
        idExpr.accept(generator);

        Identifier result = generator.getLastResult();

        System.out.println("\n\nTEST: Identifier");
        System.out.println("--------------------------------");

        // Verify that lastResult is set to "this"
        assertEquals(idExpr.f0.toString(), result.toString());
        
        // Verify no instructions were generated (just sets lastResult)
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();
        assertEquals(0, instructions.size());
        
        // Testing Identifier with register names (should mangle names)
        minijava.syntaxtree.Identifier idExprBad = new minijava.syntaxtree.Identifier(new NodeToken("t3"));
        idExprBad.accept(generator);
        result = generator.getLastResult(); 
        // Verify that lastResult is set to "this"
        assertEquals("var_" + idExprBad.f0.toString(), result.toString());
        // Verify no instructions were generated (just sets lastResult)
        instructions = generator.getCurrentInstructions();
        assertEquals(0, instructions.size());

        idExprBad = new minijava.syntaxtree.Identifier(new NodeToken("s10"));
        idExprBad.accept(generator);
        result = generator.getLastResult(); 
        // Verify that lastResult is set to "this"
        assertEquals("var_" + idExprBad.f0.toString(), result.toString());
        // Verify no instructions were generated (just sets lastResult)
        instructions = generator.getCurrentInstructions();
        assertEquals(0, instructions.size());
        
        idExprBad = new minijava.syntaxtree.Identifier(new NodeToken("a5"));
        idExprBad.accept(generator);
        result = generator.getLastResult(); 
        // Verify that lastResult is set to "this"
        assertEquals("var_" + idExprBad.f0.toString(), result.toString());
        // Verify no instructions were generated (just sets lastResult)
        instructions = generator.getCurrentInstructions();
        assertEquals(0, instructions.size());

    }

    @Test
    public void testVisitTimesExpression() {
        // Create operands: 5 * 3
        PrimaryExpression op1 = new PrimaryExpression(
            new NodeChoice(
                new IntegerLiteral(new NodeToken("5"))
            )
        );
        PrimaryExpression op2 = new PrimaryExpression(
            new NodeChoice(
                new IntegerLiteral(new NodeToken("3"))
            )
        );
        
        // Create TimesExpression using the two-arg constructor
        TimesExpression timesExpr = new TimesExpression(op1, op2);
        
        System.out.println("\n\nTEST: TimesExpression");
        System.out.println("--------------------------------");

        timesExpr.accept(generator);

        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        // Print generated Sparrow instructions
        print(instructions);
    }

    @Test
    public void testVisitMinusExpression() {
        // Create operands: 5 - 3
        PrimaryExpression op1 = new PrimaryExpression(
            new NodeChoice(
                new IntegerLiteral(new NodeToken("5"))
            )
        );
        PrimaryExpression op2 = new PrimaryExpression(
            new NodeChoice(
                new IntegerLiteral(new NodeToken("3"))
            )
        );
        
        // Create MinusExpression using the two-arg constructor
        MinusExpression minusExpr = new MinusExpression(op1, op2);
        
        System.out.println("\n\nTEST: MinusExpression");
        System.out.println("--------------------------------");

        minusExpr.accept(generator);

        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        // Print generated Sparrow instructions
        print(instructions);
    }

    @Test
    public void testVisitPlusExpression() {
        // Create operands: 5 + 3
        PrimaryExpression op1 = new PrimaryExpression(
            new NodeChoice(
                new IntegerLiteral(new NodeToken("5"))
            )
        );
        PrimaryExpression op2 = new PrimaryExpression(
            new NodeChoice(
                new IntegerLiteral(new NodeToken("3"))
            )
        );

        // Create MinusExpression using the two-arg constructor
        PlusExpression plusExpr = new PlusExpression(op1, op2);

        System.out.println("\n\nTEST: PlusExpression");
        System.out.println("--------------------------------");

        plusExpr.accept(generator);

        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        // Print generated Sparrow instructions
        print(instructions);
    }

    @Test
    public void testVisitCompareExpression() {
        // Create operands: 5 < 3
        PrimaryExpression op1 = new PrimaryExpression(
            new NodeChoice(
                new IntegerLiteral(new NodeToken("5"))
            )
        );
        PrimaryExpression op2 = new PrimaryExpression(
            new NodeChoice(
                new IntegerLiteral(new NodeToken("3"))
            )
        );
        
        // Create CompareExpression for 5 < 3
        CompareExpression compareExpr = new CompareExpression(
            op1,
            new NodeToken("<"),
            op2
        );
        
        System.out.println("\n\nTEST: CompareExpression");
        System.out.println("--------------------------------");
        
        // Visit the comparison expression
        compareExpr.accept(generator);
        
        // Get generated instructions
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        // Print generated Sparrow instructions
        print(instructions);
    }

    @Test
    public void testVisitAndExpression() {
        // Create operands: true and false
        PrimaryExpression op1 = new PrimaryExpression(
            new NodeChoice(
                new TrueLiteral()
            )
        );
        PrimaryExpression op2 = new PrimaryExpression(
            new NodeChoice(
                new FalseLiteral()
            )
        );
        
        // Create AndExpression using the two-arg constructor
        AndExpression andExpr = new AndExpression(op1, op2);
        
        System.out.println("\n\nTEST: AndExpression");
        System.out.println("--------------------------------");

        andExpr.accept(generator);

        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        // Print generated Sparrow instructions
        print(instructions);
    }

    @Test
    public void testVisitPrintStatement() {
        // Create an expression to print (integer literal 42)
        Expression expr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new IntegerLiteral(new NodeToken("42"))
                    )
                )
            )
        );
        
        // Create PrintStatement using single-arg constructor
        PrintStatement printStmt = new PrintStatement(expr);
        
        System.out.println("\n\nTEST: PrintStatement");
        System.out.println("--------------------------------");
        
        // Visit the print statement
        printStmt.accept(generator);
        
        // Get generated instructions
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();
        
        print(instructions);
    }

    @Test
    public void testVisitWhileStatement() {
        // Create condition: while (true)
        Expression condition = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new TrueLiteral()
                    )
                )
            )
        );
        
        // Create body: print(42)
        Expression printExpr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new IntegerLiteral(new NodeToken("42"))
                    )
                )
            )
        );
        PrintStatement body = new PrintStatement(printExpr);
        
        // Create while statement: while (true) print(42)
        WhileStatement whileStmt = new WhileStatement(condition, new Statement(new NodeChoice(body)));
        
        System.out.println("\n\nTEST: WhileStatement");
        System.out.println("--------------------------------");
        
        // Visit the while statement
        whileStmt.accept(generator);
        
        // Get generated instructions
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        print(instructions);
    }

    @Test
    public void testVisitIfStatement() {
        // Create condition: while (true)
        Expression condition = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new TrueLiteral()
                    )
                )
            )
        );
        
        // Create body: print(42)
        Expression printExpr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new IntegerLiteral(new NodeToken("42"))
                    )
                )
            )
        );
        PrintStatement body = new PrintStatement(printExpr);
        
        // Create while statement: while (true) print(42)
        IfStatement ifStmt = new IfStatement(condition, new Statement(new NodeChoice(body)), new Statement(new NodeChoice(body)));
        
        System.out.println("\n\nTEST: IfStatement");
        System.out.println("--------------------------------");
        
        // Visit the while statement
        ifStmt.accept(generator);
        
        // Get generated instructions
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        print(instructions);
    }

    @Test
    public void testVisitAssignmentStatement() {
        // Create an identifier (t2) and expression (t2 + 42)
        minijava.syntaxtree.Identifier id = new minijava.syntaxtree.Identifier(new NodeToken("t2"));
        minijava.syntaxtree.Identifier idY = new minijava.syntaxtree.Identifier(new NodeToken("y"));
        
        // Create expression (5)
        Expression five = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new IntegerLiteral(new NodeToken("5"))
                    )
                )
            )
        );

        // Create assignment statement: t2 = 5
        AssignmentStatement ass1 = new AssignmentStatement(id, five);

        PlusExpression sum = new PlusExpression(
            new PrimaryExpression(
                new NodeChoice(id)
            ),
            new PrimaryExpression(
                new NodeChoice(
                    new IntegerLiteral(new NodeToken("42"))
                )
            )
        );

        Expression expr = new Expression(
            new NodeChoice(sum)
        );
        
        // Create assignment statement: y = t2 + 42;
        AssignmentStatement assignStmt = new AssignmentStatement(idY, expr);
        
        System.out.println("\n\nTEST: AssignmentStatement");
        System.out.println("--------------------------------");
        
        // Visit the assignment statements
        ass1.accept(generator);
        assignStmt.accept(generator);
        
        // Get generated instructions
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        print(instructions);
    }

    @Test
    public void testVisitArrayAllocationExpression() {
        // Create array size expression: new int[5]
        Expression sizeExpr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new IntegerLiteral(new NodeToken("5"))
                    )
                )
            )
        );
        
        // Create array allocation expression
        ArrayAllocationExpression arrayAlloc = new ArrayAllocationExpression(
            new NodeToken("new"),
            new NodeToken("int"),
            new NodeToken("["),
            sizeExpr,
            new NodeToken("]")
        );
        
        System.out.println("\n\nTEST: ArrayAllocationExpression");
        System.out.println("--------------------------------");
        
        // Visit the array allocation
        arrayAlloc.accept(generator);
        
        // Get generated instructions
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        print(instructions);
    }

    @Test
    public void testVisitArrayLength() {
        // Create array reference: arr.length
        PrimaryExpression arrayRef = new PrimaryExpression(
            new NodeChoice(
                // Also testing name mangling in action
                new minijava.syntaxtree.Identifier(new NodeToken("t3"))
            )
        );
        
        // Create ArrayLength using single-arg constructor
        ArrayLength arrayLength = new ArrayLength(arrayRef);
        
        System.out.println("\n\nTEST: ArrayLength");
        System.out.println("--------------------------------");
        
        // Visit the array length expression
        arrayLength.accept(generator);
        
        // Get generated instructions
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        print(instructions);
    }

    @Test
    public void testErrorMessage() {
        // Create array size expression: new int[-3]
        Expression sizeExpr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new IntegerLiteral(new NodeToken("-3"))
                    )
                )
            )
        );
        
        // Create array allocation expression
        ArrayAllocationExpression arrayAlloc = new ArrayAllocationExpression(
            new NodeToken("new"),
            new NodeToken("int"),
            new NodeToken("["),
            sizeExpr,
            new NodeToken("]")
        );
        
        System.out.println("\n\nTEST: ErrorMessage for ArrayAllocation with negative size");
        System.out.println("--------------------------------");
        
        // Visit the array allocation
        arrayAlloc.accept(generator);
        
        // Get generated instructions
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        print(instructions);
    }

    @Test
    public void testVisitArrayLookupExpression() {
        System.out.println("\n\nTEST: ArrayLookup");
        System.out.println("--------------------------------");

        // Create array reference: arr
        PrimaryExpression arrayRef = new PrimaryExpression(
            new NodeChoice(
                new minijava.syntaxtree.Identifier(new NodeToken("arr"))
            )
        );

        // Create valid index: 2
        PrimaryExpression validIndex = new PrimaryExpression(
            new NodeChoice(
                new IntegerLiteral(new NodeToken("2"))
            )
        );

        // Create ArrayLookup for valid index: arr[2]
        ArrayLookup lookup = new ArrayLookup(arrayRef, validIndex);

        // Visit lookup
        lookup.accept(generator);
        ArrayList<Instruction> validInstructions = new ArrayList<>(generator.getCurrentInstructions());
        generator.getCurrentInstructions().clear(); // Clear instructions for next test

        // Print instructions
        print(validInstructions);
    }

    @Test
    public void testVisitArrayAssignmentStatement() {
        System.out.println("\n\nTEST: ArrayAssignmentStatement");
        System.out.println("--------------------------------");

        // Create array identifier: arr
        minijava.syntaxtree.Identifier arrayId = new minijava.syntaxtree.Identifier(new NodeToken("arr"));

        // Create index expression: 2
        Expression indexExpr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new IntegerLiteral(new NodeToken("2"))
                    )
                )
            )
        );

        // Create value expression: 42
        Expression valueExpr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new IntegerLiteral(new NodeToken("42"))
                    )
                )
            )
        );

        // Create array assignment statement: arr[2] = 42;
        ArrayAssignmentStatement assignStmt = new ArrayAssignmentStatement(
            arrayId,
            indexExpr,
            valueExpr
        );

        // Visit the array assignment statement
        assignStmt.accept(generator);
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();

        // Print generated instructions
        print(instructions);
    }

    @Test
    public void testVisitMethodDeclaration() {
        System.out.println("\n\nTEST: MethodDeclaration");
        System.out.println("--------------------------------");

        // Test 1: Method with no parameters
        // public int simpleMethod() { return 42; }
        
        // Create method type and name
        Type methodType = new Type(new NodeChoice(new IntegerType()));
        minijava.syntaxtree.Identifier methodName = new minijava.syntaxtree.Identifier(new NodeToken("simpleMethod"));
        
        // Create return expression (42)
        Expression returnExpr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new IntegerLiteral(new NodeToken("42"))
                    )
                )
            )
        );
        
        // Create empty lists for parameters, vars, and statements
        NodeOptional emptyParams = new NodeOptional();
        NodeListOptional emptyVarDecls = new NodeListOptional();
        NodeListOptional emptyStatements = new NodeListOptional();
        
        MethodDeclaration simpleMethod = new MethodDeclaration(
            methodType,      // return type
            methodName,      // method name
            emptyParams,     // no parameters
            emptyVarDecls,   // no variable declarations
            emptyStatements, // no statements
            returnExpr       // return 42
        );

        // Visit simple method
        generator.getCurrentInstructions().clear();
        simpleMethod.accept(generator);
        // System.out.println("\nMethod without parameters:");
        // print(generator.getCurrentInstructions());

        // Test 2: Method with parameters
        // public int complexMethod(int a, int b) { return a + b; }

        // Create method type and name
        methodType = new Type(new NodeChoice(new IntegerType()));
        methodName = new minijava.syntaxtree.Identifier(new NodeToken("complexMethod"));

        // Create parameter list
        FormalParameter param1 = new FormalParameter(
            new Type(new NodeChoice(new IntegerType())),
            new minijava.syntaxtree.Identifier(new NodeToken("a"))
        );

        FormalParameter param2 = new FormalParameter(
            new Type(new NodeChoice(new IntegerType())),
            new minijava.syntaxtree.Identifier(new NodeToken("b"))
        );

        NodeListOptional paramRests = new NodeListOptional(
            new FormalParameterRest(param2)
        );

        FormalParameterList paramList = new FormalParameterList(param1, paramRests);

        // Create return expression (a + b)
        Expression param1Expr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new minijava.syntaxtree.Identifier(new NodeToken("a"))
                    )
                )
            )
        );

        Expression param2Expr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new minijava.syntaxtree.Identifier(new NodeToken("b"))
                    )
                )
            )
        );

        PlusExpression plusExpr = new PlusExpression(
            new PrimaryExpression(new NodeChoice(param1Expr)),
            new PrimaryExpression(new NodeChoice(param2Expr))
        );

        Expression returnExpr2 = new Expression(
            new NodeChoice(plusExpr)
        );

        MethodDeclaration complexMethod = new MethodDeclaration(
            methodType,                    // return type
            methodName,                   // method name
            new NodeOptional(paramList),  // parameters
            emptyVarDecls,               // no variable declarations
            emptyStatements,             // no statements
            returnExpr2                  // return a + b
        );

        // Visit complex method
        generator.getCurrentInstructions().clear();
        complexMethod.accept(generator);
        System.out.println("\nMethod with parameters:");
        System.out.println(generator.getGeneratedCode());
    }

    @Test
    public void testVisitAllocationExpression() {
        System.out.println("\n\nTEST: AllocationExpression");
        System.out.println("--------------------------------");
        AllocationExpression allocExpr = new AllocationExpression(
            new minijava.syntaxtree.Identifier(
                new NodeToken("A")
            )
        );

        generator.visit(allocExpr);

        ArrayList<Instruction> instructions = generator.getCurrentInstructions();
        print(instructions);

    }

}