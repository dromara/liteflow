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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thebeastshop.liteflow.entity.config.Chain;
import com.thebeastshop.liteflow.entity.config.Condition;
import com.thebeastshop.liteflow.entity.config.Node;
import com.thebeastshop.liteflow.entity.config.ThenCondition;
import com.thebeastshop.liteflow.entity.config.WhenCondition;
import com.thebeastshop.liteflow.entity.data.DataBus;
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
				LOG.error("init flow executor cause error,cannot parse rule file{}", path, e);
			}
		}
	}
	
	public void reloadRule(){
		init();
	}

	public <T> T execute(String chainId,Object param){
		int slotIndex = -1;
		try{
			Chain chain = FlowBus.getChain(chainId);
			
			if(chain == null){
				LOG.error("couldn't find chain with the id[{}]",chainId);
			}
			
			slotIndex = DataBus.offerSlot();
			LOG.info("slot[{}] offered",slotIndex);
			if(slotIndex == -1){
				throw new Exception("there is no available slot");
			}
			
			DataBus.getSlot(slotIndex).setRequestData(param);
			
			List<Condition> conditionList = chain.getConditionList();
			
			List<Node> nodeList = null;
			Component component = null;
			for(Condition condition : conditionList){
				nodeList = condition.getNodeList();
				
				if(condition instanceof ThenCondition){
					for(Node node : nodeList){
						component = node.getInstance();
						try{
							component.setSlotIndex(slotIndex).execute();
						}catch(Throwable t){
							if(component.isContinueOnError()){
								LOG.error("component[{}] cause error,but flow is still go on",t,component.getClass().getSimpleName());
							}else{
								throw new Exception(t);
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
			LOG.error("executor cause error",e);
			return null;
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
