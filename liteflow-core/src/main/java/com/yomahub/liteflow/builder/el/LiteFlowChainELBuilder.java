package com.yomahub.liteflow.builder.el;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.*;
import com.yomahub.liteflow.common.ChainConstant;
import com.yomahub.liteflow.exception.DataNofFoundException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.condition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Chain基于代码形式的组装器
 * EL表达式规则专属组装器
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class LiteFlowChainELBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LiteFlowChainELBuilder.class);


    private Chain chain;

    /**
     * //这是主体的Condition
     * //声明这个变量，而不是用chain.getConditionList的目的，是为了辅助平滑加载
     * //虽然FlowBus里面的map都是CopyOnWrite类型的，但是在buildCondition的时候，为了平滑加载，所以不能事先把chain.getConditionList给设为空List
     * //所以在这里做一个缓存，等conditionList全部build完毕后，再去一次性替换chain里面的conditionList
     */
    private final List<Condition> conditionList;

    /**
     * EL解析引擎
     */
    public final static ExpressRunner EXPRESS_RUNNER = new ExpressRunner();

    static {
        //初始化QLExpress的Runner
        EXPRESS_RUNNER.addFunction(ChainConstant.THEN, new ThenOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.WHEN, new WhenOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.SWITCH, new SwitchOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.PRE, new PreOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.FINALLY, new FinallyOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.IF, new IfOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ELSE, Object.class, new ElseOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ELIF, Object.class, new ElifOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.TO, Object.class, new ToOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.TO.toLowerCase(), Object.class, new ToOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.DEFAULT, Object.class, new DefaultOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.TAG, Object.class, new TagOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ANY, Object.class, new AnyOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ID, Object.class, new IdOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.IGNORE_ERROR, Object.class, new IgnoreErrorOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.THREAD_POOL, Object.class, new ThreadPoolOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.NODE.toUpperCase(), new NodeOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.NODE, new NodeOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.FOR, new ForOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.WHILE, new WhileOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.ITERATOR, new IteratorOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.DO, Object.class, new DoOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.BREAK, Object.class, new BreakOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.DATA, Object.class, new DataOperator());
    }

    public static LiteFlowChainELBuilder createChain() {
        return new LiteFlowChainELBuilder();
    }

    public LiteFlowChainELBuilder() {
        chain = new Chain();
        conditionList = new ArrayList<>();
    }

    //在parser中chain的build是2段式的，因为涉及到依赖问题，以前是递归parser
    //2.6.8之后取消了递归的模式，两段式组装，先把带有chainName的chain对象放进去，第二段再组装chain里面的condition
    //所以这里setChainName的时候需要判断下

    /**
     * @return LiteFlowChainELBuilder
     * @deprecated 请使用 {@link #setChainId(String)}
     */
    public LiteFlowChainELBuilder setChainName(String chainName) {
        if (FlowBus.containChain(chainName)) {
            this.chain = FlowBus.getChain(chainName);
        } else {
            this.chain.setChainName(chainName);
        }
        return this;
    }

    public LiteFlowChainELBuilder setChainId(String chainId) {
        if (FlowBus.containChain(chainId)) {
            this.chain = FlowBus.getChain(chainId);
        } else {
            this.chain.setChainId(chainId);
        }
        return this;
    }

    public LiteFlowChainELBuilder setEL(String elStr) {
        if (StrUtil.isBlank(elStr)) {
            String errMsg = StrUtil.format("no content in this chain[{}]", chain.getChainId());
            throw new FlowSystemException(errMsg);
        }

        List<String> errorList = new ArrayList<>();
        try {
            DefaultContext<String, Object> context = new DefaultContext<>();

            //这里一定要先放chain，再放node，因为node优先于chain，所以当重名时，node会覆盖掉chain
            //往上下文里放入所有的chain，是的el表达式可以直接引用到chain
            FlowBus.getChainMap().values().forEach(chain -> context.put(chain.getChainId(), chain));

            //往上下文里放入所有的node，使得el表达式可以直接引用到nodeId
            FlowBus.getNodeMap().keySet().forEach(nodeId -> context.put(nodeId, FlowBus.getNode(nodeId)));

            //放入当前主chain的ID
            context.put(ChainConstant.CURR_CHAIN_ID, this.chain.getChainId());

            //解析el成为一个Condition
            //为什么这里只是一个Condition，而不是一个List<Condition>呢
            //这里无论多复杂的，外面必定有一个最外层的Condition，所以这里只有一个，内部可以嵌套很多层，这点和以前的不太一样
            Condition condition = (Condition) EXPRESS_RUNNER.execute(elStr, context, errorList, true, true);

            //从condition的第一层嵌套结构里拿出Pre和Finally节点
            //为什么只寻找第一层，而不往下寻找了呢？
            //因为这是一个规范，如果在后面的层级中出现pre和finally，语义上也不好理解，所以pre和finally只能定义在第一层
            //如果硬是要在后面定义，则执行的时候会忽略，相关代码已做了判断
            /*for (Executable executable : condition.getExecutableList()) {
                if (executable instanceof PreCondition) {
                    this.preConditionList.add((PreCondition) executable);
                } else if (executable instanceof FinallyCondition) {
                    this.finallyConditionList.add((FinallyCondition) executable);
                }
            }*/

            //把主要的condition加入
            this.conditionList.add(condition);
            return this;
        } catch (QLException e) {
            // EL 底层会包装异常，这里是曲线处理
            if (Objects.equals(e.getCause().getMessage(), DataNofFoundException.MSG)) {
                throw new ELParseException(String.format("[node/chain is not exist or node/chain not register]elStr=%s", elStr));
            }
            throw new ELParseException(e.getCause().getMessage());
        } catch (Exception e) {
            throw new ELParseException(e.getMessage());
        }
    }

    /**
     * EL表达式校验
     * @param elStr EL表达式
     * @return true 校验成功 false 校验失败
     */
    public static boolean validate(String elStr) {
       try {
           LiteFlowChainELBuilder.createChain().setEL(elStr);
           return Boolean.TRUE;
       } catch (ELParseException e) {
           LOG.error(e.getMessage());
       }
       return Boolean.FALSE;
    }

    public void build() {
        this.chain.setConditionList(this.conditionList);

        checkBuild();

        FlowBus.addChain(this.chain);
    }

    /**
     * build 前简单校验
     */
    private void checkBuild() {
        List<String> errorList = new ArrayList<>();
        if (StrUtil.isBlank(this.chain.getChainId())) {
            errorList.add("name is blank");
        }
        if (CollUtil.isNotEmpty(errorList)) {
            throw new RuntimeException(CollUtil.join(errorList, ",", "[", "]"));
        }
    }
}
