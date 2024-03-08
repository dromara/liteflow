package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.NoSwitchTargetNodeException;
import com.yomahub.liteflow.exception.SwitchTargetCannotBePreOrFinallyException;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;

import java.util.List;

/**
 * 选择Condition
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class SwitchCondition extends Condition {

	private final String TAG_PREFIX = "tag";

	private final String TAG_FLAG = ":";

	@Override
	public void executeCondition(Integer slotIndex) throws Exception {
		// 获取switch node
		Node switchNode = this.getSwitchNode();
		// 获取target List
		List<Executable> targetList = this.getTargetList();

		// 提前设置 chainId，避免无法在 isAccess 方法中获取到
		switchNode.setCurrChainId(this.getCurrChainId());

		// 先去判断isAccess方法，如果isAccess方法都返回false，整个SWITCH表达式不执行
		if (!switchNode.isAccess(slotIndex)) {
			return;
		}

		// 先执行switch节点
		switchNode.execute(slotIndex);

		// 拿到switch节点的结果
		String targetId = switchNode.getItemResultMetaValue(slotIndex);

		Slot slot = DataBus.getSlot(slotIndex);

		Executable targetExecutor = null;
		if (StrUtil.isNotBlank(targetId)) {
			// 这里要判断是否使用tag模式跳转
			if (targetId.contains(TAG_FLAG)) {
				String[] target = targetId.split(TAG_FLAG, 2);
				String _targetId = target[0];
				String _targetTag = target[1];
				targetExecutor = targetList.stream().filter(executable -> {
					return (StrUtil.startWith(_targetId, TAG_PREFIX) && ObjectUtil.equal(_targetTag,executable.getTag()))
							|| ((StrUtil.isEmpty(_targetId) || _targetId.equals(executable.getId()))
							&& (StrUtil.isEmpty(_targetTag) || _targetTag.equals(executable.getTag())));
				}).findFirst().orElse(null);
			}
			else {
				targetExecutor = targetList.stream()
					.filter(executable -> ObjectUtil.equal(executable.getId(),targetId) )
					.findFirst()
					.orElse(null);
			}
		}

		if (ObjectUtil.isNull(targetExecutor)) {
			// 没有匹配到执行节点，则走默认的执行节点
			targetExecutor = this.getDefaultExecutor();
		}

		if (ObjectUtil.isNotNull(targetExecutor)) {
			// switch的目标不能是Pre节点或者Finally节点
			if (targetExecutor instanceof PreCondition || targetExecutor instanceof FinallyCondition) {
				String errorInfo = StrUtil.format(
						"[{}]:switch component[{}] error, switch target node cannot be pre or finally",
						slot.getRequestId(), this.getSwitchNode().getInstance().getDisplayName());
				throw new SwitchTargetCannotBePreOrFinallyException(errorInfo);
			}
			targetExecutor.setCurrChainId(this.getCurrChainId());
			targetExecutor.execute(slotIndex);
		}
		else {
			String errorInfo = StrUtil.format("[{}]:no target node find for the component[{}],target str is [{}]",
					slot.getRequestId(), this.getSwitchNode().getInstance().getDisplayName(), targetId);
			throw new NoSwitchTargetNodeException(errorInfo);
		}
	}

	@Override
	public ConditionTypeEnum getConditionType() {
		return ConditionTypeEnum.TYPE_SWITCH;
	}

	public void addTargetItem(Executable executable) {
		this.addExecutable(ConditionKey.SWITCH_TARGET_KEY, executable);
	}

	public List<Executable> getTargetList() {
		return this.getExecutableList(ConditionKey.SWITCH_TARGET_KEY);
	}

	public void setSwitchNode(Node switchNode) {
		this.addExecutable(ConditionKey.SWITCH_KEY, switchNode);
	}

	public Node getSwitchNode() {
		return (Node) this.getExecutableOne(ConditionKey.SWITCH_KEY);
	}

	public Executable getDefaultExecutor() {
		return this.getExecutableOne(ConditionKey.SWITCH_DEFAULT_KEY);
	}

	public void setDefaultExecutor(Executable defaultExecutor) {
		this.addExecutable(ConditionKey.SWITCH_DEFAULT_KEY, defaultExecutor);
	}

}
