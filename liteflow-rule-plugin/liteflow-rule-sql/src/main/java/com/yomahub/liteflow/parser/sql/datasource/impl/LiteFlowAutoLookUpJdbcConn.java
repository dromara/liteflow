package com.yomahub.liteflow.parser.sql.datasource.impl;

import com.yomahub.liteflow.parser.sql.datasource.LiteFlowDataSourceConnect;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.util.LiteFlowJdbcUtil;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * lite flow 自动查找连接
 *
 * @author tkc
 * @since 2.12.5
 */
public class LiteFlowAutoLookUpJdbcConn implements LiteFlowDataSourceConnect {
    private static final Logger LOG = LoggerFactory.getLogger(LiteFlowAutoLookUpJdbcConn.class);

    @Override
    public boolean filter(SQLParserVO config) {
        return true;
    }

    @Override
    public Connection getConn(SQLParserVO config) throws Exception {
        return DataSourceBeanNameHolder.autoLookUpConn(config);
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

        /**
         * 自动查找可用数据源
         */
        public static Connection autoLookUpConn(SQLParserVO sqlParserVO) throws SQLException {
            Connection connection;
            Map<String, DataSource> dataSourceMap = ContextAwareHolder.loadContextAware().getBeansOfType(DataSource.class);
            if (LiteFlowJdbcUtil.DataSourceBeanNameHolder.isNotInit()) {
                synchronized (LiteFlowJdbcUtil.DataSourceBeanNameHolder.class) {
                    if (LiteFlowJdbcUtil.DataSourceBeanNameHolder.isNotInit()) {
                        String executeSql = LiteFlowJdbcUtil.buildCheckSql(sqlParserVO);
                        // 遍历数据源，多数据源场景下，判断哪个数据源有 liteflow 配置
                        for (Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
                            String dataSourceName = entry.getKey();
                            DataSource dataSource = entry.getValue();

                            if (LiteFlowJdbcUtil.checkConnectionCanExecuteSql(dataSource.getConnection(), executeSql)) {
                                // 找到数据源名称后，将其缓存起来，下次使用就不再寻找
                                LiteFlowJdbcUtil.DataSourceBeanNameHolder.init(dataSourceName);
                                if (sqlParserVO.getSqlLogEnabled()) {
                                    LOG.info("use dataSourceName[{}],has found liteflow config", dataSourceName);
                                }
                                break;
                            } else {
                                LOG.info("check dataSourceName[{}],but not has liteflow config", dataSourceName);
                            }
                        }
                    }
                }
            }
            DataSource dataSource = Optional.ofNullable(LiteFlowJdbcUtil.DataSourceBeanNameHolder.getDataSourceName())
                    .map(dataSourceMap::get)
                    .orElse(null);
            if (dataSource == null) {
                throw new ELSQLException("can not found liteflow config in dataSourceName " + dataSourceMap.keySet());
            }
            connection = dataSource.getConnection();
            if (connection == null) {
                throw new ELSQLException("can not found liteflow config in dataSourceName " + dataSourceMap.keySet());
            }
            return connection;
        }
    }
}
