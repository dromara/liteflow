package com.yomahub.liteflow.entity.flow;

import com.yomahub.liteflow.asynctool.callback.IWorker;
import com.yomahub.liteflow.asynctool.wrapper.WorkerWrapper;

import java.util.Map;

public class ParallelWorker implements IWorker<Void, String> {

    private final Executable executableItem;

    private final Integer slotIndex;

    public ParallelWorker(Executable executableItem, Integer slotIndex) {
        this.executableItem = executableItem;
        this.slotIndex = slotIndex;
    }

    @Override
    public String action(Void object, Map<String, WorkerWrapper> allWrappers) throws Exception{
        executableItem.execute(slotIndex);
        return executableItem.getExecuteName();
    }

    @Override
    public String defaultValue() {
        return executableItem.getExecuteName();
    }
}
