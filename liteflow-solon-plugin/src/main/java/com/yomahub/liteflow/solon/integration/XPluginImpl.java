package com.yomahub.liteflow.solon.integration;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.lifecycle.LifeCycle;
import com.yomahub.liteflow.lifecycle.LifeCycleHolder;
import com.yomahub.liteflow.solon.config.LiteflowAutoConfiguration;
import com.yomahub.liteflow.solon.config.LiteflowMainAutoConfiguration;
import com.yomahub.liteflow.solon.config.LiteflowMonitorProperty;
import com.yomahub.liteflow.solon.config.LiteflowProperty;
import com.yomahub.liteflow.spi.holder.DeclComponentParserHolder;
import org.noear.solon.Utils;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;

import java.util.*;

/**
 * @author noear
 * @since 2.9
 */
public class XPluginImpl implements Plugin {

	@Override
	public void start(AppContext context) {
		// 加载默认配置
		Properties defProps = Utils.loadProperties("META-INF/liteflow-default.properties");
		if (defProps != null && defProps.size() > 0) {
			defProps.forEach((k, v) -> {
				context.cfg().putIfAbsent(k, v);
			});
		}

		// 是否启用
		boolean enable = context.cfg().getBool("liteflow.enable", false);

		if (!enable) {
			return;
		}

		// 放到前面
		context.beanMake(LiteflowProperty.class);
		context.beanMake(LiteflowMonitorProperty.class);
		context.beanMake(LiteflowAutoConfiguration.class);
		context.beanMake(LiteflowMainAutoConfiguration.class);

		// 订阅生命周期实现类
		context.subWrapsOfType(LifeCycle.class, bw -> {
			LifeCycle lifeCycle = bw.raw();
			LifeCycleHolder.addLifeCycle(lifeCycle);
		});

		// 订阅 NodeComponent 组件
		context.subWrapsOfType(NodeComponent.class, bw -> {
			NodeComponent node1 = bw.raw();
			node1.setNodeId(bw.name());
			FlowBus.addManagedNode(bw.name(), bw.raw());
		});

		Set<Class<?>> liteflowMethodClassSet = new HashSet<>();

		context.beanExtractorAdd(LiteflowMethod.class, (bw, method, anno) -> {
			if (liteflowMethodClassSet.contains(bw.clz())) {
				return;
			} else {
				liteflowMethodClassSet.add(bw.clz());
			}

			List<DeclWarpBean> declWarpBeanList = DeclComponentParserHolder
					.loadDeclComponentParser()
					.parseDeclBean(bw.clz());

			for (DeclWarpBean declWarpBean : declWarpBeanList) {
				NodeComponent node1 = LiteFlowProxyUtil.proxy2NodeComponent(declWarpBean);
				FlowBus.addManagedNode(node1.getNodeId(), node1);
			}
		});

		context.beanBuilderAdd(LiteflowComponent.class, (clz, bw, anno) -> {
			if (NodeComponent.class.isAssignableFrom(clz)) {
				NodeComponent node1 = bw.raw();
				String nodeId = Utils.annoAlias(anno.id(), anno.value());

				node1.setNodeId(nodeId);
				node1.setName(anno.name());

				FlowBus.addManagedNode(nodeId, node1);
			} else {
				context.beanExtractOrProxy(bw); // 尝试提取 LiteflowMethod 函数，并支持自动代理
			}
		});
	}
}