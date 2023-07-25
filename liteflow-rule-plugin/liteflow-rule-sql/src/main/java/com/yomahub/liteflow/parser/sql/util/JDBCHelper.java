package com.yomahub.liteflow.parser.sql.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * jdbc 工具类
 *
 * @author tangkc
 * @since 2.9.0
 */
public class JDBCHelper {

    private static final String SQL_PATTERN = "SELECT {},{} FROM {} WHERE {}=?";

    private static final String SCRIPT_SQL_CHECK_PATTERN = "SELECT 1 FROM {} WHERE {}=?";

    private static final String SCRIPT_SQL_PATTERN = "SELECT {},{},{},{} FROM {} WHERE {}=?";

    private static final String SCRIPT_WITH_LANGUAG_SQL_PATTERN = "SELECT {},{},{},{},{} FROM {} WHERE {}=?";

    private static final String CHAIN_XML_PATTERN = "<chain name=\"{}\"><![CDATA[{}]]></chain>";

    private static final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    private static final String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

    private static final String NODE_ITEM_WITH_LANGUAGE_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\"><![CDATA[{}]]></node>";

    private static final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    private static final Integer FETCH_SIZE_MAX = 1000;

    private SQLParserVO sqlParserVO;

    private static JDBCHelper INSTANCE;

    /**
     * 初始化 INSTANCE
     */
    public static void init(SQLParserVO sqlParserVO) {
        try {
            INSTANCE = new JDBCHelper();
            if (StrUtil.isNotBlank(sqlParserVO.getDriverClassName())) {
                Class.forName(sqlParserVO.getDriverClassName());
            }
            INSTANCE.setSqlParserVO(sqlParserVO);
        } catch (ClassNotFoundException e) {
            throw new ELSQLException(e.getMessage());
        }
    }

    /**
     * 获取 INSTANCE
     */
    public static JDBCHelper getInstance() {
        return INSTANCE;
    }

    /**
     * 获取 ElData 数据内容
     */
    public String getContent() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String chainTableName = sqlParserVO.getChainTableName();
        String elDataField = sqlParserVO.getElDataField();
        String chainNameField = sqlParserVO.getChainNameField();
        String chainApplicationNameField = sqlParserVO.getChainApplicationNameField();
        String applicationName = sqlParserVO.getApplicationName();

        if (StrUtil.isBlank(chainTableName)) {
            throw new ELSQLException("You did not define the chainTableName property");
        }

        if (StrUtil.isBlank(applicationName) || StrUtil.isBlank(chainApplicationNameField)) {
            throw new ELSQLException("You did not define the applicationName or chainApplicationNameField property");
        }

        String sqlCmd = StrUtil.format(SQL_PATTERN, chainNameField, elDataField, chainTableName,
                chainApplicationNameField);

