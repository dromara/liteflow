package com.yomahub.liteflow.test.processFact.context;

import java.util.Date;

public class User {

    private String name;

    private int age;

    private Date birthday;

    private Company company;

    public User(String name, int age, Date birthday, Company company) {
        this.name = name;
        this.age = age;
        this.birthday = birthday;
        this.company = company;
    }

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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }
}
