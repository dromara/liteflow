package com.yomahub.liteflow.enums;

import com.yomahub.liteflow.flow.parallel.strategy.AllOfParallelExecutor;
import com.yomahub.liteflow.flow.parallel.strategy.AnyOfParallelExecutor;
import com.yomahub.liteflow.flow.parallel.strategy.ParallelStrategyExecutor;
import com.yomahub.liteflow.flow.parallel.strategy.SpecifyParallelExecutor;

/**
 * 并行策略枚举类
 *
 * @author luo yi
 * @since 2.11.0
 */
public enum ParallelStrategyEnum {

    ANY("anyOf", "完成任一任务", AnyOfParallelExecutor.class),

    ALL("allOf", "完成全部任务", AllOfParallelExecutor.class),

    SPECIFY("must", "完成指定 ID 任务", SpecifyParallelExecutor.class);

    private String strategyType;

    private String description;

    private Class<? extends ParallelStrategyExecutor> clazz;

    ParallelStrategyEnum(String strategyType, String description, Class<? extends ParallelStrategyExecutor> clazz) {
        this.strategyType = strategyType;
        this.description = description;
        this.clazz = clazz;
    }

    public String getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Class<? extends ParallelStrategyExecutor> getClazz() {
        return clazz;
    }

    public void setClazz(Class<? extends ParallelStrategyExecutor> clazz) {
        this.clazz = clazz;
    }
}
