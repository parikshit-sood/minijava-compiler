
import java.io.InputStream;

import minijava.MiniJavaParser;
import minijava.ParseException;
import minijava.syntaxtree.Goal;


public class Typecheck {
    public static void main(String[] args) {
        try {

                InputStream in = System.in;
                Goal root = new MiniJavaParser(in).Goal();

                TableBuilder tb = new TableBuilder();
                root.accept(tb, null);

                InheritanceResolver ir = new InheritanceResolver(tb.getClassTable());
                root.accept(ir, null);

                SymbolTable st = new SymbolTable(tb.getClassTable());
                TypecheckVisitor v = new TypecheckVisitor();
                root.accept(v, st);

            } catch (ParseException e) {

                System.out.println("Parse exception: " + e.toString());
                System.exit(1);

            } catch (TypeException e) {

                System.out.println("Type error");
                System.exit(0);
                
            }

            System.out.println("Program type checked successfully");
    }
}
