
import java.util.HashMap;

import minijava.syntaxtree.Goal;
import minijava.visitor.DepthFirstVisitor;

public class SparrowGenerator extends DepthFirstVisitor{
    private HashMap<String, ClassInfo> classTable;
    private StringBuilder code;
    private int tempCounter;
    private String currentClass;
    private String currentMethod;

    public SparrowGenerator(HashMap<String, ClassInfo> classTable) {
        this.classTable = classTable;
        this.code = new StringBuilder();
        this.tempCounter = 0;
    }

    // Get generated Sparrow code
    public String getGeneratedCode() {
        return code.toString();
    }

    // Get next available temp variable
    private String getNewTemp() {
        return "t" + (tempCounter++);
    }

    // Entry point
    @Override
    public void visit(Goal n) {
        n.f0.accept(this);
        n.f1.accept(this);
    }
}
