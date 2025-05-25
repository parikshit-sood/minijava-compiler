import IR.SparrowParser;
import IR.syntaxtree.Node;

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

        // Translate from Sparrow to Sparrow-V
        Translator tr = new Translator(iv.uses, iv.args, iv.regs, iv.vars, iv.liveRange, iv.argsLiveRange, iv.linearRegAlloc);
        fs = new FunctionStruct();
        root.accept(tr, fs);

        // Print Sparrow-V program to System.err
    }
}
