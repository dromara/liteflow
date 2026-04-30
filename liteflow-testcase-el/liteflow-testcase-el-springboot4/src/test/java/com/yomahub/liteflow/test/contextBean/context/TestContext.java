package com.yomahub.liteflow.test.contextBean.context;

import com.yomahub.liteflow.context.ContextBean;

@ContextBean("skuContext")
public class TestContext {

    protected String skuCode;

    protected String skuName;

    public TestContext() {
    }

    public TestContext(String skuCode, String skuName) {
        this.skuCode = skuCode;
        this.skuName = skuName;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }
}
