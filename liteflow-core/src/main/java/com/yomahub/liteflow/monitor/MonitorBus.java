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

import com.yomahub.liteflow.util.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.monitor.CompStatistics;
import com.yomahub.liteflow.util.LimitQueue;

public class MonitorBus {

	private boolean enableMonitorLog = false;

	private int queueLimit = 200;

	private long delay = 5*60*1000;

	private long period = 5*60*1000;

	private Logger LOG = LoggerFactory.getLogger(this.getClass());

	private ConcurrentHashMap<String, LimitQueue<CompStatistics>> statisticsMap = new ConcurrentHashMap<String, LimitQueue<CompStatistics>>();

	public MonitorBus(boolean enableMonitorLog) {
		this.enableMonitorLog = enableMonitorLog;
		if(this.enableMonitorLog){
			Timer timer = new Timer();
			timer.schedule(new MonitorTimeTask(this), this.getDelay(), this.getPeriod());
		}
	}

	public MonitorBus(boolean enableMonitorLog, int queueLimit, long delay, long period) {
		this.enableMonitorLog = enableMonitorLog;
		this.queueLimit = queueLimit;
		this.delay = delay;
		this.period = period;
		if(this.enableMonitorLog){
			Timer timer = new Timer();
			timer.schedule(new MonitorTimeTask(this), this.getDelay(), this.getPeriod());
		}
	}

	public void addStatistics(CompStatistics statistics){
		if(statisticsMap.containsKey(statistics.getComponentClazzName())){
			statisticsMap.get(statistics.getComponentClazzName()).offer(statistics);
		}else{
			LimitQueue<CompStatistics> queue = new LimitQueue<CompStatistics>(queueLimit);
			queue.offer(statistics);
			statisticsMap.put(statistics.getComponentClazzName(), queue);
		}
	}

	public void printStatistics(){
		try{
			Map<String, BigDecimal> compAverageTimeSpent = new HashMap<String, BigDecimal>();

			long totalTimeSpent = 0;

			for(Entry<String, LimitQueue<CompStatistics>> entry : statisticsMap.entrySet()){
				for(CompStatistics statistics : entry.getValue()){
					totalTimeSpent += statistics.getTimeSpent();
				}
				compAverageTimeSpent.put(entry.getKey(), new BigDecimal(totalTimeSpent).divide(new BigDecimal(entry.getValue().size()), 2, RoundingMode.HALF_UP));
			}

			List<Entry<String, BigDecimal>> compAverageTimeSpentEntryList = new ArrayList<>(compAverageTimeSpent.entrySet());

			Collections.sort(compAverageTimeSpentEntryList,new Comparator<Entry<String, BigDecimal>>() {
				@Override
				public int compare(Entry<String, BigDecimal> o1, Entry<String, BigDecimal> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			});

			StringBuilder logStr = new StringBuilder();
			logStr.append("以下为LiteFlow中间件统计信息：\n");
			logStr.append("======================================================================================\n");
			logStr.append("===================================SLOT INFO==========================================\n");
			logStr.append(MessageFormat.format("SLOT TOTAL SIZE : {0}\n", DataBus.SLOT_SIZE));
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

	public boolean isEnableMonitorLog() {
		return enableMonitorLog;
	}

	public void setEnableMonitorLog(boolean enableMonitorLog) {
		this.enableMonitorLog = enableMonitorLog;
	}

	public int getQueueLimit() {
		return queueLimit;
	}

	public void setQueueLimit(int queueLimit) {
		this.queueLimit = queueLimit;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public long getPeriod() {
		return period;
	}

	public void setPeriod(long period) {
		this.period = period;
	}
}
