package com.yomahub.liteflow.parser.factory;

import com.yomahub.liteflow.parser.base.BaseJsonFlowParser;
import com.yomahub.liteflow.parser.base.BaseXmlFlowParser;
import com.yomahub.liteflow.parser.base.BaseYmlFlowParser;
import com.yomahub.liteflow.parser.el.JsonFlowELParser;
import com.yomahub.liteflow.parser.el.XmlFlowELParser;
import com.yomahub.liteflow.parser.el.YmlFlowELParser;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

/**
 * Class文件
 * <p>
 *
 * @author junjun
 */
public class ClassParserFactory implements FlowParserFactory {

	@Override
	public BaseJsonFlowParser createJsonELParser(String path) {
		Class<?> c = forName(path);
		return (JsonFlowELParser) ContextAwareHolder.loadContextAware().registerBean(c);
	}

	@Override
	public BaseXmlFlowParser createXmlELParser(String path) {
		Class<?> c = forName(path);
		return (XmlFlowELParser) ContextAwareHolder.loadContextAware().registerBean(c);
	}

	@Override
	public BaseYmlFlowParser createYmlELParser(String path) {
		Class<?> c = forName(path);
		return (YmlFlowELParser) ContextAwareHolder.loadContextAware().registerBean(c);
	}

	private Class<?> forName(String path) {
		try {
			return Class.forName(path);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
