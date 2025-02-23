package com.yomahub.liteflow.parser.sql.datasource.impl;

import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.MissMavenDependencyException;
import com.yomahub.liteflow.parser.sql.datasource.LiteFlowDataSourceConnect;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Optional;

/**
 * ShardingJdbc 动态数据源
 *
 * @author tkc
 * @since 2.12.5
 */
public class ShardingJdbcDsConn implements LiteFlowDataSourceConnect {

    @Override
    public boolean filter(SQLParserVO config) {
        if (StrUtil.isBlank(config.getShardingJdbcDataSource())) {
            return false;
        }
        boolean classLoadFlag = ClassLoaderUtil.isPresent(Constant.LOAD_CLASS_NAME);
        if (!classLoadFlag) {
            throw new MissMavenDependencyException(Constant.MAVEN_GROUP_ID, Constant.MAVEN_ARTIFACT_ID);
        }
        return true;
    }

    @Override
    public Connection getConn(SQLParserVO config) throws Exception {
        ContextAware contextAware = ContextAwareHolder.loadContextAware();

        return contextAware.getBean(DataSource.class).getConnection();
    }


    public static class Constant {
        public static final String LOAD_CLASS_NAME = "org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource";
        public static final String MAVEN_GROUP_ID = "org.apache.shardingsphere";
        public static final String MAVEN_ARTIFACT_ID = "sharding-jdbc-core";
    }
}
