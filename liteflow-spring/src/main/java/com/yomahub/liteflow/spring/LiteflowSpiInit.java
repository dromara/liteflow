package com.yomahub.liteflow.spring;

import com.yomahub.liteflow.spi.holder.*;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * 初始化 SPI ,避免多线程场景下类加载器不同导致的加载不到 SPI 实现类
 *
 * @author gaibu
 */
public class LiteflowSpiInit implements SmartInitializingSingleton {

    @Override
    public void afterSingletonsInstantiated() {
        SpiFactoryInitializing.loadInit();
    }

}
