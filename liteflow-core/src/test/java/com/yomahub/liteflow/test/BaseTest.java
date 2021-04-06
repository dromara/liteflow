package com.yomahub.liteflow.test;

import com.yomahub.liteflow.spring.ComponentScanner;
import org.junit.AfterClass;

public class BaseTest {

    @AfterClass
    public static void cleanScanCache(){
        ComponentScanner.cleanCache();
    }
}
