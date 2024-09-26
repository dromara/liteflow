package com.yomahub.liteflow.test;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:application.properties")
@SpringBootTest(classes = ScriptJavaxTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.benchmark.cmp" })
public class ScriptJavaxTest {

    @Resource
    private FlowExecutor flowExecutor;

    // 测试普通脚本节点
    @Test
    public void test1() {
        ExecutorService executorService = new ThreadPoolExecutor(100, 100, 60,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(500), new ThreadFactory() {
            private final AtomicLong number = new AtomicLong();

            @Override
            public Thread newThread(Runnable r) {
                Thread newThread = Executors.defaultThreadFactory().newThread(r);
                newThread.setName("LF" + number.getAndIncrement());
                newThread.setDaemon(false);
                return newThread;
            }
        }, new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 0; i < 10000; i++) {
            executorService.submit(() -> {
                String scriptContent = ResourceUtil.readUtf8Str("classpath:javaxScript.java");
                LiteFlowNodeBuilder.createScriptNode().setId("ds").setScript(scriptContent).build();

                if(!FlowBus.containChain("chain2")){
                    LiteFlowChainELBuilder.createChain().setChainId("chain2").setEL("THEN(ds)").build();
                }
                LiteflowResponse response = flowExecutor.execute2Resp("chain2");
                DefaultContext context = response.getFirstContextBean();
                System.out.println(context.getData("salary").toString());
            });
        }
    }
}
