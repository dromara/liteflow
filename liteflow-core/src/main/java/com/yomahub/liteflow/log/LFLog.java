package com.yomahub.liteflow.log;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * 日志包装类
 * @since 2.10.5
 * @author Bryan.Zhang
 */
public class LFLog implements Logger{

    private Logger log;

    private LiteflowConfig liteflowConfig;

    public LFLog(Logger log) {
        this.log = log;
    }

    private String getRId(){
        String requestId = LFLoggerManager.getRequestId();
        if (StrUtil.isBlank(requestId)){
            return StrUtil.EMPTY;
        }else{
            return StrUtil.format("[{}]:", LFLoggerManager.getRequestId());
        }
    }

    private boolean isPrint(){
        try{
            if (ObjectUtil.isNull(liteflowConfig)){
                liteflowConfig = LiteflowConfigGetter.get();
            }

            if (ObjectUtil.isNull(liteflowConfig)){
                return true;
            }

            return liteflowConfig.getPrintExecutionLog();
        }catch (Exception e){
            //这里如果出错，肯定是在启动阶段，但是判断日志是不是应该打印，不应该报错，所以不处理错误
            //返回错误依旧打印
            return true;
        }
    }

    @Override
    public String getName() {
        return this.log.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return this.log.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        this.log.trace(getRId() + s);
    }

    @Override
    public void trace(String s, Object o) {
        this.log.trace(getRId() + s, o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        this.log.trace(getRId() + s, o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        this.log.trace(getRId() + s, objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        this.log.trace(getRId() + s, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return this.log.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        this.log.trace(marker, getRId() + s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        this.log.trace(marker, getRId() + s, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        this.log.trace(marker, getRId() + s, o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        this.log.trace(marker, getRId() + s, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        this.log.trace(marker, getRId() + s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return this.log.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        this.log.debug(getRId() + s);
    }

    @Override
    public void debug(String s, Object o) {
        this.log.debug(getRId() + s, o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        this.log.debug(getRId() + s, o, o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        this.log.debug(getRId() + s, objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        this.log.debug(getRId() + s, throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return this.log.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        this.log.debug(marker, getRId() + s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        this.log.debug(marker, getRId() + s, o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        this.log.debug(marker, getRId() + s, o, o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        this.log.debug(marker, getRId() + s, objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        this.log.debug(marker, getRId() + s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return this.log.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        if (isPrint()) {
            this.log.info(getRId() + s);
        }
    }

    @Override
    public void info(String s, Object o) {
        if (isPrint()) {
            this.log.info(getRId() + s, o);
        }
    }

    @Override
    public void info(String s, Object o, Object o1) {
        if (isPrint()) {
            this.log.info(getRId() + s, o, o1);
        }
    }

    @Override
    public void info(String s, Object... objects) {
        if (isPrint()) {
            this.log.info(getRId() + s, objects);
        }
    }

    @Override
    public void info(String s, Throwable throwable) {
        if (isPrint()) {
            this.log.info(getRId() + s, throwable);
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return this.log.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        if (isPrint()) {
            this.log.info(marker, getRId() + s);
        }
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        if (isPrint()) {
            this.log.info(marker, getRId() + s, o);
        }
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        if (isPrint()) {
            this.log.info(marker, getRId() + s, o, o1);
        }
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        if (isPrint()) {
            this.log.info(marker, getRId() + s , objects);
        }
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        if (isPrint()) {
            this.log.info(marker, getRId() + s ,throwable);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return this.log.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        if (isPrint()) {
            this.log.warn(getRId() + s);
        }
    }

    @Override
    public void warn(String s, Object o) {
        if (isPrint()) {
            this.log.warn(getRId() + s, o);
        }
    }

    @Override
    public void warn(String s, Object... objects) {
        if (isPrint()) {
            this.log.warn(getRId() + s, objects);
        }
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        if (isPrint()) {
            this.log.warn(getRId() + s, o, o1);
        }
    }

    @Override
    public void warn(String s, Throwable throwable) {
        if (isPrint()) {
            this.log.warn(getRId() + s, throwable);
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return this.log.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        if (isPrint()) {
            this.log.warn(marker, getRId() + s);
        }
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        if (isPrint()) {
            this.log.warn(marker, getRId() + s, o);
        }
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        if (isPrint()) {
            this.log.warn(marker, getRId() + s, o, o1);
        }
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        if (isPrint()) {
            this.log.warn(marker, getRId() + s, objects);
        }
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        if (isPrint()) {
            this.log.warn(marker, getRId() + s, throwable);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return this.log.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        if (isPrint()) {
            this.log.error(getRId() + s);
        }
    }

    @Override
    public void error(String s, Object o) {
        if (isPrint()) {
            this.log.error(getRId() + s, o);
        }
    }

    @Override
    public void error(String s, Object o, Object o1) {
        if (isPrint()) {
            this.log.error(getRId() + s, o, o1);
        }

    }

    @Override
    public void error(String s, Object... objects) {
        if (isPrint()) {
            this.log.error(getRId() + s, objects);
        }
    }

    @Override
    public void error(String s, Throwable throwable) {
        if (isPrint()) {
            this.log.error(getRId() + s, throwable);
        }else{
            this.log.error(getRId() + s);
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return this.log.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        if (isPrint()) {
            this.log.error(marker, getRId() + s);
        }
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        if (isPrint()) {
            this.log.error(marker, getRId() + s, o);
        }
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        if (isPrint()) {
            this.log.error(marker, getRId() + s, o, o1);
        }
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        if (isPrint()) {
            this.log.error(marker, getRId() + s, objects);
        }
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        if (isPrint()) {
            this.log.error(marker, getRId() + s, throwable);
        }
    }
}
