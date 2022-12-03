package com.yomahub.liteflow.parser.apollo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.apollo.exception.ApolloException;
import com.yomahub.liteflow.parser.apollo.util.ApolloParseHelper;
import com.yomahub.liteflow.parser.apollo.vo.ApolloParserConfigVO;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @Description:
 * @Author: zhanghua
 * @Date: 2022/12/3 13:38
 */
public class ApolloXmlELParser extends ClassXmlFlowELParser {

	private final ApolloParseHelper apolloParseHelper;

	public ApolloXmlELParser() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		try {
			ApolloParserConfigVO apolloParserConfigVO = null;
			if (MapUtil.isNotEmpty((liteflowConfig.getRuleSourceExtDataMap()))) {
				apolloParserConfigVO = BeanUtil.toBean(liteflowConfig.getRuleSourceExtDataMap(), ApolloParserConfigVO.class, CopyOptions.create());
			} else if (StrUtil.isNotBlank(liteflowConfig.getRuleSourceExtData())) {
				apolloParserConfigVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), ApolloParserConfigVO.class);
			}
			if (Objects.isNull(apolloParserConfigVO)) {
				throw new ApolloException("ruleSourceExtData or map is empty");
			}

			apolloParseHelper = new ApolloParseHelper(apolloParserConfigVO);
		} catch (Exception e) {
			throw new ApolloException(e.getMessage());
		}
	}

	@Override
	public String parseCustom() {
		Consumer<String> parseConsumer = t -> {
			try {
				parse(t);
			} catch (Exception e) {
				throw new ApolloException(e.getMessage());
			}
		};
		try {
			String content = apolloParseHelper.getContent();
			apolloParseHelper.checkContent(content);
			apolloParseHelper.listen(parseConsumer);
			return content;

		} catch (Exception e) {
			throw new ApolloException(e.getMessage());
		}
	}
}
