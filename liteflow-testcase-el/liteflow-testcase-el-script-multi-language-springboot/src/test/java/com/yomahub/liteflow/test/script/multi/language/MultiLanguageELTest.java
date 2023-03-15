package com.yomahub.liteflow.test.script.multi.language;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import groovy.lang.MetaClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Map;


/**
 * 测试springboot下的groovy脚本组件，基于xml配置
 * @author Bryan.Zhang
 * @since 2.6.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/multiLanguage/application.properties")
@SpringBootTest(classes = MultiLanguageELTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.script.multi.language.cmp"})
public class MultiLanguageELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试普通脚本节点
    @Test
    public void testMultiLanguage1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Object student = context.getData("student");
        Map<String,Object> studentMap = JsonUtil.parseObject(JsonUtil.toJsonString(student), Map.class);
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(Integer.valueOf(18), context.getData("s1"));
        Assert.assertEquals(10032, studentMap.get("studentID"));
    }
}
