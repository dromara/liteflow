package com.yomahub.liteflow.flow.instanceId;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.*;

import static com.yomahub.liteflow.common.ChainConstant.NODE_INSTANCE_PATH;
import static com.yomahub.liteflow.common.ChainConstant.USER_DIR;

/**
 * @author Jay li
 * @since 2.13.0
 */
public class DefaultNodeInstanceIdManageSpiImpl extends BaseNodeInstanceIdManageSpi {

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

}
