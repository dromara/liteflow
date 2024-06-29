package com.yomahub.liteflow.parser.sql.util;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Map;

public class LiteFlowJdbcUtil {
    private static final Logger LOG = LoggerFactory.getLogger(LiteFlowJdbcUtil.class);
    private static final String CHECK_SQL_PATTERN = "SELECT {},{} FROM {}";

    /**
     * 获取链接
     * 此方法会根据配置，判读使用指定数据源，还是IOC容器中已有的数据源
     *
     * @param sqlParserVO sql解析器参数
     * @return 返回数据库连接
     */
    public static Connection getConn(SQLParserVO sqlParserVO) {
        Connection connection = null;
        String url = sqlParserVO.getUrl();
        String username = sqlParserVO.getUsername();
        String password = sqlParserVO.getPassword();

        try {
            // 如果不配置 jdbc 连接相关配置，代表使用项目数据源
            if (sqlParserVO.isDefaultDataSource()) {
                String executeSql = buildCheckSql(sqlParserVO);
                Map<String, DataSource> dataSourceMap = ContextAwareHolder.loadContextAware().getBeansOfType(DataSource.class);
                // 遍历数据源，多数据源场景下，判断哪个数据源有 liteflow 配置
                for (Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
                    String dataSourceName = entry.getKey();
                    DataSource dataSource = entry.getValue();

                    if (checkConnectionCanExecuteSql(dataSource.getConnection(), executeSql)) {
                        connection = dataSource.getConnection();
                        LOG.info("use dataSourceName[{}],has found liteflow config", dataSourceName);
                    } else {
                        LOG.info("check dataSourceName[{}],but not has liteflow config", dataSourceName);
                    }
                }
                if (connection == null) {
                    throw new ELSQLException("can not found liteflow config in dataSourceName " + dataSourceMap.keySet());
                }
            }
            // 如果配置 jdbc 连接相关配置,代表使用指定链接信息
            else {
                connection = DriverManager.getConnection(url, username, password);
            }

        } catch (Exception e) {
            throw new ELSQLException(e);
        }

        return connection;
    }

    /**
     * 判断连接是否可以执行指定 sql
     *
     * @param conn 连接
     * @param sql  执行 sql
     */
    public static boolean checkConnectionCanExecuteSql(Connection conn, String sql) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(1);
            rs = stmt.executeQuery();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            // 关闭连接
            close(conn, stmt, rs);
        }
    }

    /**
     * 关闭
     *
     * @param conn conn
     * @param rs   rs
     */
    public static void close(Connection conn, PreparedStatement stmt, ResultSet rs) {
        // 关闭结果集
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new ELSQLException(e);
            }
        }
        // 关闭 statement
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                throw new ELSQLException(e);
            }
        }
        // 关闭连接
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new ELSQLException(e);
            }
        }
    }

    /**
     * 构建检查 sql
     *
     * @param sqlParserVO sql解析器参数
     * @return 返回组合完成的检查sql
     */
    private static String buildCheckSql(SQLParserVO sqlParserVO) {
        String chainTableName = sqlParserVO.getChainTableName();
        String elDataField = sqlParserVO.getElDataField();
        String chainNameField = sqlParserVO.getChainNameField();
        return StrUtil.format(CHECK_SQL_PATTERN, chainNameField, elDataField, chainTableName);
    }
}
