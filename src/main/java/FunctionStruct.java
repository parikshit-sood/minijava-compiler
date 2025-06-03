import java.util.HashMap;
import java.util.Map;

public class FunctionStruct {
    Map<String, Integer> varOffsets; // variable name -> stack offsets
    Map<String, Integer> argOffsets; // argument name -> stack offet
    int frameSize; // frame size

    public FunctionStruct() {
        this.frameSize = 0;
        this.varOffsets = new HashMap<>();
        this.argOffsets = new HashMap<>();
    }

    /* Setters */
    public void setFrameSize(int sz) {
        frameSize = sz;
    }

    public void setVarOffsets(Map<String, Integer> offsets) {
        varOffsets = new HashMap<>(offsets);
    }

    public void setArgOffsets(Map<String, Integer> offsets) {
        argOffsets = new HashMap<>(offsets);
    }

    /* Getters */
    public int getFrameSize() {
        return frameSize;
    }

    public Map<String, Integer> getVarOffsets() {
        return varOffsets;
    }

    public Map<String, Integer> getArgOffsets() {
        return argOffsets;
    }

    public boolean hasVar(String var) {
        return varOffsets.containsKey(var);
    }

    public boolean hasArg(String arg) {
        return argOffsets.containsKey(arg);
    }
}
