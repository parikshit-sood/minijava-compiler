
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import minijava.syntaxtree.ClassDeclaration;
import minijava.syntaxtree.ClassExtendsDeclaration;
import minijava.syntaxtree.Node;
import minijava.visitor.DepthFirstVisitor;

public class ClassCollector extends DepthFirstVisitor{
    public Map<String, String> parentMap = new HashMap<>();
    public Map<String, Node> classNodes = new HashMap<>();
    public Set<String> allClasses = new HashSet<>();

    @Override
    public void visit(ClassDeclaration n) {
        String className = n.f1.f0.toString();
        allClasses.add(className);
        classNodes.put(className, n);
    }

    @Override
    public void visit(ClassExtendsDeclaration n) {
        String className = n.f1.f0.toString();
        String parentName = n.f3.f0.toString();
        allClasses.add(className);
        parentMap.put(className, parentName);
        classNodes.put(className, n);
    }
}
