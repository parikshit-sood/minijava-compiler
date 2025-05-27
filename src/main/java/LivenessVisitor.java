import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import IR.syntaxtree.*;
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

    private void addID(String fName, String varId, int lineNumber) {
        // If varId is a function argument, no def or use needed
        if (aRegs.get(fName).containsKey(varId)) {
            if (!aIntervals.get(fName).containsKey(varId)) {
                // First instance of varId
                aIntervals.get(fName).put(varId, new Interval(lineNumber, lineNumber));
            } 
            else {
                // Recalculate interval bounds
                int first = Math.min(aIntervals.get(fName).get(varId).getFirst(), lineNumber);
                int last = Math.max(aIntervals.get(fName).get(varId).getLast(), lineNumber);

                // Update interval for function -> varId
                aIntervals.get(fName).put(varId, new Interval(first, last));
            }
            return;
        }
        
        // varId is not a function argument, update def and use maps for function -> varId
        addDef(fName, varId, lineNumber);
        addUse(fName, varId, lineNumber);
    }

    private void addDef(String fName, String varId, int lineNumber) {
        // First instance of function
        if (!defs.containsKey(fName)) {
            defs.put(fName, new HashMap<>());
        }

        if (!defs.get(fName).containsKey(varId)) {
            // First instance of varId
            defs.get(fName).put(varId, lineNumber);
        } else {
            // Store earliest instance of varId
            int first = Math.min(defs.get(fName).get(varId), lineNumber);
            defs.get(fName).put(varId, first);
        }
    }

    private void addUse(String fName, String varId, int lineNumber) {
        // First instance of function
        if (!uses.containsKey(fName)) {
            uses.put(fName, new HashMap<>());
        }

        if (!uses.get(fName).containsKey(varId)) {
            // First instance of varId
            uses.get(fName).put(varId, lineNumber);
        } else {
            // Store latest instance of varId
            int first = Math.max(uses.get(fName).get(varId), lineNumber);
            uses.get(fName).put(varId, first);
        }
    }

    private void addLabel(String fName, String label, int lineNumber) {
        // First instance of function
        if (!labels.containsKey(fName)) {
            labels.put(fName, new HashMap<>());
        }

        // Store line number of label
        labels.get(fName).put(label, lineNumber);
    }

    // -----------------
    // Core functions
    // -----------------

    /**
     * f0 -> "func"
     * f1 -> FunctionName()
     * f2 -> "("
     * f3 -> ( Identifier() )*
     * f4 -> ")"
     * f5 -> Block()
     */
    @Override
    public void visit(FunctionDeclaration n, FunctionStruct f) {
        f.name = n.f1.f0.toString();
        
        // Clear loop intervals for this function
        loopIntervals.clear();

        // New variable -> interval map for this function
        aIntervals.put(f.name, new HashMap<>());

        // Process function arguments
        if (n.f3.present()) {
            for (int i = 0; i < n.f3.size(); i++) {
                String paramName = ((Identifier) n.f3.elementAt(i)).f0.toString();
                addID(f.name, paramName, 0);
            }
        }

        // Process function block
        n.f5.accept(this, f);
        lineNum = 1;

        // Liveness analysis
        Map<String, Interval> tempRange = new HashMap<>();
        Map<String, Interval> aTempRange = new HashMap<>();
        List<Interval> intervals = new ArrayList<>();

        for (String id : defs.get(f.name).keySet()) {
            int first = defs.get(f.name).get(id);
            int last = uses.get(f.name).get(id);

            for (Interval i : loopIntervals) {
                if ((i.getFirst() <= first && i.getLast() >= first) || (i.getFirst() <= last && i.getLast() >= last)) {
                    first = Math.min(i.getFirst(), first);
                    last = Math.max(i.getLast(), last);
                }
            }

            intervals.add(new Interval(first, -1, id, false));
            intervals.add(new Interval(-1, last, id, true));
            tempRange.put(id, new Interval(first, last));
        }

        for (String id : aIntervals.get(f.name).keySet()) {
            int first = aIntervals.get(f.name).get(id).getFirst();
            int last = aIntervals.get(f.name).get(id).getLast();

            for (Interval i : loopIntervals) {
                if ((i.getFirst() <= first && i.getLast() >= first) || (i.getFirst() <= last && i.getLast() >= last)) {
                    first = Math.min(i.getFirst(), first);
                    last = Math.max(i.getLast(), last);
                }
            }

            aTempRange.put(id, new Interval(first, last));
        }

        tsIntervals.put(f.name, tempRange);
        aRanges.put(f.name, aTempRange);
        
        // Sort intervals by starting line number
        intervals.sort(Interval.comparator);

        Deque<String> availableRegs = new ArrayDeque<>(
            Arrays.asList("t0","t1","t2","t3","t4","t5","s1","s2","s3","s4","s5","s6","s7","s8"));  // Reserve s9-s11 registers as temps

        Map<String, String> tempAssignment = new HashMap<>();
        Map<String, String> assignment = new HashMap<>();

        for (Interval i : intervals) {
            // Get variable id for this interval
            String id = i.id;
            
            if (i.isEnd) {
                // Interval ends
                if (tempAssignment.containsKey(id)) {
                    // If interval was assignmed to a register in temp assignment, make it permanent
                    assignment.put(id, tempAssignment.get(id));
                    
                    // Make register available for new temp assignment
                    availableRegs.add(tempAssignment.get(id));

                    // Remove temp assignment
                    tempAssignment.remove(id);
                }
            } else {
                // Interval starts
                if (!availableRegs.isEmpty()) {
                    // Make temp assignment if possible
                    tempAssignment.put(id, availableRegs.poll());
                } else {
                    // No registers available, must spill to memory
                    int end = tempRange.get(id).getLast();
                    int otherEnd = end;
                    String otherId = id;

                    // Find longest interval
                    for (String other : tempAssignment.keySet()) {
                        int e = tempRange.get(other).getLast();
                        if (e > otherEnd) {
                            otherEnd = e;
                            otherId = other;
                        }
                    }

                    if (!otherId.equals(id)) {
                        // Spill id of longest interval to memory
                        tempAssignment.put(id, tempAssignment.get(otherId));
                        tempAssignment.remove(otherId);
                    }
                }
            }
        }

        // Make all temp assignments permanent
        assignment.putAll(tempAssignment);
        linearRegAlloc.put(f.name, assignment);
    }

    /**
     * f0 -> LabelWithColon()
     *       | SetInteger()
     *       | SetFuncName()
     *       | Add()
     *       | Subtract()
     *       | Multiply()
     *       | LessThan()
     *       | Load()
     *       | Store()
     *       | Move()
     *       | Alloc()
     *       | Print()
     *       | ErrorMessage()
     *       | Goto()
     *       | IfGoto()
     *       | Call()
     */
    @Override
    public void visit(Instruction n, FunctionStruct f) {
        f.lineNumber = lineNum++;
        n.f0.accept(this, f);
    }

    /**
     * f0 -> Label()
     * f1 -> ":"
     */
    @Override
    public void visit(LabelWithColon n, FunctionStruct f) {
        n.f0.accept(this, f);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> IntegerLiteral()
     */
    @Override
    public void visit(SetInteger n, FunctionStruct f) {
        String id = n.f0.f0.toString();
        addID(f.name, id, f.lineNumber);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "@"
     * f3 -> FunctionName()
     */
    @Override
    public void visit(SetFuncName n, FunctionStruct f) {
        String id = n.f0.f0.toString();
        addID(f.name, id, f.lineNumber);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "+"
     * f4 -> Identifier()
     */
    @Override
    public void visit(Add n, FunctionStruct f) {
        String lhs = n.f0.f0.toString();
        addID(f.name, lhs, f.lineNumber);

        String arg1 = n.f2.f0.toString();
        addID(f.name, arg1, f.lineNumber);

        String arg2 = n.f4.f0.toString();
        addID(f.name, arg2, f.lineNumber);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "-"
     * f4 -> Identifier()
     */
    @Override
    public void visit(Subtract n, FunctionStruct f) {
        String lhs = n.f0.f0.toString();
        addID(f.name, lhs, f.lineNumber);

        String arg1 = n.f2.f0.toString();
        addID(f.name, arg1, f.lineNumber);

        String arg2 = n.f4.f0.toString();
        addID(f.name, arg2, f.lineNumber);
    }

   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Identifier()
    * f3 -> "*"
    * f4 -> Identifier()
    */
    @Override
    public void visit(Multiply n, FunctionStruct f) {
        String lhs = n.f0.f0.toString();
        addID(f.name, lhs, f.lineNumber);

        String arg1 = n.f2.f0.toString();
        addID(f.name, arg1, f.lineNumber);

        String arg2 = n.f4.f0.toString();
        addID(f.name, arg2, f.lineNumber);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     * f3 -> "<"
     * f4 -> Identifier()
     */
    @Override
    public void visit(LessThan n, FunctionStruct f) {
        String lhs = n.f0.f0.toString();
        addID(f.name, lhs, f.lineNumber);

        String arg1 = n.f2.f0.toString();
        addID(f.name, arg1, f.lineNumber);

        String arg2 = n.f4.f0.toString();
        addID(f.name, arg2, f.lineNumber);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "["
     * f3 -> Identifier()
     * f4 -> "+"
     * f5 -> IntegerLiteral()
     * f6 -> "]"
     */
    @Override
    public void visit(Load n, FunctionStruct f) {
        String lhs = n.f0.f0.toString();
        addID(f.name, lhs, f.lineNumber);

        String rhs = n.f3.f0.toString();
        addID(f.name, rhs, f.lineNumber);
    }

    /**
     * f0 -> "["
     * f1 -> Identifier()
     * f2 -> "+"
     * f3 -> IntegerLiteral()
     * f4 -> "]"
     * f5 -> "="
     * f6 -> Identifier()
     */
    @Override
    public void visit(Store n, FunctionStruct f) {
        String lhs = n.f1.f0.toString();
        addID(f.name, lhs, f.lineNumber);

        String rhs = n.f6.f0.toString();
        addID(f.name, rhs, f.lineNumber);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     */
    @Override
    public void visit(Move n, FunctionStruct f) {
        String lhs = n.f0.f0.toString();
        addID(f.name, lhs, f.lineNumber);

        String rhs = n.f2.f0.toString();
        addID(f.name, rhs, f.lineNumber);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "alloc"
     * f3 -> "("
     * f4 -> Identifier()
     * f5 -> ")"
     */
    @Override
    public void visit(Alloc n, FunctionStruct f) {
        String lhs = n.f0.f0.toString();
        addID(f.name, lhs, f.lineNumber);

        String rhs = n.f4.f0.toString();
        addID(f.name, rhs, f.lineNumber);
    }

    /**
     * f0 -> "print"
     * f1 -> "("
     * f2 -> Identifier()
     * f3 -> ")"
     */
    @Override
    public void visit(Print n, FunctionStruct f) {
        String id = n.f2.f0.toString();
        addID(f.name, id, f.lineNumber);
    }

    /**
     * f0 -> "goto"
     * f1 -> Label()
     */
    @Override
    public void visit(Goto n, FunctionStruct f) {
        String label = n.f1.f0.toString();
        
        if (labels.containsKey(f.name) && labels.get(f.name).containsKey(label)) {
            int destLineNum = labels.get(f.name).get(label);

            if (destLineNum < f.lineNumber) {
                loopIntervals.add(new Interval(destLineNum, f.lineNumber));
            }
        }
    }

    /**
     * f0 -> "if0"
     * f1 -> Identifier()
     * f2 -> "goto"
     * f3 -> Label()
     */
    @Override
    public void visit(IfGoto n, FunctionStruct f) {
        String condId = n.f1.f0.toString();
        addID(f.name, condId, f.lineNumber);

        String label = n.f3.f0.toString();
        
        if (labels.containsKey(f.name) && labels.get(f.name).containsKey(label)) {
            int destLineNum = labels.get(f.name).get(label);

            if (destLineNum < f.lineNumber) {
                loopIntervals.add(new Interval(destLineNum, f.lineNumber));
            }
        }
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> "call"
     * f3 -> Identifier()
     * f4 -> "("
     * f5 -> ( Identifier() )*
     * f6 -> ")"
     */
    @Override
    public void visit(Call n, FunctionStruct f) {
        String resId = n.f0.f0.toString();
        addID(f.name, resId, f.lineNumber);

        String funcId = n.f3.f0.toString();
        addID(f.name, funcId, f.lineNumber);

        n.f5.accept(this, f);
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    @Override
    public void visit(Label n, FunctionStruct f) {
        String label = n.f0.toString();
        addLabel(f.name, label, f.lineNumber);
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    @Override
    public void visit(Identifier n, FunctionStruct f) {
        String id = n.f0.toString();
        addID(f.name, id, f.lineNumber);
    }
}
