package com.yomahub.liteflow.spi.factory;

public class SpiFactoryCleaner {

    public static void clean(){
        CmpAroundAspectFactory.clean();
        ContextAwareFactory.clean();
        ContextCmpInitFactory.clean();
        LiteflowComponentSupportFactory.clean();
        PathContentParserFactory.clean();
    }
}
