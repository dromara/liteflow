package com.yomahub.liteflow.parser.nacos.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.yomahub.liteflow.exception.ParseException;
import com.yomahub.liteflow.parser.nacos.exception.NacosException;
import com.yomahub.liteflow.parser.nacos.vo.NacosParserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Nacos client for http api
 * <a href="https://nacos.io/zh-cn/docs/open-api.html">OpenAPI</a>
 * @author mll
 * @since 2.9.0
 */
public class NacosParserHelper {
	private static final Logger LOG = LoggerFactory.getLogger(NacosParserHelper.class);

	private final NacosParserVO    nacosParserVO;
	private final Consumer<String> parseConsumer;

	private static final ConcurrentHashMap<String,String> CONFIG_MAP = new ConcurrentHashMap<>(1);

	private final ConfigService configService;

	static {
		CONFIG_MAP.put("Content","");
	}

	public NacosParserHelper(NacosParserVO nacosParserVO, Consumer<String> parseConsumer) {
		this.nacosParserVO = nacosParserVO;
		this.parseConsumer = parseConsumer;
		try{
			Properties properties = new Properties();
			properties.put(PropertyKeyConst.SERVER_ADDR, nacosParserVO.getServerAddr());
			properties.put(PropertyKeyConst.NAMESPACE,nacosParserVO.getNamespace());
			properties.put(PropertyKeyConst.USERNAME,nacosParserVO.getUsername());
			properties.put(PropertyKeyConst.PASSWORD,nacosParserVO.getPassword());
			this.configService = NacosFactory.createConfigService(properties);
			CONFIG_MAP.put("Content",configService.getConfig(nacosParserVO.getDataId(), nacosParserVO.getGroup(), 3000L));
		}catch (Exception e){
			throw new NacosException(e.getMessage());
		}
	}

	public String getContent(){
		try{
			return CONFIG_MAP.get("Content");
		}catch (Exception e){
			throw new NacosException(e.getMessage());
		}
	}

	/**
	 * 检查 content 是否合法
	 */
	public void checkContent(String content) {
		if (StrUtil.isBlank(content)) {
			String error = MessageFormat.format("the node[{0}] value is empty", nacosParserVO.toString());
			throw new ParseException(error);
		}
	}

	/**
	 * 监听 nacos 数据变化
	 */
	public void listener() {
		try {
			this.configService.addListener(nacosParserVO.getDataId(), nacosParserVO.getGroup(), new Listener() {
				@Override
				public void receiveConfigInfo(String configInfo) {
					LOG.info("stating load flow config.... {} " , configInfo);
					CONFIG_MAP.put("Content",configInfo);
					parseConsumer.accept(configInfo);
				}
				@Override
				public Executor getExecutor() {
					return null;
				}
			});
		}catch (Exception ex){
			throw new NacosException(ex.getMessage());
		}
	}

}
