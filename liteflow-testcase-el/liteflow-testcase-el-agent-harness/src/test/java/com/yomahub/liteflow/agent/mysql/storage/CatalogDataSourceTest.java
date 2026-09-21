package com.yomahub.liteflow.agent.mysql.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogDataSourceTest {
    private final DataSource delegate = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final CatalogDataSource source = new CatalogDataSource(delegate, "agent_database");

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void borrowedConnectionSelectsTheAgentDatabaseAndRestoresThePoolCatalog(boolean credentials) throws Exception {
        when(delegate.getConnection()).thenReturn(connection);
        when(delegate.getConnection("mock-user", "mock-password")).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("pool_database");
        SQLException queryFailure = new SQLException("mock query failure");
        when(connection.prepareStatement("mock-query")).thenThrow(queryFailure);
        try (var scoped = credentials ? source.getConnection("mock-user", "mock-password") : source.getConnection()) {
            verify(connection).setCatalog("agent_database");
            assertSame(queryFailure, assertThrows(SQLException.class, () -> scoped.prepareStatement("mock-query")));
            verify(connection, never()).close();
        }
        var order = inOrder(connection);
        order.verify(connection).setCatalog("agent_database");
        order.verify(connection).setCatalog("pool_database");
        order.verify(connection).close();
    }

    @Test
    void selectingAnUnavailableCatalogClosesTheBorrowedConnection() throws Exception {
        when(delegate.getConnection()).thenReturn(connection);
        SQLException cause = new SQLException("mock missing database");
        doThrow(cause).when(connection).setCatalog("agent_database");
        assertSame(cause, assertThrows(SQLException.class, source::getConnection));
        verify(connection).close();
    }

    @Test
    void restorationFailureStillReturnsTheConnectionToThePool() throws Exception {
        when(delegate.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("pool_database");
        SQLException cause = new SQLException("mock pool reset failure");
        doThrow(cause).when(connection).setCatalog("pool_database");
        Connection scoped = source.getConnection();
        assertSame(cause, assertThrows(SQLException.class, scoped::close));
        verify(connection).close();
    }

    @Test
    void alreadyClosedConnectionsDoNotAttemptCatalogRestoration() throws Exception {
        when(delegate.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("pool_database");
        Connection scoped = source.getConnection();
        when(connection.isClosed()).thenReturn(true);
        scoped.close();
        verify(connection, never()).setCatalog("pool_database");
        verify(connection).close();
    }
}
