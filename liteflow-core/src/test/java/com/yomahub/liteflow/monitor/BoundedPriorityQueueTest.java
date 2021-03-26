package com.yomahub.liteflow.monitor;

import java.util.Iterator;
import java.util.PriorityQueue;
import cn.hutool.core.collection.BoundedPriorityQueue;
import cn.hutool.core.util.RandomUtil;
import org.junit.Test;

import com.yomahub.liteflow.entity.monitor.CompStatistics;

public class BoundedPriorityQueueTest {
    
    @Test
    public void test() throws InterruptedException {
        PriorityQueue<CompStatistics> queue = new BoundedPriorityQueue<>(6);
        for (int i = 0; i < 20 ; i ++) {
            long randomTime = RandomUtil.randomLong(20);
            Thread.sleep(randomTime);
            queue.add(new CompStatistics("comp" + i, randomTime));
        }
        
        Iterator<CompStatistics> iterator = queue.iterator();
        while(iterator.hasNext()) {
            CompStatistics compStatistics = iterator.next();
            System.out.println(compStatistics.getComponentClazzName() + " " + compStatistics.getTimeSpent());
        }
    }
}
