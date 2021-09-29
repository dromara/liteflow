/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.parser.*;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.util.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.LocalXmlFlowParser;
import com.yomahub.liteflow.parser.XmlFlowParser;
import com.yomahub.liteflow.parser.ZookeeperXmlFlowParser;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 流程规则主要执行器类
 *
 * @author Bryan.Zhang
 */
public class FlowExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(FlowExecutor.class);

    private static final String ZK_CONFIG_REGEX = "[\\w\\d][\\w\\d\\.]+\\:(\\d)+(\\,[\\w\\d][\\w\\d\\.]+\\:(\\d)+)*";

    private static final String LOCAL_XML_CONFIG_REGEX = "^[\\w_\\-\\@\\/\\*]+\\.xml$";
    private static final String LOCAL_JSON_CONFIG_REGEX = "^[\\w_\\-\\@\\/\\*]+\\.json$";
    private static final String LOCAL_YML_CONFIG_REGEX = "^[\\w_\\-\\@\\/\\*]+\\.yml$";

    private static final String FORMATE_XML_CONFIG_REGEX = "xml:.+";
    private static final String FORMATE_JSON_CONFIG_REGEX = "json:.+";
    private static final String FORMATE_YML_CONFIG_REGEX = "yml:.+";
    private static final String PREFIX_FORMATE_CONFIG_REGEX = "xml:|json:|yml:";

    private static final String CLASS_CONFIG_REGEX = "^\\w+(\\.\\w+)*$";

    private LiteflowConfig liteflowConfig;

    /**
     * FlowExecutor的初始化化方式，主要用于parse规则文件
     */
    public void init() {
        if (ObjectUtil.isNull(liteflowConfig) || StrUtil.isBlank(liteflowConfig.getRuleSource())) {
            throw new ConfigErrorException("config error, please check liteflow config property");
        }
        List<String> sourceRulePathList = Lists.newArrayList(liteflowConfig.getRuleSource().split(",|;"));

        FlowParser parser = null;
        Set<String> parserNameSet = new HashSet<>();
        List<String> rulePathList = new ArrayList<>();
        for (String path : sourceRulePathList) {
            try {
                FlowParserTypeEnum pattern = matchFormatConfig(path);
                if (ObjectUtil.isNotNull(pattern)) {
                    path = ReUtil.replaceAll(path, PREFIX_FORMATE_CONFIG_REGEX, "");
                    switch (pattern) {
                        case TYPE_XML:
                            parser = matchFormatParser(path, FlowParserTypeEnum.TYPE_XML);
                            parserNameSet.add(parser.getClass().getName());
                            break;
                        case TYPE_JSON:
                            parser = matchFormatParser(path, FlowParserTypeEnum.TYPE_JSON);
                            parserNameSet.add(parser.getClass().getName());
                            break;
                        case TYPE_YML:
                            parser = matchFormatParser(path, FlowParserTypeEnum.TYPE_YML);
                            parserNameSet.add(parser.getClass().getName());
                            break;
                        default:
                            String errorMsg = StrUtil.format("can't support the format {}", path);
                            throw new ErrorSupportPathException(errorMsg);
                    }
                }
                rulePathList.add(path);

                //支持多类型的配置文件，分别解析
                if (liteflowConfig.isSupportMultipleType()) {
                    if (ObjectUtil.isNotNull(parser)) {
                        parser.parseMain(ListUtil.toList(path));
                    } else {
                        throw new ConfigErrorException("parse error, please check liteflow config property");
                    }
                }
            } catch (CyclicDependencyException e){
                LOG.error(e.getMessage());
                throw e;
            } catch (Exception e) {
                String errorMsg = StrUtil.format("init flow executor cause error,cannot find the parse for path {}", path);
                LOG.error(errorMsg, e);
                throw new FlowExecutorNotInitException(errorMsg);
            }
        }

        //单类型的配置文件，需要一起解析
        if (!liteflowConfig.isSupportMultipleType()){
            //检查Parser是否只有一个，因为多个不同的parser会造成子流程的混乱
            if (parserNameSet.size() > 1){
                String errorMsg = "cannot have multiple different parsers";
                LOG.error(errorMsg);
                throw new MultipleParsersException(errorMsg);
            }

            //进行多个配置文件的一起解析
            try{
                if (ObjectUtil.isNotNull(parser)) {
                    parser.parseMain(rulePathList);
                } else {
                    throw new ConfigErrorException("parse error, please check liteflow config property");
                }
            } catch (CyclicDependencyException e){
                LOG.error(e.getMessage());
                throw e;
            } catch (Exception e) {
                String errorMsg = StrUtil.format("init flow executor cause error,can not parse rule file {}", rulePathList);
                LOG.error(errorMsg, e);
                throw new FlowExecutorNotInitException(errorMsg);
            }
        }

        //初始化DataBus
        DataBus.init();
    }

    /**
     * 匹配路径配置，生成对应的解析器
     *
     * @param path    配置路径
     * @param pattern 格式
     * @return
     */
    private FlowParser matchFormatParser(String path, FlowParserTypeEnum pattern) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        boolean isLocalFile = isLocalConfig(path);
        if (isLocalFile) {
            LOG.info("flow info loaded from local file,path={},format type={}", path, pattern.getType());
            switch (pattern) {
                case TYPE_XML:
                    return new LocalXmlFlowParser();
                case TYPE_JSON:
                    return new LocalJsonFlowParser();
                case TYPE_YML:
                    return new LocalYmlFlowParser();
                default:
            }
        } else if (isClassConfig(path)) {
            LOG.info("flow info loaded from class config,class={},format type={}", path, pattern.getType());
            Class<?> c = Class.forName(path);
            switch (pattern) {
                case TYPE_XML:
                    return (XmlFlowParser) SpringAware.registerBean(c);
                case TYPE_JSON:
                    return (JsonFlowParser) SpringAware.registerBean(c);
                case TYPE_YML:
                    return (YmlFlowParser) SpringAware.registerBean(c);
                default:
            }
        } else if (isZKConfig(path)) {
            LOG.info("flow info loaded from Zookeeper,zkNode={},format type={}", path, pattern.getType());
            switch (pattern) {
                case TYPE_XML:
                    return new ZookeeperXmlFlowParser(liteflowConfig.getZkNode());
                case TYPE_JSON:
                    return new ZookeeperJsonFlowParser(liteflowConfig.getZkNode());
                case TYPE_YML:
                    return new ZookeeperYmlFlowParser(liteflowConfig.getZkNode());
                default:
            }
        }
        LOG.info("load flow info error, path={}, pattern={}", path, pattern.getType());
        return null;
    }

    /**
     * 判定是否为本地文件
     *
     * @param path
     * @return
     */
    private boolean isLocalConfig(String path) {
        return ReUtil.isMatch(LOCAL_XML_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_JSON_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_YML_CONFIG_REGEX, path);
    }

    /**
     * 判定是否为自定义class配置
     *
     * @param path
     * @return
     */
    private boolean isClassConfig(String path) {
        return ReUtil.isMatch(CLASS_CONFIG_REGEX, path);
    }

    /**
     * 判定是否为zk配置
     *
     * @param path
     * @return
     */
    private boolean isZKConfig(String path) {
        return ReUtil.isMatch(ZK_CONFIG_REGEX, path);
    }

    /**
     * 匹配文本格式，支持xml，json和yml
     *
     * @param path
     * @return
     */
    private FlowParserTypeEnum matchFormatConfig(String path) {
        if (ReUtil.isMatch(LOCAL_XML_CONFIG_REGEX, path) || ReUtil.isMatch(FORMATE_XML_CONFIG_REGEX, path)) {
            return FlowParserTypeEnum.TYPE_XML;
        } else if (ReUtil.isMatch(LOCAL_JSON_CONFIG_REGEX, path) || ReUtil.isMatch(FORMATE_JSON_CONFIG_REGEX, path)) {
            return FlowParserTypeEnum.TYPE_JSON;
        } else if (ReUtil.isMatch(LOCAL_YML_CONFIG_REGEX, path) || ReUtil.isMatch(FORMATE_YML_CONFIG_REGEX, path)) {
            return FlowParserTypeEnum.TYPE_YML;
        } else if (isClassConfig(path)) {
            try {
                Class clazz = Class.forName(path);
                if (ClassXmlFlowParser.class.isAssignableFrom(clazz)) {
                    return FlowParserTypeEnum.TYPE_XML;
                } else if (ClassJsonFlowParser.class.isAssignableFrom(clazz)) {
                    return FlowParserTypeEnum.TYPE_JSON;
                } else if (ClassYmlFlowParser.class.isAssignableFrom(clazz)) {
                    return FlowParserTypeEnum.TYPE_YML;
                }
            } catch (ClassNotFoundException e) {
                LOG.error(e.getMessage());
            }
        }
        return null;
    }

    public void reloadRule() {
        FlowBus.cleanCache();
        init();
    }

    /**
     * callback by implicit subflow
     *
     * @param chainId
     * @param param
     * @param slotClazz
     * @param slotIndex
     * @param <T>
     * @throws Exception
     */
    public <T extends Slot> void invoke(String chainId, Object param, Class<T> slotClazz,
                                        Integer slotIndex) throws Exception {
        this.execute(chainId, param, slotClazz, slotIndex, true);
    }

    public DefaultSlot execute(String chainId, Object param) throws Exception {
        return this.execute(chainId, param, DefaultSlot.class, null, false);
    }

    public <T extends Slot> T execute(String chainId, Object param, Class<T> slotClazz) throws Exception {
        return this.execute(chainId, param, slotClazz, null, false);
    }

    public <T extends Slot> T execute(String chainId, Object param, Class<T> slotClazz,
                                      Integer slotIndex, boolean isInnerChain) throws Exception {
        T slot = this.doExecute(chainId, param, slotClazz, slotIndex, isInnerChain);
        if (ObjectUtil.isNotNull(slot.getException())) {
            throw slot.getException();
        } else {
            return slot;
        }
    }

    public LiteflowResponse<DefaultSlot> execute2Resp(String chainId, Object param) {
        return this.execute2Resp(chainId, param, DefaultSlot.class);
    }

    public <T extends Slot> LiteflowResponse<T> execute2Resp(String chainId, Object param, Class<T> slotClazz) {
        return this.execute2Resp(chainId, param, slotClazz, null, false);
    }

    private final ArrayList<Class<? extends Exception>> notFailExceptionList = ListUtil.toList(ChainEndException.class);

    public <T extends Slot> LiteflowResponse<T> execute2Resp(String chainId, Object param, Class<T> slotClazz, Integer slotIndex,
                                                             boolean isInnerChain) {
        LiteflowResponse<T> response = new LiteflowResponse<>();

        T slot = doExecute(chainId, param, slotClazz, slotIndex, isInnerChain);

        if (ObjectUtil.isNotNull(slot.getException()) && !notFailExceptionList.contains(slot.getException().getClass())) {
            response.setSuccess(false);
            response.setMessage(slot.getException().getMessage());
            response.setCause(slot.getException());
        } else {
            response.setSuccess(true);
        }
        response.setSlot(slot);
        return response;
    }

    private <T extends Slot> T doExecute(String chainId, Object param, Class<T> slotClazz, Integer slotIndex,
                                         boolean isInnerChain) {
        if (FlowBus.needInit()) {
            init();
        }

        if (!isInnerChain && ObjectUtil.isNull(slotIndex)) {
            slotIndex = DataBus.offerSlot(slotClazz);
            LOG.info("slot[{}] offered", slotIndex);
        }

        if (slotIndex == -1) {
            throw new NoAvailableSlotException("there is no available slot");
        }

        T slot = DataBus.getSlot(slotIndex);
        if (ObjectUtil.isNull(slot)) {
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

        Chain chain = null;
        try {
            chain = FlowBus.getChain(chainId);

            if (ObjectUtil.isNull(chain)) {
                String errorMsg = StrUtil.format("couldn't find chain with the id[{}]", chainId);
                throw new ChainNotFoundException(errorMsg);
            }

            // 执行chain
            chain.execute(slotIndex);
        } catch (Exception e) {
            if (ObjectUtil.isNotNull(chain)){
                LOG.error("[{}]:chain[{}] execute error on slot[{}]", slot.getRequestId(), chain.getChainName(), slotIndex);
            }
            slot.setException(e);
        } finally {
            if (!isInnerChain) {
                slot.printStep();
                DataBus.releaseSlot(slotIndex);
            }
        }
        return slot;
    }

    public LiteflowConfig getLiteflowConfig() {
        return liteflowConfig;
    }

    public void setLiteflowConfig(LiteflowConfig liteflowConfig) {
        this.liteflowConfig = liteflowConfig;
    }

}
