/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.flow;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.enums.ExecuteTypeEnum;
import com.yomahub.liteflow.exception.ChainEndException;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.util.SpringAware;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Chain implements Executable {

    private static final Logger LOG = LoggerFactory.getLogger(Chain.class);

    private String chainName;

    private List<Condition> conditionList;

    private static int whenMaxWaitSeconds;

    static {
        LiteflowConfig liteflowConfig = SpringAware.getBean(LiteflowConfig.class);
        if (ObjectUtil.isNotNull(liteflowConfig)) {
            whenMaxWaitSeconds = liteflowConfig.getWhenMaxWaitSecond();
        } else {
            whenMaxWaitSeconds = 15;
        }
    }

    public Chain(String chainName, List<Condition> conditionList) {
        this.chainName = chainName;
        this.conditionList = conditionList;
    }

    public List<Condition> getConditionList() {
        return conditionList;
    }

    public void setConditionList(List<Condition> conditionList) {
        this.conditionList = conditionList;
    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    @Override
    public void execute(Integer slotIndex) throws Exception {
        if (CollectionUtils.isEmpty(conditionList)) {
            throw new FlowSystemException("no conditionList in this chain[" + chainName + "]");
        }

        Slot slot = DataBus.getSlot(slotIndex);

        for (Condition condition : conditionList) {
            if (condition instanceof ThenCondition) {
                for (Executable executableItem : condition.getNodeList()) {
                    try {
                        executableItem.execute(slotIndex);
                    } catch (Exception e) {
                        throw e;
                    }
                }
            } else if (condition instanceof WhenCondition) {
                final CountDownLatch latch = new CountDownLatch(condition.getNodeList().size());
                for (Executable executableItem : condition.getNodeList()) {
                    new WhenConditionThread(executableItem, slotIndex, slot.getRequestId(), latch).start();
                }
                latch.await(whenMaxWaitSeconds, TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public ExecuteTypeEnum getExecuteType() {
        return ExecuteTypeEnum.CHAIN;
    }

    @Override
    public String getExecuteName() {
        return chainName;
    }
}
