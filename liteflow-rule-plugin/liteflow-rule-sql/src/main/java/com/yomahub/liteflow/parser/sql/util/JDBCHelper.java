package com.yomahub.liteflow.parser.sql.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * jdbc 工具类
 *
 * @author tangkc
 * @since 2.9.0
 */
public class JDBCHelper {

	private static final String SQL_PATTERN = "SELECT {},{} FROM {} ";

	private static final String CHAIN_XML_PATTERN = "<chain name=\"{}\">{}</chain>";
	private static final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}</flow>";
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
				String elData = rs.getString(elDataField);
				if (StrUtil.isBlank(elData)) {
					throw new ELSQLException(StrFormatter.format("{} table exist {} field value is empty", tableName, elDataField));
				}
				String chainName = rs.getString(chainNameField);
				if (StrUtil.isBlank(elData)) {
					throw new ELSQLException(StrFormatter.format("{} table exist {} field value is empty", tableName, elDataField));
				}

				result.add(StrFormatter.format(CHAIN_XML_PATTERN, chainName, elData));
			}
		} catch (Exception e) {
			throw new ELSQLException(e.getMessage());
		} finally {
			// 关闭连接
			close(conn, stmt, rs);
		}

		String chains = CollUtil.join(result, StrUtil.CRLF);
		return StrFormatter.format(XML_PATTERN, chains);
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
	private SQLParserVO getSqlParserVO() {
		return sqlParserVO;
	}

	private void setSqlParserVO(SQLParserVO sqlParserVO) {
		this.sqlParserVO = sqlParserVO;
	}
	//#endregion
}
