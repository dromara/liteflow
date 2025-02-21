package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.CommonNodeELWrapper;
import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.WhenELWrapper;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = RetryBuilderTest.class)
@EnableAutoConfiguration
public class RetryBuilderTest extends BaseTest {

    // node上进行retry
    @Test
    public void testRetry1(){
        CommonNodeELWrapper nodeA = ELBus.element("a").retry(2);
        CommonNodeELWrapper nodeB = ELBus.element("b").retry(3);
        WhenELWrapper whenELWrapper = ELBus.when(nodeA, nodeB);
        Assertions.assertEquals("WHEN(a.retry(2),b.retry(3));", whenELWrapper.toEL());
    }

    // node上进行retry，带exception
    @Test
    public void testRetry2(){
        CommonNodeELWrapper nodeA = ELBus.element("a").retry(2, "java.lang.NullPointerException");
        CommonNodeELWrapper nodeB = ELBus.element("b").retry(3, "java.lang.NullPointerException", "java.lang.ArrayIndexOutOfBoundsException");
        WhenELWrapper whenELWrapper = ELBus.when(nodeA, nodeB);
        Assertions.assertEquals("WHEN(a.retry(2,\"java.lang.NullPointerException\"),b.retry(3,\"java.lang.NullPointerException\",\"java.lang.ArrayIndexOutOfBoundsException\"));",
                whenELWrapper.toEL());
    }

    // 在表达式上进行retry
    @Test
    public void testRetry3(){
        WhenELWrapper whenELWrapper = ELBus.when("a", "b").retry(4);
        Assertions.assertEquals("WHEN(a,b).retry(4);", whenELWrapper.toEL());
    }

    // 在表达式上进行retry, 带exception
    @Test
    public void testRetry4(){
        WhenELWrapper whenELWrapper = ELBus.when("a", "b").retry(3, "java.lang.NullPointerException", "java.lang.ArrayIndexOutOfBoundsException");
        Assertions.assertEquals("WHEN(a,b).retry(3,\"java.lang.NullPointerException\",\"java.lang.ArrayIndexOutOfBoundsException\");",
                whenELWrapper.toEL());
    }

}
