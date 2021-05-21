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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.enums.ExecuteTypeEnum;
import com.yomahub.liteflow.exception.ChainEndException;
import com.yomahub.liteflow.exception.FlowSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node节点，实现可执行器
 * @author Bryan.Zhang
 */
public class Node implements Executable{

	private static final Logger LOG = LoggerFactory.getLogger(Node.class);

	private String id;

	private String clazz;

	private NodeComponent instance;

	private final Map<String, Executable> condNodeMap = new HashMap<>();

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

	//node的执行主要逻辑
	//所有的可执行节点，其实最终都会落到node上来，因为chain中包含的也是node
	@Override
	public void execute(Integer slotIndex) throws Exception {
		if (ObjectUtil.isNull(instance)) {
			throw new FlowSystemException("there is no instance for node id " + id);
		}
		//每次执行node前，把分配的slot index信息放入threadLocal里
		instance.setSlotIndex(slotIndex);
		Slot slot = DataBus.getSlot(slotIndex);

		try{
			//判断是否可执行，所以isAccess经常作为一个组件进入的实际判断要素，用作检查slot里的参数的完备性
			if (instance.isAccess()) {

				//执行业务逻辑的主要入口
				instance.execute();

				//如果组件覆盖了isEnd方法，或者在在逻辑中主要调用了setEnd(true)的话，流程就会立马结束
				if (instance.isEnd()) {
					String errorInfo = StrUtil.format("[{}]:component[{}] lead the chain to end",slot.getRequestId(),instance.getClass().getSimpleName());
					throw new ChainEndException(errorInfo);
				}
			} else {
				LOG.info("[{}]:[X]skip component[{}] execution",slot.getRequestId(),instance.getClass().getSimpleName());
			}
		} catch (Exception e) {
			//如果组件覆盖了isContinueOnError方法，返回为true，那即便出了异常，也会继续流程
			if (instance.isContinueOnError()) {
				String errorMsg = MessageFormat.format("[{0}]:component[{1}] cause error,but flow is still go on", slot.getRequestId(),id);
				LOG.error(errorMsg,e);
			} else {
				String errorMsg = MessageFormat.format("[{0}]:component[{1}] cause error,error:{2}",slot.getRequestId(),id,e.getMessage());
				LOG.error(errorMsg,e);
				throw e;
			}
		} finally {
			//移除threadLocal里的信息
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
