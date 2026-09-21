package com.yomahub.liteflow.agent.mysql.guard;

import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.agent.testsupport.MockBeanContext;
import com.yomahub.liteflow.property.agent.AgentConfig;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** H2 supplies store metadata; the MySQL named-lock protocol is mocked at JDBC. */
class MysqlInvocationGuardTest {
    private final DataSource source = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement acquire = mock(PreparedStatement.class);
    private final PreparedStatement release = mock(PreparedStatement.class);
    private final ResultSet result = mock(ResultSet.class);
    private final JdbcDataSource database = new JdbcDataSource();
    private final AgentInvocationKey key = AgentInvocationKey.workspace("app", "conversation");
    private MockBeanContext context;
    private AgentInvocationGuard guard;

    @BeforeEach
    void configure() throws Exception {
        database.setURL("jdbc:h2:mem:guard_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1");
        try (var sqlConnection = database.getConnection(); var sql = sqlConnection.createStatement()) {
            sql.execute("CREATE SCHEMA agentscope");
            sql.execute("CREATE TABLE agentscope.agentscope_sessions (session_id VARCHAR(255), state_key VARCHAR(255),"
                    + "item_index INT, state_data LONGTEXT, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY(session_id,state_key,item_index))");
        }
        when(source.getConnection()).thenAnswer(call -> database.getConnection());
        context = new MockBeanContext(Map.of("mockSql", source));
        AgentConfig config = new AgentConfig();
        config.getSessionStore().getMysql().setDataSourceBeanName("mockSql");
        guard = new MysqlInvocationGuardProvider().resolve(config);
        doReturn(connection).when(source).getConnection();
        when(connection.prepareStatement("SELECT GET_LOCK(?, ?)")).thenReturn(acquire);
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(release);
        when(acquire.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getInt(1)).thenReturn(1);
    }

    @AfterEach
    void restore() throws Exception {
        try {
            if (guard != null) guard.close();
            try (var connection = database.getConnection(); var sql = connection.createStatement()) { sql.execute("SHUTDOWN"); }
        } finally { if (context != null) context.close(); }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 2500})
    void acquisitionRoundsTimeoutAndReleasesExactlyTheAcquiredLockOnce(long millis) throws Exception {
        var lease = guard.acquire(key, Duration.ofMillis(millis));
        assertEquals(key, lease.key());
        verify(acquire).setLong(2, millis == 0 ? 0 : Math.max(1, millis / 1000));
        ArgumentCaptor<String> address = ArgumentCaptor.forClass(String.class);
        verify(acquire).setString(eq(1), address.capture());
        assertTrue(address.getValue().matches("[a-f0-9]{64}"));
        verify(connection, never()).close();
        lease.close();
        lease.close();
        verify(release).setString(1, address.getValue());
        verify(release).execute();
        verify(release).close();
        verify(connection).close();
        verify(result).close();
        verify(acquire).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void missingOrDeniedLockResultBecomesTimeoutAndClosesConnection(boolean hasRow) throws Exception {
        when(result.next()).thenReturn(hasRow);
        when(result.getInt(1)).thenReturn(0);
        var failure = assertThrows(AgentInvocationException.class, () -> guard.acquire(key, Duration.ZERO));
        assertEquals(AgentInvocationErrorType.TIMEOUT, failure.getErrorType());
        verify(connection).close();
        verifyNoInteractions(release);
    }

    @Test
    void acquisitionFailureRetainsTheCauseAndSuppressedCleanupFailure() throws Exception {
        SQLException primary = new SQLException("mock query failure");
        SQLException cleanup = new SQLException("mock close failure");
        when(acquire.executeQuery()).thenThrow(primary);
        doThrow(cleanup).when(connection).close();
        var failure = assertThrows(AgentInvocationException.class, () -> guard.acquire(key, Duration.ZERO));
        assertSame(primary, failure.getCause());
        assertArrayEquals(new Throwable[]{cleanup}, primary.getSuppressed());
        verifyNoInteractions(release);
    }

    @Test
    void releaseFailureStillClosesConnectionAndDoesNotRepeatRelease() throws Exception {
        SQLException cause = new SQLException("mock release failure");
        when(release.execute()).thenThrow(cause);
        var held = guard.acquire(key, Duration.ZERO);
        var failure = assertThrows(AgentInvocationException.class, held::close);
        assertSame(cause, failure.getCause());
        held.close();
        verify(release).execute();
        verify(connection).close();
    }
}
