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
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.common.ChainConstant;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;
import com.yomahub.liteflow.enums.CmpStepTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ObjectConvertException;
import com.yomahub.liteflow.flow.FlowBus;
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
import com.yomahub.liteflow.util.LiteflowContextRegexMatcher;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Stack;

/**
 * 普通组件抽象类
 *
 * @author Bryan.Zhang
 * @author luo yi
 * @author Jay li
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

	private final ThreadLocal<Stack<Node>> refNodeStackTL = new ThreadLocal<>();

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
		cmpStep.setThreadName(Thread.currentThread().getName());
		slot.addStep(cmpStep);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		try {
			LOG.info("[O]start component[{}] execution", self.getDisplayName());

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

			// 步骤自定义数据设置
			cmpStep.setStepData(this.getRefNode().getStepData());

			// 结束时间设置
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
		Boolean isEnd = this.getRefNode().getIsEnd();
		if (ObjectUtil.isNull(isEnd)) {
			return false;
		}else {
			return isEnd;
		}
	}

	// 设置是否结束整个流程
	public void setIsEnd(boolean isEnd) {
		this.getRefNode().setIsEnd(isEnd);
	}

	public void setIsContinueOnError(boolean isContinueOnError) {
		this.getRefNode().setIsContinueOnErrorResult(isContinueOnError);
	}

	public Integer getSlotIndex() {
		return this.getRefNode().getSlotIndex();
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
		return this.getRefNode().getTag();
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
		return this.refNodeStackTL.get().peek();
	}

	public void setRefNode(Node refNode) {
		if (this.refNodeStackTL.get() == null){
			Stack<Node> stack = new Stack<>();
			stack.push(refNode);
			this.refNodeStackTL.set(stack);
		}else{
			Node compareNode = this.refNodeStackTL.get().peek();
			if (!compareNode.equals(refNode)) {
				this.refNodeStackTL.get().push(refNode);
			}
		}
	}

	public void removeRefNode() {
		if (this.refNodeStackTL.get() == null){
			return;
		}
		if (this.refNodeStackTL.get().size() > 1) {
			this.refNodeStackTL.get().pop();
		}else{
			this.refNodeStackTL.remove();
		}
	}

	/**
	 *
	 * @param clazz 要转换的class类型
	 * @return data对象
	 * @param <T> data的泛型
	 */
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

	public <T> List<T> getCmpDataList(Class<T> clazz) {
		String cmpData = getRefNode().getCmpData();
		if (StrUtil.isBlank(cmpData)) {
			return null;
		}
		return JsonUtil.parseList(cmpData, clazz);
	}

	public <T> T getBindData(String key, Class<T> clazz) {
		String bindData = getRefNode().getBindData(key);
		if (StrUtil.isBlank(bindData)) {
			return null;
		}

		//如果bind的value是一个正则表达式，说明要在上下文中进行搜索
		if (ReUtil.isMatch(ChainConstant.CONTEXT_SEARCH_REGEX, bindData)) {
			Object searchResult = LiteflowContextRegexMatcher.searchContext(
					this.getSlot().getContextBeanList(),
					ReUtil.getGroup1(ChainConstant.CONTEXT_SEARCH_REGEX, bindData)
			);

			if (searchResult == null){
				return null;
			}

			//搜索到的对象一定要符合给定的clazz
			if (clazz.isAssignableFrom(searchResult.getClass())) {
				return (T) searchResult;
			}else{
				String errMsg = StrUtil.format("{} cannot convert to {}", searchResult.getClass().getName(), clazz.getName());
				throw new ObjectConvertException(errMsg);
			}
		}else{
			if (clazz.equals(String.class) || clazz.equals(Object.class)) {
				return (T) bindData;
			}
			return JsonUtil.parseObject(bindData, clazz);
		}
	}

	public <T> List<T> getBindDataList(String key, Class<T> clazz) {
		String bindData = getRefNode().getBindData(key);
		if (StrUtil.isBlank(bindData)) {
			return null;
		}
		return JsonUtil.parseList(bindData, clazz);
	}

	@SuppressWarnings("unchecked")
	public <T> T getContextValue(String expression){
		return (T)LiteflowContextRegexMatcher.searchContext(this.getSlot().getContextBeanList(), expression);
	}

	public void setContextValue(String methodExpress, Object... values){
		LiteflowContextRegexMatcher.searchAndSetContext(this.getSlot().getContextBeanList(), methodExpress, values);
	}

	public Integer getLoopIndex() {
		return this.getRefNode().getLoopIndex();
	}

	public Integer getPreLoopIndex() {
		return this.getRefNode().getPreLoopIndex();
	}

	public Integer getPreNLoopIndex(int n) {
		return this.getRefNode().getPreNLoopIndex(n);
	}

	public <T> T getCurrLoopObj() {
		return this.getRefNode().getCurrLoopObject();
	}

	public <T> T getPreLoopObj() {
		return this.getRefNode().getPreLoopObject();
	}

	public <T> T getPreNLoopObj(int n) {
		return this.getRefNode().getPreNLoopObject(n);
	}

	public void setStepData(Object stepData) {
		this.getRefNode().setStepData(stepData);
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

	public long getCurrChainRuntimeId(){
		return FlowBus.getChain(getCurrChainId()).getRuntimeId();
	}

	protected String getMetaValueKey(){
		Class<?> originalClass = LiteFlowProxyUtil.getUserClass(this.getClass());
		return originalClass.getName();
	}

}
