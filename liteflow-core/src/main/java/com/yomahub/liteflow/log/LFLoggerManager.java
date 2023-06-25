package com.yomahub.liteflow.log;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志包装工厂
 * @since 2.10.5
 * @author Bryan.Zhang
 */
public class LFLoggerManager {

    private static final Map<String, LFLog> logMap = new HashMap<>();

    private static final TransmittableThreadLocal<String> requestIdTL = new TransmittableThreadLocal<>();

    public static LFLog getLogger(Class<?> clazz){
        if (logMap.containsKey(clazz.getName())){
            return logMap.get(clazz.getName());
        }else{
            Logger log = LoggerFactory.getLogger(clazz.getName());
            LFLog lfLog = new LFLog(log);
            logMap.put(clazz.getName(), lfLog);
            return lfLog;
        }
    }

    public static void setRequestId(String requestId){
        requestIdTL.set(requestId);
    }

    public static String getRequestId(){
        return requestIdTL.get();
    }

    public static void removeRequestId(){
        requestIdTL.remove();
    }
}
