public class Interval {
    int earliest;
    int latest;

    public Interval(int e, int l) {
        this.earliest = e;
        this.latest = l;
    }

    public int getEarliest() {
        return earliest;
    }

    public int getLatest() {
        return latest;
    }
}
