package com.yomahub.liteflow.agent.mysql;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.state.AgentStateStoreProvider;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreMysqlConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import io.agentscope.extensions.mysql.state.MysqlAgentStateStore;

import java.util.Objects;
import java.util.function.Function;

import javax.sql.DataSource;

/**
 * Builds the MySQL {@link AgentStateStoreType#MYSQL} backend on top of the official
 * {@code agentscope-extensions-mysql} {@link MysqlAgentStateStore}.
 *
 * <p>Connection source is either
 * {@code liteflow.agent.state-store.mysql.data-source-bean-name} (the application's
 * pooled {@code DataSource}, recommended in production) or
 * {@code liteflow.agent.state-store.mysql.jdbc-url} plus credentials (LiteFlow builds
 * and owns a driver {@code DataSource}).
 */
public final class MysqlAgentStateStoreProvider implements AgentStateStoreProvider {

	private final Function<String, Object> beanLookup;

	public MysqlAgentStateStoreProvider() {
		this(name -> ContextAwareHolder.loadContextAware().getBean(name));
	}

	public MysqlAgentStateStoreProvider(Function<String, Object> beanLookup) {
		this.beanLookup = Objects.requireNonNull(beanLookup, "beanLookup");
	}

	@Override
	public AgentStateStoreType type() {
		return AgentStateStoreType.MYSQL;
	}

	@Override
	public ResolvedAgentStateStore resolve(AgentStateStoreConfig config) {
		AgentStateStoreMysqlConfig mysql = config.getMysql();
		if (mysql == null) {
			throw new AgentConfigException("liteflow.agent.state-store.mysql must not be null");
		}
		String beanName = trimToNull(mysql.getDataSourceBeanName());
		String jdbcUrl = trimToNull(mysql.getJdbcUrl());
		if (beanName != null && jdbcUrl != null) {
			throw new AgentConfigException("state-store.mysql.data-source-bean-name and "
					+ "state-store.mysql.jdbc-url are mutually exclusive");
		}
		if (beanName == null && jdbcUrl == null) {
			throw new AgentConfigException("state-store type MYSQL requires either "
					+ "state-store.mysql.data-source-bean-name or state-store.mysql.jdbc-url");
		}
		return beanName != null ? resolveFromBean(mysql, beanName) : resolveFromUrl(mysql, jdbcUrl);
	}

	private ResolvedAgentStateStore resolveFromBean(
			AgentStateStoreMysqlConfig mysql, String beanName) {
		Object candidate;
		try {
			candidate = beanLookup.apply(beanName);
		} catch (RuntimeException | LinkageError failure) {
			throw new AgentConfigException(
					"DataSource bean '" + beanName + "' could not be resolved", failure);
		}
		if (!(candidate instanceof DataSource dataSource)) {
			throw new AgentConfigException("DataSource bean '" + beanName
					+ "' must implement javax.sql.DataSource");
		}
		return build(mysql, dataSource, false);
	}

	private ResolvedAgentStateStore resolveFromUrl(AgentStateStoreMysqlConfig mysql, String jdbcUrl) {
		MysqlDataSource dataSource;
		try {
			dataSource = new MysqlDataSource();
			dataSource.setURL(jdbcUrl);
			if (mysql.getUsername() != null && !mysql.getUsername().isBlank()) {
				dataSource.setUser(mysql.getUsername());
			}
			if (mysql.getPassword() != null && !mysql.getPassword().isBlank()) {
				dataSource.setPassword(mysql.getPassword());
			}
		} catch (RuntimeException | LinkageError failure) {
			throw new AgentConfigException(
					"state-store.mysql.jdbc-url is invalid: " + jdbcUrl, failure);
		}
		return build(mysql, dataSource, true);
	}

	private ResolvedAgentStateStore build(
			AgentStateStoreMysqlConfig mysql, DataSource dataSource, boolean owned) {
		try {
			MysqlAgentStateStore store = new MysqlAgentStateStore(
					dataSource,
					mysql.getDatabaseName(),
					mysql.getTableName(),
					mysql.isCreateIfNotExist());
			return new ResolvedAgentStateStore(store, owned);
		} catch (RuntimeException | LinkageError failure) {
			throw new AgentConfigException(
					"MySQL state store could not be created (check database/table existence "
							+ "or enable state-store.mysql.create-if-not-exist)",
					failure);
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
