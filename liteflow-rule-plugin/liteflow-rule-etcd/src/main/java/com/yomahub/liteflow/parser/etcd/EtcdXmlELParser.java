package com.yomahub.liteflow.parser.etcd;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.etcd.exception.EtcdException;
import com.yomahub.liteflow.parser.etcd.util.EtcdParserHelper;
import com.yomahub.liteflow.parser.etcd.vo.EtcdParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Objects;

/**
 * Etcd解析器实现，只支持EL形式的XML，不支持其他的形式
 *
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdXmlELParser extends ClassXmlFlowELParser {

	private final EtcdParserHelper etcdParserHelper;

	public EtcdXmlELParser() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

		try {
			EtcdParserVO etcdParserVO = null;
			if (MapUtil.isNotEmpty((liteflowConfig.getRuleSourceExtDataMap()))) {
				etcdParserVO = BeanUtil.toBean(liteflowConfig.getRuleSourceExtDataMap(), EtcdParserVO.class,
						CopyOptions.create());
			}
			else if (StrUtil.isNotBlank(liteflowConfig.getRuleSourceExtData())) {
				etcdParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), EtcdParserVO.class);
			}

			if (Objects.isNull(etcdParserVO)) {
				throw new EtcdException("rule-source-ext-data is empty");
			}

			if (StrUtil.isBlank(etcdParserVO.getChainPath())) {
				throw new EtcdException("You must configure the chainPath property");
			}
			if (StrUtil.isBlank(etcdParserVO.getEndpoints())) {
				throw new EtcdException("etcd endpoints is empty");
			}

			etcdParserHelper = new EtcdParserHelper(etcdParserVO);
		}
		catch (Exception e) {
			throw new EtcdException(e);
		}
	}

	@Override
	public String parseCustom() {

		try {
			String content = etcdParserHelper.getContent();

			FlowInitHook.addHook(() -> {
				etcdParserHelper.listen();
				return true;
			});

			return content;
		}
		catch (Exception e) {
			throw new EtcdException(e);
		}
	}

}
