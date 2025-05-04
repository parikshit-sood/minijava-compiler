package typecheck;

import java.util.HashMap;
import java.util.HashSet;

import minijava.syntaxtree.ClassExtendsDeclaration;
import minijava.visitor.GJDepthFirst;

public class InheritanceResolver extends GJDepthFirst<MJType, Void> {
    private final HashMap<String, ClassInfo> classTable;
    private HashSet<String> visiting;
    private HashSet<String> visited;

    public InheritanceResolver(HashMap<String, ClassInfo> classTable) {
        this.classTable = classTable;
        this.visiting = new HashSet<>();
        this.visited = new HashSet<>();
    }

    private void checkCycle(String className) {
        if (visiting.contains(className)) {
            throw new TypeException("Cycling inheritance detected");
        }

        if (visited.contains(className)) {
            return;
        }

        ClassInfo classObj = classTable.get(className);
        if (classObj == null) {
            return;
        }

        visiting.add(className);

        String parent = classObj.getParent();
        if (parent != null) {
            checkCycle(parent);
        }
        
        visiting.remove(className);
        visited.add(className);
    }

    @Override
    public MJType visit(ClassExtendsDeclaration n, Void argu) {
        String className = n.f1.f0.toString();
        String parentName = n.f3.f0.toString();

        checkCycle(className);

        ClassInfo childClass = classTable.get(className);
        ClassInfo parentClass = classTable.get(parentName);

        if (parentClass == null) {
            throw new TypeException("Parent class " + parentName + " is undefined");
        }

        // Copy fields from parent to child
        for (HashMap.Entry<String, MJType> entry : parentClass.getFields().entrySet()) {
            String name = entry.getKey();

            if (!(childClass.getFields().containsKey(name))) {
                childClass.getFields().put(name, entry.getValue());
            }
        }

        // Copy methods from parent to child
        for (HashMap.Entry<String, MethodInfo> entry : parentClass.getMethods().entrySet()) {
            String methodName = entry.getKey();
            MethodInfo parentMethod = entry.getValue();

            if (childClass.getMethods().containsKey(methodName)) {
                MethodInfo childMethod = childClass.getMethods().get(methodName);
                validateOverride(childMethod, parentMethod, className);
            } else {
                childClass.getMethods().put(methodName, new MethodInfo(parentMethod));
            }
        }

        return new MJType("void");
    }

    private void validateOverride(MethodInfo child, MethodInfo parent, String className) {
        
        if (!(child.getReturnType().getType().equals(parent.getReturnType().getType()))) {
            throw new TypeException("Child method " + child.getName() + " in class " + className +  " is overriding parent method " + parent.getName() + " but has different return type");
        }

        if (child.getParameters().size() != parent.getParameters().size()) {
            throw new TypeException("Child method " + child.getName() + " in class " + className + " is overriding parent method " + parent.getName() + " but has different number of parameters");
        }

        for (int i = 0; i < child.getParameters().size(); i++) {
            MJType childParam = child.getParameters().get(i).getType();
            MJType parentParam = parent.getParameters().get(i).getType();

            if (!(childParam.getType().equals(parentParam.getType()))) {
                throw new TypeException("Child method " + child.getName() + " in class " + className + " is overriding parent method " + parent.getName() + " but has different type for parameter " + i);
            }
        }
    }
}
