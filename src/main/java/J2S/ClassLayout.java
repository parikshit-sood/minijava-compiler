package J2S;

import java.util.HashMap;
import java.util.Map;

public class ClassLayout {
    private String className;
    private String parent = null;
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

    public Map<String, Integer> getFieldOffsets() { return fieldOffsets; }

    public Map<String, Integer> getMethodOffsets() { return methodOffsets; }

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

    public String getParent() { return parent; }

    /**
     * Setters
     */
    public void setClassName(String name) {
        this.className = name;
    }

    public void addField(String field, int offset, String type) {
        this.fieldOffsets.put(field, offset);
        this.fieldTypes.put(field, type);
    }

    public void addMethod(String method, int offset) {
        this.methodOffsets.put(method, offset);
    }

    public void setField(String field, String type) {
        this.fieldTypes.put(field, type);
    }

    public void setObjSize(int sz) {
        this.objSize = sz;
    }

    public void setVmtSize(int sz) { this.vmtSize = sz; }

    public void setParent(String name) { this.parent = name; }
}