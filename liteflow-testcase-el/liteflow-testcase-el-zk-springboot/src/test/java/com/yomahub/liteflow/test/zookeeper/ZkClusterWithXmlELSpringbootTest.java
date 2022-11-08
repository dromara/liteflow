package com.yomahub.liteflow.test.zookeeper;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
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
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * springboot环境下的zk cluster的测试
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/zookeeper/application-xml-cluster.properties")
@SpringBootTest(classes = ZkClusterWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.zookeeper.cmp"})
public class ZkClusterWithXmlELSpringbootTest extends BaseTest {
    
    private static final String ZK_CHAIN_PATH = "/liteflow/chain";

    private static final String ZK_SCRIPT_PATH = "/liteflow/script";

    private static TestingCluster zkCluster;
    
    @Resource
    private FlowExecutor flowExecutor;

    @BeforeClass
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

        String chain1Path = ZK_CHAIN_PATH+"/chain1";
        zkClient.createPersistent(chain1Path, true);
        zkClient.writeData(chain1Path, "THEN(a, b, c, s1);");

        String script1Path = ZK_SCRIPT_PATH+"/s1:script:脚本s1";
        zkClient.createPersistent(script1Path, true);
        zkClient.writeData(script1Path, "defaultContext.setData(\"test\",\"hello\");");

        Thread.sleep(2000L);
    }
    
    @Test
    public void testZkNodeWithXml() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("hello", context.getData("test"));
    }
}
