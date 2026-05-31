# 设计文档：skill tools 改为从容器引用已注册的 Spring/Solon bean

- 日期：2026-05-31
- 范围：`liteflow-react-agent` 模块，`SkillToolResolver`
- 状态：设计已确认，待落地

## 背景与问题

`liteflow-react-agent` 中，skill 的 `SKILL.md` frontmatter 可以声明 `tools` 字段，
列出该 skill 允许使用的 Java 工具类。当前 `SkillToolResolver.instantiateTools()`
用纯反射构造这些工具：

```java
instances.add(clazz.getDeclaredConstructor().newInstance());
```

这带来两个问题：

1. **无法依赖注入**：工具类用无参反射构造，脱离 Spring/Solon 容器，
   类里的 `@Autowired` / `@Resource` 字段不会被注入，工具无法持有 `DataSource`、
   `Service` 等容器内依赖。
2. **语义与用户预期不符**：实际场景中，这些工具类本就已经作为 `@Component` 注册在
   Spring 容器里。frontmatter 的 `tools` 字段的语义应当是「**这个 skill 允许使用
   容器里已注册的这几个工具**」（引用 / 白名单），而不是「由框架重新创建」。

## 目标

- skill 声明的工具类，从框架容器（Spring / Solon）中按**类型**取出已注册的 bean，
  使其 `@Autowired` 依赖天然生效。
- 不破坏无框架（nospring）场景与现有单元测试。
- 复用 LiteFlow 既有的 `ContextAware` SPI 抽象，保持框架无关性
  （`liteflow-react-agent-core` 不直接依赖 Spring API）。

## 非目标

- 不修改 `ContextAware` SPI 接口（现有方法已足够）。
- 不引入 `tools` 的 bean-name 引用语法（已确认只支持类全名 / 按类型取）。
- 不处理 SKILL.md 的扫盘缓存 / 热加载问题（属另一议题）。

## 设计

### 核心改动（单点）

仅修改 `SkillToolResolver.instantiateTools()` 中的实例化逻辑，
工具类**已解析为 `Class<?>`** 后，改为走 `ContextAware` 取 bean：

```java
ContextAware ctx = ContextAwareHolder.loadContextAware();
Object tool;
if (ctx.hasBean(clazz)) {
    tool = ctx.getBean(clazz);                           // 复用容器单例，依赖注入已生效
} else {
    tool = clazz.getDeclaredConstructor().newInstance();  // 降级：无容器 / 未注册时反射 new
    LOG.info("Skill '{}' tool '{}' not found in container; "
            + "fell back to reflective instantiation, dependency injection unavailable",
            skill.getName(), clazz.getName());
}
```

**为什么先 `hasBean(clazz)` 再 `getBean(clazz)`**：`SpringAware.getBean(Class)` 在
容器中不存在该类型 bean 时会抛 `NoSuchBeanDefinitionException`（而非返回 null），
而 `hasBean(Class)` 在各实现里都是 `getBeansOfType(clazz).size() > 0` 的安全判断，
可避免异常驱动的控制流。

### 三种运行环境的行为

| 环境 | `ContextAware` 实现 | `hasBean(clazz)` | 行为 |
|---|---|---|---|
| Spring | `SpringAware` | bean 已注册时 true | `getBean` 复用容器单例，**依赖注入生效** |
| Solon | `SolonContextAware` | bean 已注册时 true | 同上，从 Solon 容器取，注入生效 |
| 无框架 / 未注册 / 单元测试 | `LocalContextAware` | 恒 false | 降级反射 `new`（无注入，保留旧行为） |

### 已确认的取舍

1. **不调用 `registerBean`**：框架只 `getBean` 取用户已声明的 bean，不向容器注册新
   bean。工具的 scope / 生命周期完全由用户的 `@Component` 声明决定（默认单例，
   跨 agent 会话共享）。
2. **取不到 bean 时降级反射 `new`，不报错**：为兼容 nospring 场景与现有单元测试
   （`SkillBoxFactoryTest` 在无容器环境下断言 `CONSTRUCT_COUNT == 1`）。降级时记一条
   INFO 日志，便于诊断「为什么 `@Autowired` 是 null」。
3. **`tools` 只支持类全名（按类型取）**，与现有 frontmatter 写法完全兼容。

### 引用方式

`tools` 字段仍写类的全限定名，如：

```yaml
tools: com.example.MyDbTool
# 或 inline array
tools: [com.example.MyDbTool, com.example.MyHttpTool]
```

`SkillToolResolver` 已有的 `resolveToolClasses` / `toClassNameList` 解析逻辑保持不变，
仅替换其后的实例化方式。

## 兼容性与影响范围

- **行为变化**：工具实例由「每会话反射 new」变为「容器单例复用（Spring/Solon）+
  降级反射 new（其它）」。在 Spring 下工具变为单例，跨会话共享——需确认工具应为
  无状态（agentscope 工具对象通常无状态，状态在 `ToolCallParam` 中）。
- **顺带收益**：消除了 Spring 环境下「每个 agent 会话重复反射实例化工具」的开销。
- **依赖**：`liteflow-react-agent-core` 已依赖 `liteflow-core`
  （`ReActAgentComponent extends NodeComponent`），可直接使用
  `ContextAwareHolder` / `ContextAware`，无需新增模块依赖。
- **strict 模式**：`SkillsConfig.isStrict()` 当前控制「类找不到 / 实例化失败」的
  抛错 vs 警告，本次不改变其语义；「容器中无 bean」走降级而非 strict 判定。

## 测试计划

放置位置遵循项目规范：测试只放在 `liteflow-testcase-el` 子模块下。

1. **保留降级路径测试**：现有 `SkillBoxFactoryTest` 中无容器环境下断言
   `CONSTRUCT_COUNT == 1` 的用例继续通过（验证 `hasBean==false` 时反射 new）。
2. **新增注入路径测试**：构造一个桩 / 真实 `ContextAware`，向其放入一个工具 bean，
   断言 `instantiateTools` 返回的对象与容器中那个实例为**同一引用**
   （`assertSame`），从而验证「复用容器 bean、依赖注入生效」路径。
   - 注意 `ContextAwareHolder` 通过 `ServiceLoader` 加载且有静态缓存，测试需能
     注入 / 清理（`ContextAwareHolder.clean()`）该上下文。

## 风险

- 若用户在 frontmatter 写了类名但忘记把该类声明为 `@Component`，会静默降级为反射
  new（依赖未注入）。通过 INFO 日志缓解可诊断性。如后续反馈此场景需要更强约束，
  可在 strict 模式下改为抛错。
