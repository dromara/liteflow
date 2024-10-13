package com.yomahub.liteflow.parser.sql.datasource;

import com.yomahub.liteflow.parser.sql.datasource.impl.BaoMiDouDynamicDsConn;
import com.yomahub.liteflow.parser.sql.datasource.impl.DefaultLiteFlowJdbcConn;
import com.yomahub.liteflow.parser.sql.datasource.impl.LiteFlowAutoLookUpJdbcConn;
import com.yomahub.liteflow.parser.sql.datasource.impl.ShardingJdbcDsConn;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 数据源获取接口工厂
 *
 * @author tkc
 * @since 2.12.5
 */
public class LiteflowDataSourceConnectFactory {
    private static final List<LiteFlowDataSourceConnect> CONNECT_LIST = new CopyOnWriteArrayList<>();
    private static final Logger LOG = LoggerFactory.getLogger(LiteflowDataSourceConnectFactory.class);

    public static void register() {
        ContextAware contextAware = ContextAwareHolder.loadContextAware();
        Map<String, LiteFlowDataSourceConnect> beanMap = contextAware.getBeansOfType(LiteFlowDataSourceConnect.class);
        Collection<LiteFlowDataSourceConnect> values = beanMap.values();

        // 清空原有的列表
        CONNECT_LIST.clear();
        // 将自定义的放在最前面
        CONNECT_LIST.addAll(values);

        // 内置的几种处理器
        CONNECT_LIST.add(new DefaultLiteFlowJdbcConn());
        CONNECT_LIST.add(new BaoMiDouDynamicDsConn());
        CONNECT_LIST.add(new ShardingJdbcDsConn());

        // 自动查找放在最后，这个用于兜底处理,这个里面如果找不到，会直接抛出异常
        CONNECT_LIST.add(new LiteFlowAutoLookUpJdbcConn());
    }

    public static Optional<LiteFlowDataSourceConnect> getConnect(SQLParserVO config) {
        for (LiteFlowDataSourceConnect dataSourceConnect : CONNECT_LIST) {
            if (dataSourceConnect.filter(config)) {
                LOG.debug("use lite-flow-data-source-connect: {}", dataSourceConnect.getClass().getName());
                return Optional.of(dataSourceConnect);
            }
        }
        return Optional.empty();
    }
}
