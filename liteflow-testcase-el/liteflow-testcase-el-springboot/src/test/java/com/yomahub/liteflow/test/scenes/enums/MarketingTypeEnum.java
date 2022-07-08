package com.yomahub.liteflow.test.scenes.enums;

/**
 * 类型
 * @author nmnl
 */
public enum MarketingTypeEnum implements DescEnum {

    PAY_DISCOUNT("支付折扣活动"),

    ISSUING_COUPONS("发卷活动"),

    MESSAGE_NOTIFICATION("消息通知活动");

    private final String desc;

    @Override
    public String getDesc() {
        return desc;
    }

    MarketingTypeEnum(String desc) {
        this.desc = desc;
    }

}
