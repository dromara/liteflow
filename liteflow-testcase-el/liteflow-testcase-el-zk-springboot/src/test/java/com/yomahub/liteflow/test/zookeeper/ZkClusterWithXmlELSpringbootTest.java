package com.yomahub.liteflow.test.zookeeper;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingCluster;
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
 * springboot环境下的zk cluster的测试
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/zookeeper/application-xml-cluster.properties")
@SpringBootTest(classes = ZkClusterWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.zookeeper.cmp" })
public class ZkClusterWithXmlELSpringbootTest extends BaseTest {

	private static final String ZK_CHAIN_PATH = "/liteflow/chain";

	private static final String ZK_SCRIPT_PATH = "/liteflow/script";

	private static TestingCluster zkCluster;

	@Resource
	private FlowExecutor flowExecutor;

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("zookeeper.admin.enableServer", "false");

		zkCluster = new TestingCluster(new InstanceSpec(null, 21810, -1, -1, true, -1, -1, -1),
				new InstanceSpec(null, 21811, -1, -1, true, -1, -1, -1),
				new InstanceSpec(null, 21812, -1, -1, true, -1, -1, -1));
		zkCluster.start();
		String connectStr = zkCluster.getConnectString();

		ZkClient zkClient = new ZkClient(connectStr);
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

		String script1Path = ZK_SCRIPT_PATH + "/s1:script:脚本s1:groovy";
		zkClient.createPersistent(script1Path, true);
		zkClient.writeData(script1Path, "defaultContext.setData(\"test\",\"hello\");");

		String script2Path = ZK_SCRIPT_PATH + "/s2:script:脚本s2:js";
		zkClient.createPersistent(script2Path, true);
		zkClient.writeData(script2Path, "defaultContext.setData(\"test1\",\"hello\");");

		String script3Path = ZK_SCRIPT_PATH + "/s3:script:脚本s3";
		zkClient.createPersistent(script3Path, true);
		zkClient.writeData(script3Path, "defaultContext.setData(\"test\",\"hello\");");

		Thread.sleep(2000L);
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
	}

}
