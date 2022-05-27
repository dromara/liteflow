package com.yomahub.liteflow.test.aop;

import com.yomahub.liteflow.test.BaseTest;

/**
 * 切面场景单元测试
 * 在声明式组件场景中，自定义aspect的aop不生效的，因为生成的代理类并不是原类，也不是原类的子类，而是NodeComponent的子类
 * 所以切不到，暂且没有想出办法来解决，这个测试类暂时不用
 * @author Bryan.Zhang
 */
/*@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/aop/application.properties")
@SpringBootTest(classes = CustomAOPSpringbootTest.class)
@EnableAutoConfiguration
@Import(CustomAspect.class)
@ComponentScan({"com.yomahub.liteflow.test.aop.cmp1","com.yomahub.liteflow.test.aop.cmp2"})*/
public class CustomAOPSpringbootTest extends BaseTest {

    /*@Resource
    private FlowExecutor flowExecutor;

    //测试自定义AOP，串行场景
    @Test
    public void testCustomAopS() {
        LiteflowResponse<DefaultContext> response= flowExecutor.execute2Resp("chain1", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("before_after", response.getContextBean.getData("a"));
        Assert.assertEquals("before_after", response.getContextBean().getData("b"));
        Assert.assertEquals("before_after", response.getContextBean().getData("c"));
    }

    //测试自定义AOP，并行场景
    @Test
    public void testCustomAopP() {
        LiteflowResponse<DefaultContext> response= flowExecutor.execute2Resp("chain2", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("before_after", response.getContextBean().getData("a"));
        Assert.assertEquals("before_after", response.getContextBean().getData("b"));
        Assert.assertEquals("before_after", response.getContextBean().getData("c"));
    }*/
}
