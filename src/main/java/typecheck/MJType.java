package typecheck;
public class MJType {
    private final String type;
    private boolean isClass;

    public MJType(String type) {
        this.type = type;
    }

    public MJType(String type, boolean isClass) {
        this.type = type;
        this.isClass = isClass;
    }

    /**
     * Type getters
     * @return type of this object
     */
    public String getType() {
        return type;
    }

    /**
     * Type checkers
     * @return boolean true if type is primitive MiniJava type (int, boolean, int[]), else false
     */
    public boolean intType() {
        return !(isClass) && type.equals("int");
    }

    public boolean booleanType() {
        return !(isClass) && type.equals("boolean");
    }

    public boolean arrType() {
        return !(isClass) && type.equals("arr");
    }

    /**
     * Type checker for class type (i.e. class instances)
     * @return boolean true if type is class instance in MiniJava, else false
     */
    public boolean classType() {
        return isClass;
    }
}
