package com.yomahub.liteflow.test.sql;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author tangkc
 * @since 2.9.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/application-xml.properties")
@SpringBootTest(classes = SQLWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.sql.cmp" })
public class SQLWithXmlELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testSQLWithXml() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertEquals("a==>b==>c", response.getExecuteStepStr());

		// 修改数据库
		changeData();

		// 重新加载规则
		flowExecutor.reloadRule();
		Assert.assertEquals("a==>c==>b", flowExecutor.execute2Resp("chain1", "arg").getExecuteStepStr());
	}

	@Test
	public void testSQLWithScriptXml() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x0[if 脚本]==>a==>b", response.getExecuteStepStrWithoutTime());

		// 修改数据库
		changeScriptData();
		// 重新加载规则
		flowExecutor.reloadRule();
		Assert.assertEquals("x0[if 脚本]", flowExecutor.execute2Resp("chain3", "arg").getExecuteStepStr());
	}

	@Test
	public void testMultiLanguage1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x2[python脚本]==>x0[if 脚本]==>a==>b", response.getExecuteStepStrWithoutTime());
	}

	/**
	 * 修改数据库数据
	 */
	private void changeData() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
		Connection connection;
		try {
			connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
					sqlParserVO.getPassword());
			Statement statement = connection.createStatement();
			statement.executeUpdate("UPDATE EL_TABLE SET EL_DATA='THEN(a, c, b);' WHERE chain_name='chain1'");
		}
		catch (SQLException e) {
			throw new ELSQLException(e.getMessage());
		}
	}

	/**
	 * 修改数据库数据
	 */
	private void changeScriptData() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
		Connection connection;
		try {
			connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
					sqlParserVO.getPassword());
			Statement statement = connection.createStatement();
			statement.executeUpdate(
					"UPDATE SCRIPT_NODE_TABLE SET SCRIPT_NODE_DATA='return false;' WHERE SCRIPT_NODE_ID='x0'");
		}
		catch (SQLException e) {
			throw new ELSQLException(e.getMessage());
		}
	}

}
