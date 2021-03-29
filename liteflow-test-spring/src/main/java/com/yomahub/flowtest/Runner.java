package com.yomahub.flowtest;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Runner {

    private static final Logger log = LoggerFactory.getLogger(Runner.class);
    private static ClassPathXmlApplicationContext context;

    public static void main(String[] args) throws Throwable {
        context = new ClassPathXmlApplicationContext(new String[]{"/applicationContext.xml"});
        context.start();
        log.info("启动成功");
        FlowExecutor flowExecutor = context.getBean(FlowExecutor.class);
        LiteflowResponse response = flowExecutor.execute("chain3", "it's a request");
        System.out.println(response);

        while (true){
            Thread.sleep(60000);
        }

//        System.exit(0);
    }
}
