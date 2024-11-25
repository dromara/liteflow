package com.yomahub.liteflow.flow.instanceId;

import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;

import java.util.List;


/**
 * @author Jay li
 * @since 2.13.0
 */
public interface InstanceIdGeneratorSpi {
    // 拿文件保存路径， 不同插件不同实现
    // 读取文件内容 核对数据
    List<String> readInstanceIdFile(String chainId);

    // 写入文件保存
    void writeInstanceIdFile(List<String> instanceIdList, String chainId);

    // 根据实例id获取 节点实例定位
    String getNodeInstanceId(String chainId, String instanceId);

    // 设置node节点的实例id
    void setNodesInstanceId(Condition condition, Chain chain);

}