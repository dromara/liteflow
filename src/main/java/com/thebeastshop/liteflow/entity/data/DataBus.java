/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-8-1
 * @version 1.0
 */
package com.thebeastshop.liteflow.entity.data;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBus {
	
	private static final Logger LOG = LoggerFactory.getLogger(DataBus.class);
	
	public static final int SLOT_SIZE = 1024;
	
	public static AtomicInteger OCCUPY_COUNT = new AtomicInteger(0);
	
	private static Slot[] slots = new Slot[SLOT_SIZE];
	
	public synchronized static int offerSlot(){
		for(int i = 0; i < slots.length; i++){
			if(slots[i] == null){
				slots[i] = new Slot();
				OCCUPY_COUNT.incrementAndGet();
				return i;
			}
		}
		return -1;
	}
	
	public static Slot getSlot(int slotIndex){
		return slots[slotIndex];
	}
	
	public static void releaseSlot(int slotIndex){
		if(slots[slotIndex] != null){
			slots[slotIndex] = null;
			OCCUPY_COUNT.decrementAndGet();
		}else{
			LOG.warn("the slot[{}] has been released",slotIndex);
		}
	}
}
