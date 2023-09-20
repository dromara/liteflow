package com.yomahub.liteflow.parser.sql.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
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

    private static final String SHA_PATTERN = "SHA1({}) AS SHA1";

    private static final String SHA_PATTERN_FOR_H2 = "RAWTOHEX(HASH('SHA-1', {})) AS SHA1";

    private static final String SHA_FIELD_NAME = "SHA1";

    private static final String SCRIPT_KEY_FIELD = "script_concat";

    public static Connection conn;

    private SQLParserVO sqlParserVO;

    private Map<String, String> scriptSHAMap;

    private static final Integer FETCH_SIZE_MAX = 1000;

    LFLog LOG = LFLoggerManager.getLogger(ScriptPollingTask.class);

    public ScriptPollingTask(SQLParserVO sqlParserVO, Map<String, String> scriptSHAMap) {
        this.sqlParserVO = sqlParserVO;
        this.scriptSHAMap = scriptSHAMap;
        conn = LiteFlowJdbcUtil.getConn(sqlParserVO);
    }


    @Override
    public void run() {
        try {
            String scriptTableName = sqlParserVO.getScriptTableName();
            String scriptIdField = sqlParserVO.getScriptIdField();
            String scriptDataField = sqlParserVO.getScriptDataField();
            String scriptNameField = sqlParserVO.getScriptNameField();
            String scriptTypeField = sqlParserVO.getScriptTypeField();
            String scriptApplicationNameField = sqlParserVO.getScriptApplicationNameField();
            String applicationName = sqlParserVO.getApplicationName();
            String scriptLanguageField = sqlParserVO.getScriptLanguageField();

            String SHAField = StrUtil.format(SHA_PATTERN, scriptDataField);
            //h2数据库计算SHA的函数与MySQL不同
            if(StrUtil.equals(sqlParserVO.getDriverClassName(), "org.h2.Driver")){
                SHAField = StrUtil.format(SHA_PATTERN_FOR_H2, scriptDataField);
            }

            String KeyField;
            if (StrUtil.isNotBlank(scriptLanguageField)) {
                KeyField = StrUtil.format(CONCAT_WITH_LANGUAGE_PATTERN, scriptIdField, scriptTypeField, scriptNameField, scriptLanguageField);
            } else {
                KeyField = StrUtil.format(CONCAT_PATTERN, scriptIdField, scriptTypeField, scriptNameField);
            }

            String sqlCmd = StrUtil.format(SQL_PATTERN, KeyField, SHAField, scriptTableName, scriptApplicationNameField);
            PreparedStatement stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 设置游标拉取数量
            stmt.setFetchSize(FETCH_SIZE_MAX);
            stmt.setString(1, applicationName);
            ResultSet rs = stmt.executeQuery();

            Set<String> newScriptSet = new HashSet<>();

            while (rs.next()) {
                String scriptKey = getStringFromResultSet(rs, SCRIPT_KEY_FIELD);
                String newSHA = getStringFromResultSet(rs, SHA_FIELD_NAME);
                newScriptSet.add(scriptKey);
                //如果封装的SHAMap中不存在该script 表示该script为新增
                if (!scriptSHAMap.containsKey(scriptKey)) {
                    //获取新script内容
                    NodeSimpleVO scriptVO = convert(scriptKey);
                    ResultSet newScriptRS = getNewScriptRS(scriptDataField, scriptTableName, scriptIdField,
                            scriptVO.getNodeId(), scriptApplicationNameField, applicationName);
                    if(newScriptRS.next()) {
                        String newScriptData = getStringFromResultSet(newScriptRS, scriptDataField);
                        //新增script
                        changeScriptNode(scriptVO, newScriptData);
                        LOG.info("starting reload flow config... create script={}, new value={},", scriptKey, newScriptData);
                    }
                    //加入到shaMap
                    scriptSHAMap.put(scriptKey, newSHA);
                }
                else if (!StrUtil.equals(newSHA, scriptSHAMap.get(scriptKey))) {
                    //SHA值发生变化,表示该script的值已被修改,重新拉取变化的script
                    //获取新script内容
                    NodeSimpleVO scriptVO = convert(scriptKey);
                    ResultSet newScriptRS = getNewScriptRS(scriptDataField, scriptTableName, scriptIdField,
                            scriptVO.getNodeId(), scriptApplicationNameField, applicationName);
                    if(newScriptRS.next()) {
                        String newScriptData = getStringFromResultSet(newScriptRS, scriptDataField);
                        //修改script
                        changeScriptNode(scriptVO, newScriptData);
                        LOG.info("starting reload flow config... update scriptId={}, new value={},", scriptVO.getNodeId(), newScriptData);
                    }
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
                        NodeSimpleVO scriptVO = convert(scriptKey);
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
        }
    }

    private ResultSet getNewScriptRS(String scriptDataField, String scriptTableName, String scriptIdField,
                                     String scriptId, String scriptApplicationNameField, String applicationName) {
        ResultSet rs = null;
        String sqlCmd = StrUtil.format(NEW_SCRIPT_PATTERN, scriptDataField, scriptTableName,
                scriptIdField, scriptApplicationNameField);
        try{
            PreparedStatement stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1, scriptId);
            stmt.setString(2, applicationName);
            rs = stmt.executeQuery();
        }catch (Exception e) {
            throw new ELSQLException(e.getMessage());
        }
        return rs;
    }

    private String getStringFromResultSet(ResultSet rs, String field) throws SQLException {
        String data = rs.getString(field);
        if (StrUtil.isBlank(data)) {
            throw new ELSQLException(StrUtil.format("exist {} field value is empty", field));
        }
        return data;
    }

    /*script节点的修改/添加*/
    private void changeScriptNode(NodeSimpleVO nodeSimpleVO, String newValue) {
        // 有语言类型
        if (StrUtil.isNotBlank(nodeSimpleVO.getLanguage())) {
            LiteFlowNodeBuilder.createScriptNode()
                    .setId(nodeSimpleVO.getNodeId())
                    .setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
                    .setName(nodeSimpleVO.getName())
                    .setScript(newValue)
                    .setLanguage(nodeSimpleVO.getLanguage())
                    .build();
        }
        // 没有语言类型
        else {
            LiteFlowNodeBuilder.createScriptNode()
                    .setId(nodeSimpleVO.getNodeId())
                    .setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
                    .setName(nodeSimpleVO.getName())
                    .setScript(newValue)
                    .build();
        }
    }

    private NodeSimpleVO convert(String scriptKey){
        List<String> matchItemList = ReUtil.findAllGroup0("(?<=[^:]:)[^:]+|[^:]+(?=:[^:])", scriptKey);
        if (CollUtil.isEmpty(matchItemList)) {
            return null;
        }
        NodeSimpleVO nodeSimpleVO = new NodeSimpleVO();
        if (matchItemList.size() > 1) {
            nodeSimpleVO.setNodeId(matchItemList.get(0));
            nodeSimpleVO.setType(matchItemList.get(1));
        }

        if (matchItemList.size() > 2) {
            nodeSimpleVO.setName(matchItemList.get(2));
        }

        if (matchItemList.size() > 3) {
            nodeSimpleVO.setLanguage(matchItemList.get(3));
        }

        return nodeSimpleVO;
    }

    class NodeSimpleVO {

        private String nodeId;

        private String type;

        private String name = StrUtil.EMPTY;

        private String language;

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }
}
