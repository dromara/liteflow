package com.yomahub.liteflow.monitor;

import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.watchers.DelayWatcher;
import cn.hutool.core.lang.Singleton;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则文件监听器
 *
 * @author tangkc
 */
public class MonitorFile {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<String> PATH_LIST = new ArrayList<>();

    public static MonitorFile getInstance() {
        return Singleton.get(MonitorFile.class);
    }

    /**
     * 添加监听文件路径
     *
     * @param filePath 文件路径
     */
    public void addMonitorFilePath(String filePath) {
        PATH_LIST.add(filePath);
    }

    /**
     * 添加监听文件路径
     *
     * @param filePaths 文件路径
     */
    public void addMonitorFilePaths(List<String> filePaths) {
        PATH_LIST.addAll(filePaths);
    }

    /**
     * 创建文件监听
     */
    public void create() {
        for (String filePath : PATH_LIST) {
            // 这里只监听两种类型，文件修改和文件覆盖
            WatchMonitor.createAll(filePath, new DelayWatcher(new SimpleWatcher() {

                @Override
                public void onModify(WatchEvent<?> event, Path currentPath) {
                    logger.info("file modify,filePath={}", filePath);
                    FlowExecutorHolder.loadInstance().reloadRule();
                }

                @Override
                public void onOverflow(WatchEvent<?> event, Path currentPath) {
                    logger.info("file over flow,filePath={}", filePath);
                    FlowExecutorHolder.loadInstance().reloadRule();
                }
                // 在监听目录或文件时，如果这个文件有修改操作，JDK会多次触发modify方法，为了解决这个问题
                // 合并 500 毫秒内相同的变化
            }, 500)).start();
        }
    }

}
