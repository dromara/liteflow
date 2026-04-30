package com.yomahub.liteflow.test.searchContext.context;

import com.yomahub.liteflow.context.ContextBean;

@ContextBean("userCx")
public class UserInfoContext {

    private String info;

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
