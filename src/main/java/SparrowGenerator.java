import java.util.HashMap;

import minijava.visitor.DepthFirstVisitor;
import sparrow.Program;
import typecheck.ClassInfo;

public class SparrowGenerator extends DepthFirstVisitor{
    private HashMap<String, ClassInfo> classTable;
    private Program code;
    private int tempCounter;
    private String currentClass;
    private String currentMethod;

    public SparrowGenerator(HashMap<String, ClassInfo> classTable) {
        this.classTable = classTable;
        this.code = new Program();
        this.tempCounter = 0;
    }
}
