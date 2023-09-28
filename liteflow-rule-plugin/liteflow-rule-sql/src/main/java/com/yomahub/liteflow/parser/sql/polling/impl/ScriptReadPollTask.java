package com.yomahub.liteflow.parser.sql.polling.impl;

import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.sql.polling.AbstractSqlReadPollTask;
import com.yomahub.liteflow.parser.sql.read.SqlRead;

import java.util.List;
import java.util.Map;

/**
 * 脚本轮询任务
 *
 * @author tangkc huxinyu
 * @date 2023/9/28 11:49
 * @since 2.11.1
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
