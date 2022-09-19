package com.yomahub.liteflow.builder.el.operator.base;

import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.DataNofFoundException;

import java.util.Objects;

/**
 * Operator 常用工具类
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
     *
     * @param object object
     * @param tClass 指定类型
     * @param <T>    返回类型
     * @return T
     * @throws QLException QLException
     */
    public static <T> T convert(Object object, Class<T> tClass) throws QLException {
        if (tClass.isInstance(object)) {
            return (T) object;
        }

        throw new QLException("The caller must be " + tClass.getName() + " item");
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
                throw new QLException(DataNofFoundException.MSG);
            }
        }
    }
}
