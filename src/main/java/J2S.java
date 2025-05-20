
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minijava.MiniJavaParser;
import minijava.ParseException;
import minijava.syntaxtree.Goal;

public class J2S {
    public static void main(String[] args) {
        try {

            Goal root = new MiniJavaParser(System.in).Goal();

            // Build inheritance tree
            ClassCollector collector = new ClassCollector();
            root.accept(collector);

            List<String> sortedClasses = topoSort(collector.allClasses, collector.parentMap);

            // Preprocess class layouts
            TableBuilder tb = new TableBuilder();
            for (String className : sortedClasses) {
                collector.classNodes.get(className).accept(tb);
            }

            // Inherit fields and methods
            InheritanceResolver ir = new InheritanceResolver(tb.getLayouts());
            for (String className : sortedClasses) {
                collector.classNodes.get(className).accept(ir);
            }

            // Generate Sparrow code
            SparrowGenerator codegen = new SparrowGenerator(tb.getLayouts());
            root.accept(codegen);
            System.out.println(codegen.getGeneratedCode());

        } catch (ParseException e) {

            System.out.println("Parse exception: " + e.toString());
            System.exit(1);

        }
    }
    
    // -------------------
    // Helper functions
    // -------------------

    static List<String> topoSort(Set<String> allClasses, Map<String, String> parentMap) {
        List<String> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String c : allClasses) {
            dfs(c, parentMap, visited, sorted);
        }
        return sorted;
    }
    static void dfs(String c, Map<String, String> parentMap, Set<String> visited, List<String> sorted) {
        if (visited.contains(c)) return;
        visited.add(c);
        String parent = parentMap.get(c);
        if (parent != null) dfs(parent, parentMap, visited, sorted);
        sorted.add(c);
    }
}
