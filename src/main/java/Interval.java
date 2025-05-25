import java.util.Comparator;

public class Interval {
    int start;      // start line number
    int end;        // end line number
    String id;      // variable id
    int type;       // 0 = start, 1 = end

    public Interval(int s, int e) {
        this.start = s;
        this.end = e;
    }

    public Interval(int s, int e, String id, int type) {
        this.start = s;
        this.end = e;
        this.id = id;
        this.type = type;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public static Comparator<Interval> comparator = new Comparator<Interval>() {
        @Override
        public int compare(Interval o1, Interval o2) {
            if (o1.type != o2.type) {
                if (o1.type == 0 && o2.type == 1) {
                    if (o1.start != o2.end) {
                        return o1.start - o2.end;
                    } else {
                        return 1;
                    }
                } else {
                    if (o1.end != o2.start) {
                        return o1.end - o2.start;
                    } else {
                        return -1;
                    }
                }
            } else {
                if (o1.type == 0) {
                    return o1.start - o2.start;
                } else {
                    return o1.end - o2.end;
                }
            }
        }
    };
}
