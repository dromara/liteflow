package com.yomahub.liteflow.monitor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.watchers.DelayWatcher;
import cn.hutool.core.lang.Singleton;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 规则文件监听器
 *
 * @author tangkc
 */
public class MonitorFile {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Set<String> PATH_SET = new HashSet<>();

    public static MonitorFile getInstance() {
        return Singleton.get(MonitorFile.class);
    }

    /**
     * 添加监听文件路径
     *
     * @param path 文件路径
     */
    public void addMonitorFilePath(String path) {
        if (FileUtil.isFile(path)){
            String parentFolder = FileUtil.getParent(path, 1);
            PATH_SET.add(parentFolder);
        }else{
            PATH_SET.add(path);
        }
    }

    /**
     * 添加监听文件路径
     *
     * @param filePaths 文件路径
     */
    public void addMonitorFilePaths(List<String> filePaths) {
        filePaths.forEach(this::addMonitorFilePath);
    }

    /**
     * 创建文件监听
     */
    public void create() throws Exception{
        for (String path : PATH_SET) {
            long interval = TimeUnit.MILLISECONDS.toMillis(2);
            //不使用过滤器
            FileAlterationObserver observer = new FileAlterationObserver(new File(path));
            observer.addListener(new FileAlterationListenerAdaptor() {
                @Override
                public void onFileChange(File file) {
                    logger.info("file modify,filePath={}", file.getAbsolutePath());
                    FlowExecutorHolder.loadInstance().reloadRule();
                }

                @Override
                public void onFileDelete(File file) {
                    logger.info("file delete,filePath={}", file.getAbsolutePath());
                    FlowExecutorHolder.loadInstance().reloadRule();
                }
            });
            //创建文件变化监听器
            FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);
            // 开始监控
            monitor.start();
        }
    }
}
