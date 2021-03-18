/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.monitor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import cn.hutool.core.collection.BoundedPriorityQueue;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.util.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.monitor.CompStatistics;
import com.yomahub.liteflow.util.LimitQueue;

public class MonitorBus {

	private LiteflowConfig liteflowConfig;

	private boolean enableLog = false;

	private int queueLimit = 200;

	private long delay = 300000;

	private long preiod = 300000;

	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	private final ConcurrentHashMap<String, BoundedPriorityQueue<CompStatistics>> statisticsMap = new ConcurrentHashMap<>();

	public MonitorBus(LiteflowConfig liteflowConfig) {
		if (ObjectUtil.isNotNull(liteflowConfig.getEnableLog())){
			this.enableLog = liteflowConfig.getEnableLog();
		}

		if (ObjectUtil.isNotNull(liteflowConfig.getQueueLimit())){
			queueLimit = liteflowConfig.getQueueLimit();
		}

		if (ObjectUtil.isNotNull(liteflowConfig.getDelay())){
			delay = liteflowConfig.getDelay();
		}

		if (ObjectUtil.isNotNull(liteflowConfig.getPeriod())){
			preiod = liteflowConfig.getPeriod();
		}

		if(enableLog){
			Timer timer = new Timer();
			timer.schedule(new MonitorTimeTask(this), delay, preiod);
		}
	}

	public void addStatistics(CompStatistics statistics){
		if(statisticsMap.containsKey(statistics.getComponentClazzName())){
			statisticsMap.get(statistics.getComponentClazzName()).offer(statistics);
		}else{
			BoundedPriorityQueue<CompStatistics> queue = new BoundedPriorityQueue<>(queueLimit);
			queue.offer(statistics);
			statisticsMap.put(statistics.getComponentClazzName(), queue);
		}
	}

	public void printStatistics(){
		try{
			Map<String, BigDecimal> compAverageTimeSpent = new HashMap<String, BigDecimal>();

			long totalTimeSpent = 0;

			for(Entry<String, BoundedPriorityQueue<CompStatistics>> entry : statisticsMap.entrySet()){
				for(CompStatistics statistics : entry.getValue()){
					totalTimeSpent += statistics.getTimeSpent();
				}
				compAverageTimeSpent.put(entry.getKey(), new BigDecimal(totalTimeSpent).divide(new BigDecimal(entry.getValue().size()), 2, RoundingMode.HALF_UP));
			}

			List<Entry<String, BigDecimal>> compAverageTimeSpentEntryList = new ArrayList<>(compAverageTimeSpent.entrySet());

			Collections.sort(compAverageTimeSpentEntryList, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

			StringBuilder logStr = new StringBuilder();
			logStr.append("以下为LiteFlow中间件统计信息：\n");
			logStr.append("======================================================================================\n");
			logStr.append("===================================SLOT INFO==========================================\n");
			logStr.append(MessageFormat.format("SLOT TOTAL SIZE : {0}\n", liteflowConfig.getSlotSize()));
			logStr.append(MessageFormat.format("SLOT OCCUPY COUNT : {0}\n", DataBus.OCCUPY_COUNT));
			logStr.append("===============================TIME AVERAGE SPENT=====================================\n");
			for(Entry<String, BigDecimal> entry : compAverageTimeSpentEntryList){
				logStr.append(MessageFormat.format("COMPONENT[{0}] AVERAGE TIME SPENT : {1}\n", entry.getKey(), entry.getValue()));
			}
			logStr.append("======================================================================================\n");
			LOG.info(logStr.toString());
		}catch(Exception e){
			LOG.error("print statistics cause error",e);
		}
	}

	public LiteflowConfig getLiteflowConfig() {
		return liteflowConfig;
	}

	public void setLiteflowConfig(LiteflowConfig liteflowConfig) {
		this.liteflowConfig = liteflowConfig;
	}
}
