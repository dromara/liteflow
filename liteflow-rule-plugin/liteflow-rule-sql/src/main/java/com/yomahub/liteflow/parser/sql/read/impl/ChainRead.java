package com.yomahub.liteflow.parser.sql.read.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.constant.SqlReadConstant;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.read.AbstractSqlRead;
import com.yomahub.liteflow.parser.sql.read.vo.ChainVO;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * chain 读取
 *
 * @author tangkc
 * @author houxinyu
 * @author jay li
 * @since 2.11.1
 */
public class ChainRead extends AbstractSqlRead<ChainVO> {

    public ChainRead(SQLParserVO config) {
        super(config);
    }

    @Override
    protected ChainVO parse(ResultSet rs) throws SQLException {
        ChainVO chainVO = new ChainVO();
        chainVO.setChainId(getStringFromRsWithCheck(rs, super.config.getChainNameField()));
        chainVO.setBody(getStringFromRsWithCheck(rs, super.config.getElDataField()));
        if (StrUtil.isNotBlank(super.config.getNamespaceField())) {
            chainVO.setNamespace(getStringFromRs(rs, super.config.getNamespaceField()));
        }
        if (StrUtil.isNotBlank(super.config.getRouteField())) {
            chainVO.setRoute(getStringFromRs(rs, super.config.getRouteField()));
        }
        return chainVO;
    }

    @Override
    public boolean hasEnableFiled() {
        String chainEnableField = super.config.getChainEnableField();
        return StrUtil.isNotBlank(chainEnableField);
    }

    @Override
    public boolean getEnableFiledValue(ResultSet rs) throws SQLException {
        String chainEnableField = super.config.getChainEnableField();
        byte enable = rs.getByte(chainEnableField);

        return enable == 1;
    }

    @Override
    public String buildQuerySql() {
        if (StrUtil.isNotBlank(super.config.getChainCustomSql())) {
            return super.config.getChainCustomSql();
        }

        String chainTableName = super.config.getChainTableName();
        String chainApplicationNameField = super.config.getChainApplicationNameField();
        String applicationName = super.config.getApplicationName();

        return StrUtil.format(SqlReadConstant.SQL_PATTERN, chainTableName, chainApplicationNameField, applicationName);
    }

    @Override
    public String buildQuerySql(String chainId) {
        if (StrUtil.isNotBlank(super.config.getChainCustomSql())) {
            return super.config.getChainCustomSql();
        }

        String chainTableName = super.config.getChainTableName();
        String chainApplicationNameField = super.config.getChainApplicationNameField();
        String applicationName = super.config.getApplicationName();

        return StrUtil.format(SqlReadConstant.SQL_PATTERN_WITH_CHAIN_ID, chainTableName, chainApplicationNameField, applicationName,
                super.config.getChainNameField(), chainId);
    }

    @Override
    public void checkConfig() {
        String chainTableName = super.config.getChainTableName();
        String elDataField = super.config.getElDataField();
        String chainNameField = super.config.getChainNameField();
        String chainApplicationNameField = super.config.getChainApplicationNameField();
        String applicationName = super.config.getApplicationName();

        if (StrUtil.isBlank(chainTableName)) {
            throw new ELSQLException("You did not define the chainTableName property");
        }
        if (StrUtil.isBlank(elDataField)) {
            throw new ELSQLException("You did not define the elDataField property");
        }
        if (StrUtil.isBlank(chainNameField)) {
            throw new ELSQLException("You did not define the chainNameField property");
        }
        if (StrUtil.isBlank(chainApplicationNameField)) {
            throw new ELSQLException("You did not define the chainApplicationNameField property");
        }
        if (StrUtil.isBlank(applicationName)) {
            throw new ELSQLException("You did not define the applicationName property");
        }
    }

    @Override
    public ReadType type() {
        return ReadType.CHAIN;
    }
}
