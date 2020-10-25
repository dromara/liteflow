/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.flow;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.enums.ExecuteTypeEnum;
import com.yomahub.liteflow.exception.ChainEndException;
import com.yomahub.liteflow.exception.FlowSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node implements Executable{

	private static final Logger LOG = LoggerFactory.getLogger(Node.class);

	private String id;

	private String clazz;

	private NodeComponent instance;

	private Map<String, Executable> condNodeMap = new HashMap<String, Executable>();

	public Node(){

	}

	public Node(String id, String clazz, NodeComponent instance) {
		this.id = id;
		this.clazz = clazz;
		this.instance = instance;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public NodeComponent getInstance() {
		return instance;
	}

	public void setInstance(NodeComponent instance) {
		this.instance = instance;
	}

	public Executable getCondNode(String nodeId){
		return this.condNodeMap.get(nodeId);
	}

	public void setCondNode(String nodeId, Executable condNode){
		this.condNodeMap.put(nodeId, condNode);
	}

	@Override
	public void execute(Integer slotIndex) throws Exception {
		if(instance == null){
			throw new FlowSystemException("there is no instance for node id " + id);
		}
		instance.setSlotIndex(slotIndex);
		Slot slot = DataBus.getSlot(slotIndex);

		try{
			if(instance.isAccess()){

				instance.execute();

				if(instance.isEnd()){
					String errorInfo = StrUtil.format("[{}]:component[{}] lead the chain to end",slot.getRequestId(),instance.getClass().getSimpleName());
					throw new ChainEndException(errorInfo);
				}
			}else{
				LOG.info("[{}]:[X]skip component[{}] execution",slot.getRequestId(),instance.getClass().getSimpleName());
			}
		}catch (Exception e){
			if(instance.isContinueOnError()){
				String errorMsg = MessageFormat.format("[{0}]:component[{1}] cause error,but flow is still go on", slot.getRequestId(),id);
				LOG.error(errorMsg,e);
			}else{
				String errorMsg = MessageFormat.format("[{0}]:component[{1}] cause error,error:{2}",slot.getRequestId(),id,e.getMessage());
				LOG.error(errorMsg,e);
				throw e;
			}
		}finally {
			instance.removeSlotIndex();
			instance.removeIsEnd();
		}
	}

	@Override
	public ExecuteTypeEnum getExecuteType() {
		return ExecuteTypeEnum.NODE;
	}

	@Override
	public String getExecuteName() {
		return id;
	}
}
