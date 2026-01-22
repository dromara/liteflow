package com.yomahub.liteflow.benchmark;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.benchmark.bean.PriceCalcReqVO;
import com.yomahub.liteflow.benchmark.context.PriceContext;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.util.JsonUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@EnableAutoConfiguration
@PropertySource(value = "classpath:application.properties")
@ComponentScan("com.yomahub.liteflow.benchmark.cmp")
public class CommonExampleBenchmark {

    private ConfigurableApplicationContext applicationContext;

    private FlowExecutor flowExecutor;

    private PriceCalcReqVO req;

    @Setup
    public void setup() {
        applicationContext = SpringApplication.run(CommonExampleBenchmark.class);
        flowExecutor = applicationContext.getBean(FlowExecutor.class);
        req = JsonUtil.parseObject(ResourceUtil.readUtf8Str("reqData.json"), PriceCalcReqVO.class);
    }

    @TearDown
    public void tearDown() {
        applicationContext.close();
    }

    //执行阶段测试
    @Benchmark
    public  void test1(){
        flowExecutor.execute2Resp("mainChain", req, PriceContext.class);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CommonExampleBenchmark.class.getSimpleName())
                .mode(Mode.Throughput)
                .warmupIterations(2)//预热次数
                .measurementIterations(3)//执行次数
                .measurementTime(new TimeValue(10, TimeUnit.SECONDS))//每次执行多少时间
                .threads(100)//多少个线程
                .forks(1)//多少个进程
                .timeUnit(TimeUnit.SECONDS)
                .build();
        new Runner(opt).run();
    }
}
