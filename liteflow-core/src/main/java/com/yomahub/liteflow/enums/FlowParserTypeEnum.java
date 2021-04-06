package com.yomahub.liteflow.enums;

/**
 * @author guodongqing
 * @since 2.5.0
 */
public enum FlowParserTypeEnum {
    TYPE_XML("xml","xml"),
    TYPE_YML("yml","yml"),
    TYPE_JSON("json","json")
            ;
    private String type;
    private String name;

    FlowParserTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
