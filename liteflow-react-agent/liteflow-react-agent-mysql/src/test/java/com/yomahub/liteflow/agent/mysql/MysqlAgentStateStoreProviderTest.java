package com.yomahub.liteflow.agent.mysql;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.state.DefaultAgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MysqlAgentStateStoreProviderTest {

    private final MysqlAgentStateStoreProvider provider =
            new MysqlAgentStateStoreProvider(name -> {
                throw new IllegalStateException("no bean expected: " + name);
            });

    @Test
    void supportsMysqlType() {
        assertEquals(AgentStateStoreType.MYSQL, provider.type());
    }

    @Test
    void rejectsMissingConnectionSource() {
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(new AgentStateStoreConfig()));
        assertTrue(failure.getMessage().contains("data-source-bean-name"));
        assertTrue(failure.getMessage().contains("jdbc-url"));
    }

    @Test
    void rejectsBeanNameAndJdbcUrlTogether() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.getMysql().setDataSourceBeanName("dataSource");
        config.getMysql().setJdbcUrl("jdbc:mysql://localhost:3306/test");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(config));
        assertTrue(failure.getMessage().contains("mutually exclusive"));
    }

    @Test
    void rejectsBeanOfWrongType() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.getMysql().setDataSourceBeanName("notADatasource");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> new MysqlAgentStateStoreProvider(name -> "not a datasource").resolve(config));
        assertTrue(failure.getMessage().contains("javax.sql.DataSource"));
    }

    @Test
    void rejectsInvalidDatabaseNameBeforeConnecting() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.getMysql().setJdbcUrl("jdbc:mysql://localhost:3306/test");
        config.getMysql().setDatabaseName("bad database name!");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(config));
        assertTrue(failure.getMessage().contains("MySQL state store could not be created"));
    }

    @Test
    void unreachableDatabaseIsWrappedAsConfigException() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.getMysql().setJdbcUrl(
                "jdbc:mysql://localhost:65532/test?connectTimeout=1000&socketTimeout=1000");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(config));
        assertTrue(failure.getMessage().contains("MySQL state store could not be created"));
    }

    @Test
    void resolverDiscoversProviderThroughServiceLoader() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.setType(AgentStateStoreType.MYSQL);
        config.getMysql().setJdbcUrl(
                "jdbc:mysql://localhost:65532/test?connectTimeout=1000&socketTimeout=1000");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> new DefaultAgentStateStoreResolver().resolve(config));
        // SPI wiring proven: the error comes from the provider, not the missing-module path.
        assertTrue(failure.getMessage().contains("MySQL state store could not be created"));
    }
}
