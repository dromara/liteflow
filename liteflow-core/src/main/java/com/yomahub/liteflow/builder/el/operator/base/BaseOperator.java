package com.yomahub.liteflow.builder.el.operator.base;

import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;

/**
 * BaseOperator 为了强化 executeInner 方法，会捕获抛出的 QLException 错误，输出友好的错误提示
 *
 * @author gaibu
 * @date 2022/8/28 14:32
 */
public abstract class BaseOperator extends Operator {

    @Override
    public Object executeInner(Object[] objects) throws Exception {
        try {
            // 检查 node 和 chain 是否已经注册
            OperatorHelper.checkNodeAndChainExist(objects);
            return buildCondition(objects);
        } catch (QLException e) {
            throw e;
        } catch (Exception e) {
            throw new ELParseException("errors occurred in EL parsing");
        }
    }

    /**
     * 构建 EL 条件
     *
     * @param objects objects
     * @return Condition
     * @throws Exception Exception
     */
    public abstract Object buildCondition(Object[] objects) throws Exception;
}
