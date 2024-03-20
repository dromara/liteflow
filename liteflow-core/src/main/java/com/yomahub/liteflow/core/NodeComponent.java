/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;
import com.yomahub.liteflow.enums.CmpStepTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.entity.CmpStep;
import com.yomahub.liteflow.flow.executor.DefaultNodeExecutor;
import com.yomahub.liteflow.flow.executor.NodeExecutor;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.monitor.CompStatistics;
import com.yomahub.liteflow.monitor.MonitorBus;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.holder.CmpAroundAspectHolder;
import com.yomahub.liteflow.util.JsonUtil;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 普通组件抽象类
 *
 * @author Bryan.Zhang
 * @author luo yi
 */
public abstract class NodeComponent{

	private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

	private MonitorBus monitorBus;

	private String nodeId;

	private String name;

	private NodeTypeEnum type;

	// 这是自己的实例，取代this
	// 为何要设置这个，用this不行么，因为如果有aop去切的话，this在spring的aop里是切不到的。self对象有可能是代理过的对象
	private NodeComponent self;

	// 重试次数
	private int retryCount = 0;

	// 是否重写了rollback方法
	private boolean isRollback = false;

	// 在目标异常抛出时才重试
	private Class<? extends Exception>[] retryForExceptions = new Class[] { Exception.class };

	/** 节点执行器的类全名 */
	private Class<? extends NodeExecutor> nodeExecutorClass = DefaultNodeExecutor.class;

	/** 当前对象为单例，注册进spring上下文，但是node实例不是单例，这里通过对node实例的引用来获得一些链路属性 **/

	private final TransmittableThreadLocal<Node> refNodeTL = new TransmittableThreadLocal<>();

	/**
	 ******************* 以下的属性为线程附加属性********************
	 * 线程属性是指每一个request的值都是不一样的
	 * 这里NodeComponent是单例，所以要用ThreadLocal来修饰
	 */

	public NodeComponent() {
		// 反射判断是否重写了rollback方法
		Class<?> clazz = this.getClass();
		try {
			Method method = clazz.getDeclaredMethod("rollback");
			if(ObjectUtil.isNotNull(method)){
				this.setRollback(true);
			}
		} catch (Exception ignored) {}
	}

	public void execute() throws Exception {
		Slot slot = this.getSlot();

		// 在元数据里加入step信息
		CmpStep cmpStep = new CmpStep(nodeId, name, CmpStepTypeEnum.SINGLE);
		cmpStep.setTag(this.getTag());
		cmpStep.setInstance(this);
		cmpStep.setRefNode(this.getRefNode());
		cmpStep.setStartTime(new Date());
		slot.addStep(cmpStep);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		try {
			// 前置处理
			self.beforeProcess();

			// 主要的处理逻辑
			self.process();

			// 成功后回调方法
			self.onSuccess();

			// 步骤状态设为true
			cmpStep.setSuccess(true);
		}
		catch (Exception e) {
			// 步骤状态设为false，并加入异常
			cmpStep.setSuccess(false);
			cmpStep.setException(e);

			// 执行失败后回调方法
			// 这里要注意，失败方法本身抛出错误，只打出堆栈，往外抛出的还是主要的异常
			try {
				self.onError(e);
			}
			catch (Exception ex) {
				String errMsg = StrUtil.format("component[{}] onError method happens exception", this.getDisplayName());
				LOG.error(errMsg, ex);
			}
			throw e;
		}
		finally {
			// 后置处理
			self.afterProcess();

			stopWatch.stop();
			final long timeSpent = stopWatch.getTotalTimeMillis();
			LOG.info("component[{}] finished in {} milliseconds", this.getDisplayName(), timeSpent);

			cmpStep.setEndTime(new Date());

			// 往CmpStep中放入时间消耗信息
			cmpStep.setTimeSpent(timeSpent);

			// 性能统计
			if (ObjectUtil.isNotNull(monitorBus)) {
				CompStatistics statistics = new CompStatistics(this.getClass().getSimpleName(), timeSpent);
				monitorBus.addStatistics(statistics);
			}
		}
	}

