package com.yomahub.liteflow.parser.sql.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 用于轮询script的定时任务
 *
 * @author hxinyu
 * @since 2.11.1
 */
public class ScriptPollingTask implements Runnable {

    private static final String SQL_PATTERN = "SELECT {},{} FROM {} WHERE {}=?";

    private static final String NEW_SCRIPT_PATTERN = "SELECT {} FROM {} WHERE {}=? AND {}=?";

    private static final String CONCAT_PATTERN = "CONCAT_WS(':',{},{},{}) as script_concat";

    private static final String CONCAT_WITH_LANGUAGE_PATTERN = "CONCAT_WS(':',{},{},{},{}) as script_concat";

    private static final String SCRIPT_KEY_FIELD = "script_concat";

    private Connection conn;

    private SQLParserVO sqlParserVO;

    private Map<String, String> scriptSHAMap;

    private static final Integer FETCH_SIZE_MAX = 1000;

    LFLog LOG = LFLoggerManager.getLogger(ScriptPollingTask.class);

    public ScriptPollingTask(SQLParserVO sqlParserVO, Map<String, String> scriptSHAMap) {
        this.sqlParserVO = sqlParserVO;
        this.scriptSHAMap = scriptSHAMap;
    }


    @Override
    public void run() {
        conn = LiteFlowJdbcUtil.getConn(sqlParserVO);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String scriptTableName = sqlParserVO.getScriptTableName();
            String scriptIdField = sqlParserVO.getScriptIdField();
            String scriptDataField = sqlParserVO.getScriptDataField();
            String scriptNameField = sqlParserVO.getScriptNameField();
            String scriptTypeField = sqlParserVO.getScriptTypeField();
            String scriptApplicationNameField = sqlParserVO.getScriptApplicationNameField();
            String applicationName = sqlParserVO.getApplicationName();
            String scriptLanguageField = sqlParserVO.getScriptLanguageField();

            String KeyField;
            if (StrUtil.isNotBlank(scriptLanguageField)) {
                KeyField = StrUtil.format(CONCAT_WITH_LANGUAGE_PATTERN, scriptIdField, scriptTypeField, scriptNameField, scriptLanguageField);
            } else {
                KeyField = StrUtil.format(CONCAT_PATTERN, scriptIdField, scriptTypeField, scriptNameField);
            }

            String sqlCmd = StrUtil.format(SQL_PATTERN, KeyField, scriptDataField, scriptTableName, scriptApplicationNameField);
            stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 设置游标拉取数量
            stmt.setFetchSize(FETCH_SIZE_MAX);
            stmt.setString(1, applicationName);
            rs = stmt.executeQuery();

            Set<String> newScriptSet = new HashSet<>();

            while (rs.next()) {
                String scriptKey = getStringFromResultSet(rs, SCRIPT_KEY_FIELD);
                String newData = getStringFromResultSet(rs, scriptDataField);
                String newSHA = DigestUtil.sha1Hex(newData);
                newScriptSet.add(scriptKey);
                //如果封装的SHAMap中不存在该script 表示该script为新增
                if (!scriptSHAMap.containsKey(scriptKey)) {
                    NodeConvertHelper.NodeSimpleVO scriptVO = NodeConvertHelper.convert(scriptKey);
                    //新增script
                    NodeConvertHelper.changeScriptNode(scriptVO, newData);
                    LOG.info("starting reload flow config... create script={}, new value={},", scriptKey, newData);

                    //加入到shaMap
                    scriptSHAMap.put(scriptKey, newSHA);
                }
                else if (!StrUtil.equals(newSHA, scriptSHAMap.get(scriptKey))) {
                    //SHA值发生变化,表示该script的值已被修改,重新拉取变化的script
                    NodeConvertHelper.NodeSimpleVO scriptVO = NodeConvertHelper.convert(scriptKey);
                    //修改script
                    NodeConvertHelper.changeScriptNode(scriptVO, newData);
                    LOG.info("starting reload flow config... update scriptId={}, new value={},", scriptVO.getNodeId(), newData);

                    //修改shaMap
                    scriptSHAMap.put(scriptKey, newSHA);
                }
                //SHA值无变化,表示该chain未改变
            }

            if(scriptSHAMap.size() > newScriptSet.size()) {
                //如果遍历prepareStatement后修改过的SHAMap数量比最新script总数多, 说明有两种情况：
                // 1、删除了script
                // 2、修改了script的id/name/type:因为遍历到新的script_key时会加到SHAMap里,但没有机会删除旧的script
                // 3、上述两者结合
                //在此处遍历scriptSHAMap,把不在newScriptSet中的script删除
                //这里用iterator是为避免在遍历集合时删除元素导致ConcurrentModificationException
                Iterator<String> iterator = scriptSHAMap.keySet().iterator();
                while(iterator.hasNext()){
                    String scriptKey = iterator.next();
                    if (!newScriptSet.contains(scriptKey)) {
                        NodeConvertHelper.NodeSimpleVO scriptVO = NodeConvertHelper.convert(scriptKey);
                        //删除script
                        FlowBus.getNodeMap().remove(scriptVO.getNodeId());
                        LOG.info("starting reload flow config... delete script={}", scriptKey);
                        //修改SHAMap
                        iterator.remove();
                    }
                }
            }

        } catch (Exception e) {
            LOG.error("[Exception during SQL script polling] " + e.getMessage(), e);
        } finally {
            // 关闭连接
            LiteFlowJdbcUtil.close(conn, stmt, rs);
        }
    }

    private String getStringFromResultSet(ResultSet rs, String field) throws SQLException {
        String data = rs.getString(field);
        if (StrUtil.isBlank(data)) {
            throw new ELSQLException(StrUtil.format("exist {} field value is empty", field));
        }
        return data;
    }
}
