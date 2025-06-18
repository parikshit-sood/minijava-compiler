package S2SV;

import java.util.Comparator;

public class Interval {
    private int first;
    private int last;
    String id;
    boolean isEnd;

    public Interval(int first, int last) {
        this.first = first;
        this.last = last;
    }

    public Interval(int first, int last, String id, boolean isEnd) {
        this.first = first;
        this.last = last;
        this.id = id;
        this.isEnd = isEnd;
    }

    public int getFirst() { return first; }

    public int getLast() { return last; }

    public String getID() { return id; }

    public static Comparator<Interval> comparator = (Interval i1, Interval i2) -> {
        if (i1.isEnd != i2.isEnd) {
            if (!i1.isEnd && i2.isEnd) {
                if (i1.first != i2.last) {
                    return i1.first - i2.last;
                } else {
                    return 1;
                }
            } else {
                if (i1.isEnd && !i2.isEnd) {
                    return i1.last - i2.first;
                } else {
                    return -1;
                }
            }
        } else {
            if (!i1.isEnd) {
                return i1.first - i2.first;
            } else {
                return i1.last - i2.last;
            }
        }
    };
}
