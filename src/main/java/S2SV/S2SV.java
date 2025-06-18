package S2SV;
import IR.SparrowParser;
import IR.syntaxtree.Node;
import IR.visitor.SparrowConstructor;

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
        SparrowConstructor constructor = new SparrowConstructor();
        root.accept(constructor);
        sparrow.Program prog = constructor.getProgram();
        Translator tr = new Translator(
            lv.linearRegAlloc,
            lv.aRegs,
            lv.tsIntervals,
            lv.aRanges
        );
        tr.visit(prog);

        System.out.println(tr.prog);
    }
}