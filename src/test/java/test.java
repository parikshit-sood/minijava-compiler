// BasicOverride.mj
class A {
    public int f() {
        return 1;
    }
}

class B extends A {
    public int f() {
        return 2;
    }
}

class Main {
    public static void main(String[] args) {
        A a;
        B b;
        a = new B();
        b = new B();
        System.out.println(a.f()); // Should print 2 (dynamic dispatch)
        System.out.println(b.f()); // Should print 2
    }
}