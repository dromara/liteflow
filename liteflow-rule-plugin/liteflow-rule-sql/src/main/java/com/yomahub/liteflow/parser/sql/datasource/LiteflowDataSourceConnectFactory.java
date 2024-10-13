package com.yomahub.liteflow.parser.sql.datasource;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.parser.sql.datasource.impl.BaoMiDouDynamicDsConn;
import com.yomahub.liteflow.parser.sql.datasource.impl.ShardingJdbcDsConn;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据源获取接口工厂
 *
 * @author tkc
 * @since 2.12.5
 */
public class LiteflowDataSourceConnectFactory {
    private static List<LiteFlowDataSourceConnect> CONNECT_LIST = CollUtil.newArrayList(
            new BaoMiDouDynamicDsConn(),
            new ShardingJdbcDsConn()
    );

    public static void register() {
        ContextAware contextAware = ContextAwareHolder.loadContextAware();
        Map<String, LiteFlowDataSourceConnect> beanMap = contextAware.getBeansOfType(LiteFlowDataSourceConnect.class);
        Collection<LiteFlowDataSourceConnect> values = beanMap.values();
        CONNECT_LIST.addAll(values);

        // 根据类名去重
        CONNECT_LIST = CollUtil.distinct(CONNECT_LIST, t -> t.getClass().getName(), true);
    }

    public static Optional<LiteFlowDataSourceConnect> getConnect(SQLParserVO sqlParserVO) {
        return CONNECT_LIST.stream().filter(connect -> connect.filter(sqlParserVO)).findFirst();
    }
}
