package com.yomahub.liteflow.test.parent.cmp;

public class ParentClass {

    private  String name;

    public void setName(String name) {
        this.name = name;
    }

    public String sayHi() {
        return "hello " + name;
    }
}
