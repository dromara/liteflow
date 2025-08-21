package com.yomahub.liteflow.util;

import com.ql.util.express.ExpressRunner;
import com.yomahub.liteflow.builder.el.operator.*;
import com.yomahub.liteflow.common.ChainConstant;

/**
 * EL 工具类
 *
 * @author tangkc
 * @since 2.13.2
 */
public class QlExpressUtils {

    /**
     * EL解析引擎
     */
    private final static ExpressRunner EXPRESS_RUNNER = new ExpressRunner();

    static {
        // 初始化QLExpress的Runner
        EXPRESS_RUNNER.addFunction(ChainConstant.THEN, new ThenOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.WHEN, new WhenOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.SER, new ThenOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.PAR, new WhenOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.SWITCH, new SwitchOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.PRE, new PreOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.FINALLY, new FinallyOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.IF, new IfOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.NODE.toUpperCase(), new NodeOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.NODE, new NodeOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.FOR, new ForOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.WHILE, new WhileOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.ITERATOR, new IteratorOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.CATCH, new CatchOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.AND, new AndOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.OR, new OrOperator());
        EXPRESS_RUNNER.addFunction(ChainConstant.NOT, new NotOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ELSE, Object.class, new ElseOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ELIF, Object.class, new ElifOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.TO, Object.class, new ToOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.TO.toLowerCase(), Object.class, new ToOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.DEFAULT, Object.class, new DefaultOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.TAG, Object.class, new TagOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ANY, Object.class, new AnyOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.MUST, Object.class, new MustOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.PERCENTAGE, Object.class, new PercentageOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ID, Object.class, new IdOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.IGNORE_ERROR, Object.class, new IgnoreErrorOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.THREAD_POOL, Object.class, new ThreadPoolOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.DO, Object.class, new DoOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.BREAK, Object.class, new BreakOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.DATA, Object.class, new DataOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.MAX_WAIT_SECONDS, Object.class, new MaxWaitSecondsOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.MAX_WAIT_MILLISECONDS, Object.class, new MaxWaitMillisecondsOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.PARALLEL, Object.class, new ParallelOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.RETRY, Object.class, new RetryOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.BIND, Object.class, new BindOperator());

    }

    /**
     * 获取QLExpress的实例
     */
    public static ExpressRunner getInstance() {
        return EXPRESS_RUNNER;
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
