package com.yomahub.liteflow.flow.instanceId;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.digest.MD5;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.entity.InstanceIdDto;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static com.yomahub.liteflow.util.JsonUtil.*;
import static com.yomahub.liteflow.util.SerialsUtil.generateShortUUID;

/**
 * @author lhh
 * @since 2.13.0
 */
public abstract class BaseNodeInstanceIdManageSpi implements NodeInstanceIdManageSpi {

    // 根据实例id获取 节点实例定位
    @Override
    public String getNodeInstanceLocationById(String chainId, String instanceId) {
        if (StringUtils.isBlank(chainId) || StringUtils.isBlank(instanceId)) {
            return "";
        }

        List<String> instanceIdFile = readInstanceIdFile(chainId);

        for (int i = 1; i < instanceIdFile.size(); i++) {
            List<InstanceIdDto> instanceIdDtos = parseList(instanceIdFile.get(i), InstanceIdDto.class);
            if (instanceIdDtos == null) {
                continue;
            }

            for (InstanceIdDto dto : instanceIdDtos) {
                if (Objects.equals(dto.getInstanceId(), instanceId)) {
                    return dto.getNodeId() + "(" + dto.getIndex() + ")";
                }
            }
        }

        return "";
    }

    // 往condition里设置instanceId
    @Override
    public void setNodesInstanceId(Condition condition, Chain chain) {
        NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();

        String elMd5 = MD5.create().digestHex(chain.getEl());
        List<String> instanceIdFile = nodeInstanceIdManageSpi.readInstanceIdFile(chain.getChainId());

        // 如果文件不存在，或者文件内容不是当前el，则写入
        if (CollUtil.isEmpty(instanceIdFile) || !instanceIdFile.get(0).equals(elMd5)) {
            nodeInstanceIdManageSpi.writeInstanceIdFile(writeNodeInstanceId(condition), elMd5, chain.getChainId());
        } else {
            // 文件存在，则直接读取
            List<InstanceIdDto> instanceIdDtos = new ArrayList<>();
            for (int i = 1; i < instanceIdFile.size(); i++) {
                List<InstanceIdDto> instanceIdDtos1 = parseList(instanceIdFile.get(i), InstanceIdDto.class);
                if (instanceIdDtos1 != null) {
                    instanceIdDtos.addAll(instanceIdDtos1);
                }
            }

            condition.getExecutableGroup().forEach((key, executables) -> {
                Map<String, Integer> idCntMap = new HashMap<>();
                executables.forEach(executable -> {
                    if (executable instanceof Node) {
                        Node node = (Node) executable;
                        idCntMap.put(node.getId(), idCntMap.getOrDefault(node.getId(), -1) + 1);

                        for (InstanceIdDto dto : instanceIdDtos) {
                            if (Objects.equals(dto.getNodeId(), node.getId())
                                    && Objects.equals(dto.getChainId(), node.getCurrChainId())
                                    && Objects.equals(dto.getIndex(), idCntMap.get(node.getId()))) {
                                node.setInstanceId(dto.getInstanceId());
                                break;
                            }
                        }

                    }
                });
            });
        }
    }

    // 写入时第一行为el的md5，第二行为json格式的groupKey和对应的nodeId 和实例id
    //          instanceId  a_XXX_0
    //         {"chainId":"chain1","nodeId":"a","instanceId":"XXXX","index":0},
    private List<InstanceIdDto> writeNodeInstanceId(Condition condition) {
        ArrayList<InstanceIdDto> instanceIdDtos = new ArrayList<>();

        condition.getExecutableGroup().forEach((key, executables) -> {
            Map<String, Integer> idCntMap = new HashMap<>();
            executables.forEach(executable -> {
                if (executable instanceof Node) {
                    Node node = (Node) executable;
                    InstanceIdDto instanceIdDto = new InstanceIdDto();

                    instanceIdDto.setChainId(node.getCurrChainId());
                    instanceIdDto.setNodeId(node.getId());

                    String shortUUID = generateShortUUID();

                    idCntMap.put(node.getId(), idCntMap.getOrDefault(node.getId(), -1) + 1);

                    String instanceId = node.getId() + "_" + shortUUID + "_" + idCntMap.get(node.getId());

                    node.setInstanceId(instanceId);
                    instanceIdDto.setInstanceId(instanceId);
                    instanceIdDto.setIndex(idCntMap.get(node.getId()));

                    instanceIdDtos.add(instanceIdDto);
                }
            });
        });

        return instanceIdDtos;
    }

}
