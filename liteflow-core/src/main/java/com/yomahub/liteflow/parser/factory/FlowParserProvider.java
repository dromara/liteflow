package com.yomahub.liteflow.parser.factory;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ErrorSupportPathException;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.base.FlowParser;
import com.yomahub.liteflow.parser.el.ClassJsonFlowELParser;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.el.ClassYmlFlowELParser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.yomahub.liteflow.parser.factory.FlowParserProvider.ConfigRegexConstant.CLASS_CONFIG_REGEX;
import static com.yomahub.liteflow.parser.factory.FlowParserProvider.ConfigRegexConstant.LOCAL_EL_JSON_CONFIG_REGEX;
import static com.yomahub.liteflow.parser.factory.FlowParserProvider.ConfigRegexConstant.LOCAL_EL_XML_CONFIG_REGEX;
import static com.yomahub.liteflow.parser.factory.FlowParserProvider.ConfigRegexConstant.LOCAL_EL_YML_CONFIG_REGEX;
import static com.yomahub.liteflow.parser.factory.FlowParserProvider.ConfigRegexConstant.LOCAL_JSON_CONFIG_REGEX;
import static com.yomahub.liteflow.parser.factory.FlowParserProvider.ConfigRegexConstant.LOCAL_XML_CONFIG_REGEX;
import static com.yomahub.liteflow.parser.factory.FlowParserProvider.ConfigRegexConstant.LOCAL_YML_CONFIG_REGEX;
import static com.yomahub.liteflow.parser.factory.FlowParserProvider.ConfigRegexConstant.PREFIX_FORMAT_CONFIG_REGEX;

/**
 * 解析器提供者
 * <p>
 *
 * @author junjun
 */
public class FlowParserProvider {

	private static final LFLog LOG = LFLoggerManager.getLogger(FlowParserProvider.class);

	private static final FlowParserFactory LOCAL_PARSER_FACTORY = new LocalParserFactory();

	/**
	 * 使用 map 枚举不同类型的 Parser，用于解耦如下的 if 判断 <pre>
	 * if (ReUtil.isMatch(LOCAL_XML_CONFIG_REGEX, path)) {
	 * 	return factory.createXmlParser(path);
	 * }
	 * </pre>
	 */
	private static final Map<Predicate<String>, Function<String, FlowParser>> LOCAL_PARSER_DICT = new HashMap<Predicate<String>, Function<String, FlowParser>>() {
		{
			put(path -> ReUtil.isMatch(LOCAL_XML_CONFIG_REGEX, path), LOCAL_PARSER_FACTORY::createXmlELParser);
			put(path -> ReUtil.isMatch(LOCAL_JSON_CONFIG_REGEX, path), LOCAL_PARSER_FACTORY::createJsonELParser);
			put(path -> ReUtil.isMatch(LOCAL_YML_CONFIG_REGEX, path), LOCAL_PARSER_FACTORY::createYmlELParser);
			put(path -> ReUtil.isMatch(LOCAL_EL_XML_CONFIG_REGEX, path), LOCAL_PARSER_FACTORY::createXmlELParser);
			put(path -> ReUtil.isMatch(LOCAL_EL_JSON_CONFIG_REGEX, path), LOCAL_PARSER_FACTORY::createJsonELParser);
			put(path -> ReUtil.isMatch(LOCAL_EL_YML_CONFIG_REGEX, path), LOCAL_PARSER_FACTORY::createYmlELParser);
		}
	};

	private static final FlowParserFactory CLASS_PARSER_FACTORY = new ClassParserFactory();

	/**
	 * 使用 map 枚举不同类型的 Parser，用于解耦如下的 if 判断 <pre>
	 * if (ClassXmlFlowParser.class.isAssignableFrom(clazz)) {
	 *   return factory.createXmlParser(className);
	 * }
	 * </pre>
	 */
	private static final Map<Predicate<Class<?>>, Function<String, FlowParser>> CLASS_PARSER_DICT = new HashMap<Predicate<Class<?>>, Function<String, FlowParser>>() {
		{
			put(ClassXmlFlowELParser.class::isAssignableFrom, CLASS_PARSER_FACTORY::createXmlELParser);
			put(ClassJsonFlowELParser.class::isAssignableFrom, CLASS_PARSER_FACTORY::createJsonELParser);
			put(ClassYmlFlowELParser.class::isAssignableFrom, CLASS_PARSER_FACTORY::createYmlELParser);
		}
	};

