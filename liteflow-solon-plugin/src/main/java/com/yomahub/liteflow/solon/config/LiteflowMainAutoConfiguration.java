package com.yomahub.liteflow.solon.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AopContext;

/**
 * 主要的业务装配器
 * 在这个装配器里装配了执行器，执行器初始化类，监控器
 * 这个装配前置条件是需要LiteflowConfig，LiteflowPropertyAutoConfiguration以及SpringAware
 *
 * @author Bryan.Zhang
 * @author noear
 * @since 2.9
 */
@Configuration
public class LiteflowMainAutoConfiguration {

    @Inject(value = "${liteflow.parseOnStart}", required = false)
    boolean parseOnStart;

    @Inject
    AopContext aopContext;

    @Inject
    LiteflowConfig liteflowConfig;

    @Init
    public void flowExecutor() {
        //
        //实例化FlowExecutor
        //
        FlowExecutor flowExecutor = new FlowExecutor();
        flowExecutor.setLiteflowConfig(liteflowConfig);

        if (parseOnStart) {
            flowExecutor.init(true);
        }

        aopContext.wrapAndPut(FlowExecutor.class, flowExecutor);
    }
}
