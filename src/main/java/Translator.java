import IR.visitor.GJVoidDepthFirst;

import java.util.Map;

public class Translator extends GJVoidDepthFirst<FunctionStruct> {
    Map<String, Map<String, Integer>> uses;   // function -> variable -> last use line number
    Map<String, Map<String, String>> args;  // function -> variable -> "a" register
    Map<String, Map<String, Interval>> liveRange;   // function -> variable -> final liveness range
    Map<String, Map<String, Interval>> argsLiveRange;   // function -> args variable -> final liveness range
    Map<String, Map<String, String>> regs;  // function -> id -> register
    Map<String, Map<String, String>> vars;  // function -> id -> variable
    Map<String, Map<String, String>> linearRegAlloc;    // function -> variable -> register ... FINAL REGISTER ALLOCATION AFTER LINEAR SCANNING

    public Translator(Map<String, Map<String, Integer>> uses, Map<String, Map<String, String>> args, Map<String, Map<String, String>> regs, Map<String, Map<String, String>> vars, Map<String, Map<String, Interval>> liveRange, Map<String, Map<String, Interval>> argsLiveRange, Map<String, Map<String, String>> linearRegAlloc) {
        this.uses = uses;
        this.args = args;
        this.regs = regs;
        this.vars = vars;
        this.liveRange = liveRange;
        this.argsLiveRange = argsLiveRange;
        this.linearRegAlloc = linearRegAlloc;
    }
}
