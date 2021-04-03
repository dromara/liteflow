package com.yomahub.liteflow.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: guodongqing
 * @Date: 2021/3/25 4:47 下午
 */
public abstract class FlowParser {

    public abstract void parseMain(String path) throws Exception;

    public abstract void parse(String content) throws Exception ;

    //条件节点的正则解析
    public static RegexEntity parseNodeStr(String str) {
        List<String> list = new ArrayList<String>();
        Pattern p = Pattern.compile("[^\\)\\(]+");
        Matcher m = p.matcher(str);
        while(m.find()){
            list.add(m.group());
        }
        RegexEntity regexEntity = new RegexEntity();
        regexEntity.setItem(list.get(0).trim());
        if(list.size() > 1){
            String[] realNodeArray = list.get(1).split("\\|");
            for (int i = 0; i < realNodeArray.length; i++) {
                realNodeArray[i] = realNodeArray[i].trim();
            }
            regexEntity.setRealItemArray(realNodeArray);
        }
        return regexEntity;
    }

}
