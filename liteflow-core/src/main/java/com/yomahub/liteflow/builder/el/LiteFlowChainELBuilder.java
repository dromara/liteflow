package com.yomahub.liteflow.builder.el;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.*;
import com.yomahub.liteflow.exception.DataNofFoundException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.*;

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

    private Chain chain;

    //这是主体的Condition，不包含前置和后置
    //声明这个变量，而不是用chain.getConditionList的目的，是为了辅助平滑加载
    //虽然FlowBus里面的map都是CopyOnWrite类型的，但是在buildCondition的时候，为了平滑加载，所以不能事先把chain.getConditionList给设为空List
    //所以在这里做一个缓存，等conditionList全部build完毕后，再去一次性替换chain里面的conditionList
    private final List<Condition> conditionList;

    //前置处理Condition，用来区别主体的Condition
    private final List<Condition> preConditionList;

    //后置处理Condition，用来区别主体的Condition
    private final List<Condition> finallyConditionList;

    //EL解析引擎
    private final static ExpressRunner EXPRESS_RUNNER = new ExpressRunner();

    static {
        //初始化QLExpress的Runner
        EXPRESS_RUNNER.addFunction("THEN", new ThenOperator());
        EXPRESS_RUNNER.addFunction("WHEN", new WhenOperator());
        EXPRESS_RUNNER.addFunction("SWITCH", new SwitchOperator());
        EXPRESS_RUNNER.addFunction("PRE", new PreOperator());
        EXPRESS_RUNNER.addFunction("FINALLY", new FinallyOperator());
        EXPRESS_RUNNER.addFunction("IF", new IfOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("ELSE", Object.class, new ElseOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("ELIF", Object.class, new ElifOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("TO", Object.class, new ToOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("to", Object.class, new ToOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("tag", Object.class, new TagOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("any", Object.class, new AnyOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("id", Object.class, new IdOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("ignoreError", Object.class, new IgnoreErrorOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("threadPool", Object.class, new ThreadPoolOperator());
        EXPRESS_RUNNER.addFunction("NODE", new NodeOperator());
        EXPRESS_RUNNER.addFunction("node", new NodeOperator());
        EXPRESS_RUNNER.addFunction("FOR", new ForOperator());
        EXPRESS_RUNNER.addFunction("WHILE", new WhileOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("DO", Object.class, new DoOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod("BREAK", Object.class, new BreakOperator());
    }

    public static LiteFlowChainELBuilder createChain() {
        return new LiteFlowChainELBuilder();
    }

    public LiteFlowChainELBuilder() {
        chain = new Chain();
        conditionList = new ArrayList<>();
        preConditionList = new ArrayList<>();
        finallyConditionList = new ArrayList<>();
    }

    //在parser中chain的build是2段式的，因为涉及到依赖问题，以前是递归parser
    //2.6.8之后取消了递归的模式，两段式组装，先把带有chainName的chain对象放进去，第二段再组装chain里面的condition
    //所以这里setChainName的时候需要判断下
    public LiteFlowChainELBuilder setChainName(String chainName) {
        if (FlowBus.containChain(chainName)) {
            this.chain = FlowBus.getChain(chainName);
        } else {
            this.chain.setChainName(chainName);
        }
        return this;
    }

    public LiteFlowChainELBuilder setEL(String elStr) {
        if (StrUtil.isBlank(elStr)) {
            String errMsg = StrUtil.format("no conditionList in this chain[{}]", chain.getChainName());
            throw new FlowSystemException(errMsg);
        }

        List<String> errorList = new ArrayList<>();
        try {
            DefaultContext<String, Object> context = new DefaultContext<>();

            //这里一定要先放chain，再放node，因为node优先于chain，所以当重名时，node会覆盖掉chain
            //往上下文里放入所有的chain，是的el表达式可以直接引用到chain
            FlowBus.getChainMap().values().forEach(chain -> context.put(chain.getChainName(), chain));

            //往上下文里放入所有的node，使得el表达式可以直接引用到nodeId
            FlowBus.getNodeMap().keySet().forEach(nodeId -> context.put(nodeId, FlowBus.getNode(nodeId)));

            //解析el成为一个Condition
            //为什么这里只是一个Condition，而不是一个List<Condition>呢
            //这里无论多复杂的，外面必定有一个最外层的Condition，所以这里只有一个，内部可以嵌套很多层，这点和以前的不太一样
            Condition condition = (Condition) EXPRESS_RUNNER.execute(elStr, context, errorList, true, true);

            //从condition的第一层嵌套结构里拿出Pre和Finally节点
            //为什么只寻找第一层，而不往下寻找了呢？
            //因为这是一个规范，如果在后面的层级中出现pre和finally，语义上也不好理解，所以pre和finally只能定义在第一层
            //如果硬是要在后面定义，则执行的时候会忽略，相关代码已做了判断
            for (Executable executable : condition.getExecutableList()) {
                if (executable instanceof PreCondition) {
                    this.preConditionList.add((PreCondition) executable);
                } else if (executable instanceof FinallyCondition) {
                    this.finallyConditionList.add((FinallyCondition) executable);
                }
            }

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

    public void build() {
        this.chain.setConditionList(this.conditionList);
        this.chain.setPreConditionList(this.preConditionList);
        this.chain.setFinallyConditionList(this.finallyConditionList);

        checkBuild();

        FlowBus.addChain(this.chain);
    }

    /**
     * build 前简单校验
     */
    private void checkBuild() {
        List<String> errorList = new ArrayList<>();
        if (StrUtil.isBlank(this.chain.getChainName())) {
            errorList.add("name is blank");
        }
        if (CollUtil.isNotEmpty(errorList)) {
            throw new RuntimeException(CollUtil.join(errorList, ",", "[", "]"));
        }
    }
}
