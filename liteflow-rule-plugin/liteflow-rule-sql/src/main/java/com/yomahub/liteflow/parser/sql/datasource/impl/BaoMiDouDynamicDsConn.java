package com.yomahub.liteflow.parser.sql.datasource.impl;

import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.yomahub.liteflow.exception.MissMavenDependencyException;
import com.yomahub.liteflow.parser.sql.datasource.LiteFlowDataSourceConnect;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.Optional;

/**
 * 苞米豆动态数据源
 *
 * @author tkc
 * @since 2.12.5
 */
public class BaoMiDouDynamicDsConn implements LiteFlowDataSourceConnect {
    @Override
    public boolean filter(SQLParserVO config) {
        if (StrUtil.isBlank(config.getBaomidouDataSource())) {
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
        String dataSourceName = config.getBaomidouDataSource();
        ContextAware contextAware = ContextAwareHolder.loadContextAware();
        DynamicRoutingDataSource dynamicRoutingDataSource = contextAware.getBean(DynamicRoutingDataSource.class);
        Map<String, DataSource> dataSources = dynamicRoutingDataSource.getDataSources();
        if (!dataSources.containsKey(dataSourceName)) {
            throw new ELSQLException(StrUtil.format("can not found {} datasource", dataSourceName));
        }

        DataSource dataSource = dynamicRoutingDataSource.getDataSource(dataSourceName);
        return dataSource.getConnection();
    }

    /**
     * 常量类
     */
    public static class Constant {
        public static final String LOAD_CLASS_NAME = "com.baomidou.dynamic.datasource.DynamicRoutingDataSource";
        public static final String MAVEN_GROUP_ID = "com.baomidou";
        public static final String MAVEN_ARTIFACT_ID = "dynamic-datasource-spring-boot-starter";
    }
}
