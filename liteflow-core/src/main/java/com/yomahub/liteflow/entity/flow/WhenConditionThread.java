package com.yomahub.liteflow.entity.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * 并行器线程
 * @author Bryan.Zhang
 */
public class WhenConditionThread extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(WhenConditionThread.class);

    private Executable executableItem;

    private Integer slotIndex;

    private String requestId;

    private CountDownLatch latch;

    public WhenConditionThread(Executable executableItem,Integer slotIndex,String requestId,CountDownLatch latch){
        this.executableItem = executableItem;
        this.slotIndex = slotIndex;
        this.requestId = requestId;
        this.latch = latch;
    }

    @Override
    public void run() {
        try{
            executableItem.execute(slotIndex);
        } catch (Exception e) {
            LOG.error("item [{}] execute cause error",executableItem.getExecuteName(),e);
        } finally {
            latch.countDown();
        }
    }
}
