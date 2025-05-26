import IR.SparrowParser;
import IR.syntaxtree.Node;

public class S2SV {
    public static void main(String[] args) throws Exception {
        new SparrowParser(System.in);
        Node root = SparrowParser.Program();

        // Process function arguments
        ArgsVisitor av = new ArgsVisitor();
        root.accept(av);

        // Liveness analysis and register allocation
    }
}
