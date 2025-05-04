
import minijava.MiniJavaParser;
import minijava.ParseException;
import minijava.syntaxtree.Goal;

public class J2S {
    public static void main(String[] args) {
        try {

            // Ensure valid MiniJava program
            Goal root = new MiniJavaParser(System.in).Goal();

            TableBuilder tb = new TableBuilder();
            root.accept(tb, null);

            InheritanceResolver ir = new InheritanceResolver(tb.getClassTable());
            root.accept(ir, null);

            // Typecheck valid MiniJava program
            SymbolTable st = new SymbolTable(tb.getClassTable());
            TypecheckVisitor v = new TypecheckVisitor();
            root.accept(v, st);

            System.out.println("Program type checked successfully");

            // Generate + output Sparrow code
            SparrowGenerator codegen = new SparrowGenerator(tb.getClassTable());
            root.accept(codegen);
            System.out.println(codegen.getGeneratedCode());

        } catch (ParseException e) {

            System.out.println("Parse exception: " + e.toString());
            System.exit(1);

        } catch (TypeException e) {

            System.out.println("Type error");
            System.exit(0);
                
        }
    }
}
