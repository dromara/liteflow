package com.yomahub.liteflow.parser.sql.util;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 用于轮询chain的定时任务
 *
 * @author hxinyu
 * @since  2.11.1
 */
public class ChainPollingTask implements Runnable {

    private static final String SQL_PATTERN = "SELECT {},{} FROM {} WHERE {}=?";

    private static final String NEW_CHAIN_PATTERN = "SELECT {} FROM {} WHERE {}=? AND {}=?";

    private static final String SHA_PATTERN = "SHA1({})";

    public static Connection conn;

    private SQLParserVO sqlParserVO;

    private Map<String, String> chainSHAMap;

    private static final Integer FETCH_SIZE_MAX = 1000;

    LFLog LOG = LFLoggerManager.getLogger(ChainPollingTask.class);

    public ChainPollingTask(SQLParserVO sqlParserVO, Map<String, String> chainSHAMap) {
        this.sqlParserVO = sqlParserVO;
        this.chainSHAMap = chainSHAMap;
        conn = LiteFlowJdbcUtil.getConn(sqlParserVO);
    }

    @Override
    public void run() {
        try{
            PreparedStatement stmt;
            ResultSet rs;
            String chainTableName = sqlParserVO.getChainTableName();
            String elDataField = sqlParserVO.getElDataField();
            String chainNameField = sqlParserVO.getChainNameField();
            String chainApplicationNameField = sqlParserVO.getChainApplicationNameField();
            String applicationName = sqlParserVO.getApplicationName();

            String SHAField = StrUtil.format(SHA_PATTERN, elDataField);
            String sqlCmd = StrUtil.format(SQL_PATTERN, chainNameField, SHAField, chainTableName,
                    chainApplicationNameField);
            stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 设置游标拉取数量
            stmt.setFetchSize(FETCH_SIZE_MAX);
            stmt.setString(1, applicationName);
            rs = stmt.executeQuery();

            Set<String> newChainSet = new HashSet<>();

            while(rs.next()) {
                String chainName = getStringFromResultSet(rs, chainNameField);
                String newSHA = getStringFromResultSet(rs, SHAField);
                newChainSet.add(chainName);
                //如果封装的SHAMap中不存在该chain, 表示该chain为新增
                if(!chainSHAMap.containsKey(chainName)) {
                    //获取新chain结果
                    ResultSet newChainRS = getNewChainRS(elDataField, chainTableName, chainNameField,
                            chainApplicationNameField, applicationName, chainName);
                    if(newChainRS.next()) {
                        String newELData = getStringFromResultSet(newChainRS, elDataField);
                        //新增chain
                        LiteFlowChainELBuilder.createChain().setChainId(chainName).setEL(newELData).build();
                        LOG.info("starting reload flow config... create key={} new value={},", chainName, newELData);
                        //加入到shaMap
                        chainSHAMap.put(chainName, newSHA);
                    }
                }
                else if (!StrUtil.equals(newSHA, chainSHAMap.get(chainName))) {
                    //SHA值发生变化,表示该chain的值已被修改,重新拉取变化的chain
                    ResultSet newChainRS = getNewChainRS(elDataField, chainTableName, chainNameField,
                            chainApplicationNameField, applicationName, chainName);
                    if(newChainRS.next()) {
                        String newELData = getStringFromResultSet(newChainRS, elDataField);
                        //修改chain
                        LiteFlowChainELBuilder.createChain().setChainId(chainName).setEL(newELData).build();
                        LOG.info("starting reload flow config... update key={} new value={},", chainName, newELData);
                        //修改shaMap
                        chainSHAMap.put(chainName, newSHA);
                    }
                }
                //SHA值无变化,表示该chain未改变
            }

            if(chainSHAMap.size() > newChainSet.size()) {
                //如果遍历prepareStatement后修改过的SHAMap数量比最新chain总数多, 说明有两种情况：
                // 1、删除了chain
                // 2、修改了chainName:因为遍历到新的name时会加到SHAMap里,但没有机会删除旧的chain
                // 3、上述两者结合
                //在此处遍历chainSHAMap,把不在newChainSet中的chain删除
                for (String chainName : chainSHAMap.keySet()) {
                    if(!newChainSet.contains(chainName)){
                        FlowBus.removeChain(chainName);
                        LOG.info("starting reload flow config... delete chain={}", chainName);
                        //修改SHAMap
                        chainSHAMap.remove(chainName);
                    }
                }
            }

        }catch (Exception e) {
            LOG.error("[Exception during SQL chain polling] " + e.getMessage(), e);
        }
    }

    private ResultSet getNewChainRS(String elDataField, String chainTableName, String chainNameField,
                                    String chainApplicationNameField, String applicationName, String chainName) {
        ResultSet rs = null;
        String sqlCmd = StrUtil.format(NEW_CHAIN_PATTERN, elDataField, chainTableName,
                chainNameField, chainApplicationNameField);
        try{
            PreparedStatement stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1, chainName);
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
}