        List<String> result = new ArrayList<>();
        try {
            conn = LiteFlowJdbcUtil.getConn(sqlParserVO);
            stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 设置游标拉取数量
            stmt.setFetchSize(FETCH_SIZE_MAX);
            stmt.setString(1, applicationName);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String elData = getStringFromResultSet(rs, elDataField);
                String chainName = getStringFromResultSet(rs, chainNameField);

                result.add(StrUtil.format(CHAIN_XML_PATTERN, XmlUtil.escape(chainName), elData));
            }
        } catch (Exception e) {
            throw new ELSQLException(e.getMessage());
        } finally {
            // 关闭连接
            LiteFlowJdbcUtil.close(conn, stmt, rs);
        }

        String chainsContent = CollUtil.join(result, StrUtil.EMPTY);

        String nodesContent;
        if (hasScriptData()) {
            nodesContent = getScriptNodes();
        } else {
            nodesContent = StrUtil.EMPTY;
        }

        return StrUtil.format(XML_PATTERN, nodesContent, chainsContent);
    }

    /**
     * 获取脚本节点内容
     */
    public String getScriptNodes() {
        String scriptLanguageField = sqlParserVO.getScriptLanguageField();
        if (StrUtil.isNotBlank(scriptLanguageField)) {
            return getScriptNodesWithLanguage();
        }

        List<String> result = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String scriptTableName = sqlParserVO.getScriptTableName();
        String scriptIdField = sqlParserVO.getScriptIdField();
        String scriptDataField = sqlParserVO.getScriptDataField();
        String scriptNameField = sqlParserVO.getScriptNameField();
        String scriptTypeField = sqlParserVO.getScriptTypeField();
        String scriptApplicationNameField = sqlParserVO.getScriptApplicationNameField();
        String applicationName = sqlParserVO.getApplicationName();

        if (StrUtil.isBlank(applicationName) || StrUtil.isBlank(scriptApplicationNameField)) {
            throw new ELSQLException("You did not define the applicationName or scriptApplicationNameField property");
        }

        String sqlCmd = StrUtil.format(SCRIPT_SQL_PATTERN, scriptIdField, scriptDataField, scriptNameField,
                scriptTypeField, scriptTableName, scriptApplicationNameField);
        try {
            conn = LiteFlowJdbcUtil.getConn(sqlParserVO);
            stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 设置游标拉取数量
            stmt.setFetchSize(FETCH_SIZE_MAX);
            stmt.setString(1, applicationName);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String id = getStringFromResultSet(rs, scriptIdField);
                String data = getStringFromResultSet(rs, scriptDataField);
                String name = getStringFromResultSet(rs, scriptNameField);
                String type = getStringFromResultSet(rs, scriptTypeField);

                NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
                if (Objects.isNull(nodeTypeEnum)) {
                    throw new ELSQLException(StrUtil.format("Invalid type value[{}]", type));
                }

                if (!nodeTypeEnum.isScript()) {
                    throw new ELSQLException(StrUtil.format("The type value[{}] is not a script type", type));
                }

                result.add(StrUtil.format(NODE_ITEM_XML_PATTERN, XmlUtil.escape(id), XmlUtil.escape(name), type, data));
            }
        } catch (Exception e) {
            throw new ELSQLException(e.getMessage());
        } finally {
            // 关闭连接
            LiteFlowJdbcUtil.close(conn, stmt, rs);
        }
        return StrUtil.format(NODE_XML_PATTERN, CollUtil.join(result, StrUtil.EMPTY));
    }

    /**
     * 获取脚本节点（带语言）
     *
     * @return
     */
    public String getScriptNodesWithLanguage() {

        List<String> result = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String scriptTableName = sqlParserVO.getScriptTableName();
        String scriptIdField = sqlParserVO.getScriptIdField();
        String scriptDataField = sqlParserVO.getScriptDataField();
        String scriptNameField = sqlParserVO.getScriptNameField();
        String scriptTypeField = sqlParserVO.getScriptTypeField();
        String scriptApplicationNameField = sqlParserVO.getScriptApplicationNameField();
        String applicationName = sqlParserVO.getApplicationName();
        String scriptLanguageField = sqlParserVO.getScriptLanguageField();

        if (StrUtil.isBlank(applicationName) || StrUtil.isBlank(scriptApplicationNameField)) {
            throw new ELSQLException("You did not define the applicationName or scriptApplicationNameField property");
        }

        String sqlCmd = StrUtil.format(SCRIPT_WITH_LANGUAG_SQL_PATTERN, scriptIdField, scriptDataField, scriptNameField,
                scriptTypeField, scriptLanguageField, scriptTableName, scriptApplicationNameField);
        try {
            conn = LiteFlowJdbcUtil.getConn(sqlParserVO);
            stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 设置游标拉取数量
            stmt.setFetchSize(FETCH_SIZE_MAX);
            stmt.setString(1, applicationName);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String id = getStringFromResultSet(rs, scriptIdField);
                String data = getStringFromResultSet(rs, scriptDataField);
                String name = getStringFromResultSet(rs, scriptNameField);
                String type = getStringFromResultSet(rs, scriptTypeField);
                String language = getStringFromResultSet(rs, scriptLanguageField);

                NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
                if (Objects.isNull(nodeTypeEnum)) {
                    throw new ELSQLException(StrUtil.format("Invalid type value[{}]", type));
                }

                if (!nodeTypeEnum.isScript()) {
                    throw new ELSQLException(StrUtil.format("The type value[{}] is not a script type", type));
                }

                if (!ScriptTypeEnum.checkScriptType(language)) {
                    throw new ELSQLException(StrUtil.format("The language value[{}] is error", language));
                }

                result.add(StrUtil.format(NODE_ITEM_WITH_LANGUAGE_XML_PATTERN, XmlUtil.escape(id), XmlUtil.escape(name),
                        type, language, data));
            }
        } catch (Exception e) {
            throw new ELSQLException(e.getMessage());
        } finally {
            // 关闭连接
            LiteFlowJdbcUtil.close(conn, stmt, rs);
        }
        return StrUtil.format(NODE_XML_PATTERN, CollUtil.join(result, StrUtil.EMPTY));
    }

    private boolean hasScriptData() {
        if (StrUtil.isBlank(sqlParserVO.getScriptTableName())) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sqlCmd = StrUtil.format(SCRIPT_SQL_CHECK_PATTERN, sqlParserVO.getScriptTableName(),
                sqlParserVO.getScriptApplicationNameField());
        try {
            conn = LiteFlowJdbcUtil.getConn(sqlParserVO);
            stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(1);
            stmt.setString(1, sqlParserVO.getApplicationName());
            rs = stmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            return false;
        } finally {
            // 关闭连接
            LiteFlowJdbcUtil.close(conn, stmt, rs);
        }
    }

    private String getStringFromResultSet(ResultSet rs, String field) throws SQLException {
        String data = rs.getString(field);
        if (StrUtil.isBlank(data)) {
            throw new ELSQLException(StrUtil.format("exist {} field value is empty", field));
        }
        return data;
    }

    private SQLParserVO getSqlParserVO() {
        return sqlParserVO;
    }

    private void setSqlParserVO(SQLParserVO sqlParserVO) {
        this.sqlParserVO = sqlParserVO;
    }

}
