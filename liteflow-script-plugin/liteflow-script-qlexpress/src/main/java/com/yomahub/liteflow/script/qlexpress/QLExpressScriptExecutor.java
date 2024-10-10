package com.yomahub.liteflow.script.qlexpress;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressLoader;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.InstructionSet;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 阿里QLExpress脚本语言的执行器实现
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class QLExpressScriptExecutor extends ScriptExecutor {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private ExpressRunner expressRunner;

	private final Map<String, InstructionSet> compiledScriptMap = new CopyOnWriteHashMap<>();

	@Override
	public ScriptExecutor init() {
		expressRunner = new ExpressRunner(true, false);
		//如果有生命周期则执行相应生命周期实现
		super.lifeCycle(expressRunner);
		return this;
	}

	@Override
	public void load(String nodeId, String script) {
		try {
			compiledScriptMap.put(nodeId, (InstructionSet) compile(script));
		}
		catch (Exception e) {
			String errorMsg = StrUtil.format("script loading error for node[{}],error msg:{}", nodeId, e.getMessage());
			throw new ScriptLoadException(errorMsg);
		}
	}

	@Override
	public void unLoad(String nodeId) {
		compiledScriptMap.remove(nodeId);
	}

	@Override
	public List<String> getNodeIds() {
		return new ArrayList<>(compiledScriptMap.keySet());
	}

	@Override
	public Object executeScript(ScriptExecuteWrap wrap) throws Exception {
		List<String> errorList = new ArrayList<>();
		try {
			if (!compiledScriptMap.containsKey(wrap.getNodeId())) {
				String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
				throw new ScriptLoadException(errorMsg);
			}

			InstructionSet instructionSet = compiledScriptMap.get(wrap.getNodeId());
			DefaultContext<String, Object> context = new DefaultContext<>();

			bindParam(wrap, context::put, context::putIfAbsent);

			return expressRunner.execute(instructionSet, context, errorList, true, false);
		}
		catch (Exception e) {
			for (String scriptErrorMsg : errorList) {
				log.error("\n{}", scriptErrorMsg);
			}
			throw e;
		}
	}

	@Override
	public void cleanCache() {
		compiledScriptMap.clear();
		expressRunner.clearExpressCache();
		ReflectUtil.setFieldValue(expressRunner, "loader", new ExpressLoader(expressRunner));
	}

	@Override
	public ScriptTypeEnum scriptType() {
		return ScriptTypeEnum.QLEXPRESS;
	}

	@Override
	public Object compile(String script) throws Exception {
		return expressRunner.getInstructionSetFromLocalCache(script);
	}

}
