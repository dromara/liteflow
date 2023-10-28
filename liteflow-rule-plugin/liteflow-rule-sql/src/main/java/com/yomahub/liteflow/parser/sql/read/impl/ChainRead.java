package com.yomahub.liteflow.parser.sql.read.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.constant.SqlReadConstant;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.read.AbstractSqlRead;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * chain 读取
 *
 * @author tangkc
 * @author houxinyu
 * @since 2.11.1
 */
public class ChainRead extends AbstractSqlRead {

    public ChainRead(SQLParserVO config) {
        super(config);
    }

    @Override
    public String buildQuerySql() {
        String chainTableName = super.config.getChainTableName();
        String elDataField = super.config.getElDataField();
        String chainNameField = super.config.getChainNameField();
        String chainApplicationNameField = super.config.getChainApplicationNameField();
        String applicationName = super.config.getApplicationName();
        String chainEnableField = super.config.getChainEnableField();

        if (StrUtil.isBlank(chainTableName)) {
            throw new ELSQLException("You did not define the chainTableName property");
        }

        if (StrUtil.isBlank(applicationName) || StrUtil.isBlank(chainApplicationNameField)) {
            throw new ELSQLException("You did not define the applicationName or chainApplicationNameField property");
        }

        String sqlCmd = StrUtil.format(SqlReadConstant.SQL_PATTERN, chainNameField, elDataField, chainTableName,
                chainApplicationNameField);

        if (StrUtil.isNotBlank(chainEnableField)){
            sqlCmd = StrUtil.format("{} {}", sqlCmd, StrUtil.format(SqlReadConstant.SQL_ENABLE_PATTERN, chainEnableField));
        }

        return sqlCmd;
    }

    @Override
    public String buildXmlElement(ResultSet rs) throws SQLException {
        String elDataField = super.config.getElDataField();

        return getStringFromRs(rs, elDataField);
    }

    @Override
    public String buildXmlElementUniqueKey(ResultSet rs) throws SQLException {
        String chainNameField = super.config.getChainNameField();

        return getStringFromRsWithCheck(rs, chainNameField);
    }

    @Override
    public ReadType type() {
        return ReadType.CHAIN;
    }
}
