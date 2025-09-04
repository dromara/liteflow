package com.yomahub.liteflow.benchmark.cmp;

import com.yomahub.liteflow.benchmark.bean.PriceStepVO;
import com.yomahub.liteflow.benchmark.bean.ProductPackVO;
import com.yomahub.liteflow.benchmark.context.PriceContext;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;

/**
 * 步骤日志生成组件
 */
@Component("stepPrintCmp")
public class StepPrintCmp extends NodeComponent {

    private LFLog log = LFLoggerManager.getLogger(getClass());

    @Override
    public void process() throws Exception {
        PriceContext context = this.getContextBean(PriceContext.class);
        StringBuilder logStr = new StringBuilder();

        logStr.append(MessageFormat.format("订单号[{0}]的价格计算的明细结果:\n", context.getOrderNo()));
        logStr.append("|====================================================================\n");
        for(ProductPackVO pack : context.getProductPackList()){
            logStr.append(MessageFormat.format("|   {0} [{1}] [{2}]   {3} X {4}\n",
                    pack.getSkuName(),
                    pack.getProductCode(),
                    pack.getSkuCode(),
                    pack.getSalePrice().setScale(2, RoundingMode.HALF_UP).toString(),
                    pack.getCount()));
        }

        logStr.append("|====================================================================\n");
        for(PriceStepVO step : context.getPriceStepList()){
            logStr.append(MessageFormat.format("|   [{0} : {1}]\n",step.getStepDesc(),step.getPriceChange().setScale(2, BigDecimal.ROUND_HALF_UP).toString()));
        }
        logStr.append(MessageFormat.format("|   [最终价 : {0}]\n",context.getFinalOrderPrice().setScale(2, BigDecimal.ROUND_HALF_UP).toString()));
        logStr.append("|====================================================================\n");
        log.info(logStr.toString());
        context.setPrintLog(logStr.toString());
    }

    @Override
    public boolean isAccess() {
        PriceContext context = this.getContextBean(PriceContext.class);
        if(CollectionUtils.isNotEmpty(context.getPriceStepList())){
            return true;
        }else{
            return false;
        }
    }

}
