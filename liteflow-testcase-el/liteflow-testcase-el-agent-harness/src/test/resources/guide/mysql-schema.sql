CREATE DATABASE IF NOT EXISTS liteflow_agent
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS liteflow_agent.agent_sessions (
  session_id VARCHAR(255) NOT NULL,
  state_key VARCHAR(255) NOT NULL,
  item_index INT NOT NULL DEFAULT 0,
  state_data LONGTEXT NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id, state_key, item_index)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS liteflow_agent.agent_sessions_workspace (
  namespace_path VARCHAR(512) NOT NULL,
  item_key VARCHAR(255) NOT NULL,
  value_json LONGTEXT NOT NULL,
  version BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (namespace_path, item_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
