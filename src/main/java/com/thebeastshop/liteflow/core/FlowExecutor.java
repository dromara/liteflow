/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-31
 * @version 1.0
 */
package com.thebeastshop.liteflow.core;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thebeastshop.liteflow.entity.config.Chain;
import com.thebeastshop.liteflow.entity.config.Condition;
import com.thebeastshop.liteflow.entity.config.Node;
import com.thebeastshop.liteflow.entity.config.ThenCondition;
import com.thebeastshop.liteflow.entity.config.WhenCondition;
import com.thebeastshop.liteflow.entity.data.DataBus;
import com.thebeastshop.liteflow.entity.data.DefaultSlot;
import com.thebeastshop.liteflow.entity.data.Slot;
import com.thebeastshop.liteflow.exception.ChainNotFoundException;
import com.thebeastshop.liteflow.exception.ComponentNotAccessException;
import com.thebeastshop.liteflow.exception.FlowExecutorNotInitException;
import com.thebeastshop.liteflow.exception.FlowSystemException;
import com.thebeastshop.liteflow.exception.NoAvailableSlotException;
import com.thebeastshop.liteflow.flow.FlowBus;
import com.thebeastshop.liteflow.parser.FlowParser;

public class FlowExecutor {
	
	private static final Logger LOG = LoggerFactory.getLogger(FlowExecutor.class);
	
	private List<String> rulePath;
	
	public void init() {
		for(String path : rulePath){
			try {
				FlowParser.parseLocal(path);
			} catch (Exception e) {
				String errorMsg = MessageFormat.format("init flow executor cause error,cannot parse rule file{0}", path);
				throw new FlowExecutorNotInitException(errorMsg);
			}
		}
	}
	
	public void reloadRule(){
		init();
	}

	public <T extends Slot> T execute(String chainId,Object param){
		return execute(chainId, param, DefaultSlot.class,null,false);
	}
	
	public <T extends Slot> T execute(String chainId,Object param,Class<? extends Slot> slotClazz){
		return execute(chainId, param, slotClazz,null,false);
	}
	
	public void invoke(String chainId,Object param,Class<? extends Slot> slotClazz,Integer slotIndex){
		execute(chainId, param, slotClazz,slotIndex,true);
	}
	
	public <T extends Slot> T execute(String chainId,Object param,Class<? extends Slot> slotClazz,Integer slotIndex,boolean isInnerChain){
		Slot slot = null;
		try{
			if(FlowBus.needInit()) {
				init();
			}
			
			Chain chain = FlowBus.getChain(chainId);
			
			if(chain == null){
				String errorMsg = MessageFormat.format("couldn't find chain with the id[{0}]", chainId);
				throw new ChainNotFoundException(errorMsg);
			}
			
			if(!isInnerChain && slotIndex == null) {
				slotIndex = DataBus.offerSlot(slotClazz);
				LOG.info("slot[{}] offered",slotIndex);
			}
			
			if(slotIndex == -1){
				throw new NoAvailableSlotException("there is no available slot");
			}
			
			slot = DataBus.getSlot(slotIndex);
			if(slot == null) {
				throw new NoAvailableSlotException("the slot is not exist");
			}
			
			if(StringUtils.isBlank(slot.getRequestId())) {
				slot.generateRequestId();
				LOG.info("requestId[{}] has generated",slot.getRequestId());
			}
			
			if(!isInnerChain) {
				slot.setRequestData(param);
			}else {
				slot.setChainReqData(chainId, param);
			}
			
			List<Condition> conditionList = chain.getConditionList();
			
			List<Node> nodeList = null;
			NodeComponent component = null;
			for(Condition condition : conditionList){
				nodeList = condition.getNodeList();
				
				if(condition instanceof ThenCondition){
					for(Node node : nodeList){
						component = node.getInstance();
						try{
							component.setSlotIndex(slotIndex);
							if(component.isAccess()){
								component.execute();
								if(component.isEnd()) {
									LOG.info("[{}]:component[{}] lead the chain to end",slot.getRequestId(),component.getClass().getSimpleName());
									break;
								}
							}else {
								LOG.info("[{}]:[X]skip component[{}] execution",slot.getRequestId(),component.getClass().getSimpleName());
							}
						}catch(Throwable t){
							if(component.isContinueOnError()){
								LOG.error("[{}]:component[{}] cause error,but flow is still go on",t,slot.getRequestId(),component.getClass().getSimpleName());
							}else{
								throw t;
							}
						}
					}
				}else if(condition instanceof WhenCondition){
					final CountDownLatch latch = new CountDownLatch(nodeList.size());
					for(Node node : nodeList){
						new WhenConditionThread(node,slotIndex,slot.getRequestId(),latch).start();
					}
					latch.await(15, TimeUnit.SECONDS);
				}
			}
			return (T)slot;
		}catch(Exception e){
			LOG.error("[{}]:executor cause error",e,slot.getRequestId());
			throw new FlowSystemException("executor cause error");
		}finally{
			if(!isInnerChain) {
				slot.printStep();
				DataBus.releaseSlot(slotIndex);
			}
		}
	}
	
	private class WhenConditionThread extends Thread{
		
		private Node node;
		
		private Integer slotIndex;
		
		private String requestId;
		
		private CountDownLatch latch;
		
		public WhenConditionThread(Node node,Integer slotIndex,String requestId,CountDownLatch latch){
			this.node = node;
			this.slotIndex = slotIndex;
			this.requestId = requestId;
			this.latch = latch;
		}
		
		@Override
		public void run() {
			try{
				NodeComponent cmp = node.getInstance().setSlotIndex(slotIndex);
				if(cmp.isAccess()) {
					cmp.execute();
				}else {
					LOG.info("[{}]:[X]skip component[{}] execution",requestId,cmp.getClass().getSimpleName());
				}
			}catch(Exception e){
				LOG.error("component [{}] execute cause error",node.getClazz(),e);
			}finally{
				latch.countDown();
			}
		}
	}

	public List<String> getRulePath() {
		return rulePath;
	}

	public void setRulePath(List<String> rulePath) {
		this.rulePath = rulePath;
	}
}
