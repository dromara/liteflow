/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.parser.*;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.exception.FlowExecutorNotInitException;
import com.yomahub.liteflow.exception.NoAvailableSlotException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.LocalXmlFlowParser;
import com.yomahub.liteflow.parser.XmlFlowParser;
import com.yomahub.liteflow.parser.ZookeeperXmlFlowParser;

import java.text.MessageFormat;
import java.util.List;

/**
 * 流程规则主要执行器类
 * @author Bryan.Zhang
 */
public class FlowExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(FlowExecutor.class);

    private static final String ZK_CONFIG_REGEX = "[\\w\\d][\\w\\d\\.]+\\:(\\d)+(\\,[\\w\\d][\\w\\d\\.]+\\:(\\d)+)*";

    private static final String LOCAL_XML_CONFIG_REGEX = "^[\\w_\\-\\@\\/]+\\.xml$";
    private static final String LOCAL_JSON_CONFIG_REGEX = "^[\\w_\\-\\@\\/]+\\.json$";
    private static final String LOCAL_YML_CONFIG_REGEX = "^[\\w_\\-\\@\\/]+\\.yml$";

    private static final String FORMATE_XML_CONFIG_REGEX = "xml:.+";
    private static final String FORMATE_JSON_CONFIG_REGEX = "json:.+";
    private static final String FORMATE_YML_CONFIG_REGEX = "yml:.+";
    private static final String PREFIX_FORMATE_CONFIG_REGEX = "xml:|json:|yml:";

    private static final String CLASS_CONFIG_REGEX = "^\\w+(\\.\\w+)*$";

    private LiteflowConfig liteflowConfig;

    private String zkNode;

    /**
     * FlowExecutor的初始化化方式，主要用于parse规则文件
     */
    public void init() {
        if (ObjectUtil.isNull(liteflowConfig) || StrUtil.isBlank(liteflowConfig.getRuleSource())) {
            throw new ConfigErrorException("config error, please check liteflow config property");
        }
        List<String> rulePath = Lists.newArrayList(liteflowConfig.getRuleSource().split(",|;"));

        FlowParser parser = null;
        for (String path : rulePath) {
            try {
                String pattern = matchFormatConfig(path);
                path = ReUtil.replaceAll(path, PREFIX_FORMATE_CONFIG_REGEX, "");
                switch (pattern) {
                    case "xml" :
                        parser = matchFormatParser(path, "xml");
                        break;
                    case "json" :
                        parser = matchFormatParser(path, "json");
                        break;
                    case "yml" :
                        parser = matchFormatParser(path, "yml");
                        break;
                    default:
                        LOG.error("can't surport the format {}", path);
                }
                if(null != parser) {
                    parser.parseMain(path);
                } else {
                    throw new ConfigErrorException("parse error, please check liteflow config property");
                }
			} catch (Exception e) {
                String errorMsg = MessageFormat.format("init flow executor cause error,cannot parse rule file{0}", path);
                LOG.error(errorMsg, e);
                throw new FlowExecutorNotInitException(errorMsg);
            }
        }
    }

    /**
     * 匹配路径配置，生成对应的解析器
     * @param path 配置路径
     * @param pattern 格式
     * @return
     */
    private FlowParser matchFormatParser(String path, String pattern) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        boolean isLocalFile = isLocalConfig(path);
        if(isLocalFile) {
            switch (pattern) {
                case "xml":
                    return new LocalXmlFlowParser();
                case "json":
                    return new LocalJsonFlowParser();
                case "yml":
                    return new LocalYmlFlowParser();
                default:
            }
        } else if(isClassConfig(path)){
            Class c = Class.forName(path);
            switch (pattern) {
                case "xml":
                    return (XmlFlowParser) c.newInstance();
                case "json":
                    return (JsonFlowParser) c.newInstance();
                case "yml":
                    return (YmlFlowParser) c.newInstance();
                default:
            }
        } else if(isZKConfig(path)) {
            switch (pattern) {
                case "xml":
                    return StrUtil.isNotBlank(zkNode) ? new ZookeeperXmlFlowParser(zkNode) : new ZookeeperXmlFlowParser();
                case "json":
                    return StrUtil.isNotBlank(zkNode) ? new ZookeeperJsonFlowParser(zkNode) : new ZookeeperJsonFlowParser();
                case "yml":
                    return StrUtil.isNotBlank(zkNode) ? new ZookeeperYmlFlowParser(zkNode) : new ZookeeperYmlFlowParser();
                default:
            }
        }
        return null;
    }

    /**
     * 判定是否为本地文件
     * @param path
     * @return
     */
    private boolean isLocalConfig(String path) {
        return ReUtil.isMatch(LOCAL_XML_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_JSON_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_YML_CONFIG_REGEX, path) ;
    }

    /**
     * 判定是否为自定义class配置
     * @param path
     * @return
     */
    private boolean isClassConfig(String path) {
        return ReUtil.isMatch(CLASS_CONFIG_REGEX, path);
    }

    /**
     * 判定是否为zk配置
     * @param path
     * @return
     */
    private boolean isZKConfig(String path) {
        return ReUtil.isMatch(ZK_CONFIG_REGEX, path);
    }

    /**
     * 匹配文本格式，支持xml，json和yml
     * @param path
     * @return
     */
    private String matchFormatConfig(String path) {
        if(ReUtil.isMatch(LOCAL_XML_CONFIG_REGEX, path) || ReUtil.isMatch(FORMATE_XML_CONFIG_REGEX, path)) {
            return "xml";
        } else if(ReUtil.isMatch(LOCAL_JSON_CONFIG_REGEX, path) || ReUtil.isMatch(FORMATE_JSON_CONFIG_REGEX, path)) {
            return "json";
        } else if(ReUtil.isMatch(LOCAL_YML_CONFIG_REGEX, path) || ReUtil.isMatch(FORMATE_YML_CONFIG_REGEX, path)) {
            return "yml";
        }
        return "";
    }

    public void reloadRule() {
        init();
    }

    public void invoke(String chainId, Object param, Class<? extends Slot> slotClazz, Integer slotIndex) throws Exception {
        execute(chainId, param, slotClazz, slotIndex, true);
    }

    public LiteflowResponse execute(String chainId, Object param) throws Exception {
        return execute(chainId, param, DefaultSlot.class, null, false);
    }

	public LiteflowResponse<Slot> execute(String chainId, Object param, Class<? extends Slot> slotClazz) throws Exception {
		return execute(chainId, param, slotClazz,null,false);
	}

	public LiteflowResponse<Slot> execute(String chainId, Object param, Class<? extends Slot> slotClazz, Integer slotIndex,
									boolean isInnerChain) throws Exception {
		Slot slot = null;
    public LiteflowResponse execute(String chainId, Object param, Class<? extends Slot> slotClazz) throws Exception {
        return execute(chainId, param, slotClazz, null, false);
    }

    public LiteflowResponse execute(String chainId, Object param, Class<? extends Slot> slotClazz, Integer slotIndex,
                                    boolean isInnerChain) throws Exception {
        Slot slot = null;

        if (FlowBus.needInit()) {
            init();
        }

        Chain chain = FlowBus.getChain(chainId);

        if (ObjectUtil.isNull(chain)) {
            String errorMsg = MessageFormat.format("couldn't find chain with the id[{0}]", chainId);
            throw new ChainNotFoundException(errorMsg);
        }

        if (!isInnerChain && ObjectUtil.isNull(slotIndex)) {
            slotIndex = DataBus.offerSlot(slotClazz);
            LOG.info("slot[{}] offered", slotIndex);
        }

        if (slotIndex == -1) {
            throw new NoAvailableSlotException("there is no available slot");
        }

        slot = DataBus.getSlot(slotIndex);
        if (slot == null) {
            throw new NoAvailableSlotException("the slot is not exist");
        }

        if (StrUtil.isBlank(slot.getRequestId())) {
            slot.generateRequestId();
            LOG.info("requestId[{}] has generated", slot.getRequestId());
        }

        if (!isInnerChain) {
            slot.setRequestData(param);
            slot.setChainName(chainId);
        } else {
            slot.setChainReqData(chainId, param);
        }
        LiteflowResponse<Slot> response = new LiteflowResponse<>(slot);
        try {
            // 执行chain
            chain.execute(slotIndex);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setCause(e.getCause());
            LOG.error("[{}]:chain[{}] execute error on slot[{}]", slot.getRequestId(), chain.getChainName(), slotIndex);
        } finally {
            if (!isInnerChain) {
                slot.printStep();
                DataBus.releaseSlot(slotIndex);
            }
        }
        return response;
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
