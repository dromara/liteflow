package com.yomahub.liteflow.spi.solon;

import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.process.holder.SolonNodeHolder;
import com.yomahub.liteflow.spi.ContextCmpInit;
import org.noear.solon.Solon;

/**
 * Solon 环境容器上下文组件初始化实现（在 solon 里没有用上；机制不同）
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class SolonContextCmpInit implements ContextCmpInit {

	@Override
	public void initCmp() {
		SolonNodeHolder solonNodeHolder = SolonNodeHolder.of(Solon.context());

		solonNodeHolder.getNodeMap().forEach(FlowBus::addManagedNode);
	}

	@Override
	public int priority() {
		return 1;
	}

}
