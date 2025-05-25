import IR.SparrowParser;
import IR.syntaxtree.Node;
import IR.visitor.SparrowConstructor;

public class S2SV {
    public static void main(String[] args) throws Exception {
        new SparrowParser(System.in);
        Node root = SparrowParser.Program();

        // Process arguments into "a" registers
        ArgsProcessor ap = new ArgsProcessor();
        root.accept(ap);

        // Calculate intervals and perform liveness analysis to prepare for register allocation
        IntervalVisitor iv = new IntervalVisitor(ap.args);
        FunctionStruct fs = new FunctionStruct();
        root.accept(iv, fs);
    }
}
