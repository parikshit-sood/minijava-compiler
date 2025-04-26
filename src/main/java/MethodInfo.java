
import java.util.ArrayList;
import java.util.HashMap;

public class MethodInfo {
    private final String name;
    private MJType returnType;
    private ArrayList<Tuple> parameters;
    private HashMap<String, MJType> localVariables;

    public MethodInfo(String name, MJType type) {
        this.name = name;
        this.returnType = type;
    }

    /**
     * Setters
     */
    public void setParameters(ArrayList<Tuple> newParams) {
        parameters = newParams;
    }

    public void setLocalVariables(HashMap<String, MJType> vars) {
        localVariables = vars;
    }

    /**
     * Getters
     */
    public String getName() {
        return name;
    }

    public MJType getReturnType() {
        return returnType;
    }

    public ArrayList<Tuple> getParameters() {
        return parameters;
    }

    public HashMap<String, MJType> getLocalVariables() {
        return localVariables;
    }
}
