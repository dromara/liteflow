package com.yomahub.liteflow.parser.sql.polling.impl;

import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.sql.polling.AbstractSqlReadPollTask;
import com.yomahub.liteflow.parser.sql.read.SqlRead;

import java.util.List;
import java.util.Map;

/**
 * Copyright (C), 2021, 北京同创永益科技发展有限公司
 *
 * @author tangkc
 * @version 3.0.0
 * @description
 * @date 2023/9/28 15:03
 */
public class ScriptReadPollTask extends AbstractSqlReadPollTask {
    public ScriptReadPollTask(Map<String, String> dataMap, SqlRead read) {
        super(dataMap, read);
    }

    @Override
    public void doSave(Map<String, String> saveElementMap) {
        for (Map.Entry<String, String> entry : saveElementMap.entrySet()) {
            String scriptKey = entry.getKey();
            String newData = entry.getValue();

            NodeConvertHelper.NodeSimpleVO scriptVO = NodeConvertHelper.convert(scriptKey);
            NodeConvertHelper.changeScriptNode(scriptVO, newData);
        }
    }

    @Override
    public void doDelete(List<String> deleteElementId) {
        for (String id : deleteElementId) {
            NodeConvertHelper.NodeSimpleVO scriptVO = NodeConvertHelper.convert(id);

            //  删除script
            FlowBus.getNodeMap().remove(scriptVO.getNodeId());
        }
    }

    @Override
    public ReadType type() {
        return ReadType.SCRIPT;
    }
}
