package com.yomahub.liteflow.benchmark.bean;

import com.yomahub.liteflow.benchmark.enums.OrderChannelEnum;

import java.util.List;

public class PriceCalcReqVO {

    private Long id;

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
     * 优惠券ID
     */
    private Long couponId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isOversea() {
        return oversea;
    }

    public void setOversea(boolean oversea) {
        this.oversea = oversea;
    }
}
