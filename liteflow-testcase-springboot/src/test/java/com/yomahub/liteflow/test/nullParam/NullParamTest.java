package com.yomahub.liteflow.test.nullParam;

import com.yomahub.liteflow.core.FlowExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 单元测试:传递null param导致NPE的优化代码
 *
 * @Author LeoLee
 * @Date 2021/12/9 16:58
 * @Version 1.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/nullParam/application.properties")
@SpringBootTest(classes = NullParamTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.nullParam.cmp"})
public class NullParamTest {

    @Autowired
    private FlowExecutor flowExecutor;

    /**
     * 支持无参的flow执行，以及param 为null时的异常抛出
     * @Author LeoLee
     * @Date 17:25 2021/12/9
     */
    @Test
    public void testNullParam() throws Exception {
        //flowExecutor.execute("chain1", null);//NullParamException: data slot can't accept null param
        flowExecutor.execute("chain1");
        //flowExecutor.execute2Resp("chain1", null);//NullParamException: data slot can't accept null param
        flowExecutor.execute2Resp("chain1");
    }

}
