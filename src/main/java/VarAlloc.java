
import java.util.Map;

public class VarAlloc {
    Map<String, String> varRegMap;         // var -> register map for register allocations
    Map<String, Integer> varOffsetMap;     // var -> offset map for spilled variables
}
