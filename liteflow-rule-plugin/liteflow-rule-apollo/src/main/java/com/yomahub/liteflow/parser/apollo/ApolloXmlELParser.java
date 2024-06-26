package com.yomahub.liteflow.parser.apollo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.parser.apollo.exception.ApolloException;
import com.yomahub.liteflow.parser.apollo.util.ApolloParseHelper;
import com.yomahub.liteflow.parser.apollo.vo.ApolloParserConfigVO;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Objects;

/**
 * @author zhanghua
 * @since 2.9.5
 */
public class ApolloXmlELParser extends ClassXmlFlowELParser {

	private final ApolloParseHelper apolloParseHelper;

	public ApolloXmlELParser() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		try {
			ApolloParserConfigVO apolloParserConfigVO = null;
			if (MapUtil.isNotEmpty((liteflowConfig.getRuleSourceExtDataMap()))) {
				apolloParserConfigVO = BeanUtil.toBean(liteflowConfig.getRuleSourceExtDataMap(),
						ApolloParserConfigVO.class, CopyOptions.create());
			}
			else if (StrUtil.isNotBlank(liteflowConfig.getRuleSourceExtData())) {
				apolloParserConfigVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(),
						ApolloParserConfigVO.class);
			}

			// check config
			if (Objects.isNull(apolloParserConfigVO)) {
				throw new ApolloException("ruleSourceExtData or map is empty");
			}

			if (StrUtil.isBlank(apolloParserConfigVO.getChainNamespace())) {
				throw new ApolloException("chainNamespace is empty, you must configure the chainNamespace property");
			}

			apolloParseHelper = new ApolloParseHelper(apolloParserConfigVO);
		}
		catch (Exception e) {
			throw new ApolloException(e);
		}
	}

	@Override
	public String parseCustom() {

		try {
			String content = apolloParseHelper.getContent();
			FlowInitHook.addHook(() -> {
				apolloParseHelper.listenApollo();
				return true;
			});
			return content;

		}
		catch (Exception e) {
			throw new ApolloException(e);
		}
	}

}
