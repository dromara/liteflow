package com.yomahub.liteflow.parser.etcd.util;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.ParseException;
import com.yomahub.liteflow.parser.el.XmlFlowELParser;
import com.yomahub.liteflow.parser.etcd.EtcdClient;
import com.yomahub.liteflow.parser.etcd.exception.EtcdException;
import com.yomahub.liteflow.parser.etcd.vo.EtcdParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import io.etcd.jetcd.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.function.Consumer;

/**
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdParserHelper {

	private static final Logger LOG = LoggerFactory.getLogger(EtcdParserHelper.class);

	private final EtcdParserVO etcdParserVO;

	private EtcdClient etcdClient;

	public EtcdParserHelper(EtcdParserVO etcdParserVO) {
		this.etcdParserVO = etcdParserVO;

		try{
			this.etcdClient  = ContextAwareHolder.loadContextAware().getBean(EtcdClient.class);
			if (this.etcdClient == null) {
				Client client = Client.builder()
						.endpoints(etcdParserVO.getConnectStr().split(","))
						.build();
				this.etcdClient = new EtcdClient(client);
			}
		}catch (Exception e){
			throw new EtcdException(e.getMessage());
		}
	}

	public String getContent(){
		try{
			return this.etcdClient.get(etcdParserVO.getNodePath());
		}catch (Exception e){
			throw new EtcdException(e.getMessage());
		}
	}

	/**
	 * 检查 content 是否合法
	 */
	public void checkContent(String content) {
		if (StrUtil.isBlank(content)) {
			String error = MessageFormat.format("the node[{0}] value is empty", etcdParserVO.getNodePath());
			throw new ParseException(error);
		}
	}

	/**
	 * 监听 etcd 节点
	 */
	public void listen(Consumer<String> parseConsumer) {
		this.etcdClient.watchDataChange(this.etcdParserVO.getNodePath(), (updatePath, updateValue) -> {
			LOG.info("starting load flow config....");
			parseConsumer.accept(updateValue);
			}, null);
	}
}
