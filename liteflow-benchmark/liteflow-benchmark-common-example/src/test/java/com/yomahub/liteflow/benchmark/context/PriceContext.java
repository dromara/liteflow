package com.yomahub.liteflow.benchmark.context;

import cn.hutool.core.collection.CollectionUtil;
import com.yomahub.liteflow.benchmark.bean.PriceStepVO;
import com.yomahub.liteflow.benchmark.bean.ProductPackVO;
import com.yomahub.liteflow.benchmark.bean.PromotionPackVO;
import com.yomahub.liteflow.benchmark.enums.OrderChannelEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PriceContext {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 是否境外购
     */
    private boolean oversea;

    /**
     * 商品包
     */
    private List<ProductPackVO> productPackList;

    /**
     * 订单渠道
     */
    private OrderChannelEnum orderChannel;

    /**
     * 会员CODE
     */
    private String memberCode;

    /**
     * 优惠券
     */
    private Long couponId;

    /**
     * 优惠信息
     */
    private List<PromotionPackVO> promotionPackList;

    /**
     * 价格步骤
     */
    private List<PriceStepVO> priceStepList = new ArrayList<>();

    /**
     * 订单原始价格
     */
    private BigDecimal originalOrderPrice;

    /**
     * 订单最终价格
     */
    private BigDecimal finalOrderPrice;

    /**
     * 步骤日志
     */
    private String printLog;

    public PriceStepVO getLastestPriceStep(){
        if(CollectionUtil.isEmpty(priceStepList)){
            return null;
        }else{
            return priceStepList.get(priceStepList.size()-1);
        }
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public List<ProductPackVO> getProductPackList() {
        return productPackList;
    }

    public void setProductPackList(List<ProductPackVO> productPackList) {
        this.productPackList = productPackList;
    }

    public OrderChannelEnum getOrderChannel() {
        return orderChannel;
    }

    public void setOrderChannel(OrderChannelEnum orderChannel) {
        this.orderChannel = orderChannel;
    }

    public String getMemberCode() {
        return memberCode;
    }

    public void setMemberCode(String memberCode) {
        this.memberCode = memberCode;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public List<PriceStepVO> getPriceStepList() {
        return priceStepList;
    }

    public void setPriceStepList(List<PriceStepVO> priceStepList) {
        this.priceStepList = priceStepList;
    }

    public void addPriceStep(PriceStepVO step){
        this.priceStepList.add(step);
    }

    public List<PromotionPackVO> getPromotionPackList() {
        return promotionPackList;
    }

    public void setPromotionPackList(List<PromotionPackVO> promotionPackList) {
        this.promotionPackList = promotionPackList;
    }

    public boolean isOversea() {
        return oversea;
    }

    public void setOversea(boolean oversea) {
        this.oversea = oversea;
    }

    public BigDecimal getFinalOrderPrice() {
        return finalOrderPrice;
    }

    public void setFinalOrderPrice(BigDecimal finalOrderPrice) {
        this.finalOrderPrice = finalOrderPrice;
    }

    public BigDecimal getOriginalOrderPrice() {
        return originalOrderPrice;
    }

    public void setOriginalOrderPrice(BigDecimal originalOrderPrice) {
        this.originalOrderPrice = originalOrderPrice;
    }

    public String getPrintLog() {
        return printLog;
    }

    public void setPrintLog(String printLog) {
        this.printLog = printLog;
    }
}
