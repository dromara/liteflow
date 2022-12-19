package com.yomahub.liteflow.test.parsecustom.bean;

import org.noear.solon.annotation.Component;

@Component
public class TestBean {

    public String returnXmlContent(){
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a,b,c,d)</chain></flow>";
    }
}
