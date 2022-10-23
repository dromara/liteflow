package com.yomahub.liteflow.parser.etcd;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.etcd.exception.EtcdException;
import com.yomahub.liteflow.parser.etcd.util.EtcdParserHelper;
import com.yomahub.liteflow.parser.etcd.vo.EtcdParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Etcd解析器实现，只支持EL形式的XML，不支持其他的形式
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdXmlELParser extends ClassXmlFlowELParser {

	private final EtcdParserHelper etcdParserHelper;

	public EtcdXmlELParser() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

		try{
			EtcdParserVO etcdParserVO = null;
			if(MapUtil.isNotEmpty((liteflowConfig.getRuleSourceExtDataMap()))){
				etcdParserVO = BeanUtil.toBean(liteflowConfig.getRuleSourceExtDataMap(), EtcdParserVO.class, CopyOptions.create());
			}else if (StrUtil.isNotBlank(liteflowConfig.getRuleSourceExtData())){
				etcdParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), EtcdParserVO.class);
			}

			if (Objects.isNull(etcdParserVO)) {
				throw new EtcdException("rule-source-ext-data is empty");
			}

			if (StrUtil.isBlank(etcdParserVO.getNodePath())){
				etcdParserVO.setNodePath("/lite-flow/flow");
			}
			if (StrUtil.isBlank(etcdParserVO.getConnectStr())){
				throw new EtcdException("Etcd connect string is empty");
			}

			etcdParserHelper = new EtcdParserHelper(etcdParserVO);
		}catch (Exception e){
			throw new EtcdException(e.getMessage());
		}
	}

	@Override
	public String parseCustom() {
		Consumer<String> parseConsumer = t -> {
			try {
				parse(t);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		try {
			String content = etcdParserHelper.getContent();
			etcdParserHelper.checkContent(content);
			etcdParserHelper.listen(parseConsumer);
			return content;
		} catch (Exception e){
			throw new EtcdException(e.getMessage());
		}
	}
}
