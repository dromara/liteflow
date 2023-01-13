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
import cn.hutool.core.util.*;
import com.yomahub.liteflow.enums.InnerChainTypeEnum;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.id.IdGeneratorHolder;
import com.yomahub.liteflow.parser.base.FlowParser;
import com.yomahub.liteflow.parser.factory.FlowParserProvider;
import com.yomahub.liteflow.parser.spi.ParserClassNameSpi;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.holder.ContextCmpInitHolder;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
            this.init(true);
        }
        //初始化DataBus
        DataBus.init();
    }

    /**
     * FlowExecutor的初始化化方式，主要用于parse规则文件
     */
    public void init(boolean hook) {
        if (ObjectUtil.isNull(liteflowConfig)) {
            throw new ConfigErrorException("config error, please check liteflow config property");
        }

        //在相应的环境下进行节点的初始化工作
        //在spring体系下会获得spring扫描后的节点，接入元数据
        //在非spring体系下是一个空实现，等于不做此步骤
        ContextCmpInitHolder.loadContextCmpInit().initCmp();

        //进行id生成器的初始化
        IdGeneratorHolder.init();

        String ruleSource = liteflowConfig.getRuleSource();
        if (StrUtil.isBlank(ruleSource)) {
            //查看有没有Parser的SPI实现
            //所有的Parser的SPI实现都是以custom形式放入的，且只支持xml形式
            ServiceLoader<ParserClassNameSpi> loader = ServiceLoader.load(ParserClassNameSpi.class);
            Iterator<ParserClassNameSpi> it = loader.iterator();
            if (it.hasNext()){
                ParserClassNameSpi parserClassNameSpi = it.next();
                liteflowConfig.setRuleSource("el_xml:" + parserClassNameSpi.getSpiClassName());
            }else{
                //ruleSource为空，而且没有spi形式的扩展，那么说明真的没有ruleSource
                //这种情况有可能是基于代码动态构建的
                return;
            }
        }

        //如果有前缀的，则不需要再进行分割了，说明是一个整体
        //如果没有前缀，说明是本地文件，可能配置多个，所以需要分割
        List<String> sourceRulePathList;
        if (ReUtil.contains(PREFIX_FORMAT_CONFIG_REGEX, ruleSource)) {
            sourceRulePathList = ListUtil.toList(ruleSource);
        } else {
            String afterHandleRuleSource = ruleSource.replace(StrUtil.SPACE, StrUtil.EMPTY);
            sourceRulePathList = ListUtil.toList(afterHandleRuleSource.split(",|;"));
        }

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

        //如果是ruleSource方式的，最后判断下有没有解析出来,如果没有解析出来则报错
        if (FlowBus.getChainMap().isEmpty()){
            String errMsg = StrUtil.format("no valid rule config found in rule path [{}]", liteflowConfig.getRuleSource());
            throw new ConfigErrorException(errMsg);
        }

        //执行钩子
        if(hook){
            FlowInitHook.executeHook();
        }
    }

    //此方法就是从原有的配置源主动拉取新的进行刷新
    //和FlowBus.refreshFlowMetaData的区别就是一个为主动拉取，一个为被动监听到新的内容进行刷新
    public void reloadRule() {
        init(false);
    }

    //隐式流程的调用方法
    @Deprecated
    public void invoke(String chainId, Object param, Integer slotIndex) throws Exception {
        LiteflowResponse response = this.invoke2Resp(chainId, param, slotIndex, InnerChainTypeEnum.IN_SYNC);
        if (!response.isSuccess()){
            throw response.getCause();
        }
    }

    @Deprecated
    public void invokeInAsync(String chainId, Object param, Integer slotIndex) throws Exception {
        LiteflowResponse response = this.invoke2Resp(chainId, param, slotIndex, InnerChainTypeEnum.IN_ASYNC);
        if (!response.isSuccess()){
            throw response.getCause();
        }
    }

    public LiteflowResponse invoke2Resp(String chainId, Object param, Integer slotIndex) {
        return this.invoke2Resp(chainId, param, slotIndex, InnerChainTypeEnum.IN_SYNC);
    }

    public LiteflowResponse invoke2RespInAsync(String chainId, Object param, Integer slotIndex) {
        return this.invoke2Resp(chainId, param, slotIndex, InnerChainTypeEnum.IN_ASYNC);
    }

    //单独调用某一个node
    @Deprecated
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
        return this.execute2Resp(chainId, param, contextBeanClazzArray, null);
    }

    public LiteflowResponse execute2Resp(String chainId, Object param, Object... contextBeanArray) {
        return this.execute2Resp(chainId, param, null, contextBeanArray);
    }

    //调用一个流程并返回Future<LiteflowResponse>，允许多上下文的传入
    public Future<LiteflowResponse> execute2Future(String chainId, Object param, Class<?>... contextBeanClazzArray) {
        return ExecutorHelper.loadInstance().buildMainExecutor(liteflowConfig.getMainExecutorClass()).submit(()
                -> FlowExecutorHolder.loadInstance().execute2Resp(chainId, param, contextBeanClazzArray,null));
    }


    public Future<LiteflowResponse> execute2Future(String chainId, Object param, Object... contextBeanArray) {
        return ExecutorHelper.loadInstance().buildMainExecutor(liteflowConfig.getMainExecutorClass()).submit(()
                -> FlowExecutorHolder.loadInstance().execute2Resp(chainId, param, null, contextBeanArray));
    }

    //调用一个流程，返回默认的上下文，适用于简单的调用
    @Deprecated
    public DefaultContext execute(String chainId, Object param) throws Exception{
        LiteflowResponse response = this.execute2Resp(chainId, param, DefaultContext.class);
        if (!response.isSuccess()){
            throw response.getCause();
        }else{
            return response.getFirstContextBean();
        }
    }

    private LiteflowResponse execute2Resp(String chainId,
                                          Object param,
                                          Class<?>[] contextBeanClazzArray,
                                          Object[] contextBeanArray) {
        Slot slot = doExecute(chainId, param, contextBeanClazzArray, contextBeanArray, null, InnerChainTypeEnum.NONE);
        return LiteflowResponse.newMainResponse(slot);
    }

    private LiteflowResponse invoke2Resp(String chainId,
                                          Object param,
                                          Integer slotIndex, InnerChainTypeEnum innerChainType) {
        Slot slot = doExecute(chainId, param, null, null, slotIndex, innerChainType);
        return LiteflowResponse.newInnerResponse(chainId, slot);
    }

    private Slot doExecute(String chainId,
                           Object param,
                           Class<?>[] contextBeanClazzArray,
                           Object[] contextBeanArray,
                           Integer slotIndex,
                           InnerChainTypeEnum innerChainType) {
        if (FlowBus.needInit()) {
            init(true);
        }

        //如果不是隐式流程，那么需要分配Slot
        if (innerChainType.equals(InnerChainTypeEnum.NONE) && ObjectUtil.isNull(slotIndex)) {
            //这里可以根据class分配，也可以根据bean去分配
            if (ArrayUtil.isNotEmpty(contextBeanClazzArray)){
                slotIndex = DataBus.offerSlotByClass(ListUtil.toList(contextBeanClazzArray));
            }else{
                slotIndex = DataBus.offerSlotByBean(ListUtil.toList(contextBeanArray));
            }
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

        //如果是隐式流程，事先把subException给置空，然后把隐式流程的chainId放入slot元数据中
        //我知道这在多线程调用隐式流程中会有问题。但是考虑到这种场景的不会多，也有其他的转换方式。
        //所以暂且这么做，以后再优化
        if (!innerChainType.equals(InnerChainTypeEnum.NONE)){
            slot.removeSubException(chainId);
            slot.addSubChain(chainId);
        }

        if (StrUtil.isBlank(slot.getRequestId())) {
            slot.generateRequestId();
            if (BooleanUtil.isTrue(liteflowConfig.getPrintExecutionLog())) {
                LOG.info("requestId[{}] has generated", slot.getRequestId());
            }
        }

        if (ObjectUtil.isNotNull(param)) {
            if (innerChainType.equals(InnerChainTypeEnum.NONE)) {
                slot.setRequestData(param);
            } else if(innerChainType.equals(InnerChainTypeEnum.IN_SYNC)){
                slot.setChainReqData(chainId, param);
            } else if(innerChainType.equals(InnerChainTypeEnum.IN_ASYNC)){
                slot.setChainReqData2Queue(chainId, param);
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
                if (BooleanUtil.isTrue(liteflowConfig.getPrintExecutionLog())){
                    LOG.error(errMsg, e);
                }else{
                    LOG.error(errMsg);
                }
            }else{
                if (BooleanUtil.isTrue(liteflowConfig.getPrintExecutionLog())){
                    LOG.error(e.getMessage(), e);
                }else{
                    LOG.error(e.getMessage());
                }
            }

            //如果是正常流程需要把异常设置到slot的exception属性里
            //如果是隐式流程，则需要设置到隐式流程的exception属性里
            if (innerChainType.equals(InnerChainTypeEnum.NONE)) {
                slot.setException(e);
            }else{
                slot.setSubException(chainId, e);
            }
        } finally {
            if (innerChainType.equals(InnerChainTypeEnum.NONE)) {
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