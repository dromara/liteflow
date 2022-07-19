/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.id.IdGeneratorHolder;
import com.yomahub.liteflow.parser.*;
import com.yomahub.liteflow.parser.base.FlowParser;
import com.yomahub.liteflow.parser.factory.FlowParserProvider;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.holder.ContextCmpInitHolder;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 流程规则主要执行器类
 *
 * @author Bryan.Zhang
 */
public class FlowExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(FlowExecutor.class);

    private static final String PREFIX_FORMAT_CONFIG_REGEX = "xml:|json:|yml:|el_xml:|el_json:|el_yml:";

    private LiteflowConfig liteflowConfig;

    public FlowExecutor() {
        //设置FlowExecutor的Holder，虽然大部分地方都可以通过Spring上下文获取到，但放入Holder，还是为了某些地方能方便的取到
        FlowExecutorHolder.setHolder(this);
        //初始化DataBus
        DataBus.init();
    }

    public FlowExecutor(LiteflowConfig liteflowConfig) {
        this.liteflowConfig = liteflowConfig;
        //把liteFlowConfig设到LiteFlowGetter中去
        LiteflowConfigGetter.setLiteflowConfig(liteflowConfig);
        //设置FlowExecutor的Holder，虽然大部分地方都可以通过Spring上下文获取到，但放入Holder，还是为了某些地方能方便的取到
        FlowExecutorHolder.setHolder(this);
        if (BooleanUtil.isTrue(liteflowConfig.isParseOnStart())) {
            this.init();
        }
        //初始化DataBus
        DataBus.init();
    }

    /**
     * FlowExecutor的初始化化方式，主要用于parse规则文件
     */
    public void init() {
        if (ObjectUtil.isNull(liteflowConfig)) {
            throw new ConfigErrorException("config error, please check liteflow config property");
        }

        //在相应的环境下进行节点的初始化工作
        //在spring体系下会获得spring扫描后的节点，接入元数据
        //在非spring体系下是一个空实现，等于不做此步骤
        ContextCmpInitHolder.loadContextCmpInit().initCmp();

        //进行id生成器的初始化
        IdGeneratorHolder.init();

        //如果没有配置规则文件路径，就停止初始化。
        //规则文件路径不是一定要有，因为liteflow分基于规则和基于代码两种，有可能是动态代码构建的
        if (StrUtil.isBlank(liteflowConfig.getRuleSource())) {
            return;
        }

        List<String> sourceRulePathList = ListUtil.toList(liteflowConfig.getRuleSource().split(",|;"));

        FlowParser parser = null;
        Set<String> parserNameSet = new HashSet<>();
        List<String> rulePathList = new ArrayList<>();
        for (String path : sourceRulePathList) {
            try {
                // 查找对应的解析器
                parser = FlowParserProvider.lookup(path);
                parserNameSet.add(parser.getClass().getName());
                // 替换掉前缀标识（如：xml:/json:），保留剩下的完整地址
                path = ReUtil.replaceAll(path, PREFIX_FORMAT_CONFIG_REGEX, "");
                rulePathList.add(path);

                //支持多类型的配置文件，分别解析
                if (BooleanUtil.isTrue(liteflowConfig.isSupportMultipleType())) {
                    parser.parseMain(ListUtil.toList(path));
                }
            } catch (CyclicDependencyException e) {
                LOG.error(e.getMessage());
                throw e;
            } catch (Exception e) {
                String errorMsg = StrUtil.format("init flow executor cause error for path {},reason:{}", path, e.getMessage());
                LOG.error(e.getMessage(), e);
                throw new FlowExecutorNotInitException(errorMsg);
            }
        }

        //单类型的配置文件，需要一起解析
        if (BooleanUtil.isFalse(liteflowConfig.isSupportMultipleType())) {
            //检查Parser是否只有一个，因为多个不同的parser会造成子流程的混乱
            if (parserNameSet.size() > 1) {
                String errorMsg = "cannot have multiple different parsers";
                LOG.error(errorMsg);
                throw new MultipleParsersException(errorMsg);
            }

            //进行多个配置文件的一起解析
            try {
                if (parser != null) {
                    parser.parseMain(rulePathList);
                } else {
                    throw new ConfigErrorException("parse error, please check liteflow config property");
                }
            } catch (CyclicDependencyException e) {
                LOG.error(e.getMessage(), e);
                LOG.error(e.getMessage());
                throw e;
            } catch (ChainDuplicateException e) {
                LOG.error(e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                String errorMsg = StrUtil.format("init flow executor cause error for path {},reason: {}", rulePathList, e.getMessage());
                LOG.error(e.getMessage(), e);
                throw new FlowExecutorNotInitException(errorMsg);
            }
        }
    }

    //此方法就是从原有的配置源主动拉取新的进行刷新
    //和FlowBus.refreshFlowMetaData的区别就是一个为主动拉取，一个为被动监听到新的内容进行刷新
    public void reloadRule() {
        init();
    }

    //隐式流程的调用方法
    public void invoke(String chainId, Object param, Integer slotIndex) throws Exception {
        LiteflowResponse response = this.execute2Resp(chainId, param, null, slotIndex, true);
        if (!response.isSuccess()){
            throw response.getCause();
        }
    }

    public LiteflowResponse invoke2Resp(String chainId, Object param, Integer slotIndex) {
        return this.execute2Resp(chainId, param, null, slotIndex, true);
    }

    //单独调用某一个node
    public void invoke(String nodeId, Integer slotIndex) throws Exception {
        Node node = FlowBus.getNode(nodeId);
        node.execute(slotIndex);
    }

    //调用一个流程并返回LiteflowResponse，上下文为默认的DefaultContext，初始参数为null
    public LiteflowResponse execute2Resp(String chainId) {
        return this.execute2Resp(chainId, null, DefaultContext.class);
    }

    //调用一个流程并返回LiteflowResponse，上下文为默认的DefaultContext
    public LiteflowResponse execute2Resp(String chainId, Object param) {
        return this.execute2Resp(chainId, param, DefaultContext.class);
    }

    //调用一个流程并返回LiteflowResponse，允许多上下文的传入
    public LiteflowResponse execute2Resp(String chainId, Object param, Class<?>... contextBeanClazzArray) {
        return this.execute2Resp(chainId, param, contextBeanClazzArray, null, false);
    }

    //调用一个流程并返回Future<LiteflowResponse>，允许多上下文的传入
    public Future<LiteflowResponse> execute2Future(String chainId, Object param, Class<?>... contextBeanClazzArray) {
        return ExecutorHelper.loadInstance().buildMainExecutor(liteflowConfig.getMainExecutorClass()).submit(()
                -> FlowExecutorHolder.loadInstance().execute2Resp(chainId, param, contextBeanClazzArray, null, false));
    }

    //调用一个流程，返回默认的上下文，适用于简单的调用
    public DefaultContext execute(String chainId, Object param) throws Exception{
        LiteflowResponse response = this.execute2Resp(chainId, param, DefaultContext.class);
        if (!response.isSuccess()){
            throw response.getCause();
        }else{
            return response.getFirstContextBean();
        }
    }

    private LiteflowResponse execute2Resp(String chainId, Object param, Class<?>[] contextBeanClazzArray,
                                          Integer slotIndex, boolean isInnerChain) {
        LiteflowResponse response = new LiteflowResponse();

        Slot slot = doExecute(chainId, param, contextBeanClazzArray, slotIndex, isInnerChain);

        if (ObjectUtil.isNotNull(slot.getException())) {
            response.setSuccess(false);
            response.setMessage(slot.getException().getMessage());
            response.setCause(slot.getException());
        } else {
            response.setSuccess(true);
        }
        response.setSlot(slot);
        return response;
    }

    private Slot doExecute(String chainId, Object param, Class<?>[] contextBeanClazzArray, Integer slotIndex,
                           boolean isInnerChain) {
        if (FlowBus.needInit()) {
            init();
        }

        if (!isInnerChain && ObjectUtil.isNull(slotIndex)) {
            slotIndex = DataBus.offerSlot(ListUtil.toList(contextBeanClazzArray));
            if (BooleanUtil.isTrue(liteflowConfig.getPrintExecutionLog())) {
                LOG.info("slot[{}] offered", slotIndex);
            }
        }

        if (slotIndex == -1) {
            throw new NoAvailableSlotException("there is no available slot");
        }

        Slot slot = DataBus.getSlot(slotIndex);
        if (ObjectUtil.isNull(slot)) {
            throw new NoAvailableSlotException(StrUtil.format("the slot[{}] is not exist", slotIndex));
        }

        if (StrUtil.isBlank(slot.getRequestId())) {
            slot.generateRequestId();
            if (BooleanUtil.isTrue(liteflowConfig.getPrintExecutionLog())) {
                LOG.info("requestId[{}] has generated", slot.getRequestId());
            }
        }

        if (!isInnerChain) {
            if (ObjectUtil.isNotNull(param)) {
                slot.setRequestData(param);
            }
        } else {
            if (ObjectUtil.isNotNull(param)) {
                slot.setChainReqData(chainId, param);
            }
        }

        Chain chain = null;
        try {
            chain = FlowBus.getChain(chainId);

            if (ObjectUtil.isNull(chain)) {
                String errorMsg = StrUtil.format("[{}]:couldn't find chain with the id[{}]", slot.getRequestId(), chainId);
                throw new ChainNotFoundException(errorMsg);
            }
            // 执行chain
            chain.execute(slotIndex);
        } catch (ChainEndException e) {
            if (ObjectUtil.isNotNull(chain)) {
                String warnMsg = StrUtil.format("[{}]:chain[{}] execute end on slot[{}]", slot.getRequestId(), chain.getChainName(), slotIndex);
                LOG.warn(warnMsg);
            }
        } catch (Exception e) {
            if (ObjectUtil.isNotNull(chain)) {
                String errMsg = StrUtil.format("[{}]:chain[{}] execute error on slot[{}]", slot.getRequestId(), chain.getChainName(), slotIndex);
                LOG.error(errMsg, e);
            }else{
                LOG.error(e.getMessage(), e);
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
        //把liteFlowConfig设到LiteFlowGetter中去
        LiteflowConfigGetter.setLiteflowConfig(liteflowConfig);
    }
}