	public void doRollback() throws Exception {
		Slot slot = this.getSlot();

		boolean alreadyRollback = slot.getRollbackSteps().stream().anyMatch(cmpStep -> cmpStep.getRefNode().equals(getRefNode()));
		if (alreadyRollback){
			return;
		}

		CmpStep cmpStep = new CmpStep(nodeId, name, CmpStepTypeEnum.SINGLE);
		cmpStep.setTag(this.getTag());
		cmpStep.setInstance(this);
		cmpStep.setRefNode(this.getRefNode());
		slot.addRollbackStep(cmpStep);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		try {
			self.rollback();
		}
		catch (Exception e) {
			throw e;
		}
		finally {
			stopWatch.stop();
			final long timeSpent = stopWatch.getTotalTimeMillis();
			LOG.info("component[{}] rollback in {} milliseconds", this.getDisplayName(), timeSpent);

			// 往CmpStep中放入时间消耗信息
			cmpStep.setRollbackTimeSpent(timeSpent);
		}
	}

	public void beforeProcess() {
		// 全局切面只在spring体系下生效，这里用了spi机制取到相应环境下的实现类
		// 非spring环境下，全局切面为空实现
		CmpAroundAspectHolder.loadCmpAroundAspect().beforeProcess(this.self);
	}

	public abstract void process() throws Exception;

	public void rollback() throws Exception{
		// 如果需要失败后回滚某个方法，请覆盖这个方法
	};

	public void onSuccess() throws Exception {
		// 如果需要在成功后回调某一个方法，请覆盖这个方法
		// 全局切面只在spring体系下生效，这里用了spi机制取到相应环境下的实现类
		// 非spring环境下，全局切面为空实现
		CmpAroundAspectHolder.loadCmpAroundAspect().onSuccess(this.self);
	}

	public void onError(Exception e) throws Exception {
		// 如果需要在抛错后回调某一段逻辑，请覆盖这个方法
		// 全局切面只在spring体系下生效，这里用了spi机制取到相应环境下的实现类
		// 非spring环境下，全局切面为空实现
		CmpAroundAspectHolder.loadCmpAroundAspect().onError(this.self, e);
	}

	public void afterProcess() {
		CmpAroundAspectHolder.loadCmpAroundAspect().afterProcess(this.self);
	}

	// 是否进入该节点
	public boolean isAccess() {
		return true;
	}

	// 出错是否继续执行
	public boolean isContinueOnError() {
		return false;
	}

	// 是否结束整个流程(不往下继续执行)
	public boolean isEnd() {
		Boolean isEnd = this.refNodeTL.get().getIsEnd();
		if (ObjectUtil.isNull(isEnd)) {
			return false;
		}else {
			return isEnd;
		}
	}

	// 设置是否结束整个流程
	public void setIsEnd(boolean isEnd) {
		this.refNodeTL.get().setIsEnd(isEnd);
	}

	public void setIsContinueOnError(boolean isContinueOnError) {
		this.refNodeTL.get().setIsContinueOnErrorResult(isContinueOnError);
	}

	public Integer getSlotIndex() {
		return this.refNodeTL.get().getSlotIndex();
	}

	public Slot getSlot() {
		return DataBus.getSlot(this.getSlotIndex());
	}

	public <T> T getFirstContextBean() {
		return this.getSlot().getFirstContextBean();
	}

	public <T> T getContextBean(Class<T> contextBeanClazz) {
		return this.getSlot().getContextBean(contextBeanClazz);
	}

