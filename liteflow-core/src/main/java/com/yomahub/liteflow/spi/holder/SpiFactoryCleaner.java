package com.yomahub.liteflow.spi.holder;

public class SpiFactoryCleaner {

	public static void clean() {
		CmpAroundAspectHolder.clean();
		ContextAwareHolder.clean();
		ContextCmpInitHolder.clean();
		LiteflowComponentSupportHolder.clean();
		PathContentParserHolder.clean();
	}

}
