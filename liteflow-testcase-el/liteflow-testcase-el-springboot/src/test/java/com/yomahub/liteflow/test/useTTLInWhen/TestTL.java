package com.yomahub.liteflow.test.useTTLInWhen;

import com.alibaba.ttl.TransmittableThreadLocal;

public class TestTL {

    public static ThreadLocal<String> tl = new TransmittableThreadLocal<>();

    public static String get(){
        return tl.get();
    }

    public static void set(String value){
        tl.set(value);
    }
}
