
import java.util.HashMap;

import minijava.syntaxtree.ArrayType;
import minijava.syntaxtree.BooleanType;
import minijava.syntaxtree.ClassDeclaration;
import minijava.syntaxtree.ClassExtendsDeclaration;
import minijava.syntaxtree.FormalParameter;
import minijava.syntaxtree.FormalParameterList;
import minijava.syntaxtree.FormalParameterRest;
import minijava.syntaxtree.Identifier;
import minijava.syntaxtree.IntegerType;
import minijava.syntaxtree.MainClass;
import minijava.syntaxtree.MethodDeclaration;
import minijava.syntaxtree.Type;
import minijava.syntaxtree.TypeDeclaration;
import minijava.syntaxtree.VarDeclaration;
import minijava.visitor.GJDepthFirst;

public class TableBuilder extends GJDepthFirst<MJType, ClassInfo> {
    private HashMap<String, ClassInfo> classTable = new HashMap<>();

    public void setClassTable(HashMap<String, ClassInfo> map) {
        classTable = map;
    }

    public HashMap<String, ClassInfo> getClassTable() {
        return classTable;
    }

    @Override
    public MJType visit(MainClass n, ClassInfo currClass) {
        String className = n.f1.f0.toString();
        ClassInfo mainClass = new ClassInfo(className);
        classTable.put(className, mainClass);

        MethodInfo mainMethod = new MethodInfo("main", new MJType("MJType"));
        mainClass.getMethods().put("main", mainMethod);

        String paramName = n.f11.f0.toString();
        MJType paramType = new MJType("arr");
        mainMethod.getParameters().add(new Tuple(paramName, paramType));
        mainMethod.getLocalVariables().put(paramName, paramType);

        mainClass.setVisitingMethod(true);
        mainClass.setCurrentMethod("main");
        n.f14.accept(this, mainClass);
        mainClass.setCurrentMethod(null);
        mainClass.setVisitingMethod(false);

        return new MJType("void");
    }

    @Override
    public MJType visit(TypeDeclaration n, ClassInfo currClass) {
        n.f0.accept(this, currClass);

        return new MJType("void");
    }

    @Override
    public MJType visit(ClassDeclaration n, ClassInfo currClass) {
        String className = n.f1.f0.toString();
        ClassInfo currentClass = new ClassInfo(className);

        if (classTable.containsKey(className)) {
            throw new TypeException("Class " + className + " declared more than once");
        }
        classTable.put(className, currentClass);
        n.f3.accept(this, currentClass);
        n.f4.accept(this, currentClass);

        return new MJType("void");
    }

    @Override
    public MJType visit(ClassExtendsDeclaration n, ClassInfo currClass) {
        String className = n.f1.f0.toString();
        String parent = n.f3.f0.toString();
        ClassInfo currentClass = new ClassInfo(className);

        if (classTable.containsKey(className)) {
            throw new TypeException("Class " + className + " declared more than once");
        }

        if (!(currentClass.setParent(parent))) {
            throw new TypeException("Child class " + className + " cannot extend more than one parent");
        }

        n.f5.accept(this, currClass);
        n.f6.accept(this, currClass);

        return new MJType("void");
    }

    @Override
    public MJType visit(VarDeclaration n, ClassInfo currClass) {
        MJType t0 = n.f0.accept(this, currClass);
        String id = n.f1.f0.toString();

        if (!(currClass.getVisitingMethod())) {
            if (currClass.getFields().containsKey(id)) {
                throw new TypeException("Field variable " + id + " declared more than once");
            }
            currClass.getFields().put(id, t0);
        } else {
            MethodInfo currentMethod = currClass.getMethods().get(currClass.getCurrentMethod());
            if (currentMethod.getLocalVariables().containsKey(id)) {
                throw new TypeException("Local variable " + id + " declared more than once");
            }
            currentMethod.getLocalVariables().put(id, t0);
        }

        return new MJType("void");
    }

    @Override
    public MJType visit(MethodDeclaration n, ClassInfo currClass) {
        MJType returnType = n.f1.accept(this, currClass);
        String methodName = n.f2.f0.toString();

        MethodInfo currentMethod = new MethodInfo(methodName, returnType);
        if (currClass.getMethods().containsKey("methodName")) {
            throw new TypeException("Method " + methodName + " declared more than once in class " + currClass.getName());
        }

        currClass.getMethods().put(methodName, currentMethod);

        currClass.setVisitingMethod(true);
        currClass.setCurrentMethod(methodName);

        n.f4.accept(this, currClass);
        n.f7.accept(this, currClass);

        currClass.setCurrentMethod(null);
        currClass.setVisitingMethod(false);

        return new MJType("void");
    }

    @Override
    public MJType visit(FormalParameterList n, ClassInfo currClass) {
        n.f0.accept(this, currClass);
        n.f1.accept(this, currClass);

        return new MJType("void");
    }

    @Override
    public MJType visit(FormalParameter n, ClassInfo currClass) {
        MJType type = n.f0.accept(this, currClass);
        String name = n.f1.f0.toString();
        MethodInfo currentMethod = currClass.getMethods().get(currClass.getCurrentMethod());

        if (currentMethod.hasParameter(name)) {
            throw new TypeException("Parameter " + name + " is declared more than once in current scope");
        }

        Tuple nameType = new Tuple(name, type);
        currentMethod.getParameters().add(nameType);
        currentMethod.getLocalVariables().put(name, type);

        return new MJType("void");
    }

    @Override
    public MJType visit(FormalParameterRest n, ClassInfo currClass) {
        n.f1.accept(this, currClass);

        return new MJType("void");
    }

    @Override
    public MJType visit(Type n, ClassInfo c) {
        return n.f0.accept(this, c);
    }

    @Override
    public MJType visit(ArrayType n, ClassInfo c) {
        return new MJType("arr");
    }

    @Override
    public MJType visit(BooleanType n, ClassInfo c) {
        return new MJType("boolean");
    }

    @Override
    public MJType visit(IntegerType n, ClassInfo c) {
        return new MJType("int");
    }

    @Override
    public MJType visit(Identifier n, ClassInfo c) {
        return new MJType(n.f0.toString(), true);
    }
}
