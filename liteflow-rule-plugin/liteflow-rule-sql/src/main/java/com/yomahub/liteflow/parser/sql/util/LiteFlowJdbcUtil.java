package com.yomahub.liteflow.parser.sql.util;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.sql.datasource.LiteFlowDataSourceConnect;
import com.yomahub.liteflow.parser.sql.datasource.LiteflowDataSourceConnectFactory;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Map;
import java.util.Optional;

public class LiteFlowJdbcUtil {
    private static final Logger LOG = LoggerFactory.getLogger(LiteFlowJdbcUtil.class);
    private static final String CHECK_SQL_PATTERN = "SELECT {},{} FROM {}";

    /**
     * 获取链接
     * 此方法会从多个角度查找数据源，优先级从上到下
     * 1. 自定义连接器获取，实现了 DataBaseConnect 接口
     * 2. 没有配置数据源连接配置，判断标准参考 SqlParserVO#isDefaultDataSource()，自动寻找IOC容器中已有的数据源
     * 3. 使用数据源配置，使用 jdbc 创建连接
     *
     * @param sqlParserVO sql解析器参数
     * @return 返回数据库连接
     */
    public static Connection getConn(SQLParserVO sqlParserVO) {
        Connection connection = null;

        try {
            // 如果指定连接查找器，就使用连接查找器获取连接
            Optional<LiteFlowDataSourceConnect> connectOpt = LiteflowDataSourceConnectFactory.getConnect(sqlParserVO);
            if (connectOpt.isPresent()) {
                connection = connectOpt.get().getConn(sqlParserVO);
            } else {
                // 理论上这里不会走，因为最后一个连接查找器 LiteFlowAutoLookUpJdbcConn 没找到会抛出异常的
                // 这里是一个兜底，理论上不会走
                throw new ELSQLException("can not found connect by liteflow config");
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
    public static String buildCheckSql(SQLParserVO sqlParserVO) {
        String chainTableName = sqlParserVO.getChainTableName();
        String elDataField = sqlParserVO.getElDataField();
        String chainNameField = sqlParserVO.getChainNameField();
        return StrUtil.format(CHECK_SQL_PATTERN, chainNameField, elDataField, chainTableName);
    }

    /**
     * 关闭
     * @param conn
     * @param stmt
     */
    public static void close(Connection conn, Statement stmt) {
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

    public static class DataSourceBeanNameHolder {
        private static String DATA_SOURCE_NAME = null;

        public static synchronized void init(String dataSourceName) {
            if (DATA_SOURCE_NAME == null) {
                DATA_SOURCE_NAME = dataSourceName;
            }
        }

        public static String getDataSourceName() {
            return DATA_SOURCE_NAME;
        }

        public static boolean isNotInit() {
            return DATA_SOURCE_NAME == null;
        }
    }
}
