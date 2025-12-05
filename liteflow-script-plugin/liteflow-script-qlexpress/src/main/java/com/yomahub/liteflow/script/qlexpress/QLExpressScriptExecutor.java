package com.yomahub.liteflow.script.qlexpress;

import cn.hutool.core.util.StrUtil;
import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.QLResult;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.security.QLSecurityStrategy;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
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

	private Express4Runner expressRunner;

	private final Map<String, String> compiledScriptMap = new CopyOnWriteHashMap<>();

	@Override
	public ScriptExecutor init() {
		expressRunner = new Express4Runner(InitOptions.builder().securityStrategy(QLSecurityStrategy.open()).build());
		//如果有生命周期则执行相应生命周期实现
		super.lifeCycle(expressRunner);
		return this;
	}

	@Override
	public void load(String nodeId, String script) {
		try {
			expressRunner.parseToDefinitionWithCache(script);
			compiledScriptMap.put(nodeId, script);
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
		try {
			if (!compiledScriptMap.containsKey(wrap.getNodeId())) {
				String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
				throw new ScriptLoadException(errorMsg);
			}

			String script = compiledScriptMap.get(wrap.getNodeId());
			Map<String, Object> context = new HashMap<>();

			bindParam(wrap, context::put, context::putIfAbsent);

			QLResult expressResult = expressRunner.execute(script, context, QLOptions.builder().cache(true).build());
			return expressResult.getResult();
		}
		catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void cleanCache() {
		compiledScriptMap.clear();
        expressRunner.clearCompileCache();
		//如果有生命周期则执行相应生命周期实现
		super.lifeCycle(expressRunner);
	}

	@Override
	public ScriptTypeEnum scriptType() {
		return ScriptTypeEnum.QLEXPRESS;
	}

	@Override
	public Object compile(String script) throws Exception {
        return expressRunner.parseToDefinitionWithCache(script);
	}

}
