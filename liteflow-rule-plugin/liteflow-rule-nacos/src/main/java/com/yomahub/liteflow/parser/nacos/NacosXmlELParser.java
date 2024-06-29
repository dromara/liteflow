package com.yomahub.liteflow.parser.nacos;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.nacos.exception.NacosException;
import com.yomahub.liteflow.parser.nacos.util.NacosParserHelper;
import com.yomahub.liteflow.parser.nacos.vo.NacosParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Nacos 解析器实现，只支持EL形式的XML，不支持其他的形式
 *
 * @author mll
 * @since 2.9.0
 */
public class NacosXmlELParser extends ClassXmlFlowELParser {

	private final NacosParserHelper helper;

	public NacosXmlELParser() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

		try {
			NacosParserVO nacosParserVO = null;
			if (MapUtil.isNotEmpty((liteflowConfig.getRuleSourceExtDataMap()))) {
				nacosParserVO = BeanUtil.toBean(liteflowConfig.getRuleSourceExtDataMap(), NacosParserVO.class,
						CopyOptions.create());
			}
			else if (StrUtil.isNotBlank(liteflowConfig.getRuleSourceExtData())) {
				nacosParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), NacosParserVO.class);
			}

			if (Objects.isNull(nacosParserVO)) {
				throw new NacosException("rule-source-ext-data is empty");
			}

			if (StrUtil.isBlank(nacosParserVO.getServerAddr())) {
				nacosParserVO.setServerAddr("127.0.0.1:8848");
			}
			if (StrUtil.isBlank(nacosParserVO.getNamespace())) {
				nacosParserVO.setNamespace("");
			}
			if (StrUtil.isBlank(nacosParserVO.getDataId())) {
				nacosParserVO.setDataId("LiteFlow");
			}
			if (StrUtil.isBlank(nacosParserVO.getGroup())) {
				nacosParserVO.setGroup("LITE_FLOW_GROUP");
			}
			if (StrUtil.isBlank(nacosParserVO.getUsername())) {
				nacosParserVO.setUsername("");
			}
			if (StrUtil.isBlank(nacosParserVO.getPassword())) {
				nacosParserVO.setPassword("");
			}
			if (StrUtil.isBlank(nacosParserVO.getAccessKey())){
				nacosParserVO.setAccessKey("");
			}
			if (StrUtil.isBlank(nacosParserVO.getSecretKey())){
				nacosParserVO.setSecretKey("");
			}
			helper = new NacosParserHelper(nacosParserVO);
		}
		catch (Exception e) {
			throw new NacosException(e);
		}
	}

	@Override
	public String parseCustom() {
		Consumer<String> parseConsumer = t -> {
			try {
				parse(t);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		try {
			String content = helper.getContent();
			helper.checkContent(content);
			helper.listener(parseConsumer);
			return content;
		}
		catch (Exception e) {
			throw new NacosException(e);
		}
	}

}
