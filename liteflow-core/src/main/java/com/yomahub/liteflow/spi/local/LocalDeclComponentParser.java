package com.yomahub.liteflow.spi.local;

import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.exception.NotSupportDeclException;
import com.yomahub.liteflow.spi.DeclComponentParser;

import java.util.List;

/**
 * 声明式组件解析器非spring环境实现类
 * 非spring环境不支持声明式组件
 *
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public class LocalDeclComponentParser implements DeclComponentParser {
    @Override
    public List<DeclWarpBean> parseDeclBean(Class<?> clazz) {
        throw new NotSupportDeclException("the declaration component is not supported in non-spring environment.");
    }

    @Override
    public List<DeclWarpBean> parseDeclBean(Class<?> clazz, String nodeId, String nodeName) {
        throw new NotSupportDeclException("the declaration component is not supported in non-spring environment.");
    }

    @Override
    public int priority() {
        return 2;
    }
}
