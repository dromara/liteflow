package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = BindELBuilderTest.class)
@EnableAutoConfiguration
public class BindELBuilderTest extends BaseTest {

    @Test
    public void testBind1(){
        String actualEl = ELBus.then("a", ELBus.element("b").bind("k1", "v1")).toEL();
        String expected = "THEN(a,b.bind(\"k1\", \"v1\"));";
        System.out.println(actualEl);
        Assertions.assertEquals(expected, actualEl);
    }

    @Test
    public void testBind2(){
        String actualEl = ELBus.then("a", "b").bind("k1","v1").toEL();
        String expected = "THEN(a,b).bind(\"k1\", \"v1\");";
        System.out.println(actualEl);
        Assertions.assertEquals(expected, actualEl);
    }

    @Test
    public void testBind3(){
        String actualEl = ELBus.then("a", ELBus.element("b").bind("k1", "v1")).toEL();
        String expected = "THEN(a,b.bind(\"k1\", \"v1\"));";
        System.out.println(actualEl);
        Assertions.assertEquals(expected, actualEl);
    }

    @Test
    public void testBind4(){
        String actualEl = ELBus.then("a", ELBus.when("b","c").bind("k1", "v1")).bind("k2","v2").toEL();
        String expected = "THEN(a,WHEN(b,c).bind(\"k1\", \"v1\")).bind(\"k2\", \"v2\");";
        System.out.println(actualEl);
        Assertions.assertEquals(expected, actualEl);
    }
}
