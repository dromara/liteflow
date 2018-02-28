# 和spring进行集成
如果你的项目中使用了spring，liteFlow可以很方便和spring进行集成

## 流程配置可以省略的部分
流程配置中的`nodes`节点，可以不用配置了，支持spring的自动扫描方式。你需要在你的spring配置文件中定义
```xml
<context:component-scan base-package="com.thebeastshop.liteflow.test.component" />
<bean class="com.thebeastshop.liteflow.spring.ComponentScaner"/>
```

当然，你的组件节点也需要注册进spirng容器
```java
@Component("a")
public class AComponent extends NodeComponent 
	@Override
	public void process() {
		String str = this.getSlot().getRequestData();
		System.out.println(str);
		System.out.println("Acomponent executed!");
	}
}
```

## spring中执行器的配置
```xml
<bean id="flowExecutor" class="com.thebeastshop.liteflow.core.FlowExecutor">
	<property name="rulePath">
		<list>
			<value>/config/flow.xml</value>
		</list>
	</property>
</bean>
```
然后你的项目中通过spring拿到执行器进行调用流程。