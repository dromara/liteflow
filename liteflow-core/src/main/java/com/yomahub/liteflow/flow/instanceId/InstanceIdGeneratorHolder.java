package com.yomahub.liteflow.flow.instanceId;

import cn.hutool.core.util.ObjectUtil;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author Jay li
 */
public class InstanceIdGeneratorHolder {
    private InstanceIdGeneratorSpi instanceIdGenerator;

    private static final InstanceIdGeneratorHolder INSTANCE = new InstanceIdGeneratorHolder();

    public static void init() {
        ServiceLoader<InstanceIdGeneratorSpi> loader = ServiceLoader.load(InstanceIdGeneratorSpi.class);
        Iterator<InstanceIdGeneratorSpi> iterator = loader.iterator();
        if (iterator.hasNext()) {
            INSTANCE.setInstanceIdGenerator(iterator.next());

        } else {
            INSTANCE.setInstanceIdGenerator(new DefaultInstanceIdGeneratorSpi());
        }
    }

    public static InstanceIdGeneratorHolder getInstance() {

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
