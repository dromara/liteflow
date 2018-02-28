## 和zookeeper进行集成
liteFlow支持把配置放在zk集群中，并支持实时修改流程

# spring配置
你只需在原来配置执行器的地方，把本地xml路径换成zk地址就ok了
```xml
<!-- 这种是zk方式配置 -->
<bean id="flowExecutor" class="com.thebeastshop.liteflow.core.FlowExecutor">
	<property name="rulePath">
		<list>
			<value>127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183</value>
		</list>
	</property>
	<!--这个不配置就用默认的/lite-flow/flow节点 -->
	<property name="zkNode" value="/lite-flow/customFlow"/>
</bean>
```

如果你不加zkNode这个标签，就用默认的节点路径进行读取配置。
使用这种方式加载配置，在zk上进行更改配置。liteFlow会实时刷新配置。