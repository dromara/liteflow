package com.yomahub.liteflow.parser.sql.read;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.constant.SqlReadConstant;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.util.LiteFlowJdbcUtil;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * sql 读取抽象类，维护公共方法
 *
 * @author tangkc
 * @author houxinyu
 * @author Bryan.Zhang
 * @author Jay li
 * @since 2.11.1
 */
public abstract class AbstractSqlRead<T> implements SqlRead<T> {
    public final SQLParserVO config;
    private static LFLog LOG = LFLoggerManager.getLogger(AbstractSqlRead.class);

    public AbstractSqlRead(SQLParserVO config) {
        this.config = config;
    }

    @Override
    public List<T> read(String chainId) {
        if (!needRead()) {
            return new ArrayList<>();
        }

        checkConfig();
        String sqlCmd = buildQuerySql(chainId);
        return readList(sqlCmd);
    }


    @Override
    public List<T> read() {
        // 如果不需要读取直接返回
        if (!needRead()) {
            return new ArrayList<>();
        }

        checkConfig();
        String sqlCmd = buildQuerySql();

        return readList(sqlCmd);
    }


    private List<T> readList(String sqlCmd) {
        // 如果允许，就打印 sql 语句
        logSqlIfEnable(sqlCmd);

        List<T> result = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = LiteFlowJdbcUtil.getConn(config);
            stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 设置游标拉取数量
            stmt.setFetchSize(SqlReadConstant.FETCH_SIZE_MAX);

            rs = stmt.executeQuery();

            while (rs.next()) {
                if (hasEnableFiled()){
                    boolean enable = getEnableFiledValue(rs);
                    // 如果停用，直接跳过
                    if (!enable){
                        continue;
                    }
                }
                result.add(parse(rs));
            }
        } catch (Exception e) {
            throw new ELSQLException(e);
        } finally {
            // 关闭连接
            LiteFlowJdbcUtil.close(conn, stmt, rs);
        }

        return result;
    }

    protected abstract T parse(ResultSet rs) throws SQLException;

    /**
     * 是否包含启停字段
     */
    public abstract boolean hasEnableFiled();

    /**
     * 获取启停字段对应的字段值
     */
    public abstract boolean getEnableFiledValue(ResultSet rs) throws SQLException;

    public abstract String buildQuerySql();

    public abstract String buildQuerySql(String chainId);

    public abstract void checkConfig();

    /**
     * 是否可以读取
     * chain 默认可以读取
     * script 需要判断是否有配置
     *
     * @return 布尔值
     */
    public boolean needRead() {
        return true;
    }


    public String getStringFromRs(ResultSet rs, String field) throws SQLException {
        return rs.getString(field);
    }

    public String getStringFromRsWithCheck(ResultSet rs, String field) throws SQLException {
        String data = getStringFromRs(rs, field);
        if (StrUtil.isBlank(data)) {
            throw new ELSQLException(StrUtil.format("field[{}] value is empty", field));
        }
        return data;
    }

    private void logSqlIfEnable(String sqlCmd) {
        if (!config.getSqlLogEnabled()) {
            return;
        }
        StringBuilder strBuilder = new StringBuilder("query sql: ");
        // 如果包含启停字段
        if (config.hasEnableField()) {
            String replaceAppName = StrUtil.replaceFirst(sqlCmd, "?", "'" + config.getApplicationName() + "'");
            String executeSql = StrUtil.replaceFirst(replaceAppName, "?", Boolean.TRUE.toString());
            strBuilder.append(executeSql);
        }
        // 如果不包含启停字段
        else {
            strBuilder.append(sqlCmd.replace("?", "'" + config.getApplicationName() + "'"));
        }
        LOG.info(strBuilder.toString());
    }
}
