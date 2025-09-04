package com.yomahub.liteflow.benchmark.bean;

import com.yomahub.liteflow.benchmark.enums.PromotionTypeEnum;

public class PromotionInfoVO {

    /**
     * id
     */
    private Long id;

    /**
     * 优惠CODE
     */
    private String promotionCode;

    /**
     * 优惠名称
     */
    private String promotionName;

    /**
     * 优惠类型
     */
    private PromotionTypeEnum promotionType;

    public PromotionInfoVO() {
    }

    public PromotionInfoVO(Long id, String promotionCode, String promotionName, PromotionTypeEnum promotionType) {
        this.id = id;
        this.promotionCode = promotionCode;
        this.promotionName = promotionName;
        this.promotionType = promotionType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public PromotionTypeEnum getPromotionType() {
        return promotionType;
    }

    public void setPromotionType(PromotionTypeEnum promotionType) {
        this.promotionType = promotionType;
    }
}
