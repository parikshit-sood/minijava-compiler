class test {
    public static void main(String[] args) {
        A x;
        int y;
        x = new A();
        y = x.foo();
    }
}

class A {
    B x;
    public int foo(){
        int y;
        y = x.bar();
        return 1;
    }
}

class B {
    public int bar() {
        return 0;
    }
}