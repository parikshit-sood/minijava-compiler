package Typecheck;
import java.util.HashMap;

public class SymbolTable {
    HashMap<String, ClassInfo> classes = new HashMap<>();
    
    public SymbolTable(HashMap<String, ClassInfo> classMap) {
        this.classes = classMap;
    }

    public boolean hasClass(String id) {
        return classes.containsKey(id);
    }

    public ClassInfo getClass(String id) {
        return classes.get(id);
    }

    public boolean isSubtype(MJType lhs, MJType rhs) {
        if (!(lhs.classType() && rhs.classType())) {
            return false;
        }

        String left = lhs.getType();
        String right = rhs.getType();

        while (right != null) {
            if (left.equals(right)) {
                return true;
            }
            right = this.getClass(right).getParent();
        }

        return false;
    }
}

