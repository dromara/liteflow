package com.yomahub.liteflow.parser.spi.instanceId;

import com.yomahub.liteflow.flow.instanceId.InstanceIdGeneratorSpi;

import java.util.List;

/**
 * sql方式生成instanceId
 * @author Jay li
 */

public class SqlInstanceIdGeneratorSpi implements InstanceIdGeneratorSpi {
    @Override
    public List<String> readInstanceIdFile(String chainId) {
        return null;
    }

    @Override
    public void writeInstanceIdFile(List<String> instanceIdList, String chainId) {

    }

    @Override
    public String getNodeInstanceId(String chainId, String instanceId) {
        return "";
    }
}
