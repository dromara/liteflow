package com.yomahub.liteflow.flow.instanceId;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.digest.MD5;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.entity.InstanceInfoDto;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static com.yomahub.liteflow.util.JsonUtil.*;
import static com.yomahub.liteflow.util.SerialsUtil.generateShortUUID;

/**
 * @author lhh
 * @since 2.13.0
 */
public abstract class BaseNodeInstanceIdManageSpi implements NodeInstanceIdManageSpi {


    /**
     * 根据chainId instanceId返回Node节点信息
     */
    @Override
    public Node getNodeByIdAndInstanceId(String chainId, String instanceId) {
        if (StringUtils.isBlank(chainId) || StringUtils.isBlank(instanceId)) {
            return null;
        }
        Chain chain = FlowBus.getChain(chainId);
        if (chain == null) {
            return null;
        }

        List<Condition> conditionList = chain.getConditionList();

        return getNodeFromConditions(conditionList, instanceId);
    }


    /**
     * 根据nodeId和index返回Node节点信息
     */
    @Override
    public Node getNodeByIdAndIndex(String chainId, String nodeId, Integer index) {
        if (StringUtils.isBlank(chainId) || index == null) {
            return null;
        }
        Chain chain = FlowBus.getChain(chainId);
        if (chain == null) {
            return null;
        }

        List<Condition> conditionList = chain.getConditionList();

        return getNodeFromConditions(conditionList, nodeId, index, new HashMap<>());
    }


    /**
     * 根据nodeId返回instanceId list
     */
    @Override
    public List<String> getNodeInstanceIds(String chainId, String nodeId) {
        if (StringUtils.isBlank(chainId) || StringUtils.isBlank(nodeId)) {
            return Collections.emptyList();
        }
        // 第一行为elMd5 第二行为实例id json格式信息
        List<String> instanceIdFile = readInstanceIdFile(chainId);

        List<String> instanceIds = new ArrayList<>();
        for (int i = 1; i < instanceIdFile.size(); i++) {
            List<InstanceInfoDto> instanceInfos = parseList(instanceIdFile.get(i), InstanceInfoDto.class);

            for (InstanceInfoDto dto : instanceInfos) {
                if (Objects.equals(dto.getInstanceId(), nodeId)) {
                    instanceIds.add(dto.getInstanceId());
                }
            }
        }

        return instanceIds;
    }


    /**
     * 从conditions 根据instanceId获取node节点
     */
    private Node getNodeFromConditions(List<Condition> conditionList, String instanceId) {
        if (CollUtil.isEmpty(conditionList)) {
            return null;
        }

        for (Condition condition : conditionList) {
            List<Executable> executableList = condition.getExecutableList();
            for (Executable executable : executableList) {
                if (executable instanceof Node) {
                    Node node = (Node) executable;
                    if (Objects.equals(node.getInstanceId(), instanceId)) {
                        return node;
                    }
                } else if (executable instanceof Condition) {
                    Condition conditionTmp = (Condition) executable;
                    List<Node> allNodeInCondition = conditionTmp.getAllNodeInCondition();

                    for (Node node : allNodeInCondition) {
                        if (Objects.equals(node.getInstanceId(), instanceId)) {
                            return node;
                        }
                    }
                } else if (executable instanceof Chain) {
                    Chain chainTmp = (Chain) executable;
                    return getNodeFromConditions(chainTmp.getConditionList(), instanceId);
                }
            }
        }
        return null;
    }

    /**
     * 根据nodeId和index获取node节点
     */
    private Node getNodeFromConditions(List<Condition> conditionList, String nodeId,
                                       Integer index, HashMap<String, Integer> idCntMap) {
        if (CollUtil.isEmpty(conditionList)) {
            return null;
        }

        for (Condition condition : conditionList) {
            List<Executable> executableList = condition.getExecutableList();
            for (Executable executable : executableList) {
                if (executable instanceof Node) {
                    Node node = (Node) executable;
                    idCntMap.put(node.getId(), idCntMap.getOrDefault(node.getId(), -1) + 1);
                    if (Objects.equals(node.getId(), nodeId) && Objects.equals(idCntMap.get(node.getId()), index)) {
                        return node;
                    }
                } else if (executable instanceof Condition) {
                    Condition conditionTmp = (Condition) executable;
                    List<Node> allNodeInCondition = conditionTmp.getAllNodeInCondition();

                    for (Node node : allNodeInCondition) {
                        idCntMap.put(node.getId(), idCntMap.getOrDefault(node.getId(), -1) + 1);
                        if (Objects.equals(node.getId(), nodeId) && Objects.equals(idCntMap.get(node.getId()), index)) {
                            return node;
                        }
                    }
                } else if (executable instanceof Chain) {
                    Chain chainTmp = (Chain) executable;
                    return getNodeFromConditions(chainTmp.getConditionList(), nodeId, index, new HashMap<>());
                }
            }
        }
        return null;
    }


    /**
     * 根据实例id获取 节点实例定位
     */
    @Override
    public String getNodeLocationById(String chainId, String instanceId) {
        if (StringUtils.isBlank(chainId) || StringUtils.isBlank(instanceId)) {
            return "";
        }
        // 第一行为elMd5 第二行为实例id json格式信息
        List<String> instanceIdFile = readInstanceIdFile(chainId);

        for (int i = 1; i < instanceIdFile.size(); i++) {
            List<InstanceInfoDto> instanceInfos = parseList(instanceIdFile.get(i), InstanceInfoDto.class);

            for (InstanceInfoDto dto : instanceInfos) {
                if (Objects.equals(dto.getInstanceId(), instanceId)) {
                    return dto.getNodeId() + "(" + dto.getIndex() + ")";
                }
            }
        }

        return "";
    }

