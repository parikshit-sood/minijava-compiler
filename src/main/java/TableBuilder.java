
import java.util.ArrayList;
import java.util.HashMap;

import minijava.syntaxtree.ClassDeclaration;
import minijava.syntaxtree.FormalParameterList;
import minijava.syntaxtree.FormalParameterRest;
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

    private ArrayList<String> getAllParamTypes(FormalParameterList paramList) {
        ArrayList<String> result = new ArrayList<>();

        // First param
        String type1 = paramList.f0.f0.toString();
        result.add(type1);

        // Rest of the params
        for (Node restNode : paramList.f1.nodes) {
            FormalParameterRest rest = (FormalParameterRest) restNode;
            String typeN = rest.f1.f0.toString();
            result.add(typeN);
        }
        return result;
    }

    @Override
    public void visit(ClassDeclaration n) {
        currentClass = n.f1.f0.toString();
        ClassLayout layout = new ClassLayout();
        layout.className = currentClass;

        // Collect fields and set field offsets
        int fOffset = 4;
        for (Node node : n.f3.nodes) {
            VarDeclaration var = (VarDeclaration) node;
            String fieldName = var.f1.f0.toString();
            layout.fields.add(fieldName);
            layout.fieldOffsets.put(fieldName, fOffset);
            fOffset += 4;
        }

        // Collect methods and set method offsets
        int mOffset = 0;
        for (Node node: n.f4.nodes) {
            MethodDeclaration md = (MethodDeclaration) node;
            String methodName = currentClass + "_" + md.f2.f0.toString();
            layout.vmt.add(methodName);
            layout.methodOffsets.put(methodName, mOffset);
            mOffset += 4;

            if (md.f4.present()) {
                FormalParameterList fpl = (FormalParameterList) md.f4.node;
                ArrayList<String> allTypes = getAllParamTypes(fpl);
                layout.methodParamTypes.put(methodName, allTypes);
            } else {
                layout.methodParamTypes.put(methodName, new ArrayList<>());
            }
        }

        layout.objSize = fOffset;
        layouts.put(currentClass, layout);
    }

    // TODO: Add support for inheritance
}
