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
public class TestRunFlow {

    @Resource
    private FlowExecutor flowExecutor;

    private String init(List<String> steps) {

        String requestId = UUID.randomUUID().toString();

        caseInit(requestId, steps.stream().map(ConcurrentCase.Routers::new).collect(Collectors.toList()));

        return requestId;
    }

    @Test
    public void mixedRunByErrorResumeTest() throws Exception {
        //由于errorResume，即使p5执行失败抛出异常, p7， p8也将会执行
        String requestId = init(Arrays.asList("s1", "s2", "s3", "s4", "s5", "s6", "p3", "p4", "p6", "p7", "p8"));

        flowExecutor.execute("test-errorResume", requestId);

        caseAssertRandom(requestId);
    }


    @Test
    public void mixedRunByErrorBreakTest() throws Exception {
        //由于errorBreak，p5执行失败抛出异常, p7， p8将不会执行
        String requestId = init(Arrays.asList("s1", "s2", "s3", "s4", "s5", "s6", "p3", "p4", "p6"));

        flowExecutor.execute("test-errorBreak", requestId);

        caseAssertRandom(requestId);
    }

    @Test
    public void parallelTest() throws InterruptedException {
        //测试2个线程并发时，所执行的序列是正常的，线程安全的（slotIndex在每个执行序列chain中都是不变的）
        String requestId1 = init(Arrays.asList("c1", "c2", "c3", "c4", "c5"));
        String requestId2 = init(Arrays.asList("c6", "c7", "c8", "c9", "c10"));

        List<Thread> ts = Arrays.asList(
                newExecutor("async-concurrent1", requestId1),
                newExecutor("async-concurrent2", requestId2)
        );
        ts.forEach(Thread::start);

        for (Thread t : ts) {
            t.join();
        }

        caseAssertRandom(requestId1);
        caseAssertRandom(requestId2);
    }

    private Thread newExecutor(String chain, String requestId) {
        return new Thread(() -> {
            try {
                flowExecutor.execute(chain, requestId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
