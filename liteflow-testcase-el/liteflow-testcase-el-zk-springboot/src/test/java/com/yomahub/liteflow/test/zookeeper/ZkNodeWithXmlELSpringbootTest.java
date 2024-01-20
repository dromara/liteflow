package com.yomahub.liteflow.test.zookeeper;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * springboot环境下的zk配置源功能测试 ZK节点存储数据的格式为xml文件
 *
 * @author zendwang
 * @since 2.5.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/zookeeper/application-xml.properties")
@SpringBootTest(classes = ZkNodeWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.zookeeper.cmp" })
public class ZkNodeWithXmlELSpringbootTest extends BaseTest {

	private static final String ZK_CHAIN_PATH = "/liteflow/chain";

	private static final String ZK_SCRIPT_PATH = "/liteflow/script";

	private static TestingServer zkServer;

	@Resource
	private FlowExecutor flowExecutor;

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		zkServer = new TestingServer(21810);
		ZkClient zkClient = new ZkClient("127.0.0.1:21810");
		zkClient.setZkSerializer(new ZkSerializer() {
			@Override
			public byte[] serialize(final Object o) throws ZkMarshallingError {
				return o.toString().getBytes(StandardCharsets.UTF_8);
			}

			@Override
			public Object deserialize(final byte[] bytes) throws ZkMarshallingError {
				return new String(bytes, StandardCharsets.UTF_8);
			}
		});
		String chain1Path = ZK_CHAIN_PATH + "/chain1";
		zkClient.createPersistent(chain1Path, true);
		zkClient.writeData(chain1Path, "THEN(a, b, c, s1, s2);");

		String chain2Path = ZK_CHAIN_PATH + "/chain2";
		zkClient.createPersistent(chain2Path, true);
		zkClient.writeData(chain2Path, "THEN(a, b, c, s3);");

		String chain3Path = ZK_CHAIN_PATH + "/chain3:false";
		zkClient.createPersistent(chain3Path, true);
		zkClient.writeData(chain3Path, "THEN(a, b, c, s3);");

		String script1Path = ZK_SCRIPT_PATH + "/s1:script:脚本s1:groovy";
		zkClient.createPersistent(script1Path, true);
		zkClient.writeData(script1Path, "defaultContext.setData(\"test\",\"hello\");");

		String script2Path = ZK_SCRIPT_PATH + "/s2:script:脚本s2:js";
		zkClient.createPersistent(script2Path, true);
		zkClient.writeData(script2Path, "defaultContext.setData(\"test1\",\"hello\");");

		String script3Path = ZK_SCRIPT_PATH + "/s3:script:脚本s3";
		zkClient.createPersistent(script3Path, true);
		zkClient.writeData(script3Path, "defaultContext.setData(\"test\",\"hello\");");

		String script4Path = ZK_SCRIPT_PATH + "/s4:script:脚本s3:groovy:false";
		zkClient.createPersistent(script4Path, true);
		zkClient.writeData(script4Path, "defaultContext.setData(\"test\",\"hello\");");
	}

	@Test
	public void testZkNodeWithXmlWithLanguage() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("hello", context.getData("test"));
		Assertions.assertEquals("hello", context.getData("test1"));
	}

	@Test
	public void testZkNodeWithXml() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("hello", context.getData("test"));

		// 测试 chain 停用
		Assertions.assertThrows(ChainNotFoundException.class, () -> {
			throw flowExecutor.execute2Resp("chain3", "arg").getCause();
		});

		// 测试 script 停用
		Assertions.assertTrue(!FlowBus.getNodeMap().containsKey("s4"));
	}

	@AfterAll
	public static void tearDown() throws Exception {
		BaseTest.cleanScanCache();
		zkServer.stop();
	}

}
