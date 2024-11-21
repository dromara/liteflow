package com.yomahub.liteflow.parser.sql.datasource.impl;

import com.yomahub.liteflow.parser.sql.datasource.LiteFlowDataSourceConnect;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * lite flow 默认的数据源连接
 *
 * @author tkc
 * @since 2.12.5
 */
public class DefaultLiteFlowJdbcConn implements LiteFlowDataSourceConnect {
    @Override
    public boolean filter(SQLParserVO config) {
        return config.isUseJdbcConn();
    }

    @Override
    public Connection getConn(SQLParserVO config) throws Exception {
        String url = config.getUrl();
        String username = config.getUsername();
        String password = config.getPassword();

        return DriverManager.getConnection(url, username, password);
    }
}
