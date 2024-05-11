package com.yomahub.liteflow.test.processFact.context;

import com.yomahub.liteflow.context.ContextBean;

@ContextBean("ctx")
public class Demo3Context {

    private String data1;

    private User user;

    public Demo3Context(String data1, User user) {
        this.data1 = data1;
        this.user = user;
    }

    public String getData1() {
        return data1;
    }

    public void setData1(String data1) {
        this.data1 = data1;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
