package com.yomahub.liteflow.benchmark.bean;

import com.yomahub.liteflow.benchmark.enums.PriceTypeEnum;

import java.math.BigDecimal;

public class PriceStepVO {

    /**
     * 价格类型
     */
    private PriceTypeEnum priceType;

    /**
     * 价格类型关联id
     */
    private String extId;

    /**
     * 上一步的订单总价格
     */
    private BigDecimal prePrice;

    /**
     * 价格的变动值
     */
    private BigDecimal priceChange;

    /**
     * 这步价格计算后的订单总价格
     */
    private BigDecimal currPrice;

    /**
     * 价格步骤描述
     */
    private String stepDesc;

    public PriceStepVO(PriceTypeEnum priceType, String extId, BigDecimal prePrice, BigDecimal priceChange, BigDecimal currPrice, String stepDesc) {
        this.priceType = priceType;
        this.extId = extId;
        this.prePrice = prePrice;
        this.priceChange = priceChange;
        this.currPrice = currPrice;
        this.stepDesc = stepDesc;
    }

    public PriceTypeEnum getPriceType() {
        return priceType;
    }

    public void setPriceType(PriceTypeEnum priceType) {
        this.priceType = priceType;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public BigDecimal getPrePrice() {
        return prePrice;
    }

    public void setPrePrice(BigDecimal prePrice) {
        this.prePrice = prePrice;
    }

    public BigDecimal getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(BigDecimal priceChange) {
        this.priceChange = priceChange;
    }

    public BigDecimal getCurrPrice() {
        return currPrice;
    }

    public void setCurrPrice(BigDecimal currPrice) {
        this.currPrice = currPrice;
    }

    public String getStepDesc() {
        return stepDesc;
    }

    public void setStepDesc(String stepDesc) {
        this.stepDesc = stepDesc;
    }
}
