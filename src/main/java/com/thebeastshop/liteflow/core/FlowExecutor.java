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
import com.thebeastshop.liteflow.entity.data.AbsSlot;
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
				String errorMsg = MessageFormat.format("init flow executor cause error,cannot parse rule file{}", path);
				throw new FlowExecutorNotInitException(errorMsg);
			}
		}
	}
	
	public void reloadRule(){
		init();
	}

	public <T> T execute(String chainId,Object param){
		return execute(chainId, param, DefaultSlot.class);
	}
	
	public <T> T execute(String chainId,Object param,Class<? extends Slot> slotClazz){
		int slotIndex = -1;
		try{
			Chain chain = FlowBus.getChain(chainId);
			
			if(chain == null){
				String errorMsg = MessageFormat.format("couldn't find chain with the id[{}]", chainId);
				throw new ChainNotFoundException(errorMsg);
			}
			
			slotIndex = DataBus.offerSlot(slotClazz);
			LOG.info("slot[{}] offered",slotIndex);
			if(slotIndex == -1){
				throw new NoAvailableSlotException("there is no available slot");
			}
			
			DataBus.getSlot(slotIndex).setRequestData(param);
			
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
							}else{
								String errorMsg = MessageFormat.format("component[{}] do not gain access", component.getClass().getSimpleName());
								throw new ComponentNotAccessException(errorMsg);
							}
						}catch(Throwable t){
							if(component.isContinueOnError()){
								LOG.error("component[{}] cause error,but flow is still go on",t,component.getClass().getSimpleName());
							}else{
								throw t;
							}
						}
					}
				}else if(condition instanceof WhenCondition){
					final CountDownLatch latch = new CountDownLatch(nodeList.size());
					for(Node node : nodeList){
						new WhenConditionThread(node,slotIndex,latch).start();
					}
					latch.await(15, TimeUnit.SECONDS);
				}
			}
			DataBus.getSlot(slotIndex).printStep();
			return DataBus.getSlot(slotIndex).getResponseData();
		}catch(Exception e){
			throw new FlowSystemException("executor cause error");
		}finally{
			DataBus.releaseSlot(slotIndex);
		}
	}
	
	private class WhenConditionThread extends Thread{
		
		private Node node;
		
		private Integer slotIndex;
		
		private CountDownLatch latch;
		
		public WhenConditionThread(Node node,Integer slotIndex,CountDownLatch latch){
			this.node = node;
			this.slotIndex = slotIndex;
			this.latch = latch;
		}
		
		@Override
		public void run() {
			try{
				node.getInstance().setSlotIndex(slotIndex).execute();
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
