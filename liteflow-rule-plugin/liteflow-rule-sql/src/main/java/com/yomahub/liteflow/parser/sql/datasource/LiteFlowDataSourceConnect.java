package com.yomahub.liteflow.parser.sql.datasource;

import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.Connection;

/**
 * 数据源获取接口
 *
 * @author tkc
 * @since 2.12.5
 */
public interface LiteFlowDataSourceConnect {

    /**
     * 检查是否支持该数据源
     */
    boolean filter(SQLParserVO config);

    /**
     * 获取连接
     */
    Connection getConn(SQLParserVO config) throws Exception;

}
