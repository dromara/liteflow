package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.exception.ChainEndException;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.slot.DataBus;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Rain
 * @since 2.12.0
 *
 */
public class RetryCondition extends ThenCondition{

    private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

    private Integer retryTimes;

    private Class<? extends Exception>[] retryForExceptions = new Class[] { Exception.class };

    public Class<? extends Exception>[] getRetryForExceptions() {
        return retryForExceptions;
    }

    public void setRetryForExceptions(Class<? extends Exception>[] retryForExceptions) {
        this.retryForExceptions = retryForExceptions;
    }

    public Integer getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    @Override
    public void executeCondition(Integer slotIndex) throws Exception {
        int retryTimes = this.getRetryTimes() < 0 ? 0 : this.getRetryTimes();
        List<Class<? extends Exception>> forExceptions = Arrays.asList(this.getRetryForExceptions());
        for (int i = 0; i <= retryTimes; i ++) {
            try {
                if(i == 0) {
                    super.executeCondition(slotIndex);
                } else {
                    retry(slotIndex, i);
                }
                break;
            } catch (ChainEndException e) {
                throw e;
            } catch (Exception e) {
                // 判断抛出的异常是不是指定异常的子类
                boolean flag = forExceptions.stream().anyMatch(clazz -> clazz.isAssignableFrom(e.getClass()));
                if(!flag || i >= retryTimes) {
                    if(retryTimes > 0) {
                        String retryFailMsg = StrFormatter.format("retry fail when executing the chain[{}] because {} occurs {}.",
                                this.getCurrChainId(), this.getCurrentExecutableId(), e);
                        LOG.error(retryFailMsg);
                    }
                    throw e;
                } else {
                    DataBus.getSlot(slotIndex).removeException();
                }
            }
        }
    }

    private void retry(Integer slotIndex, int retryTime) throws Exception {
        LOG.info("{} performs {} retry ", this.getCurrentExecutableId(), retryTime);
        super.executeCondition(slotIndex);
    }

    /**
     * 获取当前组件的 id
     *
     * @return
     */
    private String getCurrentExecutableId() {
        // retryCondition 只有一个 Executable
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
