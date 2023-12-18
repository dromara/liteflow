package com.yomahub.liteflow.parser.sql.read.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.constant.SqlReadConstant;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.read.AbstractSqlRead;
import com.yomahub.liteflow.parser.sql.util.LiteFlowJdbcUtil;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * 脚本读取
 *
 * @author tangkc
 * @author houxinyu
 * @since 2.11.1
 */
public class ScriptRead extends AbstractSqlRead {

    public ScriptRead(SQLParserVO config) {
        super(config);
    }

    @Override
    public String buildQuerySql() {
        String scriptLanguageField = super.config.getScriptLanguageField();
        String scriptTableName = super.config.getScriptTableName();
        String scriptIdField = super.config.getScriptIdField();
        String scriptDataField = super.config.getScriptDataField();
        String scriptNameField = super.config.getScriptNameField();
        String scriptTypeField = super.config.getScriptTypeField();
        String scriptApplicationNameField = super.config.getScriptApplicationNameField();
        String applicationName = super.config.getApplicationName();
        String scriptEnableField = super.config.getScriptEnableField();

        if (StrUtil.isBlank(applicationName) || StrUtil.isBlank(scriptApplicationNameField)) {
            throw new ELSQLException("You did not define the applicationName or scriptApplicationNameField property");
        }

        String sqlCmd = null;
        // 脚本节点（带语言）
        if (withLanguage()) {
            sqlCmd = StrUtil.format(
                    SqlReadConstant.SCRIPT_WITH_LANGUAGE_SQL_PATTERN,
                    scriptIdField,
                    scriptDataField,
                    scriptNameField,
                    scriptTypeField,
                    scriptLanguageField,
                    scriptTableName,
                    scriptApplicationNameField
            );
        }
        // 脚本节点（不带语言）
        else {
            sqlCmd = StrUtil.format(
                    SqlReadConstant.SCRIPT_SQL_PATTERN,
                    scriptIdField,
                    scriptDataField,
                    scriptNameField,
                    scriptTypeField,
                    scriptTableName,
                    scriptApplicationNameField
            );
        }

        if (StrUtil.isNotBlank(scriptEnableField)){
            sqlCmd = StrUtil.format("{} {}", sqlCmd, StrUtil.format(SqlReadConstant.SQL_ENABLE_PATTERN, scriptEnableField));
        }

        return sqlCmd;
    }

    @Override
    public String buildXmlElement(ResultSet rs) throws SQLException {
        String scriptDataField = super.config.getScriptDataField();

        return getStringFromRs(rs, scriptDataField);

    }

    @Override
    public String buildXmlElementUniqueKey(ResultSet rs) throws SQLException {
        String scriptIdField = super.config.getScriptIdField();
        String scriptNameField = super.config.getScriptNameField();
        String scriptTypeField = super.config.getScriptTypeField();
        String scriptLanguageField = super.config.getScriptLanguageField();

        String id = getStringFromRsWithCheck(rs, scriptIdField);
        String name = getStringFromRsWithCheck(rs, scriptNameField);
        String type = getStringFromRsWithCheck(rs, scriptTypeField);
        String language = withLanguage() ? getStringFromRs(rs, scriptLanguageField) : null;

        NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
        if (Objects.isNull(nodeTypeEnum)) {
            throw new ELSQLException(StrUtil.format("Invalid type value[{}]", type));
        }

        if (!nodeTypeEnum.isScript()) {
            throw new ELSQLException(StrUtil.format("The type value[{}] is not a script type", type));
        }

        if (withLanguage() && !ScriptTypeEnum.checkScriptType(language)) {
            throw new ELSQLException(StrUtil.format("The language value[{}] is invalid", language));
        }
        List<String> keys = CollUtil.newArrayList(id, type, name);
        if (StrUtil.isNotBlank(language)) {
            keys.add(language);
        }

        return StrUtil.join(StrUtil.COLON, keys);
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
