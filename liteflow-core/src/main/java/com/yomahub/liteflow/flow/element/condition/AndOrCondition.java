package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.AndOrConditionException;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import java.util.List;

public class AndOrCondition extends Condition {

    private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

    private BooleanConditionTypeEnum booleanConditionType;

    @Override
    public void executeCondition(Integer slotIndex) throws Exception {
        List<Executable> itemList = this.getItem();


        if (CollUtil.isEmpty(itemList)){
            throw new AndOrConditionException("boolean item list is null");
        }

        boolean[] booleanArray = new boolean[itemList.size()];

        for (int i = 0; i < itemList.size(); i++) {
            Executable item = itemList.get(i);
            item.setCurrChainId(this.getCurrChainId());
            item.execute(slotIndex);
            booleanArray[i] = item.getItemResultMetaValue(slotIndex);
            LOG.info("the result of boolean component [{}] is [{}]", item.getId(), booleanArray[i]);
        }

        BooleanConditionTypeEnum booleanConditionType = this.getBooleanConditionType();

        Slot slot = DataBus.getSlot(slotIndex);

        String resultKey = StrUtil.format("{}_{}",this.getClass().getName(),this.hashCode());
        switch (booleanConditionType) {
            case AND:
                slot.setAndOrResult(resultKey, BooleanUtil.and(booleanArray));
                break;
            case OR:
                slot.setAndOrResult(resultKey, BooleanUtil.or(booleanArray));
                break;
            default:
                throw new AndOrConditionException("condition type must be 'AND' or 'OR'");
        }
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
        return ConditionTypeEnum.TYPE_AND_OR_OPT;
    }

    public void addItem(Executable item){
        this.addExecutable(ConditionKey.AND_OR_ITEM_KEY, item);
    }

    public List<Executable> getItem(){
        return this.getExecutableList(ConditionKey.AND_OR_ITEM_KEY);
    }

    public BooleanConditionTypeEnum getBooleanConditionType() {
        return booleanConditionType;
    }

    public void setBooleanConditionType(BooleanConditionTypeEnum booleanConditionType) {
        this.booleanConditionType = booleanConditionType;
    }
}
