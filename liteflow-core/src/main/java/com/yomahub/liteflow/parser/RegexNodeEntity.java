package com.yomahub.liteflow.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 节点解析，主要用于解析节点name的重命名
 * @author Bryan.Zhang
 * @since 2.6.2
 */
public class RegexNodeEntity {

    private static final Pattern p = Pattern.compile("[^\\[\\]]+");

    private String id;

    private String tag;

    public static RegexNodeEntity parse(String itemStr){
        List<String> list = new ArrayList<String>();
        Matcher m = p.matcher(itemStr);
        while(m.find()){
            list.add(m.group());
        }

        RegexNodeEntity regexNodeEntity = new RegexNodeEntity();
        regexNodeEntity.setId(list.get(0));
        try{
            regexNodeEntity.setTag(list.get(1));
        }catch (Exception ignored){}
        return regexNodeEntity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
