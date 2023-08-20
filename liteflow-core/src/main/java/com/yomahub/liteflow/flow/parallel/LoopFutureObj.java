package com.yomahub.liteflow.flow.parallel;

/**
 * 并行循环各个并行子项的执行结果对象
 * 如果该子项执行成功，则success为true，ex为null
 * 否则success为false，ex为程序抛出异常
 *
 * @author zhhhhy
 * @since 2.11.0
 */

public class LoopFutureObj {
    private String executorName;
    private boolean success;
    private Exception ex;


    public static LoopFutureObj success(String executorName) {
        LoopFutureObj result = new LoopFutureObj();
        result.setSuccess(true);
        result.setExecutorName(executorName);
        return result;
    }

    public static LoopFutureObj fail(String executorName, Exception ex) {
        LoopFutureObj result = new LoopFutureObj();
        result.setSuccess(false);
        result.setExecutorName(executorName);
        result.setEx(ex);
        return result;
    }

    public Exception getEx() {
        return ex;
    }

    public String getExecutorName() {
        return executorName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setEx(Exception ex) {
        this.ex = ex;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
