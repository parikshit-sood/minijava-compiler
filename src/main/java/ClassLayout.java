
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ClassLayout {
    private String className;
    private List<String> fields = new ArrayList<>();
    private List<String> methods = new ArrayList<>();
    private Map<String, String> fieldTypes = new HashMap<>();
    private Map<String, Integer> fieldOffsets = new HashMap<>();
    private Map<String, Integer> methodOffsets = new HashMap<>();

    private int objSize;
    private int vmtSize;

    /**
     * Getters
     */
    public String getClassName() {
        return className;
    }

    public boolean hasMethod(String name) {
        return methodOffsets.containsKey(name);
    }

    public boolean hasField(String name) {
        return fieldOffsets.containsKey(name);
    }

    public List<String> getFields() { return fields; }

    public List<String> getMethods() { return methods; }

    public int getFieldOffset(String name) {
        return fieldOffsets.get(name);
    }

    public String getFieldType(String name) { return fieldTypes.get(name); }

    public int getMethodOffset(String name) {
        return methodOffsets.get(name);
    }

    public int getObjSize() {
        return objSize;
    }

    public int getVmtSize() { return vmtSize; }

    /**
     * Setters
     */
    public void setClassName(String name) {
        this.className = name;
    }

    public void setFieldOffset(String field, int offset) {
        this.fieldOffsets.put(field, offset);
    }

    public void setMethodOffset(String method, int offset) {
        this.methodOffsets.put(method, offset);
    }

    public void setObjSize(int sz) {
        this.objSize = sz;
    }

    public void setVmtSize(int sz) { this.vmtSize = sz; }
}