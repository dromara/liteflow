package com.yomahub.liteflow.monitor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Singleton;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 规则文件监听器
 *
 * @author tangkc
 */
public class MonitorFile {

	private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

	private final Set<String> PATH_SET = new HashSet<>();

	// 保存所有监控器实例,用于后续清理
	private final List<FileAlterationMonitor> monitors = new ArrayList<>();

	// 线程安全锁
	private final Object lock = new Object();

	public static MonitorFile getInstance() {
		return Singleton.get(MonitorFile.class);
	}

	/**
	 * 添加监听文件路径
	 * @param path 文件路径
	 */
	public void addMonitorFilePath(String path) {
		if (FileUtil.isFile(path)) {
			String parentFolder = FileUtil.getParent(path, 1);
			PATH_SET.add(parentFolder);
		}
		else {
			PATH_SET.add(path);
		}
	}

	/**
	 * 添加监听文件路径
	 * @param filePaths 文件路径
	 */
	public void addMonitorFilePaths(List<String> filePaths) {
		filePaths.forEach(this::addMonitorFilePath);
	}

	/**
	 * 创建文件监听
     */
	public void create() throws Exception {
		synchronized (lock) {
			// 防止重复创建监控
			if (!monitors.isEmpty()) {
				LOG.warn("Monitor already created, skipping...");
				return;
			}

			for (String path : PATH_SET) {
				long interval = TimeUnit.MILLISECONDS.toMillis(2);
				// 不使用过滤器
				FileAlterationObserver observer = new FileAlterationObserver(new File(path));
				observer.addListener(new FileAlterationListenerAdaptor() {
					@Override
					public void onFileChange(File file) {
						LOG.info("file modify,filePath={}", file.getAbsolutePath());
						this.reloadRule();
					}

					@Override
					public void onFileDelete(File file) {
						LOG.info("file delete,filePath={}", file.getAbsolutePath());
						this.reloadRule();
					}

					@Override
					public void onFileCreate(File file) {
						LOG.info("file create,filePath={}", file.getAbsolutePath());
						this.reloadRule();
					}

					private void reloadRule() {
						try {
							FlowExecutorHolder.loadInstance().reloadRule();
						} catch (Exception e) {
							LOG.error("reload rule error", e);
						}
					}
				});
				// 创建文件变化监听器
				FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);
				// 开始监控
				monitor.start();
				// 保存监控器引用,用于后续清理
				monitors.add(monitor);
			}
		}
	}

	/**
	 * 停止所有文件监控并清理资源
	 * 主要用于测试环境的清理,确保测试隔离
	 */
	public void destroy() {
		synchronized (lock) {
			LOG.info("Destroying MonitorFile, stopping {} monitors", monitors.size());

			// 停止所有监控线程
			for (FileAlterationMonitor monitor : monitors) {
				try {
					monitor.stop(1000);  // 最多等待1秒
					LOG.debug("Monitor stopped successfully");
				} catch (Exception e) {
					LOG.error("Error stopping monitor", e);
				}
			}

			// 清空监控器列表
			monitors.clear();
			// 清空路径集合
			PATH_SET.clear();

			LOG.info("MonitorFile destroyed successfully");
		}
	}

	/**
	 * 检查是否有活动的监控
	 * @return true 如果有活动的监控, false 否则
	 */
	public boolean isMonitoring() {
		synchronized (lock) {
			return !monitors.isEmpty();
		}
	}

}
