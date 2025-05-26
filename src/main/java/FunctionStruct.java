
import java.util.List;

public class FunctionStruct {
    String name;                // function name
    List<Interval> intervals;   // liveness intervals
    VarAlloc varAllocs;         // final variable allocations
    int nextStackOffset;        // next stack offset for spilled variables
}