	public <T> T getContextBean(String contextName) {
		return this.getSlot().getContextBean(contextName);
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public NodeComponent getSelf() {
		return self;
	}

	public void setSelf(NodeComponent self) {
		this.self = self;
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

	public <T> void sendPrivateDeliveryData(String nodeId, T t) {
		this.getSlot().setPrivateDeliveryData(nodeId, t);
	}

	public <T> T getPrivateDeliveryData() {
		return this.getSlot().getPrivateDeliveryData(this.getNodeId());
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public Class<? extends Exception>[] getRetryForExceptions() {
		return retryForExceptions;
	}

	public void setRetryForExceptions(Class<? extends Exception>[] retryForExceptions) {
		this.retryForExceptions = retryForExceptions;
	}

	public Class<? extends NodeExecutor> getNodeExecutorClass() {
		return nodeExecutorClass;
	}

	public void setNodeExecutorClass(Class<? extends NodeExecutor> nodeExecutorClass) {
		this.nodeExecutorClass = nodeExecutorClass;
	}

	public String getTag() {
		return this.refNodeTL.get().getTag();
	}

	public MonitorBus getMonitorBus() {
		return monitorBus;
	}

	public void setMonitorBus(MonitorBus monitorBus) {
		this.monitorBus = monitorBus;
	}

	public <T> T getRequestData() {
		return getSlot().getRequestData();
	}

	public <T> T getSubChainReqData() {
		return getSlot().getChainReqData(this.getCurrChainId());
	}

	public <T> T getSubChainReqDataInAsync() {
		return getSlot().getChainReqDataFromQueue(this.getCurrChainId());
	}

	public boolean isRollback() {
		return isRollback;
	}

	public void setRollback(boolean rollback) {
		isRollback = rollback;
	}

	/**
	 * @deprecated 请使用 {@link #getChainId()}
	 * @return String
	 */
	@Deprecated
	public String getChainName() {
		return getSlot().getChainName();
	}

	public String getChainId() {
		return getSlot().getChainId();
	}

	public String getDisplayName() {
		if (StrUtil.isEmpty(this.name)) {
			return this.nodeId;
		}
		else {
			return StrUtil.format("{}({})", this.nodeId, this.name);
		}
	}

	public String getCurrChainId() {
		return getRefNode().getCurrChainId();
	}

	public Node getRefNode() {
		return this.refNodeTL.get();
	}

	public void setRefNode(Node refNode) {
		this.refNodeTL.set(refNode);
	}

	public void removeRefNode() {
		this.refNodeTL.remove();
	}

	public <T> T getCmpData(Class<T> clazz) {
		String cmpData = getRefNode().getCmpData();
		if (StrUtil.isBlank(cmpData)) {
			return null;
		}
		if (clazz.equals(String.class) || clazz.equals(Object.class)) {
			return (T) cmpData;
		}
		return JsonUtil.parseObject(cmpData, clazz);
	}

	public Integer getLoopIndex() {
		return this.refNodeTL.get().getLoopIndex();
	}

	public <T> T getCurrLoopObj() {
		return this.refNodeTL.get().getCurrLoopObject();
	}

	@Deprecated
	public void invoke(String chainId, Object param) throws Exception {
		FlowExecutorHolder.loadInstance().invoke(chainId, param, this.getSlotIndex());
	}

	public LiteflowResponse invoke2Resp(String chainId, Object param) {
		return FlowExecutorHolder.loadInstance().invoke2Resp(chainId, param, this.getSlotIndex());
	}

	@Deprecated
	public void invokeInAsync(String chainId, Object param) throws Exception {
		FlowExecutorHolder.loadInstance().invokeInAsync(chainId, param, this.getSlotIndex());
	}

	public LiteflowResponse invoke2RespInAsync(String chainId, Object param) {
		return FlowExecutorHolder.loadInstance().invoke2RespInAsync(chainId, param, this.getSlotIndex());
	}

	public <T> T getItemResultMetaValue(Integer slotIndex){
		return null;
	}

	protected String getMetaValueKey(){
		Class<?> originalClass = LiteFlowProxyUtil.getUserClass(this.getClass());
		return originalClass.getName();
	}
}
