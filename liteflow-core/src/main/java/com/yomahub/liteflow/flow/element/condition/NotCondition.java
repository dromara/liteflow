package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;

public class NotCondition extends Condition {

    @Override
    public void executeCondition(Integer slotIndex) throws Exception {
        Executable item = this.getItem();

        item.setCurrChainId(this.getCurrChainId());
        item.execute(slotIndex);
        boolean flag = item.getItemResultMetaValue(slotIndex);

        Slot slot = DataBus.getSlot(slotIndex);

        String resultKey = StrUtil.format("{}_{}",this.getClass().getName(),this.hashCode());
        slot.setAndOrResult(resultKey, !flag);
    }


    @Override
    @SuppressWarnings("unchecked")
    public Boolean getItemResultMetaValue(Integer slotIndex) {
        Slot slot = DataBus.getSlot(slotIndex);
        String resultKey = StrUtil.format("{}_{}",this.getClass().getName(),this.hashCode());
        return slot.getAndOrResult(resultKey);
    }

    @Override
    public ConditionTypeEnum getConditionType() {
        return ConditionTypeEnum.TYPE_NOT_OPT;
    }

    public void setItem(Executable item){
        this.addExecutable(ConditionKey.NOT_ITEM_KEY, item);
    }

    public Executable getItem(){
        return this.getExecutableOne(ConditionKey.NOT_ITEM_KEY);
    }

}
