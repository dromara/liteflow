### 概述
liteFlow是一个轻量级的组件式流程框架，帮助解耦业务代码，让每一个业务片段都是一个组件

* 提供本地xml的流程配置（后续全面支持spring式流程配置）
* 提供基于spring的扫描方式注入component
* 提供串行和并行2种模式。
* 提供条件节点的模式。
* 消除组件之间参数传递，引入数据总线概念。
* 自带简单的监控，能够知道每个组件的运行平均时间。消耗内存。（每隔10分钟会自动打印）

### Quick Start
1. 定义组件需继承Component，项目启动时会被自动发现。
2. 定义xml配置(例子)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<flow>
	<!-- 如果和spring集成，以下<nodes>配置可以不要 -->
	<nodes>
		<node id="a" class="com.thebeastshop.liteflow.test.component.AComponent"/>
		<node id="b" class="com.thebeastshop.liteflow.test.component.BComponent"/>
		<node id="c" class="com.thebeastshop.liteflow.test.component.CComponent"/>
		<node id="d" class="com.thebeastshop.liteflow.test.component.DComponent"/>
		<node id="e" class="com.thebeastshop.liteflow.test.component.EComponent"/>
		<node id="f" class="com.thebeastshop.liteflow.test.component.FComponent"/>
		<node id="g" class="com.thebeastshop.liteflow.test.component.GComponent"/>
	</nodes>
	
	<chain name="chain1">
		<then value="a,c"/> <!-- then代表串行 -->
		<when value="b,d"/> <!-- when代表并行 -->
		<then value="e,f,g"/>
	</chain>

	<chain name="chain2">
		<then value="a,cond(b|d)"/> <!-- cond节点是条件节点，根据cond节点路由到b节点或者d节点 -->
		<then value="e,f,g"/>
	</chain>
	
	<chain name="chain3">
		<then value="a,c,g"/>
		<when value="b,e"/>
		<then value="d,f"/>
	</chain>
</flow>
```
3.spring里声明执行器
```xml
<bean id="flowExecutor" class="com.thebeastshop.liteflow.core.FlowExecutor" init-method="init">
	<property name="rulePath">
		<list>
			<value>flow.xml</value>
		</list>
	</property>
</bean>

<!-- 自动扫描注入到spring的component组件 -->
<bean class="com.thebeastshop.liteflow.spring.ComponentScaner"/>

```
4.开始一个流程
```java
executor.execute("chain2", 参数);
```