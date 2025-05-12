
import java.util.ArrayList;
import java.util.HashMap;

public class ClassLayout {
    String className;
    ArrayList<String> fields = new ArrayList<>();
    ArrayList<String> vmt = new ArrayList<>();
    HashMap<String, Integer> fieldOffsets = new HashMap<>();
    HashMap<String, Integer> methodOffsets = new HashMap<>();
    HashMap<String, ArrayList<String>> methodParamTypes = new HashMap<>();

    int objSize;
}