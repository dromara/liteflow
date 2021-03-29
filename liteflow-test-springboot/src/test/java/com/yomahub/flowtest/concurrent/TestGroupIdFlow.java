package com.yomahub.flowtest.concurrent;

import com.yomahub.liteflow.core.FlowExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.yomahub.flowtest.concurrent.ConcurrentCase.caseAssertRandom;
import static com.yomahub.flowtest.concurrent.ConcurrentCase.caseInit;

/**
 * 测试流程的顺序执行、并发执行等
 * @author justin.xu
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestGroupIdFlow {

    @Resource
    private FlowExecutor flowExecutor;

    private String init(List<String> steps) {

        String requestId = UUID.randomUUID().toString();

        caseInit(requestId, steps.stream().map(ConcurrentCase.Routers::new).collect(Collectors.toList()));

        return requestId;
    }

    @Test
    public void whenConditionGroupTest() throws Exception {
        //由于errorResume，即使p5执行失败抛出异常, p7， p8也将会执行
        String requestId = init(Arrays.asList("s1", "s2", "s3", "s4", "s5", "s6", "p3", "p4", "p6", "p7", "p8"));

        flowExecutor.execute("test-groupId", requestId);

        caseAssertRandom(requestId);
    }
}
