import java.util.*;

import IR.syntaxtree.*;
import IR.visitor.GJVoidDepthFirst;

public class IntervalVisitor extends GJVoidDepthFirst<FunctionStruct>{
    Map<String, Map<String, Integer>> defs;   // function -> variable -> first def line number
    Map<String, Map<String, Integer>> uses;   // function -> variable -> last use line number
    Map<String, Map<String, String>> args;  // function -> variable -> "a" register
    Map<String, Map<String, Interval>> argsIntervals;   // function -> variable -> liveness interval
    Map<String, Map<String, Interval>> liveRange;   // function -> variable -> final liveness range
    Map<String, Map<String, Interval>> argsLiveRange;   // function -> args variable -> final liveness range
    Map<String, Map<String, String>> regs;  // function -> id -> register
    Map<String, Map<String, String>> vars;  // function -> id -> variable
    Map<String, Map<String, Integer>> labels;   // function -> label -> line number
    Map<String, Map<String, String>> linearRegAlloc;    // function -> variable -> register ... FINAL REGISTER ALLOCATION AFTER LINEAR SCANNING
    List<Interval> loopArr;
    int lineNum;

    public IntervalVisitor(Map<String, Map<String, String>> argsProcessed) {
        this.defs = new HashMap<>();
        this.uses = new HashMap<>();
        this.argsIntervals = new HashMap<>();
        this.liveRange = new HashMap<>();
        this.argsLiveRange = new HashMap<>();
        this.regs = new HashMap<>();
        this.vars = new HashMap<>();
        this.labels = new HashMap<>();
        this.loopArr = new ArrayList<>();
        this.linearRegAlloc = new HashMap<>();
        this.args = argsProcessed;
        this.lineNum = 1;
    }

    // ---------------------
    // Helper functions
    // ---------------------

    /**
     * Check liveness information for variable in all registers, update as needed
     *
     * @param id : Name of variable
     * @param funcName : Name of function
     * @param lineNum : Line number of variable definition or usage
     */
    private void upsertId(String id, String funcName, int lineNum) {
        // Check if id an argument, live in "a" registers
        if (args.get(funcName).containsKey(id)) {
            if (!argsIntervals.get(funcName).containsKey(id)) {
                // First instance of an argument, set interval bounds
                argsIntervals.get(funcName).put(id, new Interval(lineNum, lineNum));
            } else {
                // Calculate new interval for this id
                int earliest = Math.min(lineNum, argsIntervals.get(funcName).get(id).getStart());
                int latest = Math.max(lineNum, argsIntervals.get(funcName).get(id).getEnd());

                // Update interval mapping for id
                argsIntervals.get(funcName).put(id, new Interval(earliest, latest));
            }
            return;
        }

        // Not an argument, update the def and use maps for id
        upsertDef(id, funcName, lineNum);
        upsertUse(id, funcName, lineNum);
    }

    /**
     * Update the earliest line number for variable definition in the function
     *
     * @param id : Name of variable
     * @param funcName : Name of function
     * @param lineNum : Line number of variable definition
     */
    private void upsertDef(String id, String funcName, int lineNum) {
        // Check if first instance of this function
        if (!defs.containsKey(funcName)) {
            defs.put(funcName, new HashMap<>());
        }

        // Upsert the earliest line number for this id definition
        if (defs.get(funcName).containsKey(id)) {
            int earliest = Math.min(lineNum, defs.get(funcName).get(id));
            defs.get(funcName).put(id, earliest);
        } else {
            defs.get(funcName).put(id, lineNum);
        }
    }

    /**
     * Update the latest line number for variable usage in the function
     *
     * @param id : Name of variable
     * @param funcName : Name of function
     * @param lineNum : Line number of variable use
     */
    private void upsertUse(String id, String funcName, int lineNum) {
        // Check if first instance of this function
        if (!uses.containsKey(funcName)) {
            uses.put(funcName, new HashMap<>());
        }

        // Upsert the latest line number where this id is used
        if (uses.get(funcName).containsKey(id)) {
            int latest = Math.max(lineNum, uses.get(funcName).get(id));
            uses.get(funcName).put(id, latest);
        } else {
            uses.get(funcName).put(id, lineNum);
        }
    }

