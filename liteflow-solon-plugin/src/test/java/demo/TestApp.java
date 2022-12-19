package demo;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.snack.ONode;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.SolonTest;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * @author noear 2022/9/21 created
 */

@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:demo/app.yml")
public class TestApp {
    @Inject
    FlowExecutor flowExecutor;

    @Test
    public void test() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        System.out.println(ONode.stringify(response));

        assert response.isSuccess();
    }
}