	/**
	 * 根据配置的地址找到对应的解析器
	 */
	public static FlowParser lookup(String path) throws Exception {
		// 自定义类必须实现以上实现类，否则报错
		String errorMsg = StrUtil.format("can't support the format {}", path);

		// 本地文件
		if (isLocalConfig(path)) {
			// 遍历枚举 map 找到对应 factory
			Predicate<String> dictKey = LOCAL_PARSER_DICT.keySet()
				.stream()
				.filter(key -> key.test(path))
				.findFirst()
				.orElseThrow(() -> new ErrorSupportPathException(errorMsg));

			LOG.info("flow info loaded from local file,path={}", path);
			return LOCAL_PARSER_DICT.get(dictKey).apply(path);
		}
		// 自定义 class 配置
		else if (isClassConfig(path)) {
			// 获取最终的className，因为有些可能className前面带了文件类型的标识，比如json:x.x.x.x
			String className = ReUtil.replaceAll(path, PREFIX_FORMAT_CONFIG_REGEX, "");
			Class<?> clazz = Class.forName(className);

			// 遍历枚举 map 找到对应 factory
			Predicate<Class<?>> dictKey = CLASS_PARSER_DICT.keySet()
				.stream()
				.filter(key -> key.test(clazz))
				.findFirst()
				.orElseThrow(() -> new ErrorSupportPathException(errorMsg));

			LOG.info("flow info loaded from class config with el,class={}", className);
			return CLASS_PARSER_DICT.get(dictKey).apply(className);
		}

		// not found
		throw new ErrorSupportPathException(errorMsg);
	}

	/**
	 * 判定是否为本地文件
	 */
	private static boolean isLocalConfig(String path) {
		return ReUtil.isMatch(LOCAL_XML_CONFIG_REGEX, path) || ReUtil.isMatch(LOCAL_JSON_CONFIG_REGEX, path)
				|| ReUtil.isMatch(LOCAL_YML_CONFIG_REGEX, path) || ReUtil.isMatch(LOCAL_EL_XML_CONFIG_REGEX, path)
				|| ReUtil.isMatch(LOCAL_EL_JSON_CONFIG_REGEX, path) || ReUtil.isMatch(LOCAL_EL_YML_CONFIG_REGEX, path);
	}

	/**
	 * 判定是否为自定义class配置
	 */
	private static boolean isClassConfig(String path) {
		return ReUtil.isMatch(CLASS_CONFIG_REGEX, path);
	}

	/**
	 * 统一管理类的常量
	 */
	protected static class ConfigRegexConstant {

		public static final String LOCAL_XML_CONFIG_REGEX = "^[\\w\\:\\-\\.\\@\\/\\\\\\*]+\\.xml$";

		public static final String LOCAL_JSON_CONFIG_REGEX = "^[\\w\\:\\-\\.\\@\\/\\\\\\*]+\\.json$";

		public static final String LOCAL_YML_CONFIG_REGEX = "^[\\w\\:\\-\\.\\@\\/\\\\\\*]+\\.yml$";

		public static final String LOCAL_EL_XML_CONFIG_REGEX = "^[\\w\\:\\-\\.\\@\\/\\\\\\*]+\\.el\\.xml$";

		public static final String LOCAL_EL_JSON_CONFIG_REGEX = "^[\\w\\:\\-\\.\\@\\/\\\\\\*]+\\.el\\.json$";

		public static final String LOCAL_EL_YML_CONFIG_REGEX = "^[\\w\\:\\-\\.\\@\\/\\\\\\*]+\\.el\\.yml$";

		public static final String PREFIX_FORMAT_CONFIG_REGEX = "xml:|json:|yml:|el_xml:|el_json:|el_yml:";

		public static final String CLASS_CONFIG_REGEX = "^(xml:|json:|yml:|el_xml:|el_json:|el_yml:)?\\w+(\\.\\w+)*$";

	}

}
