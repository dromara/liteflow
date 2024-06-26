package com.yomahub.liteflow.parser.nacos.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.yomahub.liteflow.exception.ParseException;
import com.yomahub.liteflow.parser.nacos.exception.NacosException;
import com.yomahub.liteflow.parser.nacos.vo.NacosParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Nacos client for http api
 * <a href="https://nacos.io/zh-cn/docs/open-api.html">OpenAPI</a>
 *
 * @author mll
 * @since 2.9.0
 */
public class NacosParserHelper {

	private static final Logger LOG = LoggerFactory.getLogger(NacosParserHelper.class);

	private final NacosParserVO nacosParserVO;

	private NacosConfigService configService;

	public NacosParserHelper(NacosParserVO nacosParserVO) {
		this.nacosParserVO = nacosParserVO;
		try {
			try {
				this.configService = ContextAwareHolder.loadContextAware().getBean(NacosConfigService.class);
			}
			catch (Exception ignored) {
			}
			if (this.configService == null) {
                Properties properties = getProperties(nacosParserVO);
                this.configService = new NacosConfigService(properties);
			}
		}
		catch (Exception e) {
			throw new NacosException(e);
		}
	}

    private static Properties getProperties(NacosParserVO nacosParserVO) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosParserVO.getServerAddr());
        properties.put(PropertyKeyConst.NAMESPACE, nacosParserVO.getNamespace());
        if (StrUtil.isNotEmpty(nacosParserVO.getUsername())) {
			// 用户名密码模式 填写用户名就必有密码
            if (StrUtil.isEmpty(PropertyKeyConst.PASSWORD)){
                throw new NacosException("Nacos config password is empty");
            }
            // 历史版本会使用用户名密码
            properties.put(PropertyKeyConst.USERNAME, nacosParserVO.getUsername());
            properties.put(PropertyKeyConst.PASSWORD, nacosParserVO.getPassword());
        } else if (StrUtil.isNotEmpty(PropertyKeyConst.ACCESS_KEY)){
            // 以下为阿里云RAM子账号使用 填写了ak就必有sk
            if (StrUtil.isEmpty(PropertyKeyConst.SECRET_KEY)){
                throw new NacosException("Nacos config secretKey is empty");
            }
            properties.put(PropertyKeyConst.ACCESS_KEY, nacosParserVO.getAccessKey());
            properties.put(PropertyKeyConst.SECRET_KEY, nacosParserVO.getSecretKey());
        }
        return properties;
    }

	public String getContent() {
		try {
			return configService.getConfig(nacosParserVO.getDataId(), nacosParserVO.getGroup(), 3000L);
		}
		catch (Exception e) {
			throw new NacosException(e);
		}
	}

	/**
	 * 检查 content 是否合法
	 */
	public void checkContent(String content) {
		if (StrUtil.isBlank(content)) {
			String error = StrUtil.format("the node[{}] value is empty", nacosParserVO.toString());
			throw new ParseException(error);
		}
	}

	/**
	 * 监听 nacos 数据变化
	 */
	public void listener(Consumer<String> parseConsumer) {
		try {
			this.configService.addListener(nacosParserVO.getDataId(), nacosParserVO.getGroup(), new Listener() {
				@Override
				public void receiveConfigInfo(String configInfo) {
					LOG.info("stating load flow config.... {} ", configInfo);
					parseConsumer.accept(configInfo);
				}

				@Override
				public Executor getExecutor() {
					return null;
				}
			});
		}
		catch (Exception ex) {
			throw new NacosException(ex);
		}
	}

}
