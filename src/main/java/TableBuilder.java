
import java.util.HashMap;

import minijava.syntaxtree.ClassDeclaration;
import minijava.syntaxtree.MethodDeclaration;
import minijava.syntaxtree.Node;
import minijava.syntaxtree.VarDeclaration;
import minijava.visitor.DepthFirstVisitor;

public class TableBuilder extends DepthFirstVisitor {

    private HashMap<String, ClassLayout> layouts = new HashMap<>();
    private String currentClass;

    public HashMap<String, ClassLayout> getLayouts() {
        return layouts;
    }

    @Override
    public void visit(ClassDeclaration n) {
        currentClass = n.f1.f0.toString();
        ClassLayout layout = new ClassLayout();
        layout.setClassName(currentClass);;

        // Collect fields and set field offsets
        int fOffset = 4;
        for (Node node : n.f3.nodes) {
            VarDeclaration var = (VarDeclaration) node;
            String fieldName = var.f1.f0.toString();
            layout.setFieldOffset(fieldName, fOffset);
            fOffset += 4;
        }

        // Collect methods and set method offsets
        int mOffset = 0;
        for (Node node: n.f4.nodes) {
            MethodDeclaration md = (MethodDeclaration) node;
            String methodName = currentClass + "_" + md.f2.f0.toString();
            layout.setMethodOffset(methodName, mOffset);
            mOffset += 4;
        }

        layout.setObjSize(fOffset);
        layout.setVmtSize(mOffset);
        layouts.put(currentClass, layout);
    }

    // TODO: Add support for inheritance
}
