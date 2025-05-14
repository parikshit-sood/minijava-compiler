
import minijava.MiniJavaParser;
import minijava.ParseException;
import minijava.syntaxtree.Goal;

public class J2S {
    public static void main(String[] args) {
        try {

            Goal root = new MiniJavaParser(System.in).Goal();

            // Build fields table + virtual method table
            TableBuilder tb = new TableBuilder();
            root.accept(tb);

            // Generate + output Sparrow code
            SparrowGenerator codegen = new SparrowGenerator(tb.getLayouts());
            root.accept(codegen);
            System.out.println(codegen.getGeneratedCode());

        } catch (ParseException e) {

            System.out.println("Parse exception: " + e.toString());
            System.exit(1);

        }
    }
}
