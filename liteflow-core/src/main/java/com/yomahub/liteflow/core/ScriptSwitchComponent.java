package com.yomahub.liteflow.core;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.ScriptExecutorFactory;

import java.util.Map;

/**
 * 脚本条件节点
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class ScriptSwitchComponent extends NodeSwitchComponent implements ScriptComponent {

	private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

	private ScriptExecutor scriptExecutor;

	@Override
	public String processSwitch() throws Exception {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		return (String) scriptExecutor.execute(wrap);
	}

	@Override
	public void loadScript(String script, String language) {
		LOG.info("load script for component[{}]", getDisplayName());
		scriptExecutor = ScriptExecutorFactory.loadInstance().getScriptExecutor(language);
		scriptExecutor.load(getNodeId(), script);
	}

	@Override
	public boolean isAccess() {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		return scriptExecutor.executeIsAccess(wrap);
	}

	@Override
	public boolean isContinueOnError() {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		return scriptExecutor.executeIsContinueOnError(wrap);
	}

	@Override
	public boolean isEnd() {
		if (!ScriptTypeEnum.JAVA.getDisplayName().equals(this.getRefNode().getLanguage())){
			return super.isEnd();
		}
		ScriptExecuteWrap wrap = this.buildWrap(this);
		return scriptExecutor.executeIsEnd(wrap);
	}

	@Override
	public void beforeProcess() {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		scriptExecutor.executeBeforeProcess(wrap);
	}

	@Override
	public void afterProcess() {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		scriptExecutor.executeAfterProcess(wrap);
	}

	@Override
	public void onSuccess() throws Exception {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		scriptExecutor.executeOnSuccess(wrap);
	}

	@Override
	public void onError(Exception e) throws Exception {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		scriptExecutor.executeOnError(wrap, e);
	}

	@Override
	public void rollback() throws Exception {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		scriptExecutor.executeRollback(wrap);
	}
}
