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

        VTranslator tr = new VTranslator();
        tr.visit(program);
        System.out.println(tr.riscProgram.toString());
        // System.err.println(program.toString());
    }
}