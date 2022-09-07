package com.yomahub.liteflow.builder;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.common.LocalDefaultFlowConstant;
import com.yomahub.liteflow.flow.element.condition.Condition;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;

/**
 * WhenCondition基于代码形式的组装器
 * 这个为LiteFlowConditionBuilder的子类，因为when有单独的设置项，所以区分开
 * @author Bryan.Zhang
 * @since 2.6.8
 */
public class LiteFlowWhenConditionBuilder extends LiteFlowConditionBuilder{

    public LiteFlowWhenConditionBuilder(Condition condition) {
        super(condition);
    }

    public LiteFlowWhenConditionBuilder setErrorResume(boolean errorResume){
        WhenCondition whenCondition = (WhenCondition) this.condition;
        whenCondition.setErrorResume(errorResume);
        return this;
    }

    public LiteFlowWhenConditionBuilder setErrorResume(String errorResume){
        if (StrUtil.isBlank(errorResume)){
            return this;
        }
        return setErrorResume(Boolean.parseBoolean(errorResume));
    }

    public LiteFlowWhenConditionBuilder setGroup(String group){
        WhenCondition whenCondition = (WhenCondition) this.condition;
        if (StrUtil.isBlank(group)){
            whenCondition.setGroup(LocalDefaultFlowConstant.DEFAULT);
        }else{
            whenCondition.setGroup(group);
        }
        return this;
    }

    public LiteFlowWhenConditionBuilder setAny(boolean any){
        WhenCondition whenCondition = (WhenCondition) this.condition;
        whenCondition.setAny(any);
        return this;
    }

    public LiteFlowWhenConditionBuilder setAny(String any){
        if (StrUtil.isBlank(any)){
            return this;
        }
        return setAny(Boolean.parseBoolean(any));
    }


    public LiteFlowWhenConditionBuilder setThreadExecutorClass(String executorServiceName){
        WhenCondition whenCondition = (WhenCondition) this.condition;
        if (StrUtil.isBlank(executorServiceName)) {
            return this;
        }
        whenCondition.setThreadExecutorClass(executorServiceName);
        return this;
    }
}
