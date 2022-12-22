package com.yomahub.liteflow.test.slotOffer;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.parallel.ParallelSupplier;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * springboot环境EL常规的例子测试
 * @author Bryan.Zhang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SlotOfferELSpringbootTest.class)
@EnableAutoConfiguration
public class SlotOfferELSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //并发200，一共6w条线程去获取Slot的测试
    @Test
    public void testSlotOffer() throws Exception{
        Set<Integer> set = new ConcurrentHashSet<>();
        Set<CompletableFuture<Boolean>> futureSet = new ConcurrentHashSet<>();
        Set<String> error = new ConcurrentHashSet<>();
        for (int i = 0; i < 60000; i++) {
            futureSet.add(CompletableFuture.supplyAsync(() -> {
                int index=0;
                try{
                    index = DataBus.offerSlotByClass(ListUtil.toList(DefaultContext.class));
                    boolean flag = set.add(index);
                    if (!flag){
                        error.add(Integer.toString(index));
                    }
                }catch (Exception e) {
                    error.add(e.getMessage());
                }finally {
                    DataBus.releaseSlot(index);
                    boolean flag = set.remove(index);
                    if(!flag){
                        error.add(Integer.toString(index));
                    }
                }
                return Boolean.TRUE;
            }));
        }
        CompletableFuture<Void> resultFuture = CompletableFuture.allOf(futureSet.toArray(new CompletableFuture[]{}));

        resultFuture.get();

        Assert.assertEquals(0, set.size());
        Assert.assertEquals(0, error.size());
        Assert.assertEquals(0, DataBus.OCCUPY_COUNT.get());
    }


}
