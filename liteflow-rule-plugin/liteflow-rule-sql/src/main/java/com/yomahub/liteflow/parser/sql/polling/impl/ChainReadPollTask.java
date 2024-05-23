package com.yomahub.liteflow.parser.sql.polling.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.sql.polling.AbstractSqlReadPollTask;
import com.yomahub.liteflow.parser.sql.read.SqlRead;
import com.yomahub.liteflow.parser.sql.read.vo.ChainVO;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * chain 读取任务
 *
 * @author tangkc
 * @author houxinyu
 * @since 2.11.1
 */
public class ChainReadPollTask extends AbstractSqlReadPollTask<ChainVO> {

    public ChainReadPollTask(SqlRead<ChainVO> read) {
        super(read);
    }

    @Override
    public void doSave(List<ChainVO> saveElementList) {
        saveElementList.forEach(chainVO ->
                LiteFlowChainELBuilder.createChain().setChainId(chainVO.getChainId())
                .setRoute(chainVO.getRoute())
                .setNamespace(chainVO.getNamespace())
                .setEL(chainVO.getBody())
                .build());
    }

    @Override
    public void doDelete(List<String> deleteElementId) {
        for (String id : deleteElementId) {
            FlowBus.removeChain(id);
        }
    }

    @Override
    protected String getKey(ChainVO chainVO) {
        return chainVO.getChainId();
    }

    @Override
    protected String getValue(ChainVO chainVO) {
        return chainVO.getBody();
    }

    @Override
    protected String getExtValue(ChainVO chainVO) {
        return chainVO.getRoute();
    }

    @Override
    public ReadType type() {
        return ReadType.CHAIN;
    }

}
