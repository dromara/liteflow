package com.yomahub.liteflow.parser.sql.polling.impl;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.sql.polling.AbstractSqlReadPollTask;
import com.yomahub.liteflow.parser.sql.read.SqlRead;

import java.util.List;
import java.util.Map;

/**
 * chain 读取任务
 *
 * @author tangkc
 * @author houxinyu
 * @since 2.11.1
 */
public class ChainReadPollTask extends AbstractSqlReadPollTask {

    public ChainReadPollTask(SqlRead read) {
        super(read);
    }

    @Override
    public void doSave(Map<String, String> saveElementMap) {
        for (Map.Entry<String, String> entry : saveElementMap.entrySet()) {
            String chainName = entry.getKey();
            String newData = entry.getValue();

            LiteFlowChainELBuilder.createChain().setChainId(chainName).setEL(newData).build();
        }
    }

    @Override
    public void doDelete(List<String> deleteElementId) {
        for (String id : deleteElementId) {
            FlowBus.removeChain(id);
        }
    }

    @Override
    public ReadType type() {
        return ReadType.CHAIN;
    }

}
