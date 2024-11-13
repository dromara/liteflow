package com.yomahub.liteflow.flow.instanceId;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.yomahub.liteflow.util.JsonUtil.parseObject;

/**
 * @author Jay li
 * @since 2.12.4
 */
public interface InstanceIdGeneratorSpi {
    // 拿文件保存路径， 不同插件不同实现
    // 读取文件内容 核对数据
    List<String> readInstanceIdFile(String chainId);

    // 写入文件保存
    void writeInstanceIdFile(List<String> instanceIdList,String chainId);

    // 根据实例id获取 节点实例定位
    default String getNodeInstanceId(String chainId, String instanceId) {
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
}
