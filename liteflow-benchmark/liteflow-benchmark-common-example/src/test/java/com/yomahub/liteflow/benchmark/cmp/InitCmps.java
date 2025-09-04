package com.yomahub.liteflow.benchmark.cmp;

import cn.hutool.core.collection.CollectionUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.benchmark.bean.PriceCalcReqVO;
import com.yomahub.liteflow.benchmark.bean.PriceStepVO;
import com.yomahub.liteflow.benchmark.bean.ProductPackVO;
import com.yomahub.liteflow.benchmark.context.PriceContext;
import com.yomahub.liteflow.benchmark.enums.PriceTypeEnum;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

import java.math.BigDecimal;
import java.util.List;

@LiteflowComponent
public class InitCmps {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "checkCmp")
    public void processCheckCmp(NodeComponent bindCmp){
        //拿到请求参数
        PriceCalcReqVO req = bindCmp.getRequestData();
        //参数验证完成
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "slotInitCmp")
    public void processSlotInitCmp(NodeComponent bindCmp){
        //把主要参数冗余到slot里
        PriceCalcReqVO req = bindCmp.getRequestData();
        PriceContext context = bindCmp.getContextBean(PriceContext.class);
        context.setOrderNo(req.getOrderNo());
        context.setOversea(req.isOversea());
        context.setMemberCode(req.getMemberCode());
        context.setOrderChannel(req.getOrderChannel());
        context.setProductPackList(req.getProductPackList());
        context.setCouponId(req.getCouponId());
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.IS_ACCESS, nodeId = "slotInitCmp")
    public boolean accessSlotInitCmp(NodeComponent bindCmp){
        PriceCalcReqVO req = bindCmp.getRequestData();
        if(req != null){
            return true;
        }else{
            return false;
        }
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "priceStepInitCmp")
    public void processPriceStepInitCmp(NodeComponent bindCmp){
        PriceContext context = bindCmp.getContextBean(PriceContext.class);

        //初始化价格步骤
        List<ProductPackVO> packList = context.getProductPackList();
        BigDecimal totalOriginalPrice = new BigDecimal(0);
        for(ProductPackVO packItem : packList){
            totalOriginalPrice = totalOriginalPrice.add(packItem.getSalePrice().multiply(new BigDecimal(packItem.getCount())));
        }
        context.addPriceStep(new PriceStepVO(PriceTypeEnum.ORIGINAL,
                null,
                null,
                totalOriginalPrice,
                totalOriginalPrice,
                PriceTypeEnum.ORIGINAL.getName()));
        context.setOriginalOrderPrice(totalOriginalPrice);
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.IS_ACCESS, nodeId = "priceStepInitCmp")
    public boolean accessPriceStepInitCmp(NodeComponent bindCmp){
        PriceContext context = bindCmp.getContextBean(PriceContext.class);
        if(CollectionUtil.isNotEmpty(context.getProductPackList())){
            return true;
        }else{
            return false;
        }
    }
}
