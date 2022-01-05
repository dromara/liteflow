package com.yomahub.liteflow.test.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * spring环境下 rule-source 参数支持通配符，支持模式匹配
 * @author zendwang
 * @since 2.5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/config/local-rule-source-pattern-match.xml")
public class LocalRuleSourcePatternMatchSpringTest extends BaseTest {
    
    @Resource
    private FlowExecutor executor;
    
    /**
     * 匹配的文件
     * config/springgroup0/flow0.json
     * config/springgroup1/flow0.json
     */
    @Test
    public void testLocalJsonRuleSourcePatternMatch() {
        LiteflowResponse<DefaultSlot> response0 = executor.execute2Resp("chain1", "arg");
        Assert.assertEquals("a==>b==>c", response0.getSlot().getExecuteStepStr());
        LiteflowResponse<DefaultSlot> response1 = executor.execute2Resp("chain3", "arg");
        Assert.assertEquals("a==>c==>f==>g", response1.getSlot().getExecuteStepStr());
    }
}
