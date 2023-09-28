package com.yomahub.liteflow.parser.sql.polling.impl;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.helper.ParserHelper;
import com.yomahub.liteflow.parser.sql.polling.AbstractSqlReadPollTask;
import com.yomahub.liteflow.parser.sql.polling.SqlReadPollTask;
import com.yomahub.liteflow.parser.sql.read.SqlRead;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.yomahub.liteflow.common.ChainConstant.ID;
import static com.yomahub.liteflow.common.ChainConstant.NAME;

/**
 * Copyright (C), 2021, 北京同创永益科技发展有限公司
 *
 * @author tangkc
 * @version 3.0.0
 * @description
 * @date 2023/9/28 14:46
 */
public class ChainReadPollTask extends AbstractSqlReadPollTask {

    public ChainReadPollTask(Map<String, String> dataMap, SqlRead read) {
        super(dataMap, read);
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
