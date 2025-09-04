/**
 * <p>Title: beast-price</p>
 * <p>Description: 价格计算服务</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @Date 2017-11-27
 */
package com.yomahub.liteflow.benchmark.enums;

/**
 * 商品来源枚举
 */
public enum SkuSourceEnum {
	RAW(0, "自购"),
	GIFT(3, "买赠"),
	ADDITION(4,"换购"),
	BENEFIT(5,"权益商品");

    private Integer code;

    private String name;

    SkuSourceEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
