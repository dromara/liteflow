package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.collection.CollUtil;
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
import java.util.function.Predicate;

public class AndOrCondition extends Condition {

    private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

    private BooleanConditionTypeEnum booleanConditionType;

    @Override
    public void executeCondition(Integer slotIndex) throws Exception {
        List<Executable> itemList = this.getItem();

        if (CollUtil.isEmpty(itemList)){
            throw new AndOrConditionException("boolean item list is null");
        }

        BooleanConditionTypeEnum booleanConditionType = this.getBooleanConditionType();

        Slot slot = DataBus.getSlot(slotIndex);

        String resultKey = StrUtil.format("{}_{}",this.getClass().getName(),this.hashCode());
        switch (booleanConditionType) {
            case AND:
                slot.setAndOrResult(resultKey, itemList.stream().allMatch(new AndOrConditionPredicate(slotIndex)));
                break;
            case OR:
                slot.setAndOrResult(resultKey, itemList.stream().anyMatch(new AndOrConditionPredicate(slotIndex)));
                break;
            default:
                throw new AndOrConditionException("condition type must be 'AND' or 'OR'");
        }
    }

    private class AndOrConditionPredicate implements Predicate<Executable> {

        private final Integer slotIndex;

        public AndOrConditionPredicate(Integer slotIndex) {
            this.slotIndex = slotIndex;
        }

        @Override
        public boolean test(Executable executable) {
            try {
                executable.setCurrChainId(getCurrChainId());
                executable.execute(slotIndex);
                return executable.getItemResultMetaValue(slotIndex);
            } catch (Exception e) {
                throw new AndOrConditionException(e.getMessage());
            }
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
