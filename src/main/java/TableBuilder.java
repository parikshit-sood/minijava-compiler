
import java.util.HashMap;

import minijava.syntaxtree.ArrayType;
import minijava.syntaxtree.BooleanType;
import minijava.syntaxtree.ClassDeclaration;
import minijava.syntaxtree.IntegerType;
import minijava.syntaxtree.MethodDeclaration;
import minijava.syntaxtree.Node;
import minijava.syntaxtree.NodeSequence;
import minijava.syntaxtree.VarDeclaration;
import minijava.visitor.DepthFirstVisitor;

public class TableBuilder extends DepthFirstVisitor {

    private HashMap<String, ClassLayout> layouts = new HashMap<>();
    private String currentClass;

    public HashMap<String, ClassLayout> getLayouts() {
        return layouts;
    }

    private String typeString(minijava.syntaxtree.Type tp) {
        Node n = tp.f0.choice;

        if (n instanceof BooleanType) return "boolean";
        if (n instanceof IntegerType) return "int";
        if ((n instanceof ArrayType) || (n instanceof NodeSequence)) return "arr";

        return ((minijava.syntaxtree.Identifier) n).f0.toString();
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
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
            String fieldType = typeString(var.f0);
            layout.addField(fieldName, fOffset, fieldType);
            fOffset += 4;
        }

        // Collect methods and set method offsets
        int mOffset = 0;
        for (Node node: n.f4.nodes) {
            MethodDeclaration md = (MethodDeclaration) node;
            String methodName = currentClass + "_" + md.f2.f0.toString();
            layout.addMethod(methodName, mOffset);
            mOffset += 4;
        }

        layout.setObjSize(fOffset);
        layout.setVmtSize(mOffset);
        layouts.put(currentClass, layout);
    }

    // @Override
    // public void visit(ClassExtendsDeclaration n) {
    //     // Do nothing in this stage for extends relations
    //     return;
    // }
}
