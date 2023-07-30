package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.exception.WhenTimeoutException;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;

import java.util.concurrent.TimeoutException;

/**
 * 超时控制 Condition
 *
 * @author DaleLee
 * @since 2.11.0
 */
public class TimeoutCondition extends WhenCondition {

    @Override
    public void executeCondition(Integer slotIndex) throws Exception {
        try {
            super.executeCondition(slotIndex);
        } catch (WhenTimeoutException ex) {
            // 将 WhenTimeoutException 转换为 TimeoutException
            String errMsg = StrFormatter.format("Timed out when executing the chain [{}] because [{}] exceeded {} {}.",
                    this.getCurrChainId(), this.getCurrentExecutableId(), this.getMaxWaitTime(), this.getMaxWaitTimeUnit().toString().toLowerCase());
            throw new TimeoutException(errMsg);
        }
    }

    /**
     * 获取当前组件的 id
     *
     * @return
     */
    private String getCurrentExecutableId() {
        // TimeoutCondition 只有一个 Executable
        Executable executable = this.getExecutableList().get(0);
        if (ObjectUtil.isNotNull(executable.getId())) {
            // 已经有 id 了
            return executable.getId();
        }
        // 定义 id
        switch (executable.getExecuteType()) {
            // chain 和 node 一般都有 id
            case CHAIN:
                return ((Chain) executable).getChainId();
            case CONDITION:
                return "condition-" + ((Condition) executable).getConditionType().getName();
            case NODE:
                return "node-" + ((Node) executable).getType().getCode();
            default:
                return "unknown-executable";
        }
    }
}
