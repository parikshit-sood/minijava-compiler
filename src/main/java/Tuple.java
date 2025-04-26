public class Tuple {
    private final String name;
    private final MJType type;

    public Tuple(String name, MJType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public MJType getType() {
        return type;
    }
}
