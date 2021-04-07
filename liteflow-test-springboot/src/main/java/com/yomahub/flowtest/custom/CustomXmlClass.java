package com.yomahub.flowtest.custom;

import com.yomahub.liteflow.parser.ClassXmlFlowParser;

/**
 * @Author: guodongqing
 * @since: 2.5.0
 */
public class CustomXmlClass extends ClassXmlFlowParser {
    @Override
    public String parseCustom() {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<flow>\n" +
                "\t<chain name=\"chain4\">\n" +
                "\t\t<then value=\"a,cond(b|d)\"/>\n" +
                "\t\t<then value=\"e,f,g\"/>\n" +
                "\t</chain>\n" +
                "</flow>";
        return content;
    }
}
