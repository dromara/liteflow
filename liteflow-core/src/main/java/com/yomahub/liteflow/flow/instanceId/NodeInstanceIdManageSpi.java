package com.yomahub.liteflow.flow.instanceId;

import cn.hutool.core.util.ObjectUtil;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author Jay li
 * @since 2.13.0
 */
public class NodeInstanceIdManageSpi {
    private InstanceIdGeneratorSpi instanceIdGenerator;

    private static final NodeInstanceIdManageSpi INSTANCE = new NodeInstanceIdManageSpi();

    public static void init() {
        ServiceLoader<InstanceIdGeneratorSpi> loader = ServiceLoader.load(InstanceIdGeneratorSpi.class);
        Iterator<InstanceIdGeneratorSpi> iterator = loader.iterator();
        if (iterator.hasNext()) {
            INSTANCE.setInstanceIdGenerator(iterator.next());
        } else {
            INSTANCE.setInstanceIdGenerator(new DefaultInstanceIdGeneratorSpi());
        }
    }

    public static NodeInstanceIdManageSpi getInstance() {
        return INSTANCE;
    }

    public InstanceIdGeneratorSpi getInstanceIdGenerator() {
        if (ObjectUtil.isNull(instanceIdGenerator)) {
            init();
        }
        return instanceIdGenerator;
    }

    public void setInstanceIdGenerator(InstanceIdGeneratorSpi instanceIdGenerator) {
        this.instanceIdGenerator = instanceIdGenerator;
    }

}
