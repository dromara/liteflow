package com.yomahub.liteflow.parser.etcd;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.etcd.exception.EtcdException;
import com.yomahub.liteflow.parser.etcd.util.EtcdParserHelper;
import com.yomahub.liteflow.parser.etcd.vo.EtcdParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.function.Consumer;

/**
 * Etcd解析器实现，只支持EL形式的XML，不支持其他的形式
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdXmlELParser extends ClassXmlFlowELParser {

	private final EtcdParserHelper etcdParserHelper;

	public EtcdXmlELParser() {
		Consumer<String> parseConsumer = t -> {
			try {
				parse(t);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};

		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

		if (StrUtil.isBlank(liteflowConfig.getRuleSourceExtData())){
			throw new EtcdException("rule-source-ext-data is empty");
		}

		try{
			EtcdParserVO etcdParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), EtcdParserVO.class);
			assert etcdParserVO != null;

			if (StrUtil.isBlank(etcdParserVO.getNodePath())){
				etcdParserVO.setNodePath("/lite-flow/flow");
			}
			if (StrUtil.isBlank(etcdParserVO.getConnectStr())){
				throw new EtcdException("Etcd connect string is empty");
			}

			etcdParserHelper = new EtcdParserHelper(etcdParserVO, parseConsumer);
		}catch (Exception e){
			throw new EtcdException(e.getMessage());
		}
	}

	@Override
	public String parseCustom() {
		try {
			String content = etcdParserHelper.getContent();
			etcdParserHelper.checkContent(content);
			etcdParserHelper.watchRule();
			return content;
		} catch (Exception e){
			throw new EtcdException(e.getMessage());
		}
	}
}
