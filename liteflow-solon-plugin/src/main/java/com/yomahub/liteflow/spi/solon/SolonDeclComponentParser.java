package com.yomahub.liteflow.spi.solon;

import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.exception.NotSupportDeclException;
import com.yomahub.liteflow.spi.DeclComponentParser;

import java.util.List;

/**
 * Solon 环境声明式组件解析器实现（在 solon 里没有用上；机制不同）
 *
 * @author noear
 * */
public class SolonDeclComponentParser implements DeclComponentParser {
    @Override
    public List<DeclWarpBean> parseDeclBean(Class<?> clazz) {
        throw new NotSupportDeclException("the declaration component is not supported in solon environment.");
    }

    @Override
    public List<DeclWarpBean> parseDeclBean(Class<?> clazz, String nodeId, String nodeName) {
        throw new NotSupportDeclException("the declaration component is not supported in solon environment.");
    }

    @Override
    public int priority() {
        return 1;
    }
}
