
import java.util.HashMap;
import java.util.List;

import minijava.syntaxtree.*;
import minijava.visitor.DepthFirstVisitor;

public class InheritanceResolver extends DepthFirstVisitor{
    HashMap<String, ClassLayout> layouts;

    public InheritanceResolver(HashMap<String, ClassLayout> tbLayout) {
        this.layouts = tbLayout;
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
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public void visit(ClassExtendsDeclaration n) {
        String childName = n.f1.f0.toString();
        String parentName = n.f3.f0.toString();

        ClassLayout parentLayout = layouts.get(parentName);
        ClassLayout thisLayout = new ClassLayout();

        thisLayout.setClassName(childName);

        // Process child fields
        int fOffset = 4;

        for (Node node : n.f5.nodes) {
            VarDeclaration var = (VarDeclaration) node;
            String fieldName = var.f1.f0.toString();
            String fieldType = typeString(var.f0);
            thisLayout.addField(fieldName, fOffset, fieldType);
            fOffset += 4;
        }

        // Process parent fields
        List<String> parentFields = parentLayout.getFields();

        for (String pField : parentFields) {
            if (thisLayout.hasField(pField)) {
                continue;
            }
            String pType = parentLayout.getFieldType(pField);
            thisLayout.addField(pField, fOffset, pType);
            fOffset += 4;
        }

        // Process child methods
        int mOffset = 0;

        for (Node node : n.f6.nodes) {
            MethodDeclaration md = (MethodDeclaration) node;

            String methodName = childName + "_" + md.f2.f0.toString();
            thisLayout.addMethod(methodName, mOffset);
            mOffset += 4;
        }

        // Process parent methods
        List<String> parentMethods = parentLayout.getMethods();

        for (String pMethod : parentMethods) {
            String mName = pMethod.split("_")[1];

            if (thisLayout.hasMethod(childName + "_" + mName)) {
                continue;
            }

            thisLayout.addMethod(pMethod, mOffset);
            mOffset += 4;
        }

        thisLayout.setObjSize(fOffset);
        thisLayout.setVmtSize(mOffset);

        layouts.put(childName, thisLayout);
    }
}