    /**
     * Add line number for each label in function
     *
     * @param label : Name of label
     * @param funcName : Name of function
     * @param lineNum : Line number of label
     */
    private void addLabel(String label, String funcName, int lineNum) {
        // Check if first instance of this function
        if (!labels.containsKey(funcName)) {
            labels.put(funcName, new HashMap<>());
        }

        // Map line number of label
        labels.get(funcName).put(label, lineNum);
    }

    // ---------------------
    // Core functions
    // ---------------------

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

        // Clear loop array
        loopArr.clear();

        // Track registers and variables for this function
        regs.put(f.name, new HashMap<>());
        vars.put(f.name, new HashMap<>());

        // Track argument liveness intervals for this function
        argsIntervals.put(f.name, new HashMap<>());

        // Add preprocessed a2-a7 with line 0
        if (n.f3.present()) {
            while (n.f3.elements().hasMoreElements()) {
                String paramName = ((Identifier)n.f3.elements()).f0.toString();
                upsertId(paramName, f.name, 0);
            }
        }

        // Process function statements
        n.f5.accept(this, f);
        lineNum = 1;

        // Do liveness analysis to calculate final ranges
        Map<String, Interval> range = new HashMap<>();
        Map<String, Interval> argsRange = new HashMap<>();
        List<Interval> intervals = new ArrayList<>();

        for (String id : defs.get(f.name).keySet()) {
            int start = defs.get(f.name).get(id);
            int end = uses.get(f.name).get(id);

            for (Interval i : loopArr) {
                if ((i.getStart() <= start && i.getEnd() >= start) || (i.getStart() <= end && i.getEnd() >= end)) {
                    start = Math.min(start, i.getStart());
                    end = Math.max(end, i.getEnd());
                }
            }

            intervals.add(new Interval(start, -1, id, 0));
            intervals.add(new Interval(-1, end, id, 1));
            range.put(id, new Interval(start, end));
        }

        // Liveness analysis for arguments
        for (String id : argsIntervals.get(f.name).keySet()) {
            int start = argsIntervals.get(f.name).get(id).getStart();
            int end = argsIntervals.get(f.name).get(id).getEnd();

            for (Interval i : loopArr) {
                if ((i.getStart() <= start && i.getEnd() >= start) || (i.getStart() <= end && i.getEnd() >= end)) {
                    start = Math.min(start, i.getStart());
                    end = Math.max(end, i.getEnd());
                }
            }

            argsRange.put(id, new Interval(start, end));
        }

        liveRange.put(f.name, range);
        argsLiveRange.put(f.name, argsRange);

        // Sort live ranges intervals by starting line
        intervals.sort(Interval.comparator);

