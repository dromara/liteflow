package com.yomahub.liteflow.test.iterator;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.List;

@TestPropertySource(value = "classpath:/iterator/application_IADIXE.properties")
@SpringBootTest(classes = IteratorCase_IADIXE.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.iterator.cmp" })
public class IteratorCase_IADIXE extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    // 最简单的情况
    @Test
    public void testIt1() throws Exception {
        List<String> list = ListUtil.toList("1", "2", "3");
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", list);
        Assertions.assertTrue(response.isSuccess());
    }

}
