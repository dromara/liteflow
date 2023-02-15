package com.yomahub.liteflow.monitor;

import cn.hutool.core.collection.CollUtil;
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
        for (String filePath : CollUtil.distinct(PATH_LIST)) {
            // 这里只监听两种类型，文件修改和文件覆盖
            WatchMonitor.createAll(filePath, new SimpleWatcher(){
                @Override
                public void onModify(WatchEvent<?> event, Path currentPath) {
                    logger.info("file modify,filePath={}", filePath);
                    FlowExecutorHolder.loadInstance().reloadRule();
                }
            }).start();
        }
    }

}
