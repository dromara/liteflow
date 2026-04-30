package com.yomahub.liteflow.test.contextBean.context;

import java.math.BigDecimal;

public class TestSubContext extends TestContext{

    private BigDecimal price;



    public TestSubContext(String skuCode, String skuName, BigDecimal price) {
        super(skuCode, skuName);
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
