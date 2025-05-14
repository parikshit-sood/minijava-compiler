class test {
    public static void main(String[] args) {
        int[] a;
        int x;
        boolean b;
        boolean c;
        int d;
        b = true;
        c = false;
        x = 2;

        if (b && (!c)) {
            a = new int[x];
        } else {
            a = new int[3];
        }

        d = 0;

        System.out.println(a.length);
    }
}