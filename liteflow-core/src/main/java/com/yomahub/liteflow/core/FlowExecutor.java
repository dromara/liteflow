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
import com.google.common.collect.Lists;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.parser.FlowParser;
import com.yomahub.liteflow.parser.factory.FlowParserProvider;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.slot.Slot;
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

    private static final String PREFIX_FORMATE_CONFIG_REGEX = "xml:|json:|yml:";

    private LiteflowConfig liteflowConfig;

    public FlowExecutor() {
        //设置FlowExecutor的Holder，虽然大部分地方都可以通过Spring上下文获取到，但放入Holder，还是为了某些地方能方便的取到
        FlowExecutorHolder.setHolder(this);
        //初始化DataBus
        DataBus.init();
    }

    public FlowExecutor(LiteflowConfig liteflowConfig){
        this.liteflowConfig = liteflowConfig;
        //把liteFlowConfig设到LiteFlowGetter中去
        LiteflowConfigGetter.setLiteflowConfig(liteflowConfig);
        //设置FlowExecutor的Holder，虽然大部分地方都可以通过Spring上下文获取到，但放入Holder，还是为了某些地方能方便的取到
        FlowExecutorHolder.setHolder(this);
        if (liteflowConfig.isParseOnStart()){
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

        if (StrUtil.isBlank(liteflowConfig.getRuleSource())){
            return;
        }

        // 获取配置文件地址，','或者';'分割
        List<String> sourceRulePathList = Lists.newArrayList(liteflowConfig.getRuleSource().split("[,;]"));

        FlowParser parser = null;
        Set<String> parserNameSet = new HashSet<>();
        List<String> rulePathList = new ArrayList<>();
        for (String path : sourceRulePathList) {
            try {
                // 查找对应的解析器
                parser = FlowParserProvider.lookup(path);
                parserNameSet.add(parser.getClass().getName());
                // 替换掉zk配置的前缀
                path = ReUtil.replaceAll(path, PREFIX_FORMATE_CONFIG_REGEX, "");
                rulePathList.add(path);

                //支持多类型的配置文件，分别解析
                if (liteflowConfig.isSupportMultipleType()) {
                    parser.parseMain(ListUtil.toList(path));
                }
            } catch (CyclicDependencyException e){
                LOG.error(e.getMessage());
                throw e;
            } catch (Exception e) {
                String errorMsg = StrUtil.format("init flow executor cause error for path {},reason:{}", path, e.getMessage());
                LOG.error(e.getMessage(), e);
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
        this.execute(chainId, param, null, slotIndex, true);
    }

    public <T> LiteflowResponse<T> invoke2Resp(String chainId, Object param, Integer slotIndex){
        return this.execute2Resp(chainId, param, null, slotIndex, true);
    }

    //单独调用某一个node
    public void invoke(String nodeId, Integer slotIndex) throws Exception {
        Node node = FlowBus.getNode(nodeId);
        node.execute(slotIndex);
    }

    public DefaultContext execute(String chainId) throws Exception {
        return this.execute(chainId, null, DefaultContext.class, null, false);
    }

    public DefaultContext execute(String chainId, Object param) throws Exception {
        return this.execute(chainId, param, DefaultContext.class, null, false);
    }

    public <T> T execute(String chainId, Object param, Class<T> contextBeanClazz) throws Exception {
        return this.execute(chainId, param, contextBeanClazz, null, false);
    }

    private <T> T execute(String chainId, Object param, Class<T> contextBeanClazz,
                                      Integer slotIndex, boolean isInnerChain) throws Exception {
        Slot<T> slot = this.doExecute(chainId, param, contextBeanClazz, slotIndex, isInnerChain);
        if (ObjectUtil.isNotNull(slot.getException())) {
            throw slot.getException();
        } else {
            return slot.getContextBean();
        }
    }

    public LiteflowResponse<DefaultContext> execute2Resp(String chainId) {
        return this.execute2Resp(chainId, null, DefaultContext.class);
    }

    public LiteflowResponse<DefaultContext> execute2Resp(String chainId, Object param) {
        return this.execute2Resp(chainId, param, DefaultContext.class);
    }

    public <T> LiteflowResponse<T> execute2Resp(String chainId, Object param, Class<T> contextBeanClazz) {
        return this.execute2Resp(chainId, param, contextBeanClazz, null, false);
    }

    public <T> Future<LiteflowResponse<T>> execute2Future(String chainId, Object param, Class<T> contextBeanClazz) {
        return ExecutorHelper.loadInstance().buildMainExecutor(liteflowConfig.getMainExecutorClass()).submit(()
                -> FlowExecutorHolder.loadInstance().execute2Resp(chainId, param, contextBeanClazz, null, false));

    }

    public <T> LiteflowResponse<T> execute2Resp(String chainId, Object param, Class<T> contextBeanClazz,
                                                Integer slotIndex, boolean isInnerChain) {
        LiteflowResponse<T> response = new LiteflowResponse<>();

        Slot<T> slot = doExecute(chainId, param, contextBeanClazz, slotIndex, isInnerChain);

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

    private <T> Slot<T> doExecute(String chainId, Object param, Class<T> contextBeanClazz, Integer slotIndex,
                                         boolean isInnerChain) {
        if (FlowBus.needInit()) {
            init();
        }

        if (!isInnerChain && ObjectUtil.isNull(slotIndex)) {
            slotIndex = DataBus.offerSlot(contextBeanClazz);
            if (BooleanUtil.isTrue(liteflowConfig.getPrintExecutionLog())){
                LOG.info("slot[{}] offered", slotIndex);
            }
        }

        if (slotIndex == -1) {
            throw new NoAvailableSlotException("there is no available slot");
        }

        Slot<T> slot = DataBus.getSlot(slotIndex);
        if (ObjectUtil.isNull(slot)) {
            throw new NoAvailableSlotException(StrUtil.format("the slot[{}] is not exist", slotIndex));
        }

        if (StrUtil.isBlank(slot.getRequestId())) {
            slot.generateRequestId();
            if (BooleanUtil.isTrue(liteflowConfig.getPrintExecutionLog())){
                LOG.info("requestId[{}] has generated", slot.getRequestId());
            }
        }

        if (!isInnerChain) {
            if (ObjectUtil.isNotNull(param)){
                slot.setRequestData(param);
            }
        } else {
            if (ObjectUtil.isNotNull(param)){
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
            // 执行前置
            chain.executePre(slotIndex);
            // 执行chain
            chain.execute(slotIndex);
        } catch (ChainEndException e) {
            if (ObjectUtil.isNotNull(chain)){
                String warnMsg = StrUtil.format("[{}]:chain[{}] execute end on slot[{}]", slot.getRequestId(), chain.getChainName(), slotIndex);
                LOG.warn(warnMsg);
            }
        } catch (Exception e) {
            if (ObjectUtil.isNotNull(chain)){
                String errMsg = StrUtil.format("[{}]:chain[{}] execute error on slot[{}]", slot.getRequestId(), chain.getChainName(), slotIndex);
                LOG.error(errMsg, e);
            }
            slot.setException(e);
        } finally {
            try{
                if (ObjectUtil.isNotNull(chain)){
                    chain.executeFinally(slotIndex);
                }
            }catch (Exception e){
                String errMsg = StrUtil.format("[{}]:an exception occurred during the finally Component execution in chain[{}]", slot.getRequestId(), chain.getChainName());
                LOG.error(errMsg, e);
            }

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