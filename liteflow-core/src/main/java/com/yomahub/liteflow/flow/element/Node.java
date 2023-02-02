/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element;

import java.text.MessageFormat;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.flow.executor.NodeExecutor;
import com.yomahub.liteflow.flow.executor.NodeExecutorHelper;
import com.yomahub.liteflow.enums.ExecuteTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ChainEndException;
import com.yomahub.liteflow.exception.FlowSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node节点，实现可执行器
 * Node节点并不是单例的，每构建一次都会copy出一个新的实例
 * @author Bryan.Zhang
 */
public class Node implements Executable,Cloneable{

	private static final Logger LOG = LoggerFactory.getLogger(Node.class);

	private String id;

	private String name;

	private String clazz;

	private NodeTypeEnum type;

	private String script;

	private NodeComponent instance;

	private String tag;

	private String cmpData;

	private String currChainId;

	private TransmittableThreadLocal<Integer> loopIndexTL = new TransmittableThreadLocal<>();

	private TransmittableThreadLocal<Object> currLoopObject = new TransmittableThreadLocal<>();

	public Node(){

	}

	public Node(NodeComponent instance) {
		this.id = instance.getNodeId();
		this.name = instance.getName();
		this.instance = instance;
		this.type = instance.getType();
		this.clazz = instance.getClass().getName();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public NodeTypeEnum getType() {
		return type;
	}

	public void setType(NodeTypeEnum type) {
		this.type = type;
	}

	public NodeComponent getInstance() {
		return instance;
	}

	public void setInstance(NodeComponent instance) {
		this.instance = instance;
	}

	//node的执行主要逻辑
	//所有的可执行节点，其实最终都会落到node上来，因为chain中包含的也是node
	@Override
	public void execute(Integer slotIndex) throws Exception {
		if (ObjectUtil.isNull(instance)) {
			throw new FlowSystemException("there is no instance for node id " + id);
		}

		Slot slot = DataBus.getSlot(slotIndex);
		try {
			//把线程属性赋值给组件对象
			instance.setSlotIndex(slotIndex);
			instance.setRefNode(this);

			LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

			//判断是否可执行，所以isAccess经常作为一个组件进入的实际判断要素，用作检查slot里的参数的完备性
			if (instance.isAccess()) {
				//根据配置判断是否打印执行中的日志
				if (BooleanUtil.isTrue(liteflowConfig.getPrintExecutionLog())){
					LOG.info("[{}]:[O]start component[{}] execution",slot.getRequestId(), instance.getDisplayName());
				}

				//这里开始进行重试的逻辑和主逻辑的运行
				NodeExecutor nodeExecutor = NodeExecutorHelper.loadInstance().buildNodeExecutor(instance.getNodeExecutorClass());
				// 调用节点执行器进行执行
				nodeExecutor.execute(instance);
				//如果组件覆盖了isEnd方法，或者在在逻辑中主要调用了setEnd(true)的话，流程就会立马结束
				if (instance.isEnd()) {
					String errorInfo = StrUtil.format("[{}]:[{}] lead the chain to end", slot.getRequestId(), instance.getDisplayName());
					throw new ChainEndException(errorInfo);
				}
			} else {
				if (BooleanUtil.isTrue(liteflowConfig.getPrintExecutionLog())){
					LOG.info("[{}]:[X]skip component[{}] execution", slot.getRequestId(), instance.getDisplayName());
				}
			}
		} catch (ChainEndException e){
			throw e;
		} catch (Exception e) {
			//如果组件覆盖了isContinueOnError方法，返回为true，那即便出了异常，也会继续流程
			if (instance.isContinueOnError()) {
				String errorMsg = MessageFormat.format("[{0}]:component[{1}] cause error,but flow is still go on", slot.getRequestId(),id);
				LOG.error(errorMsg);
			} else {
				String errorMsg = MessageFormat.format("[{0}]:component[{1}] cause error,error:{2}",slot.getRequestId(),id,e.getMessage());
				LOG.error(errorMsg);
				throw e;
			}
		} finally {
			//移除threadLocal里的信息
			instance.removeSlotIndex();
			instance.removeIsEnd();
			instance.removeRefNode();
			removeLoopIndex();
		}
	}

	//在同步场景并不会单独执行这方法，同步场景会在execute里面去判断isAccess。
	//但是在异步场景的any=true情况下，如果isAccess返回了false，那么异步的any有可能会认为这个组件先执行完。就会导致不正常
	//增加这个方法是为了在异步的时候，先去过滤掉isAccess为false的异步组件。然后再异步执行。
	//详情见这个issue:https://gitee.com/dromara/liteFlow/issues/I4XRBA
	@Override
	public boolean isAccess(Integer slotIndex) throws Exception {
		//把线程属性赋值给组件对象
		instance.setSlotIndex(slotIndex);
		instance.setRefNode(this);
		return instance.isAccess();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public Node copy() throws Exception{
		return (Node) this.clone();
	}

	@Override
	public ExecuteTypeEnum getExecuteType() {
		return ExecuteTypeEnum.NODE;
	}

	@Override
	public String getExecuteId() {
		return id;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public String getCmpData() {
		return cmpData;
	}

	public void setCmpData(String cmpData) {
		this.cmpData = cmpData;
	}

	@Override
	public void setCurrChainId(String currentChainId) {
		this.currChainId = currentChainId;
	}

	public String getCurrChainId() {
		return currChainId;
	}

	public void setLoopIndex(int index){
		this.loopIndexTL.set(index);
	}

	public Integer getLoopIndex(){
		return this.loopIndexTL.get();
	}

	public void removeLoopIndex(){
		this.loopIndexTL.remove();
	}

	public void setCurrLoopObject(Object obj){
		this.currLoopObject.set(obj);
	}

	public <T> T getCurrLoopObject(){
		return (T)this.currLoopObject.get();
	}

	public void removeCurrLoopObject(){
		this.currLoopObject.remove();
	}
}
