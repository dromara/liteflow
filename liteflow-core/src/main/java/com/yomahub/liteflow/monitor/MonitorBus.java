/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.monitor;

import cn.hutool.core.util.BooleanUtil;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.util.BoundedPriorityBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 监控类元数据，打印执行器类
 *
 * @author Bryan.Zhang
 */
public class MonitorBus {

	private LiteflowConfig liteflowConfig;

	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	private final ConcurrentHashMap<String, BoundedPriorityBlockingQueue<CompStatistics>> statisticsMap = new ConcurrentHashMap<>();

	private final ScheduledExecutorService printLogScheduler = Executors.newScheduledThreadPool(1);

	public MonitorBus(LiteflowConfig liteflowConfig) {
		this.liteflowConfig = liteflowConfig;

		if (BooleanUtil.isTrue(liteflowConfig.getEnableLog())) {
			this.printLogScheduler.scheduleAtFixedRate(new MonitorTimeTask(this), liteflowConfig.getDelay(),
					liteflowConfig.getPeriod(), TimeUnit.MILLISECONDS);
		}
	}

	public void addStatistics(CompStatistics statistics) {
		if (statisticsMap.containsKey(statistics.getComponentClazzName())) {
			statisticsMap.get(statistics.getComponentClazzName()).add(statistics);
		}
		else {
			BoundedPriorityBlockingQueue<CompStatistics> queue = new BoundedPriorityBlockingQueue<>(
					liteflowConfig.getQueueLimit());
			queue.offer(statistics);
			statisticsMap.put(statistics.getComponentClazzName(), queue);
		}
	}

	public void printStatistics() {
		try {
			Map<String, BigDecimal> compAverageTimeSpent = new HashMap<String, BigDecimal>();

			for (Entry<String, BoundedPriorityBlockingQueue<CompStatistics>> entry : statisticsMap.entrySet()) {
				long totalTimeSpent = 0;
				for (CompStatistics statistics : entry.getValue()) {
					totalTimeSpent += statistics.getTimeSpent();
				}
				compAverageTimeSpent.put(entry.getKey(), new BigDecimal(totalTimeSpent)
					.divide(new BigDecimal(entry.getValue().size()), 2, RoundingMode.HALF_UP));
			}

			List<Entry<String, BigDecimal>> compAverageTimeSpentEntryList = new ArrayList<>(
					compAverageTimeSpent.entrySet());

			Collections.sort(compAverageTimeSpentEntryList, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

			StringBuilder logStr = new StringBuilder();
			logStr.append("以下为LiteFlow中间件统计信息：\n");
			logStr.append("======================================================================================\n");
			logStr.append("===================================SLOT INFO==========================================\n");
			logStr.append(MessageFormat.format("SLOT TOTAL SIZE : {0}\n", liteflowConfig.getSlotSize()));
			logStr.append(MessageFormat.format("SLOT OCCUPY COUNT : {0}\n", DataBus.OCCUPY_COUNT));
			logStr.append("===============================TIME AVERAGE SPENT=====================================\n");
			for (Entry<String, BigDecimal> entry : compAverageTimeSpentEntryList) {
				logStr.append(MessageFormat.format("COMPONENT[{0}] AVERAGE TIME SPENT : {1}\n", entry.getKey(),
						entry.getValue()));
			}
			logStr.append("======================================================================================\n");
			LOG.info(logStr.toString());
		}
		catch (Exception e) {
			LOG.error("print statistics cause error", e);
		}
	}

	public LiteflowConfig getLiteflowConfig() {
		return liteflowConfig;
	}

	public void setLiteflowConfig(LiteflowConfig liteflowConfig) {
		this.liteflowConfig = liteflowConfig;
	}

	public void closeScheduler() {
		this.printLogScheduler.shutdown();
	}

	public ConcurrentHashMap<String, BoundedPriorityBlockingQueue<CompStatistics>> getStatisticsMap() {
		return statisticsMap;
	}

}
