package com.yomahub.liteflow.property;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.util.SpringAware;

/**
 * liteflow的配置获取器
 */
public class LiteflowConfigGetter {

    public static LiteflowConfig get(){
        LiteflowConfig liteflowConfig = SpringAware.getBean(LiteflowConfig.class);
        //这里liteflowConfig不可能为null
        //如果在springboot环境，由于自动装配，所以不可能为null
        //在spring环境，如果xml没配置，在FlowExecutor的init时候就已经报错了
        //只有在非spring环境下，是为null
        if (ObjectUtil.isNull(liteflowConfig)){
            liteflowConfig = new LiteflowConfig();
        }
        return liteflowConfig;
    }
}
