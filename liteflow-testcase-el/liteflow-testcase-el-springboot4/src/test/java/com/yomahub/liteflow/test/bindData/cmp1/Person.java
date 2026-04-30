package com.yomahub.liteflow.test.bindData.cmp1;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

public class Person {

    private String name;
    private int age;
    private Date birth1;
    private LocalDate birth2;
    private LocalDateTime birth3;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Date getBirth1() {
        return birth1;
    }

    public void setBirth1(Date birth1) {
        this.birth1 = birth1;
    }

    public LocalDate getBirth2() {
        return birth2;
    }

    public void setBirth2(LocalDate birth2) {
        this.birth2 = birth2;
    }

    public LocalDateTime getBirth3() {
        return birth3;
    }

    public void setBirth3(LocalDateTime birth3) {
        this.birth3 = birth3;
    }
}
