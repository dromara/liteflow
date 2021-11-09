package com.yomahub.liteflow.entity.flow.parallel;

/**
 * 并行异步CompletableFuture里的值对象
 * @author Bryan.Zhang
 * @since 2.6.4
 */
public class WhenFutureObj {

    private boolean success;

    private boolean timeout;

    private String executorName;

    private Exception ex;

    public static WhenFutureObj success(String executorName){
        WhenFutureObj result = new WhenFutureObj();
        result.setSuccess(true);
        result.setTimeout(false);
        result.setExecutorName(executorName);
        return result;
    }

    public static WhenFutureObj fail(String executorName, Exception ex){
        WhenFutureObj result = new WhenFutureObj();
        result.setSuccess(false);
        result.setTimeout(false);
        result.setExecutorName(executorName);
        result.setEx(ex);
        return result;
    }

    public static WhenFutureObj timeOut(String executorName){
        WhenFutureObj result = new WhenFutureObj();
        result.setSuccess(false);
        result.setTimeout(true);
        result.setExecutorName(executorName);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
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
