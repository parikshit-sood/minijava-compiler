
import minijava.MiniJavaParser;
import minijava.ParseException;
import minijava.syntaxtree.Goal;

public class J2S {
    public static void main(String[] args) {
        try {

            Goal root = new MiniJavaParser(System.in).Goal();

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
