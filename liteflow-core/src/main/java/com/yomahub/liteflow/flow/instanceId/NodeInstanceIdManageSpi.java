package com.yomahub.liteflow.flow.instanceId;

import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.entity.InstanceInfoDto;

import java.util.List;


/**
 * @author Jay li
 * @since 2.13.0
 */
public interface NodeInstanceIdManageSpi {

    // 拿文件保存路径， 不同插件不同实现
    // 读取文件内容 核对数据
    List<String> readInstanceIdFile(String chainId);

    // 写入文件保存
    void writeInstanceIdFile(List<InstanceInfoDto> instanceIdList, String elMd5, String chainId);

    // 根据实例id获取 节点实例定位
    int getNodeLocationById(String chainId, String instanceId);

    // 根据chainId instanceId返回Node节点信息
    Node getNodeByIdAndInstanceId(String chainId, String instanceId);

    // 根据nodeId和index返回Node节点信息
    Node getNodeByIdAndIndex(String chainId, String nodeId, Integer index);

    // 根据nodeId返回instanceId list
    List<String> getNodeInstanceIds(String chainId, String nodeId);

    // 设置node节点的实例id
    void setNodesInstanceId(Condition condition, Chain chain);

}