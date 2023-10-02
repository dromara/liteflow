package com.yomahub.liteflow.test.sql;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.util.JDBCHelper;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author hxinyu
 * @since 2.11.1
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/application-poll-xml.properties")
@SpringBootTest(classes = SQLWithXmlELSpringbootPollingTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.sql.cmp" })
public class SQLWithXmlELSpringbootPollingTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	static LFLog LOG = LFLoggerManager.getLogger(SQLWithXmlELSpringbootPollingTest.class);

	@AfterAll
    public static void after(){
        try{
            //关闭定时轮询线程池
            Field pollExecutor = JDBCHelper.class.getDeclaredField("pollExecutor");
            pollExecutor.setAccessible(true);
            ScheduledThreadPoolExecutor threadPoolExecutor = (ScheduledThreadPoolExecutor) pollExecutor.get(null);
            threadPoolExecutor.shutdownNow();
			LOG.info("[SQL Polling thread pool closed]");
        }catch (Exception ignored) {
            LOG.error("[SQL Polling thread pool not closed]", ignored);
        }
    }

	@Test
	public void testSQLWithXml() throws InterruptedException {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());

		// 修改chain
		changeData();
		Thread.sleep(4000);
		Assertions.assertEquals("a==>c==>b", flowExecutor.execute2Resp("chain1", "arg").getExecuteStepStr());

		// 新增chain
		insertData();
		Thread.sleep(4000);
		Assertions.assertEquals("a==>b", flowExecutor.execute2Resp("chain5", "arg").getExecuteStepStr());

		// 删除 chain
		deleteData();
		Thread.sleep(4000);
		Exception cause = flowExecutor.execute2Resp("chain5", "arg").getCause();
		Assertions.assertTrue(cause instanceof ChainNotFoundException,"删除 chain 测试失败");
	}


	@Test
	public void testSQLWithScriptXml() throws InterruptedException {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("x1[if 脚本]==>a==>b", response.getExecuteStepStrWithoutTime());

		// 修改script
		changeScriptData();
		Thread.sleep(4000);
		Assertions.assertEquals("x1[if 脚本]", flowExecutor.execute2Resp("chain2", "arg").getExecuteStepStr());

		// 新増script
		insertScriptData();
		Thread.sleep(2500);
		insertChainData();
		Thread.sleep(2500);
		response = flowExecutor.execute2Resp("chain6", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertEquals("a==>x3[x3脚本]", response.getExecuteStepStrWithoutTime());
		Assertions.assertEquals("hello", context.getData("test"));

	}

	/**
	 * 删除chain数据
	 */
	private void deleteData(){
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
		Connection connection;
		try {
			connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
					sqlParserVO.getPassword());
			Statement statement = connection.createStatement();
			statement.executeUpdate("DELETE FROM  EL_TABLE WHERE chain_name='chain5'");
		}
		catch (SQLException e) {
			throw new ELSQLException(e.getMessage());
		}
	}

	/**
	 * 修改chain数据
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
	 * 增加chain数据
	 */
	private void insertData() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
		Connection connection;
		try {
			connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
					sqlParserVO.getPassword());
			Statement statement = connection.createStatement();
			statement.executeUpdate("INSERT INTO EL_TABLE (APPLICATION_NAME,CHAIN_NAME,EL_DATA) values ('demo','chain5','THEN(a, b);')");
		}
		catch (SQLException e) {
			throw new ELSQLException(e.getMessage());
		}
	}

	/**
	 * 修改脚本数据
	 */
	private void changeScriptData() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
		Connection connection;
		try {
			connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
					sqlParserVO.getPassword());
			Statement statement = connection.createStatement();
			//修改script data
			statement.executeUpdate(
					"UPDATE SCRIPT_NODE_TABLE SET SCRIPT_NODE_DATA='return false' WHERE SCRIPT_NODE_ID='x1'");
			//修改script名
			statement.executeUpdate(
					"UPDATE SCRIPT_NODE_TABLE SET SCRIPT_NODE_NAME='x0_script' WHERE SCRIPT_NODE_ID='x0'");
		}
		catch (SQLException e) {
			throw new ELSQLException(e.getMessage());
		}
	}

	/**
	 * 增加脚本数据
	 */
	private void insertScriptData() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
		Connection connection;
		try {
			connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
					sqlParserVO.getPassword());
			Statement statement = connection.createStatement();
			statement.executeUpdate(
					"INSERT INTO SCRIPT_NODE_TABLE (APPLICATION_NAME,SCRIPT_NODE_ID,SCRIPT_NODE_NAME,SCRIPT_NODE_TYPE,SCRIPT_NODE_DATA,SCRIPT_LANGUAGE) values ('demo','x3','x3脚本','script','defaultContext.setData(\"test\",\"hello\");','groovy');");
		}
		catch (SQLException e) {
			throw new ELSQLException(e.getMessage());
		}
	}

	/**
	 * 删除脚本
	 */
	private void deleteScriptData() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
		Connection connection;
		try {
			connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
					sqlParserVO.getPassword());
			Statement statement = connection.createStatement();
			statement.executeUpdate(
					"DELETE FROM SCRIPT_NODE_TABLE WHERE SCRIPT_NODE_ID = 'x3'");
		}
		catch (SQLException e) {
			throw new ELSQLException(e.getMessage());
		}
	}

	private void insertChainData() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
		Connection connection;
		try {
			connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
					sqlParserVO.getPassword());
			Statement statement = connection.createStatement();
			statement.executeUpdate(
					"INSERT INTO EL_TABLE (APPLICATION_NAME,CHAIN_NAME,EL_DATA) values ('demo','chain6','THEN(a, x3);');");
		}
		catch (SQLException e) {
			throw new ELSQLException(e.getMessage());
		}
	}
}
