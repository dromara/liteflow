package com.yomahub.liteflow.test.subflow.context;

import cn.hutool.core.collection.ConcurrentHashSet;

public class TestContext {

    private ConcurrentHashSet<String> set = new ConcurrentHashSet<>();

    public void add2Set(String value){
        set.add(value);
    }

    public ConcurrentHashSet<String> getSet(){
        return set;
    }
}
