import java.io.InputStream;

import IR.SparrowParser;
import IR.registers.Registers;
import IR.syntaxtree.Node;
import IR.visitor.SparrowVConstructor;
import sparrowv.Program;

public class SV2V {
    public static void main(String[] args) throws Exception {
        Registers.SetRiscVregs();
        InputStream in = System.in;
        new SparrowParser(in);
        Node root = SparrowParser.Program();
        SparrowVConstructor constructor = new SparrowVConstructor();
        root.accept(constructor);
        Program program = constructor.getProgram();

        // Pre-compute stack offsets and stack frame size
        OffsetVisitor ov = new OffsetVisitor();
        ov.visit(program);

        // Translate Sparrow-V to RISC-V
        VTranslator tr = new VTranslator(ov.funcVarOffsets, ov.funcFrameSizes);
        tr.visit(program);
        System.out.println(tr.riscProgram.toString());
    }
}