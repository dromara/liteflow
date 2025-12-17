package com.yomahub.liteflow.script.qlexpress;

import cn.hutool.core.util.StrUtil;
import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.QLResult;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.ClassSupplier;
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

	/**
	 * 预设的ClassSupplier，用于延迟初始化场景（如插件化）
	 * 在Spring初始化完成后，插件加载器可以通过 setClassSupplier 方法设置自定义的ClassSupplier，
	 * 然后调用 reinit() 方法重新初始化QLExpress引擎
	 */
	private static volatile ClassSupplier presetClassSupplier;

	/**
	 * 设置预设的ClassSupplier，用于插件化场景
	 * 应在插件类加载器准备好之后、调用reinit()之前设置
	 *
	 * @param classSupplier 自定义的ClassSupplier实现
	 */
	public static void setClassSupplier(ClassSupplier classSupplier) {
		presetClassSupplier = classSupplier;
	}

	/**
	 * 获取当前预设的ClassSupplier
	 *
	 * @return 当前预设的ClassSupplier，如果未设置则返回null
	 */
	public static ClassSupplier getClassSupplier() {
		return presetClassSupplier;
	}

	@Override
	public ScriptExecutor init() {
		return doInit(presetClassSupplier);
	}

	/**
	 * 使用指定的ClassSupplier重新初始化QLExpress引擎
	 * 此方法适用于插件化场景，在插件类加载器准备好之后调用
	 *
	 * @param classSupplier 自定义的ClassSupplier实现
	 * @return this
	 */
	public ScriptExecutor reinit(ClassSupplier classSupplier) {
		// 清理已编译的脚本缓存
		compiledScriptMap.clear();
		if (expressRunner != null) {
			expressRunner.clearCompileCache();
		}
		// 更新预设的ClassSupplier
		presetClassSupplier = classSupplier;
		return doInit(classSupplier);
	}

	/**
	 * 内部初始化方法
	 */
	private ScriptExecutor doInit(ClassSupplier classSupplier) {
		InitOptions.Builder optionsBuilder = InitOptions.builder()
				.securityStrategy(QLSecurityStrategy.open());

		// 使用传入的ClassSupplier
		if (classSupplier != null) {
			optionsBuilder.classSupplier(classSupplier);
			log.info("QLExpress4 using custom ClassSupplier: {}", classSupplier.getClass().getName());
		}

		expressRunner = new Express4Runner(optionsBuilder.build());
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
