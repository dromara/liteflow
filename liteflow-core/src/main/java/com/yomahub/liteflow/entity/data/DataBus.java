/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.data;

import java.util.concurrent.atomic.AtomicInteger;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.util.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据BUS，主要用来管理Slot，用以分配和回收
 * @author Bryan.Zhang
 */
public class DataBus {

	private static final Logger LOG = LoggerFactory.getLogger(DataBus.class);

	public static AtomicInteger OCCUPY_COUNT = new AtomicInteger(0);

	private static Slot[] slots;

	static {
		LiteflowConfig liteflowConfig = SpringAware.getBean(LiteflowConfig.class);

		if (ObjectUtil.isNull(liteflowConfig)){
			throw new ConfigErrorException("config error, please check liteflow config property");
		}
		int slotSize = liteflowConfig.getSlotSize();
		slots = new Slot[slotSize];
	}

	public synchronized static int offerSlot(Class<? extends Slot> slotClazz){
		try{
			for(int i = 0; i < slots.length; i++){
				if(slots[i] == null){
					slots[i] = slotClazz.newInstance();
					OCCUPY_COUNT.incrementAndGet();
					return i;
				}
			}
		}catch(Exception e){
			LOG.error("offer slot error",e);
			return -1;
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Slot> T getSlot(int slotIndex){
		return (T)slots[slotIndex];
	}

	public static void releaseSlot(int slotIndex){
		if(slots[slotIndex] != null){
			LOG.info("[{}]:slot[{}] released",slots[slotIndex].getRequestId(),slotIndex);
			slots[slotIndex] = null;
			OCCUPY_COUNT.decrementAndGet();
		}else{
			LOG.warn("slot[{}] already has been released",slotIndex);
		}
	}
}
