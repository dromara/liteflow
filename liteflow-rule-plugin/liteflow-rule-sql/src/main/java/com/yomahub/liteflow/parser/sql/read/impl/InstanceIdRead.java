package com.yomahub.liteflow.parser.sql.read.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.constant.SqlReadConstant;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.read.AbstractSqlRead;
import com.yomahub.liteflow.parser.sql.read.vo.InstanceIdVO;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import org.apache.commons.lang.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Jay li
 * @since 2.13.0
 */

public class InstanceIdRead extends AbstractSqlRead<InstanceIdVO> {

    public InstanceIdRead(SQLParserVO config) {
        super(config);
    }

    @Override
    protected InstanceIdVO parse(ResultSet rs) throws SQLException {
        InstanceIdVO idVO = new InstanceIdVO();
        idVO.setChainId(getStringFromRsWithCheck(rs, super.config.getInstanceChainIdField()));
        idVO.setElDataMd5(getStringFromRsWithCheck(rs, super.config.getElDataMd5Field()));
        idVO.setNodeInstanceIdMapJson(getStringFromRsWithCheck(rs, super.config.getNodeInstanceIdMapJsonField()));
        return idVO;
    }

    @Override
    public boolean hasEnableFiled() {
        return true;
    }

    @Override
    public boolean getEnableFiledValue(ResultSet rs) {
        return true;
    }

    @Override
    public String buildQuerySql() {
        String tableName = super.config.getInstanceIdTableName();
        String applicationNameField = super.config.getInstanceIdApplicationNameField();
        String applicationName = super.config.getApplicationName();

        return StrUtil.format(SqlReadConstant.SQL_PATTERN, tableName,
                applicationNameField, applicationName);
    }

    @Override
    public String buildQuerySql(String chainId) {
        String tableName = super.config.getInstanceIdTableName();
        String chainNameField = super.config.getInstanceChainIdField();
        String instanceIdApplicationNameField = super.config.getInstanceIdApplicationNameField();
        String applicationName = super.config.getApplicationName();

        if (StringUtils.isEmpty(chainId)) {
            throw new IllegalArgumentException("You did not define the chainId");
        }
        return StrUtil.format(SqlReadConstant.SQL_PATTERN_WITH_CHAIN_ID, tableName, instanceIdApplicationNameField
                , applicationName, chainNameField, chainId);
    }

    @Override
    public void checkConfig() {
        String tableName = super.config.getInstanceIdTableName();
        String chainNameField = super.config.getInstanceChainIdField();
        if (StrUtil.isBlank(tableName)) {
            throw new ELSQLException("You did not define the tableName property");
        }
        if (StrUtil.isBlank(chainNameField)) {
            throw new ELSQLException("You did not define the chainNameField property");
        }
    }

    @Override
    public ReadType type() {
        return null;
    }
}
