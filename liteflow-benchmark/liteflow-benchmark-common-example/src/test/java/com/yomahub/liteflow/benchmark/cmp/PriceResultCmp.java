package com.yomahub.liteflow.benchmark.cmp;

import cn.hutool.core.collection.CollectionUtil;
import com.yomahub.liteflow.benchmark.bean.PriceStepVO;
import com.yomahub.liteflow.benchmark.context.PriceContext;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单最终价格计算器
 */
@Component("priceResultCmp")
public class PriceResultCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        //算出订单最后的价格，因为priceChange有正负，所以这里统一加起来
        PriceContext context = this.getContextBean(PriceContext.class);
        BigDecimal finalPrice = new BigDecimal(0);
        for(PriceStepVO step : context.getPriceStepList()){
            finalPrice = finalPrice.add(step.getPriceChange());
        }
        context.setFinalOrderPrice(finalPrice);
    }

    @Override
    public boolean isAccess() {
        PriceContext context = this.getContextBean(PriceContext.class);
        if(CollectionUtil.isNotEmpty(context.getPriceStepList())){
            return true;
        }else{
            return false;
        }
    }
}
