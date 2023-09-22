package com.yomahub.liteflow.parser.sql.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
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
import java.util.Iterator;
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

    private Connection conn;

    private SQLParserVO sqlParserVO;

    private Map<String, String> chainSHAMap;

    private static final Integer FETCH_SIZE_MAX = 1000;

    LFLog LOG = LFLoggerManager.getLogger(ChainPollingTask.class);

    public ChainPollingTask(SQLParserVO sqlParserVO, Map<String, String> chainSHAMap) {
        this.sqlParserVO = sqlParserVO;
        this.chainSHAMap = chainSHAMap;
    }

    @Override
    public void run() {
        conn = LiteFlowJdbcUtil.getConn(sqlParserVO);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try{
            String chainTableName = sqlParserVO.getChainTableName();
            String elDataField = sqlParserVO.getElDataField();
            String chainNameField = sqlParserVO.getChainNameField();
            String chainApplicationNameField = sqlParserVO.getChainApplicationNameField();
            String applicationName = sqlParserVO.getApplicationName();

            String sqlCmd = StrUtil.format(SQL_PATTERN, chainNameField, elDataField, chainTableName,
                    chainApplicationNameField);
            stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 设置游标拉取数量
            stmt.setFetchSize(FETCH_SIZE_MAX);
            stmt.setString(1, applicationName);
            rs = stmt.executeQuery();

            Set<String> newChainSet = new HashSet<>();

            while(rs.next()) {
                String chainName = getStringFromResultSet(rs, chainNameField);
                String newData = getStringFromResultSet(rs, elDataField);
                String newSHA = DigestUtil.sha1Hex(newData);
                newChainSet.add(chainName);
                //如果封装的SHAMap中不存在该chain, 表示该chain为新增
                if(!chainSHAMap.containsKey(chainName)) {
                    //新增chain
                    LiteFlowChainELBuilder.createChain().setChainId(chainName).setEL(newData).build();
                    LOG.info("starting reload flow config... create chain={}, new value={},", chainName, newData);
                    //加入到shaMap
                    chainSHAMap.put(chainName, newSHA);

                }
                else if (!StrUtil.equals(newSHA, chainSHAMap.get(chainName))) {
                    //SHA值发生变化,表示该chain的值已被修改,重新拉取变化的chain
                    //修改chain
                    LiteFlowChainELBuilder.createChain().setChainId(chainName).setEL(newData).build();
                    LOG.info("starting reload flow config... update chain={}, new value={},", chainName, newData);
                    //修改shaMap
                    chainSHAMap.put(chainName, newSHA);
                }
                //SHA值无变化,表示该chain未改变
            }

            if(chainSHAMap.size() > newChainSet.size()) {
                //如果遍历prepareStatement后修改过的SHAMap数量比最新chain总数多, 说明有两种情况：
                // 1、删除了chain
                // 2、修改了chainName:因为遍历到新的name时会加到SHAMap里,但没有机会删除旧的chain
                // 3、上述两者结合
                //在此处遍历chainSHAMap,把不在newChainSet中的chain删除
                //这里用iterator是为避免在遍历集合时删除元素导致ConcurrentModificationException
                Iterator<String> iterator = chainSHAMap.keySet().iterator();
                while(iterator.hasNext()) {
                    String chainName = iterator.next();
                    if(!newChainSet.contains(chainName)) {
                        FlowBus.removeChain(chainName);
                        LOG.info("starting reload flow config... delete chain={}", chainName);
                        //修改SHAMap
                        iterator.remove();
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("[Exception during SQL chain polling] " + e.getMessage(), e);
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
