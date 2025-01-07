package com.yomahub.liteflow.parser.sql.read.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.constant.SqlReadConstant;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.read.AbstractSqlRead;
import com.yomahub.liteflow.parser.sql.read.vo.ScriptVO;
import com.yomahub.liteflow.parser.sql.util.LiteFlowJdbcUtil;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * 脚本读取
 *
 * @author tangkc
 * @author houxinyu
 * @author jay li
 * @since 2.11.1
 */
public class ScriptRead extends AbstractSqlRead<ScriptVO> {

    public ScriptRead(SQLParserVO config) {
        super(config);
    }

    @Override
    protected ScriptVO parse(ResultSet rs) throws SQLException {
        ScriptVO scriptVO = new ScriptVO();
        scriptVO.setNodeId(getStringFromRsWithCheck(rs, super.config.getScriptIdField()));
        scriptVO.setName(getStringFromRs(rs, super.config.getScriptNameField()));
        scriptVO.setType(getStringFromRsWithCheck(rs, super.config.getScriptTypeField()));
        scriptVO.setLanguage(getStringFromRs(rs, super.config.getScriptLanguageField()));
        scriptVO.setScript(getStringFromRsWithCheck(rs, super.config.getScriptDataField()));
        return scriptVO;
    }

    @Override
    public boolean hasEnableFiled() {
        String scriptEnableField = super.config.getScriptEnableField();
        return StrUtil.isNotBlank(scriptEnableField);
    }

    @Override
    public boolean getEnableFiledValue(ResultSet rs) throws SQLException {
        String scriptEnableField = super.config.getScriptEnableField();
        byte enable = rs.getByte(scriptEnableField);

        return enable == 1;
    }

    @Override
    public String buildQuerySql() {
        if (StringUtils.isNotBlank(super.config.getScriptCustomSql())) {
            return super.config.getScriptCustomSql();
        }

        String scriptTableName = super.config.getScriptTableName();
        String scriptApplicationNameField = super.config.getScriptApplicationNameField();
        String applicationName = super.config.getApplicationName();
        return StrUtil.format(
                SqlReadConstant.SCRIPT_SQL_PATTERN,
                scriptTableName,
                scriptApplicationNameField,
                applicationName);
    }

    @Override
    public String buildQuerySql(String scriptNodeId) {
        if (StringUtils.isNotBlank(super.config.getScriptCustomSql())) {
            return super.config.getScriptCustomSql();
        }

        String scriptTableName = super.config.getScriptTableName();
        String scriptApplicationNameField = super.config.getScriptApplicationNameField();
        String applicationName = super.config.getApplicationName();
        String scriptIdField = super.config.getScriptIdField();
        return StrUtil.format(SqlReadConstant.SQL_PATTERN_WITH_CHAIN_ID,
                scriptTableName, scriptApplicationNameField, applicationName,
                scriptIdField, scriptNodeId);
    }

    @Override
    public void checkConfig() {
        String scriptTableName = super.config.getScriptTableName();
        String scriptIdField = super.config.getScriptIdField();
        String scriptDataField = super.config.getScriptDataField();
        String scriptTypeField = super.config.getScriptTypeField();
        String scriptApplicationNameField = super.config.getScriptApplicationNameField();

        if (StrUtil.isBlank(scriptTableName)) {
            throw new ELSQLException("You did not define the scriptTableName property");
        }
        if (StrUtil.isBlank(scriptIdField)) {
            throw new ELSQLException("You did not define the scriptIdField property");
        }
        if (StrUtil.isBlank(scriptDataField)) {
            throw new ELSQLException("You did not define the scriptDataField property");
        }
        if (StrUtil.isBlank(scriptTypeField)) {
            throw new ELSQLException("You did not define the scriptTypeField property");
        }
        if (StrUtil.isBlank(scriptApplicationNameField)) {
            throw new ELSQLException("You did not define the scriptApplicationNameField property");
        }
    }

    @Override
    public boolean needRead() {
        if (StrUtil.isBlank(super.config.getScriptTableName())) {
            return false;
        }

        String sqlCmd = StrUtil.format(
                SqlReadConstant.SCRIPT_SQL_CHECK_PATTERN,
                super.config.getScriptTableName()
        );

        Connection conn = LiteFlowJdbcUtil.getConn(super.config);
        return LiteFlowJdbcUtil.checkConnectionCanExecuteSql(conn, sqlCmd);
    }

    @Override
    public ReadType type() {
        return ReadType.SCRIPT;
    }

    /**
     * 脚本是否带语言
     */
    private boolean withLanguage() {
        return StrUtil.isNotBlank(super.config.getScriptLanguageField());
    }
}
