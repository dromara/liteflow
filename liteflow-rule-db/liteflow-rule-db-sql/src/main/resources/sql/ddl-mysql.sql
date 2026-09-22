CREATE TABLE IF NOT EXISTS `${prefix}chain` (
  `application_name` VARCHAR(64) NOT NULL,
  `chain_id` VARCHAR(128) NOT NULL,
  `namespace` VARCHAR(64) DEFAULT NULL,
  `el_data` LONGTEXT NOT NULL,
  `route_data` LONGTEXT DEFAULT NULL,
  `version` BIGINT NOT NULL DEFAULT 1,
  `content_md5` CHAR(32) NOT NULL,
  `enable` TINYINT NOT NULL DEFAULT 1,
  `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`application_name`, `chain_id`)
) DEFAULT CHARACTER SET utf8mb4;
CREATE TABLE IF NOT EXISTS `${prefix}script` (
  `application_name` VARCHAR(64) NOT NULL,
  `node_id` VARCHAR(128) NOT NULL,
  `script_name` VARCHAR(128) DEFAULT NULL,
  `script_type` VARCHAR(32) NOT NULL,
  `script_language` VARCHAR(32) DEFAULT NULL,
  `script_data` LONGTEXT NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 1,
  `content_md5` CHAR(32) NOT NULL,
  `enable` TINYINT NOT NULL DEFAULT 1,
  `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`application_name`, `node_id`)
) DEFAULT CHARACTER SET utf8mb4;
CREATE TABLE IF NOT EXISTS `${prefix}change_log` (
  `seq` BIGINT NOT NULL AUTO_INCREMENT,
  `application_name` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(16) NOT NULL,
  `target_id` VARCHAR(128) NOT NULL,
  `op` VARCHAR(16) NOT NULL,
  `version` BIGINT NOT NULL,
  `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`seq`),
  KEY `idx_app_seq` (`application_name`, `seq`)
) DEFAULT CHARACTER SET utf8mb4;
CREATE TABLE IF NOT EXISTS `${prefix}change_lock` (
  `lock_id` TINYINT NOT NULL,
  PRIMARY KEY (`lock_id`)
) DEFAULT CHARACTER SET utf8mb4;
INSERT IGNORE INTO `${prefix}change_lock` (`lock_id`) VALUES (1);
