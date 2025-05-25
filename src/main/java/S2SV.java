import IR.SparrowParser;
import IR.syntaxtree.Node;

public class S2SV {
    public static void main(String[] args) throws Exception {
        new SparrowParser(System.in);
        Node root = SparrowParser.Program();

        // Process arguments into "a" registers
        ArgsProcessor ap = new ArgsProcessor();
        root.accept(ap);

        // Calculate intervals and do register allocation
        RegAlloc ra = new RegAlloc(ap.args);
        FunctionStruct fs = new FunctionStruct();
        root.accept(ra, fs);

        // Translate from Sparrow to Sparrow-V
        Translator tr = new Translator(ra.uses, ra.args, ra.regs, ra.vars, ra.liveRange, ra.argsLiveRange, ra.linearRegAlloc);
        fs = new FunctionStruct();
        root.accept(tr, fs);

        // Print Sparrow-V program to System.err
    }
}
