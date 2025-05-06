
import minijava.MiniJavaParser;
import minijava.ParseException;
import minijava.syntaxtree.Goal;
import typecheck.InheritanceResolver;
import typecheck.TableBuilder;

public class J2S {
    public static void main(String[] args) {
        try {

            Goal root = new MiniJavaParser(System.in).Goal();

            // Build class table
            TableBuilder tb = new TableBuilder();
            root.accept(tb, null);

            // Resolve inheritance
            InheritanceResolver ir = new InheritanceResolver(tb.getClassTable());
            root.accept(ir, null);

            // TODO: Store methods, arrays onto heap

            // Generate + output Sparrow code
            SparrowGenerator codegen = new SparrowGenerator();
            root.accept(codegen);
            System.out.println(codegen.getGeneratedCode());

        } catch (ParseException e) {

            System.out.println("Parse exception: " + e.toString());
            System.exit(1);

        }
    }
}
