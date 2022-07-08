package com.yomahub.liteflow.test.scenes.enums;

/**
 * 模式
 * @author mll
 */
public enum MarketingModeEnum implements DescEnum {

    EVENT("事件模式"),

    MANUAL("手动模式"),

    TIMING("定时模式");

    private final String desc;

    @Override
    public String getDesc() {
        return desc;
    }

    MarketingModeEnum(String desc) {
        this.desc = desc;
    }

}
