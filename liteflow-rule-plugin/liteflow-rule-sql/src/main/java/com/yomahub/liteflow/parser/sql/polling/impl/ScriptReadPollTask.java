package com.yomahub.liteflow.parser.sql.polling.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.sql.polling.AbstractSqlReadPollTask;
import com.yomahub.liteflow.parser.sql.read.SqlRead;
import com.yomahub.liteflow.parser.sql.read.vo.ScriptVO;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 脚本轮询任务
 *
 * @author tangkc
 * @author houxinyu
 * @since 2.11.1
 */
public class ScriptReadPollTask extends AbstractSqlReadPollTask<ScriptVO> {
    public ScriptReadPollTask(SqlRead<ScriptVO> read) {
        super(read);
    }

    @Override
    public void doSave(List<ScriptVO> saveElementList) {
        saveElementList.forEach(scriptVO -> LiteFlowNodeBuilder.createScriptNode()
                .setId(scriptVO.getNodeId())
                .setType(NodeTypeEnum.getEnumByCode(scriptVO.getType()))
                .setName(scriptVO.getName())
                .setScript(scriptVO.getScript())
                .setLanguage(scriptVO.getLanguage())
                .build());
    }

    @Override
    public void doDelete(List<String> deleteElementId) {
        for (String id : deleteElementId) {
            //  删除script
            FlowBus.unloadScriptNode(id);
        }
    }

    @Override
    protected String getKey(ScriptVO scriptVO) {
        return scriptVO.getNodeId();
    }

    @Override
    protected String getValue(ScriptVO scriptVO) {
        return scriptVO.getScript();
    }

    @Override
    protected String getExtValue(ScriptVO scriptVO) {
        return StrUtil.EMPTY;
    }

    @Override
    public ReadType type() {
        return ReadType.SCRIPT;
    }
}
