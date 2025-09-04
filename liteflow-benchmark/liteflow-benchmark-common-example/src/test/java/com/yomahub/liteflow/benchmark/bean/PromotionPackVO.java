package com.yomahub.liteflow.benchmark.bean;

import java.util.List;

public class PromotionPackVO extends PromotionInfoVO{

    /**
     * 这个优惠活动关联的商品包
     */
    private List<ProductPackVO> relatedProductPackList;

    public List<ProductPackVO> getRelatedProductPackList() {
        return relatedProductPackList;
    }

    public void setRelatedProductPackList(List<ProductPackVO> relatedProductPackList) {
        this.relatedProductPackList = relatedProductPackList;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null){
            return false;
        }else{
            if(getClass() != obj.getClass()){
                return false;
            }else{
                if(((PromotionPackVO)obj).getId().equals(this.getId())){
                    return true;
                }else{
                    return false;
                }
            }
        }
    }

}
