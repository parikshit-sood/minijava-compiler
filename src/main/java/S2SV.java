import IR.SparrowParser;
import IR.syntaxtree.Node;

public class S2SV {
    public static void main(String[] args) throws Exception {
        new SparrowParser(System.in);
        Node root = SparrowParser.Program();

        // Allocate assignment registers
        ArgsVisitor av = new ArgsVisitor();
        root.accept(av, new FunctionStruct());

        // Fast liveness analysis and register allocation
        LivenessVisitor lv = new LivenessVisitor(av.aRegs);
        root.accept(lv, new FunctionStruct());

        // Generate SparrowV code
        Translator tr = new Translator(
            lv.linearRegAlloc,
            lv.aRegs
        );

        // System.out.println(root.accept(tr, new FunctionStruct()));
    }
}