## 使用自定义的配置源
如果你不想用本地的配置，也不打算使用zk作为配置持久化工具。liteFlow支持自定义的配置源的扩展点。

# 创建自定义配置源的类
在你的项目中创建一个类继承`ClassXmlFlowParser`这个类
```java
public class TestCustomParser extends ClassXmlFlowParser {

	@Override
	public String parseCustom() {
		System.out.println("进入自定义parser");
		String xmlContent = null;
		//这里需要自己扩展从自定义的地方获取配置
		return xmlContent;
	}
}
```

# spring配置
spring中需要改的地方还是执行器的配置，只需要在配置的路径地方放入自定义类的类路径即可
```xml
<bean id="flowExecutor" class="com.thebeastshop.liteflow.core.FlowExecutor">
	<property name="rulePath">
		<list>
			<value>com.thebeastshop.liteflow.test.TestCustomParser</value>
		</list>
	</property>
</bean>
```