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

    private final Executable executableItem;

    private final Integer slotIndex;

    private final String requestId;

    private final CountDownLatch latch;

    private final int retryCount;

    public ParallelCallable(Executable executableItem, Integer slotIndex, String requestId, CountDownLatch latch, int retryCount) {
        this.executableItem = executableItem;
        this.slotIndex = slotIndex;
        this.requestId = requestId;
        this.latch = latch;
        this.retryCount = retryCount;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            boolean flag = true;
            for (int i = 0; i <= retryCount; i++) {
                try{
                    if (i > 0){
                        LOG.info("[{}]:component[{}] performs {} retry", requestId, executableItem.getExecuteName(), i+1);
                    }
                    executableItem.execute(slotIndex);
                    flag = true;
                    break;
                }catch (Exception e){
                    if (i >= retryCount){
                        LOG.error("requestId [{}], item [{}] execute error", requestId, executableItem.getExecuteName());
                        flag = false;
                        break;
                    }
                }
            }
            return flag;
        } finally {
            latch.countDown();
        }
    }
}
