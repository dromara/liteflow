package com.yomahub.liteflow.agent.mysql;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.state.DefaultAgentStateStoreResolver;
import com.yomahub.liteflow.agent.testsupport.MockBeanContext;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import static org.mockito.Mockito.*;
import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;
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
        assertEquals(AgentSessionStoreType.MYSQL, provider.type());
    }

    @Test
    void rejectsMissingConnectionSource() {
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(new AgentSessionStoreConfig()));
        assertTrue(failure.getMessage().contains("data-source-bean-name"));
        assertTrue(failure.getMessage().contains("jdbc-url"));
    }

    @Test
    void rejectsBeanNameAndJdbcUrlTogether() {
        AgentSessionStoreConfig config = new AgentSessionStoreConfig();
        config.getMysql().setDataSourceBeanName("dataSource");
        config.getMysql().setJdbcUrl("jdbc:mysql://localhost:3306/test");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(config));
        assertTrue(failure.getMessage().contains("mutually exclusive"));
    }

    @Test
    void rejectsBeanOfWrongType() {
        AgentSessionStoreConfig config = new AgentSessionStoreConfig();
        config.getMysql().setDataSourceBeanName("notADatasource");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> new MysqlAgentStateStoreProvider(name -> "not a datasource").resolve(config));
        assertTrue(failure.getMessage().contains("javax.sql.DataSource"));
    }

    @Test
    void rejectsInvalidDatabaseNameBeforeConnecting() {
        AgentSessionStoreConfig config = new AgentSessionStoreConfig();
        config.getMysql().setJdbcUrl("jdbc:mysql://localhost:3306/test");
        config.getMysql().setDatabaseName("bad database name!");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(config));
        assertTrue(failure.getMessage().contains("MySQL state store could not be created"));
    }

    @Test
    void mockedConnectionFailureIsWrappedAsConfigException() throws Exception {
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("mock connection failure"));
        AgentSessionStoreConfig config = new AgentSessionStoreConfig();
        config.getMysql().setDataSourceBeanName("mockSql");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> new MysqlAgentStateStoreProvider(name -> source).resolve(config));
        assertTrue(failure.getMessage().contains("MySQL state store could not be created"));
        verify(source).getConnection();
    }

    @Test
    void resolverDiscoversProviderThroughServiceLoader() throws Exception {
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("mock connection failure"));
        AgentSessionStoreConfig config = new AgentSessionStoreConfig();
        config.setType(AgentSessionStoreType.MYSQL);
        config.getMysql().setDataSourceBeanName("mockSql");
        try (var context = new MockBeanContext(Map.of("mockSql", source))) {
            AgentConfigException failure = assertThrows(AgentConfigException.class,
                    () -> new DefaultAgentStateStoreResolver().resolve(config));
            assertTrue(failure.getMessage().contains("MySQL state store could not be created"));
            verify(source).getConnection();
        }
    }
}
