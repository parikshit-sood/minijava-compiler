import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import IR.visitor.GJVoidDepthFirst;

public class LivenessVisitor extends GJVoidDepthFirst<FunctionStruct> {
    Map<String, Map<String, Integer>> defs;     // function -> variable -> line number of first instance
    Map<String, Map<String, Integer>> uses;     // function -> variable -> line number of last instance
    Map<String, Map<String, String>> aRegs;     // function -> variable -> "a" register
    Map<String, Map<String, Integer>> labels;   // function -> label -> line number
    Map<String, Map<String, String>> linearRegAlloc;    // function -> variables -> register allocations
    Map<String, Map<String, Interval>> tsIntervals;     // intervals for variables allocating to "t" and "s" registers
    Map<String, Map<String, Interval>> aIntervals;      // intervals for argument variables in "a" registers
    Map<String, Map<String, Interval>> aRanges;         // liveness ranges for argument variables
    List<Interval> loopIntervals;       // Intervals inside loops

    int lineNum;

    public LivenessVisitor(Map<String, Map<String, String>> aRegs) {
        this.defs = new HashMap<>();
        this.uses = new HashMap<>();
        this.aRegs = aRegs;
        this.labels = new HashMap<>();
        this.linearRegAlloc = new HashMap<>();
        this.tsIntervals = new HashMap<>();
        this.aIntervals = new HashMap<>();
        this.aRanges = new HashMap<>();
        this.loopIntervals = new ArrayList<>();
        this.lineNum = 1;
    }

    // -----------------
    // Helper functions
    // -----------------

    private void addID(String fName, String varId, int lineNum) {
        // If varId is a function argument, no def or use needed
        if (aRegs.get(fName).containsKey(varId)) {
            if (!aIntervals.get(fName).containsKey(varId)) {
                // First instance of varId
                aIntervals.get(fName).put(varId, new Interval(lineNum, lineNum));
            } 
            else {
                // Recalculate interval bounds
                int first = Math.min(aIntervals.get(fName).get(varId).getFirst(), lineNum);
                int last = Math.max(aIntervals.get(fName).get(varId).getLast(), lineNum);

                // Update interval for function -> varId
                aIntervals.get(fName).put(varId, new Interval(first, last));
            }
            return;
        }
        
        // varId is not a function argument, update def and use maps for function -> varId
        addDef(fName, varId, lineNum);
        addUse(fName, varId, lineNum);
    }

    private void addDef(String fName, String varId, int lineNum) {
        // First instance of function
        if (!defs.containsKey(fName)) {
            defs.put(fName, new HashMap<>());
        }

        if (!defs.get(fName).containsKey(varId)) {
            // First instance of varId
            defs.get(fName).put(varId, lineNum);
        } else {
            // Store earliest instance of varId
            int first = Math.min(defs.get(fName).get(varId), lineNum);
            defs.get(fName).put(varId, first);
        }
    }

    private void addUse(String fName, String varId, int lineNum) {
        // First instance of function
        if (!uses.containsKey(fName)) {
            uses.put(fName, new HashMap<>());
        }

        if (!uses.get(fName).containsKey(varId)) {
            // First instance of varId
            uses.get(fName).put(varId, lineNum);
        } else {
            // Store latest instance of varId
            int first = Math.max(uses.get(fName).get(varId), lineNum);
            uses.get(fName).put(varId, first);
        }
    }

    private void addLabel(String fName, String label, int lineNum) {
        // First instance of function
        if (!labels.containsKey(fName)) {
            labels.put(fName, new HashMap<>());
        }

        // Store line number of label
        labels.get(fName).put(label, lineNum);
    }

    // -----------------
    // Core functions
    // -----------------

    
}
