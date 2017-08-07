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
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.thebeastshop.liteflow.entity.data.DataBus;
import com.thebeastshop.liteflow.entity.monitor.CompStatistics;
import com.thebeastshop.liteflow.util.LimitQueue;

public class MonitorBus {
	
	private static final int QUEUE_LIMIT_SIZE = 200;
	
	private static ConcurrentHashMap<String, LimitQueue<CompStatistics>> statisticsMap = new ConcurrentHashMap<String, LimitQueue<CompStatistics>>();

	static{
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				MonitorBus.printStatistics();
			}
		}, 30*1000L, 10*60*1000L);
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
		System.out.println("======================================================================================");
		System.out.println("===================================SLOT INFO==========================================");
		System.out.println("SLOT TOTAL SIZE : "+DataBus.SLOT_SIZE);
		System.out.println("SLOT OCCUPY COUNT : "+DataBus.OCCUPY_COUNT);
		System.out.println("===============================TIME AVERAGE SPENT=====================================");
		for(Entry<String, Long> entry : compAverageTimeSpent.entrySet()){
			System.out.println("COMPONENT["+entry.getKey()+"] AVERAGE TIME SPENT : " + entry.getValue());
		}
		System.out.println("==============================MEMORY AVERAGE SPENT====================================");
		for(Entry<String, Long> entry : compAverageMemorySpent.entrySet()){
			System.out.println("COMPONENT["+entry.getKey()+"] AVERAGE MEMORY SPENT : "+ new BigDecimal(entry.getValue()).divide(new BigDecimal(1024), 2, RoundingMode.HALF_UP) + "K");
		}
		System.out.println("======================================================================================");
	}
}
