package com.yomahub.liteflow.test.def;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit5Extension;

@ExtendWith(SolonJUnit5Extension.class)
@Import(profiles = "classpath:/application.properties")
public class SQL4SolonTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    @Test
    public void testSQLWithXml() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());
    }

    @Test
    public void testSQLWithScriptXml() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("x0[if 脚本]==>a==>b", response.getExecuteStepStrWithoutTime());
    }
}
