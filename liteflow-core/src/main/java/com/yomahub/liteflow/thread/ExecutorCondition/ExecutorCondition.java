package com.yomahub.liteflow.thread.ExecutorCondition;


/**
 * <p>Title: ExecutorCondition</p>
 * <p>Description: 执行器条件对象</p>
 *
 * @author jason
 * @since 2.13.0
 */
public class ExecutorCondition {
    private final boolean conditionLevel;
    private final boolean chainLevel;
    private final String conditionExecutorClass;

    private ExecutorCondition(
            boolean conditionLevel,
            boolean chainLevel,
            String conditionExecutorClass) {
        this.conditionLevel = conditionLevel;
        this.chainLevel = chainLevel;
        this.conditionExecutorClass = conditionExecutorClass;
    }

    public static ExecutorCondition create(
            boolean conditionLevel,
            boolean chainLevel,
            String conditionExecutorClass
    ) {
        return new ExecutorCondition(
                conditionLevel,
                chainLevel,
                conditionExecutorClass
        );
    }

    public boolean isConditionLevel() {
        return conditionLevel;
    }

    public boolean isChainLevel() {
        return chainLevel;
    }

    public String getConditionExecutorClass() {
        return conditionExecutorClass;
    }
}