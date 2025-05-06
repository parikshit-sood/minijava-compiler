import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import IR.token.Identifier;
import minijava.syntaxtree.AndExpression;
import minijava.syntaxtree.ArrayAllocationExpression;
import minijava.syntaxtree.ArrayLength;
import minijava.syntaxtree.ArrayLookup;
import minijava.syntaxtree.AssignmentStatement;
import minijava.syntaxtree.CompareExpression;
import minijava.syntaxtree.Expression;
import minijava.syntaxtree.FalseLiteral;
import minijava.syntaxtree.IfStatement;
import minijava.syntaxtree.IntegerLiteral;
import minijava.syntaxtree.MinusExpression;
import minijava.syntaxtree.NodeChoice;
import minijava.syntaxtree.NodeToken;
import minijava.syntaxtree.NotExpression;
import minijava.syntaxtree.PlusExpression;
import minijava.syntaxtree.PrimaryExpression;
import minijava.syntaxtree.PrintStatement;
import minijava.syntaxtree.Statement;
import minijava.syntaxtree.ThisExpression;
import minijava.syntaxtree.TimesExpression;
import minijava.syntaxtree.TrueLiteral;
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
        generator = new SparrowGenerator();
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
        // Create an identifier (x) and expression (42)
        minijava.syntaxtree.Identifier id = new minijava.syntaxtree.Identifier(new NodeToken("x"));
        Expression expr = new Expression(
            new NodeChoice(
                new PrimaryExpression(
                    new NodeChoice(
                        new IntegerLiteral(new NodeToken("42"))
                    )
                )
            )
        );
        
        // Create assignment statement: x = 42;
        AssignmentStatement assignStmt = new AssignmentStatement(id, expr);
        
        System.out.println("\n\nTEST: AssignmentStatement");
        System.out.println("--------------------------------");
        
        // Visit the assignment statement
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
                new minijava.syntaxtree.Identifier(new NodeToken("arr"))
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
    public void testArrayLookup() {
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

        // Create invalid index: -1
        PrimaryExpression invalidIndex = new PrimaryExpression(
            new NodeChoice(
                new IntegerLiteral(new NodeToken("-1"))
            )
        );

        // Create ArrayLookup for valid index: arr[2]
        ArrayLookup validLookup = new ArrayLookup(arrayRef, validIndex);

        // Create ArrayLookup for invalid index: arr[-1]
        ArrayLookup invalidLookup = new ArrayLookup(arrayRef, invalidIndex);

        // Visit valid lookup
        validLookup.accept(generator);
        ArrayList<Instruction> validInstructions = new ArrayList<>(generator.getCurrentInstructions());
        generator.getCurrentInstructions().clear(); // Clear instructions for next test

        // Visit invalid lookup
        invalidLookup.accept(generator);
        ArrayList<Instruction> invalidInstructions = new ArrayList<>(generator.getCurrentInstructions());

        // Print valid instructions
        System.out.println("Valid ArrayLookup Instructions:");
        print(validInstructions);

        // Print invalid instructions
        System.out.println("\nInvalid ArrayLookup Instructions:");
        print(invalidInstructions);
    }

}