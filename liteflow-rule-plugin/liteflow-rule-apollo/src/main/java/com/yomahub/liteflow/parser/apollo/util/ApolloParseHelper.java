package com.yomahub.liteflow.parser.apollo.util;

import cn.hutool.core.util.StrUtil;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.yomahub.liteflow.exception.ParseException;
import com.yomahub.liteflow.parser.apollo.exception.ApolloException;
import com.yomahub.liteflow.parser.apollo.vo.ApolloParserConfigVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @Description:
 * @Author: zhanghua
 * @Date: 2022/12/3 13:47
 */
public class ApolloParseHelper {


	private static final Logger LOG = LoggerFactory.getLogger(ApolloParseHelper.class);

	private final ApolloParserConfigVO apolloParserConfigVO;

	private ConfigFile liteflowConfigFile;

	public ApolloParseHelper(ApolloParserConfigVO apolloParserConfigVO) {
		this.apolloParserConfigVO = apolloParserConfigVO;
		try {
			liteflowConfigFile = ConfigService.getConfigFile(apolloParserConfigVO.getNamespace(), ConfigFileFormat.XML);
		} catch (Exception e) {
			LOG.error("[ApolloParseHelper] liteflowConfigFile get init error, apolloParserConfigVO:{}", apolloParserConfigVO);
			throw new ApolloException(e.getMessage());
		}
	}

	public String getContent() {
		try {
			ConfigFile configFile = ConfigService.getConfigFile(apolloParserConfigVO.getNamespace(), ConfigFileFormat.XML);
			String content;
			if (Objects.isNull(configFile) || StrUtil.isBlank(content = configFile.getContent())) {
				throw new ApolloException(String.format("not find config file, namespace:%s", apolloParserConfigVO.getNamespace()));
			}
			return content;
		} catch (Exception e) {
			throw new ApolloException(e.getMessage());
		}
	}

	/**
	 * 检查 content 是否合法
	 */
	public void checkContent(String content) {
		if (StrUtil.isBlank(content)) {
			String error = StrUtil.format("the node[{}] value is empty", apolloParserConfigVO.toString());
			throw new ParseException(error);
		}
	}

	/**
	 * 监听 apollo 数据变化
	 */
	public void listen(Consumer<String> parseConsumer) {
		try {
			liteflowConfigFile.addChangeListener(configFileChangeEvent -> {
				String newContext = configFileChangeEvent.getNewValue();
				parseConsumer.accept(newContext);
			});
		} catch (Exception e) {
			throw new ApolloException(e.getMessage());
		}
	}


}
