package com.yomahub.liteflow.springboot.config;

import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.thread.ExecutorHelper;
import com.yomahub.liteflow.util.LiteFlowExecutorPoolShutdown;
import com.yomahub.liteflow.util.SpringAware;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.ExecutorService;

/**
 * 线程池装配类
 * 这个装配前置条件是需要LiteflowConfig，LiteflowPropertyAutoConfiguration以及SpringAware
 * @author justin.xu
 */
@Configuration
@AutoConfigureAfter({LiteflowPropertyAutoConfiguration.class})
@ConditionalOnProperty(prefix = "liteflow", name = "enable", havingValue = "true")
@ConditionalOnBean(LiteflowConfig.class)
@Import(SpringAware.class)
public class LiteflowExecutorAutoConfiguration {

    @Bean("whenExecutors")
    public ExecutorService executorService(LiteflowConfig liteflowConfig) {
        return ExecutorHelper.loadInstance().buildExecutor();
    }

    //为什么要注释掉这个@Bean？
    //LiteFlowExecutorPoolShutdown这个类会在spring上下文移除这个bean的时候执行，也就是应用被停止或者kill的时候
    //这个类主要用于卸载掉线程池，会等待线程池中的线程执行完，再卸载掉，相当于一个钩子
    //但这段代码在实际中并没有太多用处，就算结束掉应用进程时很多公司也会优雅停机。就显得这段代码很鸡肋
    //之所以注释掉，是因为在单元测试中，每一个testcase结束时都会调这个方法。
    //当异步线程配置超时的时候。由于这个方法会去关闭掉线程池，会导致单元测试在所有一起运行时(单个运行没有问题)会出错
    //按理说这个方法会等待线程池里的全部线程执行完再销毁，但是事实上在单元测试中的确会报错。具体原因还没深究，由于这个类比较鸡肋，就干脆不注册了。
    //@Bean
    public LiteFlowExecutorPoolShutdown liteFlowExecutorPoolShutdown() {
        return new LiteFlowExecutorPoolShutdown();
    }
}
