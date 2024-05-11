package com.yomahub.liteflow.test.processFact.context;

public class Company {

    private String name;

    private String address;

    private int headCount;

    public Company(String name, String address, int headCount) {
        this.name = name;
        this.address = address;
        this.headCount = headCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getHeadCount() {
        return headCount;
    }

    public void setHeadCount(int headCount) {
        this.headCount = headCount;
    }
}
