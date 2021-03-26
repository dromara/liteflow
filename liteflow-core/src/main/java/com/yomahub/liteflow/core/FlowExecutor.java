/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.exception.FlowExecutorNotInitException;
import com.yomahub.liteflow.exception.NoAvailableSlotException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.LocalXmlFlowParser;
import com.yomahub.liteflow.parser.XmlFlowParser;
import com.yomahub.liteflow.parser.ZookeeperXmlFlowParser;

import java.util.concurrent.ExecutorService;

/**
 * 流程规则主要执行器类
 * @author Bryan.Zhang
 */
public class FlowExecutor {

	private static final Logger LOG = LoggerFactory.getLogger(FlowExecutor.class);

	private LiteflowConfig liteflowConfig;

	private String zkNode;

	//FlowExecutor的初始化化方式，主要用于parse规则文件
	public void init() {
		if (ObjectUtil.isNull(liteflowConfig) || StrUtil.isBlank(liteflowConfig.getRuleSource())){
			throw new ConfigErrorException("config error, please check liteflow config property");
		}

		List<String> rulePath = Lists.newArrayList(liteflowConfig.getRuleSource().split(",|;"));

		XmlFlowParser parser = null;
		for(String path : rulePath){
			try {

				if(isLocalConfig(path)) { //判断是否是本地的xml文件
					parser = new LocalXmlFlowParser();
				}else if(isZKConfig(path)){ //判断是否是zk配置
					if(StringUtils.isNotBlank(zkNode)) {
						parser = new ZookeeperXmlFlowParser(zkNode);
					}else {
						parser = new ZookeeperXmlFlowParser();
					}
				}else if(isClassConfig(path)) { //判断是否是自定义配置
					Class c = Class.forName(path);
					parser = (XmlFlowParser)c.newInstance();
				}
				parser.parseMain(path);
			} catch (Exception e) {
				String errorMsg = MessageFormat.format("init flow executor cause error,cannot parse rule file{0}", path);
				LOG.error(errorMsg,e);
				throw new FlowExecutorNotInitException(errorMsg);
			}
		}
	}

	private boolean isZKConfig(String path) {
		Pattern p = Pattern.compile("[\\w\\d][\\w\\d\\.]+\\:(\\d)+(\\,[\\w\\d][\\w\\d\\.]+\\:(\\d)+)*");
	    Matcher m = p.matcher(path);
	    return m.find();
	}

	private boolean isLocalConfig(String path) {
		Pattern p = Pattern.compile("^[\\w_\\-\\@\\/]+\\.xml$");
	    Matcher m = p.matcher(path);
	    return m.find();
	}

	private boolean isClassConfig(String path) {
		Pattern p = Pattern.compile("^\\w+(\\.\\w+)*$");
	    Matcher m = p.matcher(path);
	    return m.find();
	}

	public void reloadRule(){
		init();
	}

	public <T extends Slot> T execute(String chainId,Object param) throws Exception{
		return execute(chainId, param, DefaultSlot.class,null,false);
	}

	public <T extends Slot> T execute(String chainId,Object param,Class<? extends Slot> slotClazz) throws Exception{
		return execute(chainId, param, slotClazz,null,false);
	}

	public void invoke(String chainId,Object param,Class<? extends Slot> slotClazz,Integer slotIndex) throws Exception{
		execute(chainId, param, slotClazz,slotIndex,true);
	}

	public <T extends Slot> T execute(String chainId,Object param,Class<? extends Slot> slotClazz,Integer slotIndex,boolean isInnerChain) throws Exception{
		Slot slot = null;

		if(FlowBus.needInit()) {
			init();
		}

		Chain chain = FlowBus.getChain(chainId);

		if(chain == null){
			String errorMsg = MessageFormat.format("couldn't find chain with the id[{0}]", chainId);
			throw new ChainNotFoundException(errorMsg);
		}

		if(!isInnerChain && slotIndex == null) {
			slotIndex = DataBus.offerSlot(slotClazz);
			LOG.info("slot[{}] offered",slotIndex);
		}

		if(slotIndex == -1){
			throw new NoAvailableSlotException("there is no available slot");
		}

		slot = DataBus.getSlot(slotIndex);
		if(slot == null) {
			throw new NoAvailableSlotException("the slot is not exist");
		}

		if(StringUtils.isBlank(slot.getRequestId())) {
			slot.generateRequestId();
			LOG.info("requestId[{}] has generated",slot.getRequestId());
		}

		if(!isInnerChain) {
			slot.setRequestData(param);
			slot.setChainName(chainId);
		}else {
			slot.setChainReqData(chainId, param);
		}

		try {
			// 执行chain
			chain.execute(slotIndex);
		} catch (Exception e) {
			LOG.error("[{}]:chain[{}] execute error on slot[{}]", slot.getRequestId(), chain.getChainName(), slotIndex);
			slot.setSuccess(false);
			slot.setErrorMsg(e.getMessage());
		} finally {
			if (!isInnerChain) {
				slot.printStep();
				DataBus.releaseSlot(slotIndex);
			}
		}
		return (T)slot;
	}

	public String getZkNode() {
		return zkNode;
	}

	public void setZkNode(String zkNode) {
		this.zkNode = zkNode;
	}

	public LiteflowConfig getLiteflowConfig() {
		return liteflowConfig;
	}

	public void setLiteflowConfig(LiteflowConfig liteflowConfig) {
		this.liteflowConfig = liteflowConfig;
	}

}
