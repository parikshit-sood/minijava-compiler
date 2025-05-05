import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import IR.token.Identifier;
import minijava.syntaxtree.Expression;
import minijava.syntaxtree.FalseLiteral;
import minijava.syntaxtree.NodeChoice;
import minijava.syntaxtree.NodeToken;
import minijava.syntaxtree.NotExpression;
import minijava.syntaxtree.PrimaryExpression;
import minijava.syntaxtree.ThisExpression;
import sparrow.Instruction;

public class SparrowGeneratorTest {
    private SparrowGenerator generator;

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
                            new FalseLiteral(new NodeToken("false")), 
                            0
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
        for (Instruction instr: instructions) {
            System.out.println(instr.toString());
        }
        System.out.println("\n...Passed");
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

        System.out.println("\n...Passed");
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

        System.out.println("\n...Passed");
    }
}