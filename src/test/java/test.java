class test {
    public static void main(String[] args) {
        A x;
        boolean op1;

        x = new A();
        op1 = false;

        if (op1 && x.foo()) {
            System.out.println(1);
        } else {
            System.out.println(2);
        }
    }
}

class A {
    public boolean foo() {
        System.out.println(3);
        return true;
    }
}

// Shortcircuiting AND expression
// print only 2 if shortcircuited
// else, prints 3 then 2