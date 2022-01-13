package com.yomahub.liteflow.builder;

import com.yomahub.liteflow.enums.ConditionTypeEnum;

/**
 * WhenCondition基于代码形式的组装器
 * 这个为LiteFlowConditionBuilder的子类，因为when有单独的设置项，所以区分开
 * @author Bryan.Zhang
 * @since 2.6.8
 */
public class LiteFlowWhenConditionBuilder extends LiteFlowConditionBuilder{

    public LiteFlowWhenConditionBuilder(ConditionTypeEnum conditionType) {
        super(conditionType);
    }

    public LiteFlowConditionBuilder setErrorResume(boolean errorResume){
        this.condition.setErrorResume(errorResume);
        return this;
    }

    public LiteFlowConditionBuilder setGroup(String group){
        this.condition.setGroup(group);
        return this;
    }

    public LiteFlowConditionBuilder setAny(boolean any){
        this.condition.setAny(any);
        return this;
    }
}
