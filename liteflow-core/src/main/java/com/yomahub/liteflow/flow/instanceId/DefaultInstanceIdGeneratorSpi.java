package com.yomahub.liteflow.flow.instanceId;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.*;

import static com.yomahub.liteflow.common.ChainConstant.NODE_INSTANCE_PATH;
import static com.yomahub.liteflow.common.ChainConstant.USER_DIR;
import static com.yomahub.liteflow.util.JsonUtil.parseObject;

/**
 * @author Jay li
 */
public class DefaultInstanceIdGeneratorSpi implements InstanceIdGeneratorSpi {

    private final String basePath = System.getProperty(USER_DIR) + File.separator + NODE_INSTANCE_PATH + File.separator;

    @Override
    public List<String> readInstanceIdFile(String chainId) {
        if (StringUtils.isBlank(chainId)) {
            return Collections.emptyList();
        }

        File nodeDir = new File(basePath + chainId);
        if (FileUtil.isEmpty(nodeDir)) {
            return Collections.emptyList();
        }
        return FileUtil.readLines(nodeDir.getPath(), CharsetUtil.UTF_8);
    }

    @Override
    public void writeInstanceIdFile(List<String> instanceIdList, String chainId) {
        if (StringUtils.isBlank(chainId) || CollUtil.isEmpty(instanceIdList)) {
            return;
        }
        File nodeDir = new File(basePath + chainId);

        FileUtil.writeLines(instanceIdList, nodeDir.getPath(), CharsetUtil.UTF_8);
    }

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
}