    /**
     * 往condition里设置instanceId
     */
    @Override
    public void setNodesInstanceId(Condition condition, Chain chain) {
        NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();

        String elMd5 = MD5.create().digestHex(chain.getEl());
        String chainId = chain.getChainId();
        List<String> instanceIdFile = nodeInstanceIdManageSpi.readInstanceIdFile(chainId);

        // 如果文件不存在，或者文件内容不是当前el，则写入
        if (CollUtil.isEmpty(instanceIdFile) || !instanceIdFile.get(0).equals(elMd5)) {
            nodeInstanceIdManageSpi.writeInstanceIdFile(writeNodeInstanceId(condition, chainId), elMd5, chainId);
        } else {
            // 文件存在，则直接读取
            List<InstanceInfoDto> instanceInfos = new ArrayList<>();
            for (int i = 1; i < instanceIdFile.size(); i++) {
                instanceInfos = parseList(instanceIdFile.get(i), InstanceInfoDto.class);
            }
            List<InstanceInfoDto> finalInstanceInfos = instanceInfos;
            Map<String, Integer> idCntMap = new HashMap<>();

            setInstanceIdFromFile(finalInstanceInfos, chainId, condition.getExecutableGroup(), idCntMap);
        }
    }

    /**
     * 从instanceIdFile里设置instanceId
     */
    private void setInstanceIdFromFile(List<InstanceInfoDto> finalInstanceInfos, String chainId,
                                       Map<String, List<Executable>> executableGroup, Map<String, Integer> idCntMap) {
        if (CollUtil.isEmpty(executableGroup)) {
            return;
        }

        executableGroup.forEach((key, executables) -> {
            executables.forEach(executable -> {
                if (executable instanceof Node) {
                    Node node = (Node) executable;
                    idCntMap.put(node.getId(), idCntMap.getOrDefault(node.getId(), -1) + 1);

                    for (InstanceInfoDto dto : finalInstanceInfos) {
                        if (Objects.equals(dto.getNodeId(), node.getId())
                                && Objects.equals(dto.getChainId(), chainId)
                                && Objects.equals(dto.getIndex(), idCntMap.get(node.getId()))) {
                            node.setInstanceId(dto.getInstanceId());
                            break;
                        }
                    }
                } else if (executable instanceof Condition) {
                    Condition conditionTmp = (Condition) executable;
                    setInstanceIdFromFile(finalInstanceInfos, chainId, conditionTmp.getExecutableGroup(), idCntMap);
                } else if (executable instanceof Chain) {
                    Chain chainTmp = (Chain) executable;
                    List<Condition> conditionList = chainTmp.getConditionList();
                    conditionList.forEach(condition ->
                            setInstanceIdFromFile(finalInstanceInfos, chainId, condition.getExecutableGroup(), idCntMap));
                }
            });
        });
    }

    /**
     * 写入时第一行为el的md5，第二行为json格式的groupKey和对应的nodeId 和实例id
     * instanceId  a_XXX_0
     * {"chainId":"chain1","nodeId":"a","instanceId":"XXXX","index":0},
     */
    private List<InstanceInfoDto> writeNodeInstanceId(Condition condition, String chainId) {
        ArrayList<InstanceInfoDto> instanceInfos = new ArrayList<>();

        addInstanceIdFromExecutableGroup(instanceInfos, condition.getExecutableGroup(), chainId, new HashMap<>());

        return instanceInfos;
    }

    // 往instanceInfos里添加实例id
    private void addInstanceIdFromExecutableGroup(List<InstanceInfoDto> instanceInfos, Map<String, List<Executable>> executableGroup,
                                                  String chainId, Map<String, Integer> idCntMap) {
        if (CollUtil.isEmpty(executableGroup)) {
            return;
        }
        executableGroup.forEach((key, executables) -> {
            executables.forEach(executable -> {
                if (executable instanceof Node) {
                    Node node = (Node) executable;
                    InstanceInfoDto instanceInfoDto = new InstanceInfoDto();

                    instanceInfoDto.setChainId(chainId);
                    instanceInfoDto.setNodeId(node.getId());

                    String shortUUID = generateShortUUID();

                    idCntMap.put(node.getId(), idCntMap.getOrDefault(node.getId(), -1) + 1);

                    String instanceId = node.getId() + "_" + shortUUID + "_" + idCntMap.get(node.getId());

                    node.setInstanceId(instanceId);
                    instanceInfoDto.setInstanceId(instanceId);
                    instanceInfoDto.setIndex(idCntMap.get(node.getId()));

                    instanceInfos.add(instanceInfoDto);
                } else if (executable instanceof Condition) {
                    Condition conditionTmp = (Condition) executable;
                    addInstanceIdFromExecutableGroup(instanceInfos, conditionTmp.getExecutableGroup(), chainId, idCntMap);
                } else if (executable instanceof Chain) {
                    Chain chainTmp = (Chain) executable;
                    List<Condition> conditionList = chainTmp.getConditionList();
                    conditionList.forEach(condition -> addInstanceIdFromExecutableGroup(instanceInfos, condition.getExecutableGroup(), chainId, idCntMap));
                }
            });
        });
    }

}
