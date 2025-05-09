
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
        layout.className = currentClass;

        // Collect fields
        for (Node node : n.f3.nodes) {
            VarDeclaration var = (VarDeclaration) node;
            String fieldName = var.f1.f0.toString();
            layout.fields.add(fieldName);
        }

        // Collect methods
        for (Node node: n.f4.nodes) {
            MethodDeclaration md = (MethodDeclaration) node;
            String methodName = md.f2.f0.toString();
            layout.vmt.add(currentClass + "_" + methodName);
        }

        // Set field offsets (field offsets start at 4, offset 0 = vmt pointer)
        for (int i = 0; i < layout.fields.size(); i++) {
            layout.fieldOffsets.put(layout.fields.get(i), 4 * i + 4);
        }

        // Set method offsets (start at 0 in vmt)
        for (int i = 0; i < layout.vmt.size(); i++) {
            String methodName = layout.vmt.get(i);
            layout.methodOffsets.put(methodName, 4 * i);
        }

        // Store class object size (4 + 4 * numFields)
        layout.objSize = 4 + (4 * layout.fields.size());

        layouts.put(currentClass, layout);
    }

    // TODO: Add support for inheritance
}
