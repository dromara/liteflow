package com.yomahub.liteflow.test.executor;

import com.yomahub.liteflow.entity.data.AbsSlot;

public class CustomSlot extends AbsSlot {
    private String name;
    
    public String getName() {
        return name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
}
