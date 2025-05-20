class Main {
    public static void main(String[] args) {
        B b;
        int y;
        b = new B();
        y = b.foo();
    }
}

class B extends A {
    int x;
    public int bar() {
        x = x + 1;
        System.out.println(x);
        return x;
    }
}

class A {
    int x;

    public int foo() {
        System.out.println(x);
        return x;
    }
}