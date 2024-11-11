package com.yomahub.liteflow.flow.instanceId;

import java.util.List;

/**
 * @author Jay li
 */
public interface InstanceIdGeneratorSpi {
    // 拿文件保存路径， 不同插件不同实现
    // 读取文件内容 核对数据
    List<String> readInstanceIdFile(String chainId);

    // 写入文件保存
    void writeInstanceIdFile(List<String> instanceIdList,String chainId);

    // 根据实例id获取 节点实例定位
    String getNodeInstanceId(String chainId, String instanceId);
}
