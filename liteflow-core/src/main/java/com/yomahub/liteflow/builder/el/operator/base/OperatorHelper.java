package com.yomahub.liteflow.builder.el.operator.base;

import cn.hutool.core.util.StrUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.DataNotFoundException;
import com.yomahub.liteflow.flow.element.Node;

import java.util.Objects;

/**
 * Operator 常用工具类
 *
 * @author gaibu
 * @since 2.8.6
 */
public class OperatorHelper {

    /**
     * 检查参数数量，不等于1
     *
     * @param objects objects
     * @throws QLException QLException
     */
    public static void checkObjectSizeNeqOne(Object[] objects) throws QLException {
        checkObjectSizeNeq(objects, 1);
    }

    /**
     * 检查参数数量，不等于 size
     *
     * @param objects objects
     * @param size    参数数量
     * @throws QLException QLException
     */
    public static void checkObjectSizeNeq(Object[] objects, int size) throws QLException {
        checkObjectSizeGtZero(objects);
        if (objects.length != size) {
            throw new QLException("parameter error");
        }
    }

    /**
     * 检查参数数量，大于 0
     *
     * @param objects objects
     * @throws QLException QLException
     */
    public static void checkObjectSizeGtZero(Object[] objects) throws QLException {
        if (objects.length == 0) {
            throw new QLException("parameter is empty");
        }
    }

    /**
     * 检查参数数量，大于 2
     *
     * @param objects objects
     * @throws QLException QLException
     */
    public static void checkObjectSizeGtTwo(Object[] objects) throws QLException {
        checkObjectSizeGtZero(objects);
        if (objects.length <= 1) {
            throw new QLException("parameter error");
        }
    }

    /**
     * 检查参数数量，等于 2
     *
     * @param objects objects
     * @throws QLException QLException
     */
    public static void checkObjectSizeEqTwo(Object[] objects) throws QLException {
        checkObjectSizeEq(objects, 2);
    }

    /**
     * 检查参数数量，等于 3
     *
     * @param objects objects
     * @throws QLException QLException
     */
    public static void checkObjectSizeEqThree(Object[] objects) throws QLException {
        checkObjectSizeEq(objects, 3);
    }

    /**
     * 检查参数数量，等于 size
     *
     * @param objects objects
     * @param size    参数数量
     * @throws QLException QLException
     */
    public static void checkObjectSizeEq(Object[] objects, int size) throws QLException {
        checkObjectSizeGtZero(objects);

        if (objects.length != size) {
            throw new QLException("parameter error");
        }
    }

    /**
     * 检查参数数量，等于 size1 或 size2
     *
     * @param objects objects
     * @param size1   参数数量1
     * @param size2   参数数量2
     * @throws QLException QLException
     */
    public static void checkObjectSizeEq(Object[] objects, int size1, int size2) throws QLException {
        checkObjectSizeGtZero(objects);

        if (objects.length != size1 && objects.length != size2) {
            throw new QLException("parameter error");
        }
    }

    /**
     * 转换 object 为指定的类型
     * 如果是Node类型的则进行copy
     * 为什么要进行copy呢？因为原先的Node都是存放在FlowBus的NodeMap中的。有些属性在EL中不是全局的，属于当前这个chain的。
     * 所以要进行copy动作
     */
    public static <T> T convert(Object object, Class<T> clazz) throws QLException {
        String errorMsg = StrUtil.format("The parameter must be {} item", clazz.getSimpleName());
        return convert(object, clazz, errorMsg);
    }

    /**
     * 转换 object 为指定的类型,自定义错误信息
     * 如果是Node类型的则进行copy
     */
    public static <T> T convert(Object object, Class<T> clazz, String errorMsg) throws QLException {
        try {
            if (clazz.isAssignableFrom(object.getClass())) {
                if (clazz.equals(Node.class)) {
                    Node node = (Node) object;
                    return (T) node.copy();
                } else {
                    return (T) object;
                }
            }
        } catch (Exception e) {
            throw new QLException("An error occurred while copying an object");
        }

        throw new QLException(errorMsg);
    }

    /**
     * 检查 node 和 chain 是否已经注册
     *
     * @param objects objects
     * @throws QLException QLException
     */
    public static void checkNodeAndChainExist(Object[] objects) throws QLException {
        for (Object object : objects) {
            if (Objects.isNull(object)) {
                throw new QLException(DataNotFoundException.MSG);
            }
        }
    }
}
