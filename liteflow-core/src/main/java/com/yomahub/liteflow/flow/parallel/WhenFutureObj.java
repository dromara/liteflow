package com.yomahub.liteflow.flow.parallel;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.WhenTimeoutException;

/**
 * 并行异步CompletableFuture里的值对象
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
public class WhenFutureObj {

	private boolean success;

	private boolean timeout;

	private String executorId;

	private Exception ex;

	public static WhenFutureObj success(String executorId) {
		WhenFutureObj result = new WhenFutureObj();
		result.setSuccess(true);
		result.setTimeout(false);
		result.setExecutorId(executorId);
		return result;
	}

	public static WhenFutureObj fail(String executorId, Exception ex) {
		WhenFutureObj result = new WhenFutureObj();
		result.setSuccess(false);
		result.setTimeout(false);
		result.setExecutorId(executorId);
		result.setEx(ex);
		return result;
	}

	public static WhenFutureObj timeOut(String executorId) {
		WhenFutureObj result = new WhenFutureObj();
		result.setSuccess(false);
		result.setTimeout(true);
		result.setExecutorId(executorId);
		result.setEx(new WhenTimeoutException(
				StrUtil.format("Timed out when executing the component[{}]",executorId)));
		return result;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getExecutorId() {
		return executorId;
	}

	public void setExecutorId(String executorId) {
		this.executorId = executorId;
	}

	public Exception getEx() {
		return ex;
	}

	public void setEx(Exception ex) {
		this.ex = ex;
	}

	public boolean isTimeout() {
		return timeout;
	}

	public void setTimeout(boolean timeout) {
		this.timeout = timeout;
	}

}
