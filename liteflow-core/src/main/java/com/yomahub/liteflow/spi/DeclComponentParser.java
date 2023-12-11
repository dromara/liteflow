package com.yomahub.liteflow.spi;

import com.yomahub.liteflow.core.proxy.DeclWarpBean;

import java.util.List;

/**
 * 声明式组件解析器接口
 *
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public interface DeclComponentParser extends SpiPriority {

    List<DeclWarpBean> parseDeclBean(Class<?> clazz);

    List<DeclWarpBean> parseDeclBean(Class<?> clazz, String nodeId, String nodeName);
}
