package com.yomahub.liteflow.test.zookeeper;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.test.BaseTest;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.curator.test.TestingServer;
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
import java.util.concurrent.CountDownLatch;

/**
 * springboot环境下的zk配置源功能测试
 * ZK节点存储数据的格式为xml文件
 * @author zendwang
 * @since 2.5.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/zookeeper/application-xml.properties")
@SpringBootTest(classes = ZkNodeWithXmlSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.zookeeper.cmp"})
public class ZkNodeWithXmlSpringbootTest extends BaseTest {
    
    private static final String ZK_NODE_PATH = "/lite-flow/flow";

    private static TestingServer zkServer;
    
    @Resource
    private FlowExecutor flowExecutor;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        zkServer = new TestingServer(21810);
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            String data = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\"><then value=\"a,b,c\"/></chain></flow>";
            ZkClient zkClient = new ZkClient("127.0.0.1:21810");
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
            zkClient.close();
            latch.countDown();
        }).start();
        latch.await();
    }
    
    @Test
    public void test() throws Exception {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        zkServer.stop();
    }
}
