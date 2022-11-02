package com.yomahub.liteflow.parser.sql.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.script.ScriptExecutor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * jdbc 工具类
 *
 * @author tangkc
 * @since 2.9.0
 */
public class JDBCHelper {

	private static final String SQL_PATTERN = "SELECT {},{} FROM {} ";

	private static final String SCRIPT_SQL_PATTERN = "SELECT {},{},{},{},{} FROM {} ";

	private static final String CHAIN_XML_PATTERN = "<chain name=\"{}\">{}</chain>";
	private static final String NODE_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\"><![CDATA[{}]]></node>";
	private static final String NODES_XML_PATTERN = "<nodes>{}</nodes>";
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
			Class.forName(sqlParserVO.getDriverClassName());
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
	 * 获取链接
	 */
	public Connection getConn() {
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(), sqlParserVO.getPassword());
		} catch (SQLException e) {
			throw new ELSQLException(e.getMessage());
		}
		return connection;
	}

	/**
	 * 获取 ElData 数据内容
	 */
	public String getElDataContent() {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String elDataField = sqlParserVO.getElDataField();
		String chainNameField = sqlParserVO.getChainNameField();
		String tableName = sqlParserVO.getTableName();
		String sqlCmd = StrFormatter.format(SQL_PATTERN, chainNameField, elDataField, tableName);

		List<String> result = new ArrayList<>();
		try {
			conn = getConn();
			stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			// 设置游标拉取数量
			stmt.setFetchSize(FETCH_SIZE_MAX);
			rs = stmt.executeQuery();

			while (rs.next()) {
				String elData = getStringFromResultSet(rs, elDataField);
				String chainName = getStringFromResultSet(rs, chainNameField);

				result.add(StrFormatter.format(CHAIN_XML_PATTERN, chainName, elData));
			}
		} catch (Exception e) {
			throw new ELSQLException(e.getMessage());
		} finally {
			// 关闭连接
			close(conn, stmt, rs);
		}

		String chains = CollUtil.join(result, StrUtil.EMPTY);
		// 根据 SPI 判断是否需要添加 script node 节点
		ServiceLoader<ScriptExecutor> loader = ServiceLoader.load(ScriptExecutor.class);
		if (loader.iterator().hasNext()) {
			String nodes = getScriptNodes();
			return StrFormatter.format(XML_PATTERN, StrFormatter.format(NODES_XML_PATTERN, nodes), chains);
		}

		return StrFormatter.format(XML_PATTERN, StrUtil.EMPTY, chains);
	}

	public String getScriptNodes() {
		List<String> result = new ArrayList<>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String scriptNodeTableName = sqlParserVO.getScriptNodeTableName();
		String scriptNodeIdField = sqlParserVO.getScriptNodeIdField();
		String scriptNodeDataField = sqlParserVO.getScriptNodeDataField();
		String scriptNodeNameField = sqlParserVO.getScriptNodeNameField();
		String scriptNodeLanguageField = sqlParserVO.getScriptNodeLanguageField();
		String scriptNodeTypeField = sqlParserVO.getScriptNodeTypeField();


		String sqlCmd = StrFormatter.format(
				SCRIPT_SQL_PATTERN,
				scriptNodeIdField,
				scriptNodeDataField,
				scriptNodeNameField,
				scriptNodeLanguageField,
				scriptNodeTypeField,
				scriptNodeTableName
		);
		try {
			conn = getConn();
			stmt = conn.prepareStatement(sqlCmd, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			// 设置游标拉取数量
			stmt.setFetchSize(FETCH_SIZE_MAX);
			rs = stmt.executeQuery();

			while (rs.next()) {
				String id = getStringFromResultSet(rs, scriptNodeIdField);
				String data = getStringFromResultSet(rs, scriptNodeDataField);
				String name = getStringFromResultSet(rs, scriptNodeNameField);
				String type = getStringFromResultSet(rs, scriptNodeTypeField);
				String language = getStringFromResultSet(rs, scriptNodeLanguageField);

				NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
				if (Objects.isNull(nodeTypeEnum)){
					throw new ELSQLException(StrUtil.format("Invalid type value[{}]", type));
				}

				if (!nodeTypeEnum.isScript()) {
					throw new ELSQLException(StrUtil.format("The type value[{}] is not a script type", type));
				}

				result.add(StrFormatter.format(NODE_XML_PATTERN, id, name, type, language, data));
			}
		} catch (Exception e) {
			throw new ELSQLException(e.getMessage());
		} finally {
			// 关闭连接
			close(conn, stmt, rs);
		}
		return CollUtil.join(result, StrUtil.EMPTY);
	}

	/**
	 * 关闭连接
	 *
	 * @param conn conn
	 * @param stmt stmt
	 * @param rs   rs
	 */
	private void close(Connection conn, PreparedStatement stmt, ResultSet rs) {
		// 关闭连接
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				throw new ELSQLException(e.getMessage());
			}
		}
		// 关闭 statement
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				throw new ELSQLException(e.getMessage());
			}
		}
		//关闭结果集
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				throw new ELSQLException(e.getMessage());
			}
		}
	}


	//#region get set method
	private String getStringFromResultSet(ResultSet rs, String field) throws SQLException {
		String data = rs.getString(field);
		if (StrUtil.isBlank(data)) {
			throw new ELSQLException(StrFormatter.format("exist {} field value is empty", field));
		}
		return data;
	}

	private SQLParserVO getSqlParserVO() {
		return sqlParserVO;
	}

	private void setSqlParserVO(SQLParserVO sqlParserVO) {
		this.sqlParserVO = sqlParserVO;
	}
	//#endregion
}
