package com.yomahub.liteflow.util;

/**
 * 三元值对象
 *
 * @author Bryan.Zhang
 * @since 2.13.3
 */
public class TupleOf3<A, B, C> {

    private A a;

    private B b;

    private C c;

    public TupleOf3(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
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

    public C getC() {
        return c;
    }

    public void setC(C c) {
        this.c = c;
    }
}
