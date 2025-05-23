import IR.SparrowParser;
import IR.syntaxtree.Program;
import IR.visitor.GJDepthFirst;

public class S2SV {
    public static void main(String[] args) throws Exception {
        new SparrowParser(System.in);

        Program prog = SparrowParser.Program();
        prog.accept(new GJDepthFirst<>(), null);
    }
}
