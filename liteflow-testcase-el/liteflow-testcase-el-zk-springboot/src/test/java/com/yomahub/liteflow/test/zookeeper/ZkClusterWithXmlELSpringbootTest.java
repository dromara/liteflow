package com.yomahub.liteflow.test.zookeeper;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingCluster;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.nio.charset.Charset;

/**
 * springboot环境下的zk cluster的测试
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/zookeeper/application-xml-cluster.properties")
@SpringBootTest(classes = ZkClusterWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.zookeeper.cmp"})
public class ZkClusterWithXmlELSpringbootTest extends BaseTest {
    
    private static final String ZK_NODE_PATH = "/lite-flow/flow";

    private static TestingCluster zkCluster;
    
    @Resource
    private FlowExecutor flowExecutor;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        zkCluster = new TestingCluster(new InstanceSpec(null, 21810, -1, -1, true, -1, -1, -1),
                new InstanceSpec(null, 21811, -1, -1, true, -1, -1, -1),
                new InstanceSpec(null, 21812, -1, -1, true, -1, -1, -1));
        zkCluster.start();
        String connectStr = zkCluster.getConnectString();

        String data = ResourceUtil.readUtf8Str("zookeeper/flow.xml");
        ZkClient zkClient = new ZkClient(connectStr);
        zkClient.setZkSerializer(new ZkSerializer() {
            @Override
            public byte[] serialize(final Object o) throws ZkMarshallingError {
                return o.toString().getBytes(Charset.forName("UTF-8"));
            }

            @Override
            public Object deserialize(final byte[] bytes) throws ZkMarshallingError {
                return new String(bytes, Charset.forName("UTF-8"));
            }
        });
        zkClient.createPersistent(ZK_NODE_PATH, true);
        zkClient.writeData(ZK_NODE_PATH, data);
    }
    
    @Test
    public void testZkNodeWithXml() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        zkCluster.stop();
    }
}
