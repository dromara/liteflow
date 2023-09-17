package com.yomahub.liteflow.flow.id;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.RequestIdGeneratorException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

/**
 * Id 生成器帮助器
 *
 * @author tangkc
 */
public class IdGeneratorHolder {

	private RequestIdGenerator requestIdGenerator;

	private static IdGeneratorHolder INSTANCE = new IdGeneratorHolder();

	public static void init() {
		try {
			LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
			String requestIdGeneratorClass = liteflowConfig.getRequestIdGeneratorClass();

			RequestIdGenerator requestIdGenerator;
			if (StrUtil.isBlank(requestIdGeneratorClass)) {
				requestIdGenerator = new DefaultRequestIdGenerator();
			}
			else {
				Class<RequestIdGenerator> idGenerateClass = (Class<RequestIdGenerator>) Class
					.forName(requestIdGeneratorClass);
				requestIdGenerator = ContextAwareHolder.loadContextAware().registerBean(idGenerateClass);
			}
			INSTANCE.setRequestIdGenerator(requestIdGenerator);
		}
		catch (Exception e) {
			throw new RequestIdGeneratorException(e.getMessage());
		}
	}

	public static IdGeneratorHolder getInstance() {
		return INSTANCE;
	}

	public String generate() {
		if (ObjectUtil.isNull(requestIdGenerator)){
			init();
		}
		return requestIdGenerator.generate();
	}

	public RequestIdGenerator getRequestIdGenerator() {
		return requestIdGenerator;
	}

	public void setRequestIdGenerator(RequestIdGenerator requestIdGenerator) {
		this.requestIdGenerator = requestIdGenerator;
	}

}
