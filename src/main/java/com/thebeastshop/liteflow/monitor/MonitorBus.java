/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-8-4
 * @version 1.0
 */
package com.thebeastshop.liteflow.monitor;

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

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thebeastshop.liteflow.entity.data.DataBus;
import com.thebeastshop.liteflow.entity.monitor.CompStatistics;
import com.thebeastshop.liteflow.util.LimitQueue;

public class MonitorBus {
	
	private static final int QUEUE_LIMIT_SIZE = 200;
	
	private static final Logger LOG = LoggerFactory.getLogger(MonitorBus.class);
	
	private static ConcurrentHashMap<String, LimitQueue<CompStatistics>> statisticsMap = new ConcurrentHashMap<String, LimitQueue<CompStatistics>>();

	static{
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				MonitorBus.printStatistics();
			}
		}, 1*60*1000L, 5*60*1000L);
	}
	
	public static void addStatistics(CompStatistics statistics){
		if(statisticsMap.containsKey(statistics.getComponentClazzName())){
			statisticsMap.get(statistics.getComponentClazzName()).add(statistics);
		}else{
			LimitQueue<CompStatistics> queue = new LimitQueue<CompStatistics>(QUEUE_LIMIT_SIZE);
			queue.add(statistics);
			statisticsMap.put(statistics.getComponentClazzName(), queue);
		}
	}
	
	public static void printStatistics(){
		try{
			Map<String, Long> compAverageTimeSpent = new HashMap<String, Long>();
			Map<String, Long> compAverageMemorySpent = new HashMap<String, Long>();
			
			long totalTimeSpent = 0;
			long totalMemorySpent = 0;
			
			for(Entry<String, LimitQueue<CompStatistics>> entry : statisticsMap.entrySet()){
				for(CompStatistics statistics : entry.getValue()){
					totalTimeSpent += statistics.getTimeSpent();
					totalMemorySpent += statistics.getMemorySpent();
				}
				compAverageTimeSpent.put(entry.getKey(), new BigDecimal(totalTimeSpent).divide(new BigDecimal(entry.getValue().size()), 2, RoundingMode.HALF_UP).longValue());
				compAverageMemorySpent.put(entry.getKey(), new BigDecimal(totalMemorySpent).divide(new BigDecimal(entry.getValue().size()), 2, RoundingMode.HALF_UP).longValue());
			}
			
			List<Entry<String, Long>> compAverageTimeSpentEntryList = new ArrayList<>(compAverageTimeSpent.entrySet());
			List<Entry<String, Long>> compAverageMemorySpentEntryList = new ArrayList<>(compAverageMemorySpent.entrySet());
			
			Collections.sort(compAverageTimeSpentEntryList,new Comparator<Entry<String, Long>>() {
				@Override
				public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			});
			
			Collections.sort(compAverageMemorySpentEntryList,new Comparator<Entry<String, Long>>() {
				@Override
				public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
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
			for(Entry<String, Long> entry : compAverageTimeSpentEntryList){
				logStr.append(MessageFormat.format("COMPONENT[{0}] AVERAGE TIME SPENT : {1}\n", entry.getKey(), entry.getValue()));
			}
			logStr.append("==============================MEMORY AVERAGE SPENT====================================\n");
			for(Entry<String, Long> entry : compAverageMemorySpentEntryList){
				logStr.append(MessageFormat.format("COMPONENT[{0}] AVERAGE MEMORY SPENT : {1}K\n", entry.getKey(), new BigDecimal(entry.getValue()).divide(new BigDecimal(1024), 2, RoundingMode.HALF_UP)));
			}
			logStr.append("======================================================================================\n");
			LOG.info(logStr.toString());
		}catch(Exception e){
			LOG.error("print statistics cause error",e);
		}
	}
}
