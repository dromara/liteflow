package com.yomahub.liteflow.benchmark.cmp;

import cn.hutool.core.collection.CollectionUtil;
import com.yomahub.liteflow.benchmark.bean.PriceStepVO;
import com.yomahub.liteflow.benchmark.bean.ProductPackVO;
import com.yomahub.liteflow.benchmark.bean.PromotionPackVO;
import com.yomahub.liteflow.benchmark.context.PriceContext;
import com.yomahub.liteflow.benchmark.enums.PriceTypeEnum;
import com.yomahub.liteflow.benchmark.enums.PromotionTypeEnum;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 满减计算组件
 */
@Component("fullCutCmp")
public class FullCutCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        PriceContext slot = this.getContextBean(PriceContext.class);
        PromotionPackVO promotionPack = getMatchPromotion();

        /***这里Mock下根据优惠信息查到的满减信息为：满100，减5块***/
        BigDecimal triggerPrice = new BigDecimal(100);
        BigDecimal cutPrice = new BigDecimal(5);

        //从PromotionPack对象中取到这个优惠关联的商品信息，判断是否超过了触发满减的金额
        BigDecimal reletedProductTotalPrice = new BigDecimal(0);
        for(ProductPackVO productPack : promotionPack.getRelatedProductPackList()){
            reletedProductTotalPrice = reletedProductTotalPrice.add(productPack.getSalePrice().multiply(new BigDecimal(productPack.getCount())));
        }
        if (reletedProductTotalPrice.compareTo(triggerPrice) >= 0){
            BigDecimal prePrice = slot.getLastestPriceStep().getCurrPrice();
            BigDecimal currPrice = prePrice.subtract(cutPrice);

            slot.addPriceStep(new PriceStepVO(PriceTypeEnum.PROMOTION_DISCOUNT,
                    promotionPack.getId().toString(),
                    prePrice,
                    currPrice.subtract(prePrice),
                    currPrice,
                    PriceTypeEnum.PROMOTION_DISCOUNT.getName() + "[满减]"));
        }

    }

    @Override
    public boolean isAccess() {
        //过滤出优惠信息列表中有没有满减这个活动，如果有，则进入这个组件，反义就不进入
        PromotionPackVO promotionPack = getMatchPromotion();
        if(promotionPack != null){
            return true;
        }else{
            return false;
        }
    }

    private PromotionPackVO getMatchPromotion(){
        PriceContext context = this.getContextBean(PriceContext.class);

        List<PromotionPackVO> matchList = context.getPromotionPackList().stream().filter(promotionPackVO -> {
            if(promotionPackVO.getPromotionType().equals(PromotionTypeEnum.FULL_CUT)){
                return true;
            }else{
                return false;
            }
        }).collect(Collectors.toList());

        if(CollectionUtil.isNotEmpty(matchList)){
            return matchList.get(0);
        }else{
            return null;
        }
    }
}
