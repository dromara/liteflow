package com.yomahub.liteflow.test.resizeSlot;

import cn.hutool.core.util.ReflectUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * springboot环境下slot扩容测试
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/resizeSlot/application.properties")
@SpringBootTest(classes = ResizeSlotSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.resizeSlot.cmp"})
public class ResizeSlotSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testSpringboot() throws Exception{
        ExecutorService pool = Executors.newCachedThreadPool();

        List<Future<LiteflowResponse<DefaultSlot>>> futureList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Future<LiteflowResponse<DefaultSlot>> future = pool.submit(() -> flowExecutor.execute2Resp("chain1", "arg"));
            futureList.add(future);
        }

        for(Future<LiteflowResponse<DefaultSlot>> future : futureList){
            Assert.assertTrue(future.get().isSuccess());
        }

        //取到static的对象QUEUE
        Field field = ReflectUtil.getField(DataBus.class, "QUEUE");
        ConcurrentLinkedQueue<Integer> queue = (ConcurrentLinkedQueue<Integer>) ReflectUtil.getStaticFieldValue(field);

        //因为初始slotSize是4，按照0.75的扩容比，要满足100个线程，应该扩容6次，6次之后应该是扩容到114
        Assert.assertEquals(queue.size(),114);
    }
}
