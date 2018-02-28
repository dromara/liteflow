# 快速开始
liteflow需要你的项目使用maven
## 依赖
```xml
<dependency>
	<groupId>com.thebeastshop.liteflow</groupId>
	<artifactId>liteflow</artifactId>
	<version>${liteFlow.version}</version>
</dependency>
```
## 流程配置文件
```xml
<?xml version="1.0" encoding="UTF-8"?>
<flow>
	<nodes>
		<node id="a" class="com.thebeastshop.liteflow.test.component.AComponent"/>
		<node id="b" class="com.thebeastshop.liteflow.test.component.BComponent"/>
		<node id="c" class="com.thebeastshop.liteflow.test.component.CComponent"/>
		<node id="d" class="com.thebeastshop.liteflow.test.component.DComponent"/>
		<node id="e" class="com.thebeastshop.liteflow.test.component.EComponent"/>
	</nodes>
	
	<chain name="demoChain">
		<then value="a,b,c"/> <!-- then表示串行 -->
		<when value="d,e"/> <!-- when表示并行 -->
	</chain>
</flow>
```

component为组件，这里你需要实现这些组件，每个组件继承`NodeComponent`类
```java
public class AComponent extends NodeComponent {

	@Override
	public void process() {
		String str = this.getSlot().getRequestData();
		System.out.println(str);
		System.out.println("Acomponent executed!");
	}
}
```

chain为流程链，每个链上可配置多个组件节点。目前执行的模式分串行和并行2种。
串行标签为`then`，并行标签为`when`。
在串行的模式下，以下2种写法是等价的,可以根据业务需要来把不同种类的节点放一行里。
```xml
<then value="a,b,c,d"/>
```
```xml
<then value="a,b"/>
<then value="c,d"/>
```

## 执行流程链
```java
FlowExecutor executor = new FlowExecutor();
executor.setRulePath(Arrays.asList(new String[]{"/config/flow.xml"}));
executor.init();
Slot slot = executor.execute("demoChain", "arg");
```

如果你的项目使用spring，推荐参考[和Spring进行集成](http://123.206.92.144:3000/#/runwithspring)