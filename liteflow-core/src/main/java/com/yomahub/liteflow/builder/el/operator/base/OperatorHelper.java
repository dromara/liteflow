package com.yomahub.liteflow.builder.el.operator.base;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.enums.ExecuteableTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.DataNotFoundException;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
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
	 * 检查参数数量，大于等于 2
	 * @param objects objects
	 * @throws QLException QLException
	 */
	public static void checkObjectSizeGteTwo(Object[] objects) throws QLException {
		checkObjectSizeGtZero(objects);
		if (objects.length < 2) {
			throw new QLException("parameter size error");
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
			throw new QLException("parameter size error");
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
			throw new QLException("parameter size error");
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
	@SuppressWarnings("unchecked")
	public static <T> T convert(Object object, Class<T> clazz, String errorMsg) throws QLException {
		try {
			if (clazz.isAssignableFrom(object.getClass())) {
				if (object instanceof Node) {
					Node node = (Node) object;
					if (node.isCloned()){
						return (T) node;
					}else {
						return (T) node.clone();
					}
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
	 * 检查对象是否为一个正常可执行的对象。
	 * 大部分的Node,Condition,Chain都是正常可执行的对象，这个检查是为了避免THEN(sw,if)类的情况(sw是选择组件，if是条件组件)，这种就不能放在THEN里
	 */
	public static void checkObjMustBeCommonTypeItem(Object object) throws Exception{
		if (!(object instanceof Executable)){
			throw new QLException("The parameter must be Executable item.");
		}
		Executable item = (Executable) object;
		if (item.getExecuteType().equals(ExecuteableTypeEnum.NODE)){
			Node node = (Node) item;
			if (!ListUtil.toList(NodeTypeEnum.COMMON, NodeTypeEnum.SCRIPT, NodeTypeEnum.FALLBACK).contains(node.getType())){
				throw new QLException(StrUtil.format("The node[{}] must be a common type component", node.getId()));
			}
		}
	}

	/**
	 * 所谓Boolean item，指的是那些最终的结果值为布尔类型的Item
	 * 布尔类型的items有，if，while，break类型的Node，以及AndOrCondition以及NotCondition
	 * @param object 检查的对象
	 * @throws Exception 检查过程中抛的错
	 */
	public static void checkObjMustBeBooleanTypeItem(Object object) throws Exception{
		if (!(object instanceof Executable)){
			throw new QLException("The parameter must be Executable item.");
		}
		Executable item = (Executable) object;
		if (item.getExecuteType().equals(ExecuteableTypeEnum.NODE)){
			Node node = (Node) item;
			if (!ListUtil.toList(NodeTypeEnum.BOOLEAN,
					NodeTypeEnum.BOOLEAN_SCRIPT,
					NodeTypeEnum.FALLBACK).contains(node.getType())){
				throw new QLException(StrUtil.format("The node[{}] must be boolean type Node.", node.getId()));
			}
		}else if(item.getExecuteType().equals(ExecuteableTypeEnum.CONDITION)){
			Condition condition = (Condition) item;
			if (!ListUtil.toList(ConditionTypeEnum.TYPE_AND_OR_OPT, ConditionTypeEnum.TYPE_NOT_OPT).contains(condition.getConditionType())){
				throw new QLException(StrUtil.format("The condition[{}] must be boolean type Condition.", condition.getId()));
			}
		}else{
			throw new QLException("The parameter error.");
		}
	}

	public static void checkObjMustBeForTypeItem(Object object) throws Exception{
		if (!(object instanceof Executable)){
			throw new QLException("The parameter must be Executable item.");
		}
		Executable item = (Executable) object;
		if (item.getExecuteType().equals(ExecuteableTypeEnum.NODE)){
			Node node = (Node) item;
			if (!ListUtil.toList(NodeTypeEnum.FOR,
					NodeTypeEnum.FOR_SCRIPT,
					NodeTypeEnum.FALLBACK).contains(node.getType())){
				throw new QLException(StrUtil.format("The node[{}] must be For type Node.", node.getId()));
			}
		}else{
			throw new QLException("The parameter error.");
		}
	}

	public static void checkObjMustBeIteratorTypeItem(Object object) throws Exception{
		if (!(object instanceof Executable)){
			throw new QLException("The parameter must be Executable item.");
		}
		Executable item = (Executable) object;
		if (item.getExecuteType().equals(ExecuteableTypeEnum.NODE)){
			Node node = (Node) item;
			if (!ListUtil.toList(NodeTypeEnum.ITERATOR,
					NodeTypeEnum.FALLBACK).contains(node.getType())){
				throw new QLException(StrUtil.format("The node[{}] must be Iterator type Node.", node.getId()));
			}
		}else{
			throw new QLException("The parameter error.");
		}
	}

	public static void checkObjMustBeSwitchTypeItem(Object object) throws Exception{
		if (!(object instanceof Executable)){
			throw new QLException("The parameter must be Executable item.");
		}
		Executable item = (Executable) object;
		if (item.getExecuteType().equals(ExecuteableTypeEnum.NODE)){
			Node node = (Node) item;
			if (!ListUtil.toList(NodeTypeEnum.SWITCH,
					NodeTypeEnum.SWITCH_SCRIPT,
					NodeTypeEnum.FALLBACK).contains(node.getType())){
				throw new QLException(StrUtil.format("The node[{}] must be Switch type Node.", node.getId()));
			}
		}else{
			throw new QLException("The parameter error.");
		}
	}
}
