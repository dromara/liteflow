package com.yomahub.liteflow.builder;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.common.LocalDefaultFlowConstant;
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

    public LiteFlowWhenConditionBuilder setErrorResume(boolean errorResume){
        this.condition.setErrorResume(errorResume);
        return this;
    }

    public LiteFlowWhenConditionBuilder setErrorResume(String errorResume){
        if (StrUtil.isBlank(errorResume)){
            return this;
        }
        return setErrorResume(Boolean.parseBoolean(errorResume));
    }

    public LiteFlowWhenConditionBuilder setGroup(String group){
        if (StrUtil.isBlank(group)){
            this.condition.setGroup(LocalDefaultFlowConstant.DEFAULT);
        }else{
            this.condition.setGroup(group);
        }
        return this;
    }

    public LiteFlowWhenConditionBuilder setAny(boolean any){
        this.condition.setAny(any);
        return this;
    }

    public LiteFlowWhenConditionBuilder setAny(String any){
        if (StrUtil.isBlank(any)){
            return this;
        }
        return setAny(Boolean.parseBoolean(any));
    }
}
