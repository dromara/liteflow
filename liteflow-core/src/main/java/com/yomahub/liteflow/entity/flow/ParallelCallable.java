package com.yomahub.liteflow.entity.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Callable;

import java.util.concurrent.CountDownLatch;

/**
 * 并行器线程
 * @author Bryan.Zhang
 */
public class ParallelCallable implements Callable<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(ParallelCallable.class);

    private Executable executableItem;

    private Integer slotIndex;

    private String requestId;

    private CountDownLatch latch;

    public ParallelCallable(Executable executableItem, Integer slotIndex, String requestId, CountDownLatch latch) {
        this.executableItem = executableItem;
        this.slotIndex = slotIndex;
        this.requestId = requestId;
        this.latch = latch;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            executableItem.execute(slotIndex);

            return true;
        }catch(Exception e){
            LOG.error("requestId [{}], item [{}] execute error", requestId, executableItem.getExecuteName());

            return false;
        } finally {
            latch.countDown();
        }
    }
}
