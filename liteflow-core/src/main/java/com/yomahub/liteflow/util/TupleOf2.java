package com.yomahub.liteflow.util;

public class TupleOf2<A, B> {

    private A a;

    private B b;

    public TupleOf2(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public void setA(A a) {
        this.a = a;
    }

    public void setB(B b) {
        this.b = b;
    }
}
