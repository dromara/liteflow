package com.yomahub.liteflow.spi.solon;

import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.exception.NotSupportDeclException;
import com.yomahub.liteflow.spi.DeclComponentParser;

import java.util.List;

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
