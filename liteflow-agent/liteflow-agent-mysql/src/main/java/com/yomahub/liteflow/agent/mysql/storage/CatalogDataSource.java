package com.yomahub.liteflow.agent.mysql.storage;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.logging.Logger;

/** Applies the configured database even when an application-owned pool selects another catalog. */
final class CatalogDataSource implements DataSource {
    private final DataSource delegate;
    private final String catalog;
    CatalogDataSource(DataSource delegate, String catalog) { this.delegate = delegate; this.catalog = catalog; }
    private Connection scoped(Connection connection) throws SQLException {
        String previous = connection.getCatalog();
        try { connection.setCatalog(catalog); }
        catch (SQLException failure) { connection.close(); throw failure; }
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                        try { if (!connection.isClosed()) connection.setCatalog(previous); }
                        finally { connection.close(); }
                        return null;
                    }
                    try { return method.invoke(connection, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
    @Override public Connection getConnection() throws SQLException { return scoped(delegate.getConnection()); }
    @Override public Connection getConnection(String user, String password) throws SQLException { return scoped(delegate.getConnection(user, password)); }
    @Override public PrintWriter getLogWriter() throws SQLException { return delegate.getLogWriter(); }
    @Override public void setLogWriter(PrintWriter out) throws SQLException { delegate.setLogWriter(out); }
    @Override public void setLoginTimeout(int seconds) throws SQLException { delegate.setLoginTimeout(seconds); }
    @Override public int getLoginTimeout() throws SQLException { return delegate.getLoginTimeout(); }
    @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { return delegate.getParentLogger(); }
    @Override public <T> T unwrap(Class<T> type) throws SQLException { return delegate.unwrap(type); }
    @Override public boolean isWrapperFor(Class<?> type) throws SQLException { return delegate.isWrapperFor(type); }
}
