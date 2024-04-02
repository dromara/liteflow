/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.ExecuteableTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ChainEndException;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.flow.executor.NodeExecutor;
import com.yomahub.liteflow.flow.executor.NodeExecutorHelper;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;

/**
 * Node节点，实现可执行器 Node节点并不是单例的，每构建一次都会copy出一个新的实例
 *
 * @author Bryan.Zhang
 * @author luo yi
 */
public class Node implements Executable, Cloneable, Rollbackable{

	private static final LFLog LOG = LFLoggerManager.getLogger(Node.class);

	private String id;

	private String name;

	private String clazz;

	private NodeTypeEnum type;

	private String script;

	private String language;

	// 增加该注解，避免在使用 Jackson 序列化检测循环引用时出现不必要异常
	@JsonIgnore
	private NodeComponent instance;

	private String tag;

	private String cmpData;

	private String currChainId;

	// node 的 isAccess 结果，主要用于 WhenCondition 的提前 isAccess 判断，避免 isAccess 方法重复执行
	private TransmittableThreadLocal<Boolean> accessResult = new TransmittableThreadLocal<>();

	// 循环下标
	private TransmittableThreadLocal<Integer> loopIndexTL = new TransmittableThreadLocal<>();

	// 迭代对象
	private TransmittableThreadLocal<Object> currLoopObject = new TransmittableThreadLocal<>();

	// 当前slot的index
	private TransmittableThreadLocal<Integer> slotIndexTL = new TransmittableThreadLocal<>();

	// 是否结束整个流程，这个只对串行流程有效，并行流程无效
	private TransmittableThreadLocal<Boolean> isEndTL = new TransmittableThreadLocal<>();

	// isContinueOnError 结果
	private TransmittableThreadLocal<Boolean> isContinueOnErrorResult =  new TransmittableThreadLocal<>();

	public Node() {

	}

