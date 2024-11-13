package com.yomahub.liteflow.parser.sql.read.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.constant.SqlReadConstant;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.read.AbstractSqlRead;
import com.yomahub.liteflow.parser.sql.read.vo.InstanceIdVO;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Jay li
 * @since 2.12.4
 */

public class InstanceIdRead extends AbstractSqlRead<InstanceIdVO> {

    public InstanceIdRead(SQLParserVO config) {
        super(config);
    }

    @Override
    protected InstanceIdVO parse(ResultSet rs) throws SQLException {
        InstanceIdVO idVO = new InstanceIdVO();
        idVO.setChainName(getStringFromRsWithCheck(rs, super.config.getInstanceChainNameField()));
        idVO.setElDataMd5(getStringFromRsWithCheck(rs, super.config.getElDataMd5Field()));
        idVO.setGroupKeyInstanceId(getStringFromRsWithCheck(rs, super.config.getGroupKeyInstanceIdField()));
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
    public String buildQuerySql(String... exceptions) {
        String tableName = super.config.getInstanceIdTableName();
        String chainNameField = super.config.getInstanceChainNameField();

        if (ArrayUtil.isEmpty(exceptions)) {
            throw new IllegalArgumentException("You did not define the chainName");
        }
        return StrUtil.format(SqlReadConstant.SQL_PATTERN, tableName, chainNameField, exceptions[0]);
    }

    @Override
    public void checkConfig() {
        String tableName = super.config.getInstanceIdTableName();
        String chainNameField = super.config.getInstanceChainNameField();
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
