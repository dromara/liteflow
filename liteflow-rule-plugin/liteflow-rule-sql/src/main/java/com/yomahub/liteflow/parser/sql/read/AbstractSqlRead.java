package com.yomahub.liteflow.parser.sql.read;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.constant.SqlReadConstant;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.read.impl.ScriptRead;
import com.yomahub.liteflow.parser.sql.util.LiteFlowJdbcUtil;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (C), 2021, 北京同创永益科技发展有限公司
 *
 * @author tangkc
 * @version 3.0.0
 * @description
 * @date 2023/9/28 11:26
 */
public abstract class AbstractSqlRead implements SqlRead {
    public final SQLParserVO config;
    private static LFLog LOG = LFLoggerManager.getLogger(AbstractSqlRead.class);

    public AbstractSqlRead(SQLParserVO config) {
        this.config = config;
    }

    @Override
    public Map<String/*规则唯一键*/, String/*规则内容*/> read() {
        // 如果不需要读取直接返回
        if (!needRead()) {
            return new HashMap<>();
        }

        Map<String/*规则唯一键*/, String/*规则*/> result = new HashMap<>();
        String sqlCmd = buildQuerySql();
        if (config.getSqlLogEnabled()) {
            LOG.info("query sql:{}", sqlCmd.replace("?", "'" + config.getApplicationName() + "'"));
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = LiteFlowJdbcUtil.getConn(config);
            stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 设置游标拉取数量
            stmt.setFetchSize(SqlReadConstant.FETCH_SIZE_MAX);
            stmt.setString(1, config.getApplicationName());
            rs = stmt.executeQuery();

            while (rs.next()) {
                String xml = buildXmlElement(rs);
                String uniqueKey = buildXmlElementUniqueKey(rs);

                result.put(uniqueKey, xml);
            }
        } catch (Exception e) {
            throw new ELSQLException(e.getMessage());
        } finally {
            // 关闭连接
            LiteFlowJdbcUtil.close(conn, stmt, rs);
        }

        return result;
    }

    public abstract String buildQuerySql();

    public abstract String buildXmlElement(ResultSet rs) throws SQLException;

    public abstract String buildXmlElementUniqueKey(ResultSet rs) throws SQLException;

    /**
     * 是否可以读取
     * chain 默认可以读取
     * script 需要判断是否有配置
     */
    public boolean needRead() {
        return true;
    }


    public String getStringFromResultSet(ResultSet rs, String field) throws SQLException {
        String data = rs.getString(field);
        if (StrUtil.isBlank(data)) {
            throw new ELSQLException(StrUtil.format("exist {} field value is empty", field));
        }
        return data;
    }
}
