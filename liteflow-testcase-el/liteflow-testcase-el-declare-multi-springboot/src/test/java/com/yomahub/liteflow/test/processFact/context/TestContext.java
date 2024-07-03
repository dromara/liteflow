package com.yomahub.liteflow.test.processFact.context;

import com.yomahub.liteflow.context.ContextBean;

@ContextBean("testCxt")
public class TestContext {

    private User user;

    private String data1;

    public TestContext(User user, String data1) {
        this.user = user;
        this.data1 = data1;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getData1() {
        return data1;
    }

    public void setData1(String data1) {
        this.data1 = data1;
    }
}
