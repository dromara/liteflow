package com.yomahub.liteflow.script;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.lifecycle.LifeCycleHolder;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.holder.CmpAroundAspectHolder;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 脚本执行器接口
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public abstract class ScriptExecutor {

	public ScriptExecutor init(){
		lifeCycle(null);
		return this;
	}

	public void lifeCycle(Object engine){
		if (CollUtil.isNotEmpty(LifeCycleHolder.getPostProcessScriptEngineInitLifeCycleList())){
			LifeCycleHolder.getPostProcessScriptEngineInitLifeCycleList().forEach(
					postProcessAfterScriptEngineInitLifeCycle -> postProcessAfterScriptEngineInitLifeCycle.postProcessAfterScriptEngineInit(engine)
			);
		}
	}

	public abstract void load(String nodeId, String script);

	// 卸载脚本（不包含 node）
	public abstract void unLoad(String nodeId);

	// 获取该执行器下的所有 nodeId
	public abstract List<String> getNodeIds();

	public Object execute(ScriptExecuteWrap wrap) throws Exception{
		try {
			return executeScript(wrap);
		}catch (Exception e) {
			if (ObjectUtil.isNotNull(e.getCause()) && e.getCause() instanceof LiteFlowException) {
				throw (LiteFlowException) e.getCause();
			}
			else if (ObjectUtil.isNotNull(e.getCause()) && e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
			else {
				throw e;
			}
		}
	}

	public abstract Object executeScript(ScriptExecuteWrap wrap) throws Exception;

	public abstract void cleanCache();

	public abstract ScriptTypeEnum scriptType();

	public void bindParam(ScriptExecuteWrap wrap, BiConsumer<String, Object> putConsumer, BiConsumer<String, Object> putIfAbsentConsumer){
		// 往脚本语言绑定表里循环增加绑定上下文的key
		// key的规则为自定义上下文的simpleName
		// 比如你的自定义上下文为AbcContext，那么key就为:abcContext
		// 这里不统一放一个map的原因是考虑到有些用户会调用上下文里的方法，而不是参数，所以脚本语言的绑定表里也是放多个上下文
		DataBus.getContextBeanList(wrap.getSlotIndex()).forEach(tuple -> putConsumer.accept(tuple.get(0), tuple.get(1)));

		// 把wrap对象转换成元数据map
		Map<String, Object> metaMap = BeanUtil.beanToMap(wrap);

		// 在元数据里放入主Chain的流程参数
		Slot slot = DataBus.getSlot(wrap.getSlotIndex());
		metaMap.put("requestData", slot.getRequestData());

		// 如果有隐式流程，则放入隐式流程的流程参数
		Object subRequestData = slot.getChainReqData(wrap.getCurrChainId());
		if (ObjectUtil.isNotNull(subRequestData)) {
			metaMap.put("subRequestData", subRequestData);
		}

		// 往脚本上下文里放入元数据
		putConsumer.accept("_meta", metaMap);

		// 放入用户自己定义的bean
		ScriptBeanManager.getScriptBeanMap().forEach(putIfAbsentConsumer);
	}

	/**
	 * 利用相应框架编译脚本
	 *
	 * @param script 脚本
	 * @return boolean
	 * @throws Exception 例外
	 */
	public abstract Object compile(String script) throws Exception;

	public boolean executeIsAccess(ScriptExecuteWrap wrap){
		return true;
	}

	public boolean executeIsContinueOnError(ScriptExecuteWrap wrap){
		return false;
	}

	public boolean executeIsEnd(ScriptExecuteWrap wrap){
		return false;
	}

	public void executeBeforeProcess(ScriptExecuteWrap wrap){
		CmpAroundAspectHolder.loadCmpAroundAspect().beforeProcess(wrap.getCmp());
	}

	public void executeAfterProcess(ScriptExecuteWrap wrap){
		CmpAroundAspectHolder.loadCmpAroundAspect().afterProcess(wrap.getCmp());
	}

	public void executeOnSuccess(ScriptExecuteWrap wrap) throws Exception{
		CmpAroundAspectHolder.loadCmpAroundAspect().onSuccess(wrap.getCmp());
	}

	public void executeOnError(ScriptExecuteWrap wrap, Exception e) throws Exception{
		CmpAroundAspectHolder.loadCmpAroundAspect().onError(wrap.getCmp(), e);
	}

	public void executeRollback(ScriptExecuteWrap wrap) throws Exception{

	}
}