        Deque<String> availableRegs = new ArrayDeque<>(
                Arrays.asList("t0", "t1", "t2", "t3", "t4", "t5", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11"));

        Map<String, String> tempAssignment = new HashMap<>();
        Map<String, String> assignment = new HashMap<>();

        for (Interval i : intervals) {
            if (i.type == 1) {
                // Process end of liveness interval for variable id
                if (tempAssignment.containsKey(i.id)) {
                    // Transfer temp assignment into permanent assignment, free allocated register
                    assignment.put(i.id, tempAssignment.get(i.id));
                    availableRegs.add(tempAssignment.get(i.id));
                    tempAssignment.remove(i.id);
                }
            } else {
                if (!availableRegs.isEmpty()) {
                    // Greedily allocate temp register to id
                    tempAssignment.put(i.id, availableRegs.poll());
                } else {
                    // Find longest temp assignment and spill that into memory
                    int otherEnd = range.get(i.id).getEnd();
                    String otherId = i.id;

                    for (String other : tempAssignment.keySet()) {
                        int compEnd = range.get(other).getEnd();
                        if (compEnd > otherEnd) {
                            otherEnd = compEnd;
                            otherId = other;
                        }
                    }

                    if (!otherId.equals(i.id)) {
                        String otherReg = tempAssignment.get(otherId);
                        tempAssignment.remove(otherId);
                        tempAssignment.put(i.id, otherReg);
                    }
                }
            }
        }

        // Make remaining temp assignments permanent and store register allocations for current function
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
        f.lineNumber = lineNum;
        lineNum++;
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
        upsertId(id, f.name, f.lineNumber);
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
        upsertId(id, f.name, f.lineNumber);
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
        upsertId(lhs, f.name, f.lineNumber);

        String op1 = n.f2.f0.toString();
        upsertId(op1, f.name, f.lineNumber);

        String op2 = n.f4.f0.toString();
        upsertId(op2, f.name, f.lineNumber);
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
        upsertId(lhs, f.name, f.lineNumber);

        String op1 = n.f2.f0.toString();
        upsertId(op1, f.name, f.lineNumber);

        String op2 = n.f4.f0.toString();
        upsertId(op2, f.name, f.lineNumber);
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
        upsertId(lhs, f.name, f.lineNumber);

        String op1 = n.f2.f0.toString();
        upsertId(op1, f.name, f.lineNumber);

        String op2 = n.f4.f0.toString();
        upsertId(op2, f.name, f.lineNumber);
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
        upsertId(lhs, f.name, f.lineNumber);

        String op1 = n.f2.f0.toString();
        upsertId(op1, f.name, f.lineNumber);

        String op2 = n.f4.f0.toString();
        upsertId(op2, f.name, f.lineNumber);
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
        upsertId(lhs, f.name, f.lineNumber);

        String base = n.f3.f0.toString();
        upsertId(base, f.name, f.lineNumber);
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
        upsertId(lhs, f.name, f.lineNumber);

        String val = n.f6.f0.toString();
        upsertId(val, f.name, f.lineNumber);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Identifier()
     */
    @Override
    public void visit(Move n, FunctionStruct f) {
        String lhs = n.f0.f0.toString();
        upsertId(lhs, f.name, f.lineNumber);

        String rhs = n.f2.f0.toString();
        upsertId(rhs, f.name, f.lineNumber);
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
        upsertId(lhs, f.name, f.lineNumber);

        String bytes = n.f4.f0.toString();
        upsertId(bytes, f.name, f.lineNumber);
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
        upsertId(id, f.name, f.lineNumber);
    }

    /**
     * f0 -> "goto"
     * f1 -> Label()
     */
    @Override
    public void visit(Goto n, FunctionStruct f) {
        String label = n.f1.f0.toString();

        if (labels.containsKey(f.name) && labels.get(f.name).containsKey(label)) {
            int gotoDest = labels.get(f.name).get(label);
            if (gotoDest < f.lineNumber) {
                loopArr.add(new Interval(gotoDest, f.lineNumber));
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
        String cond = n.f1.f0.toString();
        upsertId(cond, f.name, f.lineNumber);

        String label = n.f3.f0.toString();
        if (labels.containsKey(f.name) && labels.get(f.name).containsKey(label)) {
            int gotoDest = labels.get(f.name).get(label);
            if (gotoDest < f.lineNumber) {
                loopArr.add(new Interval(gotoDest, f.lineNumber));
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
        String res = n.f0.f0.toString();
        upsertId(res, f.name, f.lineNumber);

        String func = n.f3.f0.toString();
        upsertId(func, f.name, f.lineNumber);

        n.f5.accept(this, f);
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    @Override
    public void visit(Label n, FunctionStruct f) {
        String name = n.f0.toString();
        addLabel(name, f.name, f.lineNumber);
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    @Override
    public void visit(Identifier n, FunctionStruct f) {
        String name = n.f0.toString();
        upsertId(name, f.name, f.lineNumber);
    }
}
