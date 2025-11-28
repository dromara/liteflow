package com.yomahub.liteflow.util;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.security.QLSecurityStrategy;
import com.yomahub.liteflow.builder.el.operator.*;
import com.yomahub.liteflow.common.ChainConstant;

/**
 * EL 工具类
 *
 * @author tangkc
 * @since 2.15.1
 */
public class QlExpressUtils {

    /**
     * EL解析引擎
     */
    private final static Express4Runner EXPRESS_RUNNER = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    /**
     * 上下文搜索解析引擎
     */
    private final static Express4Runner CONTEXT_SEARCH_EXPRESS_RUNNER = new Express4Runner(InitOptions.builder().securityStrategy(QLSecurityStrategy.open()).build());

    static {
        // 初始化QLExpress的Runner
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.THEN, new ThenOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.WHEN, new WhenOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.SER, new ThenOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.PAR, new WhenOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.SWITCH, new SwitchOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.PRE, new PreOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.FINALLY, new FinallyOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.IF, new IfOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.NODE.toUpperCase(), new NodeOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.NODE, new NodeOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.FOR, new ForOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.WHILE, new WhileOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.ITERATOR, new IteratorOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.CATCH, new CatchOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.AND, new AndOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.OR, new OrOperator());
        EXPRESS_RUNNER.addVarArgsFunction(ChainConstant.NOT, new NotOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.ELSE, Object.class, new ElseOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.ELIF, Object.class, new ElifOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.TO, Object.class, new ToOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.TO.toLowerCase(), Object.class, new ToOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.DEFAULT, Object.class, new DefaultOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.TAG, Object.class, new TagOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.ANY, Object.class, new AnyOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.MUST, Object.class, new MustOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.PERCENTAGE, Object.class, new PercentageOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.ID, Object.class, new IdOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.IGNORE_ERROR, Object.class, new IgnoreErrorOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.THREAD_POOL, Object.class, new ThreadPoolOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.DO, Object.class, new DoOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.BREAK, Object.class, new BreakOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.DATA, Object.class, new DataOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.MAX_WAIT_SECONDS, Object.class, new MaxWaitSecondsOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.MAX_WAIT_MILLISECONDS, Object.class, new MaxWaitMillisecondsOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.PARALLEL, Object.class, new ParallelOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.RETRY, Object.class, new RetryOperator());
        EXPRESS_RUNNER.addExtendFunction(ChainConstant.BIND, Object.class, new BindOperator());

    }

    /**
     * 获取QLExpress的实例
     */
    public static Express4Runner getELExpressRunner() {
        return EXPRESS_RUNNER;
    }

    /**
     * 获取上下文搜索的QLExpress实例
     */
    public static Express4Runner getContextSearchExpressRunner() {
        return CONTEXT_SEARCH_EXPRESS_RUNNER;
    }

    /**
     * 检查变量名是否符合 变量命名规则
     *
     * @param variableName 变量名
     * @return 如果符合规范返回 true，否则返回 false
     */
    public static boolean checkVariableName(String variableName) {
        if (variableName == null || variableName.isEmpty()) {
            return false;
        }

        // 首字符必须是合法的 Java 标识符起始字符
        if (!Character.isJavaIdentifierStart(variableName.charAt(0))) {
            return false;
        }

        // 后续字符必须是合法的 Java 标识符部分
        for (int i = 1; i < variableName.length(); i++) {
            if (!Character.isJavaIdentifierPart(variableName.charAt(i))) {
                return false;
            }
        }

        return true;
    }
}
