package com.yomahub.liteflow.spi.holder;

public class SpiFactoryInitializing {

	public static void clean() {
		CmpAroundAspectHolder.clean();
		ContextAwareHolder.clean();
		ContextCmpInitHolder.clean();
		DeclComponentParserHolder.clean();
		LiteflowComponentSupportHolder.clean();
		PathContentParserHolder.clean();
	}

	public static void loadInit(){
		CmpAroundAspectHolder.loadCmpAroundAspect();
		ContextAwareHolder.loadContextAware();
		ContextCmpInitHolder.loadContextCmpInit();
		DeclComponentParserHolder.loadDeclComponentParser();
		LiteflowComponentSupportHolder.loadLiteflowComponentSupport();
		PathContentParserHolder.loadContextAware();
	}

}
