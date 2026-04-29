package com.yomahub.liteflow.agent.session.factory;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import com.yomahub.liteflow.property.agent.MysqlMemoryConfig;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import io.agentscope.core.session.Session;
import io.agentscope.core.session.mysql.MysqlSession;

import javax.sql.DataSource;

/**
 * 支持 {@link MemoryStorageMode#MYSQL} 模式。使用用户提供的
 * {@link DataSource} bean；LiteFlow 不自行创建 JDBC 连接池。
 */
public class MysqlAgentSessionFactory implements AgentSessionFactory {

    @Override
    public MemoryStorageMode mode() {
        return MemoryStorageMode.MYSQL;
    }

    @Override
    public Session create(AgentConfig cfg) {
        MysqlMemoryConfig mc = cfg.getSession().getMemory().getMysql();
        if (mc.getDataSourceBeanName() == null || mc.getDataSourceBeanName().trim().isEmpty()) {
            throw new AgentConfigException(
                    "liteflow.agent.session.memory.mysql.dataSourceBeanName is required when mode=MYSQL");
        }
        Object bean = ContextAwareHolder.loadContextAware().getBean(mc.getDataSourceBeanName());
        if (bean == null) {
            throw new AgentConfigException("DataSource bean not found: " + mc.getDataSourceBeanName());
        }
        if (!(bean instanceof DataSource)) {
            throw new AgentConfigException("Bean '" + mc.getDataSourceBeanName() + "' is not a DataSource; got "
                    + bean.getClass().getName());
        }
        DataSource ds = (DataSource) bean;
        String db = mc.getDatabaseName();
        String table = mc.getTableName();
        boolean hasCustom = (db != null && !db.isEmpty()) || (table != null && !table.isEmpty());
        try {
            if (hasCustom) {
                return new MysqlSession(ds, db, table, mc.isCreateIfNotExist());
            }
            return new MysqlSession(ds, mc.isCreateIfNotExist());
        } catch (Exception e) {
            throw new AgentConfigException("Failed to build MysqlSession", e);
        }
    }
}
