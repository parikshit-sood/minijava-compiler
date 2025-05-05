import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import minijava.syntaxtree.Expression;
import minijava.syntaxtree.FalseLiteral;
import minijava.syntaxtree.NodeChoice;
import minijava.syntaxtree.NodeToken;
import minijava.syntaxtree.NotExpression;
import minijava.syntaxtree.PrimaryExpression;
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
        
        // Visit the NOT expression
        notExpr.accept(generator);
        
        // Get generated instructions
        ArrayList<Instruction> instructions = generator.getCurrentInstructions();
        
        // Print generated Sparrow instructions
        System.out.println("\n\nTEST: NotExpression");
        System.out.println("--------------------------------");
        for (Instruction instr: instructions) {
            System.out.println(instr.toString());
        }
    }
}