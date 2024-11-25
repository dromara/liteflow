package com.yomahub.liteflow.flow.instanceId;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.digest.MD5;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Node;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.yomahub.liteflow.util.JsonUtil.parseObject;
import static com.yomahub.liteflow.util.JsonUtil.toJsonString;
import static com.yomahub.liteflow.util.SerialsUtil.generateShortUUID;

/**
 * @author lhh
 * @since 2.13.0
 */
public abstract class BaseInstanceIdGeneratorSpi implements InstanceIdGeneratorSpi {

    // 根据实例id获取 节点实例定位
    @Override
    public String getNodeInstanceId(String chainId, String instanceId) {
        if (StringUtils.isBlank(chainId) || StringUtils.isBlank(instanceId)) {
            return "";
        }

        List<String> instanceIdFile = readInstanceIdFile(chainId);

        for (int i = 1; i < instanceIdFile.size(); i++) {
            JsonNode groupKeyAndInstanceIds = parseObject(instanceIdFile.get(i));

            if (groupKeyAndInstanceIds == null) {
                continue;
            }

            Iterator<String> fieldNames = groupKeyAndInstanceIds.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                JsonNode valueNode = groupKeyAndInstanceIds.get(key);
                if (valueNode.isArray()) {
                    Map<String, Integer> map = new HashMap<>();
                    for (int j = 1; j < valueNode.size(); j+=2) {
                        String nodeId = valueNode.get(j - 1).asText();
                        map.put(nodeId, map.getOrDefault(nodeId, -1)+1);
                        if (instanceId.equals(valueNode.get(j).asText())) {
                            return nodeId + "(" + (map.get(nodeId)) + ")";
                        }
                    }
                }
            }
        }

        return "";
    }

    // 往condition里设置instanceId
    @Override
    public void setNodesInstanceId(Condition condition, Chain chain) {
        InstanceIdGeneratorSpi instanceIdGenerator = NodeInstanceIdManageSpi.getInstance().getInstanceIdGenerator();

        String elMd5 = MD5.create().digestHex(chain.getEl());
        List<String> instanceIdFile = instanceIdGenerator.readInstanceIdFile(chain.getChainId());

        // 如果文件不存在，或者文件内容不是当前el，则写入
        if (CollUtil.isEmpty(instanceIdFile) || !instanceIdFile.get(0).equals(elMd5)) {
            instanceIdGenerator.writeInstanceIdFile(writeNodeInstanceId(condition, elMd5), chain.getChainId());
        } else {
            // 文件存在，则直接读取
            Map<String, List<String>> executableMap = new HashMap<>();
            for (int i = 1; i < instanceIdFile.size(); i++) {
                JsonNode groupKeyAndInstanceIds = parseObject(instanceIdFile.get(i));
                if (groupKeyAndInstanceIds == null) {
                    continue;
                }

                Iterator<String> fieldNames = groupKeyAndInstanceIds.fieldNames();
                while (fieldNames.hasNext()) {
                    String key = fieldNames.next();
                    JsonNode valueNode = groupKeyAndInstanceIds.get(key);
                    if (valueNode.isArray()) {
                        List<String> valueList = new ArrayList<>();
                        for (int j = 1; j < valueNode.size(); j+=2) {
                            valueList.add(valueNode.get(j).asText());
                        }
                        executableMap.put(key, valueList);
                    }
                }
            }

            condition.getExecutableGroup().forEach((key, executables) -> {
                AtomicInteger index = new AtomicInteger(0);
                executables.forEach(executable -> {
                    if (executableMap.containsKey(key)) {
                        if (executable instanceof Node) {
                            ((Node) executable).setInstanceId((executableMap.get(key).get(index.getAndIncrement())));
                        }
                    }
                });
            });
        }
    }

    // 写入时第一行为el的md5，第二行为json格式的groupKey和对应的nodeId 和实例id
    private List<String> writeNodeInstanceId(Condition condition, String elMd5) {
        ArrayList<String> writeList = new ArrayList<>();
        writeList.add(elMd5);
        Map<String, List<String>> groupKeyAndInstanceIds = new HashMap<>();

        condition.getExecutableGroup().forEach((key, executables) -> {
            List<String> instanceIds = new ArrayList<>();
            executables.forEach(executable -> {
                if (executable instanceof Node) {
                    ((Node) executable).setInstanceId(generateShortUUID());
                    instanceIds.add(executable.getId());
                    instanceIds.add(((Node) executable).getInstanceId());
                }
            });
            groupKeyAndInstanceIds.put(key, instanceIds);
        });
        writeList.add(toJsonString(groupKeyAndInstanceIds));

        return writeList;
    }

}
