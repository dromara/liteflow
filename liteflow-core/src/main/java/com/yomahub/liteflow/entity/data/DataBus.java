/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.data;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 数据BUS，主要用来管理Slot，用以分配和回收
 * @author Bryan.Zhang
 */
public class DataBus {

	private static final Logger LOG = LoggerFactory.getLogger(DataBus.class);

	public static AtomicInteger OCCUPY_COUNT = new AtomicInteger(0);

	//这里为什么采用ConcurrentHashMap作为slot存放的容器？
	//因为ConcurrentHashMap的随机取值复杂度也和数组一样为O(1)，并且没有并发问题，还有自动扩容的功能
	//用数组的话，扩容涉及copy，线程安全问题还要自己处理
	private static final ConcurrentHashMap<Integer, Slot> SLOTS;

	private static final ConcurrentLinkedQueue<Integer> QUEUE;

	//当前slot的下标index的最大值
	private static Integer currentIndexMaxValue;

	static {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		currentIndexMaxValue = liteflowConfig.getSlotSize();

		SLOTS = new ConcurrentHashMap<>();
		QUEUE = IntStream.range(0, currentIndexMaxValue).boxed().collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
	}

	public static int offerSlot(Class<? extends Slot> slotClazz) {
		try {
			Slot slot = slotClazz.newInstance();

			//这里有没有并发问题？
			//没有，因为QUEUE的类型为ConcurrentLinkedQueue，并发情况下，每次取到的index不会相同
			//当然前提是QUEUE里面的值不会重复，但是这个是由其他机制来保证的
			Integer slotIndex = QUEUE.poll();

			if (ObjectUtil.isNull(slotIndex)){
				//只有在扩容的时候需要用到synchronized重量级锁
				//扩一次容，增强原来size的0.75，因为初始slot容量为1024，从某种层面来说，即便并发很大。但是扩容的次数不会很多。
				//因为单个机器的tps上限总归是有一个极限的，不可能无限制的增长。
				synchronized (DataBus.class){
					//在扩容的一刹那，去竞争这个锁的线程还是有一些，所以获得这个锁的线程这里要再次取一次。如果为null，再真正扩容
					slotIndex = QUEUE.poll();
					if (ObjectUtil.isNull(slotIndex)){
						int nextMaxIndex = (int) Math.round(currentIndexMaxValue * 1.75);
						QUEUE.addAll(IntStream.range(currentIndexMaxValue, nextMaxIndex).boxed().collect(Collectors.toCollection(ConcurrentLinkedQueue::new)));
						currentIndexMaxValue = nextMaxIndex;
						//扩容好，从队列里再取出扩容好的index
						slotIndex = QUEUE.poll();
					}
				}
			}

			if (ObjectUtil.isNotNull(slotIndex)) {
				SLOTS.put(slotIndex, slot);
				OCCUPY_COUNT.incrementAndGet();
				return slotIndex;
			}
		} catch (Exception e) {
			LOG.error("offer slot error", e);
			return -1;
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Slot> T getSlot(int slotIndex){
		return (T)SLOTS.get(slotIndex);
	}

	public static void releaseSlot(int slotIndex){
		if(ObjectUtil.isNotNull(SLOTS.get(slotIndex))){
			LOG.info("[{}]:slot[{}] released",SLOTS.get(slotIndex).getRequestId(),slotIndex);
			SLOTS.remove(slotIndex);
			QUEUE.add(slotIndex);
			OCCUPY_COUNT.decrementAndGet();
		}else{
			LOG.warn("slot[{}] already has been released",slotIndex);
		}
	}
}
