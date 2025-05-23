import IR.SparrowParser;
import IR.syntaxtree.Node;
import IR.visitor.SparrowConstructor;

public class S2SV {
    public static void main(String[] args) throws Exception {
        new SparrowParser(System.in);

        Node root = SparrowParser.Program();
        SparrowConstructor constructor = new SparrowConstructor();
        root.accept(constructor);
        sparrow.Program program = constructor.getProgram();
        System.err.println(program.toString());
    }
}
