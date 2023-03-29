package com.yomahub.liteflow.builder.el.operator.base;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.DataNotFoundException;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.AndOrCondition;
import com.yomahub.liteflow.flow.element.condition.NotCondition;

import java.util.Objects;

/**
 * Operator 常用工具类
 *
 * @author gaibu
 * @since 2.8.6
 */
public class OperatorHelper {

	/**
	 * 检查参数数量，大于 0
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
	 * 检查参数数量，等于 1
	 * @param objects objects
	 * @throws QLException QLException
	 */
	public static void checkObjectSizeEqOne(Object[] objects) throws QLException {
		checkObjectSizeEq(objects, 1);
	}

	/**
	 * 检查参数数量，等于 2
	 * @param objects objects
	 * @throws QLException QLException
	 */
	public static void checkObjectSizeEqTwo(Object[] objects) throws QLException {
		checkObjectSizeEq(objects, 2);
	}

	/**
	 * 检查参数数量，等于 3
	 * @param objects objects
	 * @throws QLException QLException
	 */
	public static void checkObjectSizeEqThree(Object[] objects) throws QLException {
		checkObjectSizeEq(objects, 3);
	}

	/**
	 * 检查参数数量，等于 size
	 * @param objects objects
	 * @param size 参数数量
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
	 * @param objects objects
	 * @param size1 参数数量1
	 * @param size2 参数数量2
	 * @throws QLException QLException
	 */
	public static void checkObjectSizeEq(Object[] objects, int size1, int size2) throws QLException {
		checkObjectSizeGtZero(objects);

		if (objects.length != size1 && objects.length != size2) {
			throw new QLException("parameter error");
		}
	}

	/**
	 * 转换 object 为指定的类型 如果是Node类型的则进行copy
	 * 为什么要进行copy呢？因为原先的Node都是存放在FlowBus的NodeMap中的。有些属性在EL中不是全局的，属于当前这个chain的。 所以要进行copy动作
	 */
	public static <T> T convert(Object object, Class<T> clazz) throws QLException {
		String errorMsg = StrUtil.format("The parameter must be {} item", clazz.getSimpleName());
		return convert(object, clazz, errorMsg);
	}

	/**
	 * 转换 object 为指定的类型,自定义错误信息 如果是Node类型的则进行copy
	 */
	public static <T> T convert(Object object, Class<T> clazz, String errorMsg) throws QLException {
		try {
			if (clazz.isAssignableFrom(object.getClass())) {
				if (clazz.equals(Node.class)) {
					Node node = (Node) object;
					return (T) node.copy();
				}
				else {
					return (T) object;
				}
			}
		}
		catch (Exception e) {
			throw new QLException("An error occurred while copying an object");
		}

		throw new QLException(errorMsg);
	}

	public static void checkItemNotNull(Object[] objects) throws QLException {
		for (Object object : objects) {
			if (Objects.isNull(object)) {
				throw new QLException(DataNotFoundException.MSG);
			}
		}
	}

	/**
	 * 所谓Boolean item，指的是那些最终的结果值为布尔类型的Item
	 * 布尔类型的items有，if，while，break类型的Node，以及AndOrCondition以及NotCondition
	 */
	public static void checkObjectMustBeBooleanItem(Object object) throws Exception{
		if (!(object instanceof Node && ListUtil.toList(
				NodeTypeEnum.IF, NodeTypeEnum.IF_SCRIPT,
				NodeTypeEnum.WHILE, NodeTypeEnum.WHILE_SCRIPT,
				NodeTypeEnum.BREAK, NodeTypeEnum.BREAK_SCRIPT).contains(((Node) object).getType())
				|| object instanceof AndOrCondition || object instanceof NotCondition)) {
			throw new QLException("The first parameter must be boolean type Node or boolean type condition");
		}
	}
}
