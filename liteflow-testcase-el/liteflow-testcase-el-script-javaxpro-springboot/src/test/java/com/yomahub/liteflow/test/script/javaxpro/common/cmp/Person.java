package com.yomahub.liteflow.test.script.javaxpro.common.cmp;

public class Person {
    private String name;

    private Integer salary;

    public Person(String name, Integer salary) {
        this.name = name;
        this.salary = salary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSalary() {
        return salary;
    }

    public void setSalary(Integer salary) {
        this.salary = salary;
    }
}
