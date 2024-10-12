package com.yomahub.liteflow.solon.integration;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;
import com.yomahub.liteflow.lifecycle.LifeCycle;
import com.yomahub.liteflow.lifecycle.LifeCycleHolder;
import com.yomahub.liteflow.process.holder.SolonNodeIdHolder;
import com.yomahub.liteflow.solon.config.LiteflowAutoConfiguration;
import com.yomahub.liteflow.solon.config.LiteflowMainAutoConfiguration;
import com.yomahub.liteflow.solon.config.LiteflowMonitorProperty;
import com.yomahub.liteflow.solon.config.LiteflowProperty;
import com.yomahub.liteflow.spi.holder.DeclComponentParserHolder;
import org.noear.solon.Utils;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.BeanWrap;
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

		SolonNodeIdHolder nodeIdHolder = SolonNodeIdHolder.of(context);

		// 放到前面
		context.beanMake(LiteflowProperty.class);
		context.beanMake(LiteflowMonitorProperty.class);
		context.beanMake(LiteflowAutoConfiguration.class);
		context.beanMake(LiteflowMainAutoConfiguration.class);

		// 订阅生命周期实现类
		context.subBeansOfType(LifeCycle.class, LifeCycleHolder::addLifeCycle);

		// 订阅 @Component 或别的方式产生的 NodeComponent
		context.subWrapsOfType(NodeComponent.class, bw->{
			if (Utils.isNotEmpty(bw.name())) {
				NodeComponent node1 = bw.raw();
				node1.setNodeId(bw.name());

				nodeIdHolder.add(node1.getNodeId());
			}
		});

		Set<Class<?>> liteflowMethodClassSet = new HashSet<>();

		//添加 @LiteflowMethod 注解处理
		context.beanExtractorAdd(LiteflowMethod.class, (bw, method, anno) -> {
			if (liteflowMethodClassSet.contains(bw.clz())) {
				//避免重复处理类
				return;
			} else {
				liteflowMethodClassSet.add(bw.clz());
			}

			List<DeclWarpBean> declWarpBeanList = DeclComponentParserHolder
					.loadDeclComponentParser()
					.parseDeclBean(bw.clz());

			for (DeclWarpBean declWarpBean : declWarpBeanList) {
				NodeComponent node1 = LiteFlowProxyUtil.proxy2NodeComponent(declWarpBean);

				BeanWrap node1Bw = context.wrap(node1.getNodeId(), node1);
				context.putWrap(node1.getNodeId(), node1Bw);

				nodeIdHolder.add(node1.getNodeId());
			}
		});

		//添加 @LiteflowComponent 注解处理
		context.beanBuilderAdd(LiteflowComponent.class, (clz, bw, anno) -> {
			if(NodeComponent.class.isAssignableFrom(clz)) {
				String nodeId = Utils.annoAlias(anno.id(), anno.value());
				if (Utils.isNotEmpty(nodeId)) {
					NodeComponent node1 = bw.raw();
					node1.setNodeId(nodeId);
					node1.setName(anno.name());

					context.putWrap(node1.getNodeId(), bw);

					nodeIdHolder.add(node1.getNodeId());
				}
			}

			// 支持动态代理与函数提取
			context.beanExtractOrProxy(bw);
		});
	}
}