	public Node(NodeComponent instance) {
		this.id = instance.getNodeId();
		this.name = instance.getName();
		this.instance = instance;
		this.type = instance.getType();
		this.clazz = instance.getClass().getName();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getTag() {
		return tag;
	}

	@Override
	public void setTag(String tag) {
		this.tag = tag;
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

	// node的执行主要逻辑
	// 所有的可执行节点，其实最终都会落到node上来，因为chain中包含的也是node
	@Override
	public void execute(Integer slotIndex) throws Exception {
		if (ObjectUtil.isNull(instance)) {
			throw new FlowSystemException("there is no instance for node id " + id);
		}

		try {
			// 把线程属性赋值给组件对象
			this.setSlotIndex(slotIndex);
			instance.setRefNode(this);

			// 判断是否可执行，所以isAccess经常作为一个组件进入的实际判断要素，用作检查slot里的参数的完备性
			if (getAccessResult() || instance.isAccess()) {
				LOG.info("[O]start component[{}] execution", instance.getDisplayName());

				// 这里开始进行重试的逻辑和主逻辑的运行
				NodeExecutor nodeExecutor = NodeExecutorHelper.loadInstance()
					.buildNodeExecutor(instance.getNodeExecutorClass());
				// 调用节点执行器进行执行
				nodeExecutor.execute(instance);
			} else {
				LOG.info("[X]skip component[{}] execution", instance.getDisplayName());
			}
			// 如果组件覆盖了isEnd方法，或者在在逻辑中主要调用了setEnd(true)的话，流程就会立马结束
			if (instance.isEnd()) {
				String errorInfo = StrUtil.format("[{}] lead the chain to end", instance.getDisplayName());
				throw new ChainEndException(errorInfo);
			}
		}
		catch (ChainEndException e) {
			throw e;
		}
		catch (Exception e) {
			// 如果组件覆盖了isEnd方法，或者在在逻辑中主要调用了setEnd(true)的话，流程就会立马结束
			if (instance.isEnd()) {
				String errorInfo = StrUtil.format("[{}] lead the chain to end", instance.getDisplayName());
				throw new ChainEndException(errorInfo);
			}
			// 如果组件覆盖了isContinueOnError方法，返回为true，那即便出了异常，也会继续流程
			else if (getIsContinueOnErrorResult() || instance.isContinueOnError()) {
				String errorMsg = StrUtil.format("component[{}] cause error,but flow is still go on", id);
				LOG.error(errorMsg);
			}
			else {
				String errorMsg = StrUtil.format("component[{}] cause error,error:{}", id, e.getMessage());
				LOG.error(errorMsg);
				throw e;
			}
		}
		finally {
			// 移除threadLocal里的信息
			this.getInstance().removeRefNode();
			removeSlotIndex();
			removeIsEnd();
			removeLoopIndex();
			removeAccessResult();
			removeIsContinueOnErrorResult();
		}
	}

	// 回滚的主要逻辑
	@Override
	public void rollback(Integer slotIndex) throws Exception {

		Slot slot = DataBus.getSlot(slotIndex);
		try {
			// 把线程属性赋值给组件对象
			this.setSlotIndex(slotIndex);
			instance.setRefNode(this);
			instance.doRollback();
		}
		catch (Exception e) {
			String errorMsg = StrUtil.format("component[{}] rollback error,error:{}", id, e.getMessage());
			LOG.error(errorMsg);
		}
		finally {
			// 移除threadLocal里的信息
			this.removeSlotIndex();
			instance.removeRefNode();
		}
	}

	// 在同步场景并不会单独执行这方法，同步场景会在execute里面去判断isAccess。
	// 但是在异步场景的any=true情况下，如果isAccess返回了false，那么异步的any有可能会认为这个组件先执行完。就会导致不正常
	// 增加这个方法是为了在异步的时候，先去过滤掉isAccess为false的异步组件。然后再异步执行。
	// 详情见这个issue:https://gitee.com/dromara/liteFlow/issues/I4XRBA
	@Override
	public boolean isAccess(Integer slotIndex) throws Exception {
		// 把线程属性赋值给组件对象
		this.setSlotIndex(slotIndex);
		instance.setRefNode(this);
		return instance.isAccess();
	}

	@Override
	public ExecuteableTypeEnum getExecuteType() {
		return ExecuteableTypeEnum.NODE;
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

	public boolean getAccessResult() {
		Boolean result = this.accessResult.get();
		return result != null && result;
	}

	public void setAccessResult(boolean accessResult) {
		this.accessResult.set(accessResult);
	}

	public void removeAccessResult() {
		this.accessResult.remove();
	}

	public boolean getIsContinueOnErrorResult() {
		Boolean result = this.isContinueOnErrorResult.get();
		return result != null && result;
	}

	public void setIsContinueOnErrorResult(boolean accessResult) {
		this.isContinueOnErrorResult.set(accessResult);
	}

	public void removeIsContinueOnErrorResult() {
		this.isContinueOnErrorResult.remove();
	}

	public void setLoopIndex(int index) {
		this.loopIndexTL.set(index);
	}

	public Integer getLoopIndex() {
		return this.loopIndexTL.get();
	}

	public void removeLoopIndex() {
		this.loopIndexTL.remove();
	}

	public void setCurrLoopObject(Object obj) {
		this.currLoopObject.set(obj);
	}

	public <T> T getCurrLoopObject() {
		return (T) this.currLoopObject.get();
	}

	public void removeCurrLoopObject() {
		this.currLoopObject.remove();
	}

	public Integer getSlotIndex(){
		return this.slotIndexTL.get();
	}

	public void setSlotIndex(Integer slotIndex){
		this.slotIndexTL.set(slotIndex);
	}

	public void removeSlotIndex(){
		this.slotIndexTL.remove();
	}

	public Boolean getIsEnd(){
		return this.isEndTL.get();
	}

	public void setIsEnd(Boolean isEnd){
		this.isEndTL.set(isEnd);
	}

	public void removeIsEnd(){
		this.isEndTL.remove();
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	@Override
	public <T> T getItemResultMetaValue(Integer slotIndex) {
		return instance.getItemResultMetaValue(slotIndex);
	}

	@Override
	public Node clone() throws CloneNotSupportedException {
		Node node = (Node)super.clone();
		node.loopIndexTL = new TransmittableThreadLocal<>();
		node.currLoopObject = new TransmittableThreadLocal<>();
		node.accessResult = new TransmittableThreadLocal<>();
		node.slotIndexTL = new TransmittableThreadLocal<>();
		node.isEndTL = new TransmittableThreadLocal<>();
		node.isContinueOnErrorResult = new TransmittableThreadLocal<>();
		return node;
	}
}
