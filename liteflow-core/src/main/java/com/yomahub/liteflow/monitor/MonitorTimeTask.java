package com.yomahub.liteflow.monitor;

import java.util.TimerTask;

public class MonitorTimeTask extends TimerTask {

    private MonitorBus monitorBus;

    public MonitorTimeTask(MonitorBus monitorBus) {
        this.monitorBus = monitorBus;
    }

    @Override
    public void run() {
        monitorBus.printStatistics();
    }
}
