import java.util.HashMap;

public class ClassInfo {
    private final String name;
    private String parent;
    private HashMap<String, MJType> fields = new HashMap<>();
    private HashMap<String, MethodInfo> methods = new HashMap<>();
    private boolean visitingMethod = false;
    private String currentMethod;

    public ClassInfo(String name) {
        this.name = name;
    }

    /**
     * Setters
     */
    public boolean setParent(String parentName) {
        if (parent == null) {
            parent = parentName;
            return true;
        }
        return false;
    }

    public void setFields(HashMap<String, MJType> newFields) {
        fields = newFields;
    }

    public void setMethods(HashMap<String, MethodInfo> newMethods) {
        methods = newMethods;
    }

    public void setVisitingMethod(boolean status) {
        visitingMethod = status;
    }

    public void setCurrentMethod(String newMethod) {
        currentMethod = newMethod;
    }

    /**
     * Getters
     */
    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }

    public HashMap<String, MJType> getFields() {
        return fields;
    }

    public HashMap<String, MethodInfo> getMethods() {
        return methods;
    }

    public boolean getVisitingMethod() {
        return visitingMethod;
    }

    public String getCurrentMethod() {
        return currentMethod;
    }
    
}
