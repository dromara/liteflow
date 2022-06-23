package com.yomahub.liteflow.flow.id;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.RequestIdGeneratorException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import java.util.Objects;

/**
 * Id 生成器帮助器
 *
 * @author tangkc
 */
public class IdGeneratorHelper {

    private static RequestIdGenerator requestIdGenerator;

    private volatile static IdGeneratorHelper INSTANCE;

    private IdGeneratorHelper() {

    }

    public static IdGeneratorHelper getInstance() {
        if (Objects.isNull(INSTANCE)) {
            synchronized (IdGeneratorHelper.class) {
                if (Objects.isNull(INSTANCE)) {
                    INSTANCE = new IdGeneratorHelper();
                    LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
                    String requestIdGeneratorClass = liteflowConfig.getRequestIdGeneratorClass();
                    if (StrUtil.isBlank(requestIdGeneratorClass)) {
                        requestIdGenerator = new DefaultRequestIdGenerator();
                    }
                    try {
                        Class<RequestIdGenerator> idGenerateClass = (Class<RequestIdGenerator>) Class.forName(requestIdGeneratorClass);
                        requestIdGenerator = ContextAwareHolder.loadContextAware().registerBean(idGenerateClass);
                    } catch (Exception e) {
                        throw new RequestIdGeneratorException(e.getMessage());
                    }
                }
            }
        }

        return INSTANCE;
    }

    public String generate() {
        return requestIdGenerator.generate();
    }

    public void clear(){
        INSTANCE = null;
    }

}
