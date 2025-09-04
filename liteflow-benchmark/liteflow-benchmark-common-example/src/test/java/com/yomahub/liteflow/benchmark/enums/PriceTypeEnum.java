package com.yomahub.liteflow.benchmark.enums;

public enum PriceTypeEnum {
    ORIGINAL(0, "原始价格"),
	MEMBER_DISCOUNT(1, "会员折扣"),
	PROMOTION_DISCOUNT(2, "促销优惠"),
    COUPON_DISCOUNT(3, "优惠券抵扣"),
    POSTAGE(4, "国内运费"),
    OVERSEAS_POSTAGE(5, "海淘运费"),
    POSTAGE_FREE(6, "实付99元免运费");

    private Integer code;

    private String name;

    PriceTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName(){
        return name;
    }
}
