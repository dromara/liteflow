# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

LiteFlow (v2.15.1) is a lightweight rules engine framework for complex component-based business orchestration. It uses a DSL to drive workflows with support for hot reload and 10 scripting languages. The project targets Java 8+ and has 2000+ test cases.

**Official Documentation**: https://liteflow.cc/pages/5816c5/

## Commands

### Build
```bash
# Build entire project (uses 'compile' profile by default, includes test modules)
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Build specific module
mvn clean package -DskipTests -pl liteflow-core

# Build for release (production modules only, excludes tests)
mvn clean package -DskipTests -P release
```

### Run Tests
```bash
# Run all tests
mvn test

# Run tests for specific module (30+ test modules in liteflow-testcase-el/)
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-springboot

# Run single test class
mvn test -pl liteflow-core -Dtest=FlowExecutorTest

# Run specific test method
mvn test -pl liteflow-core -Dtest=FlowExecutorTest#testExecute
```

### Other Commands
```bash
# Dependency tree
mvn dependency:tree

# View module structure
ls liteflow-*/pom.xml
```

## High-Level Architecture

### Core Execution Model

**FlowExecutor** → **Chain** → **Condition Tree** → **Node Components**

1. **FlowExecutor**: Entry point for executing workflows (`execute2Resp(chainId, param)`)
2. **FlowBus**: Central metadata registry for all chains and nodes (thread-safe, supports hot reload)
3. **Chain**: Named workflow composed of an EL expression that compiles to a Condition tree
4. **Slot/DataBus**: Thread-safe context management using slot pooling (configurable `slotSize`)
5. **NodeComponent**: Base class for all business logic components

### Key Architectural Patterns

#### 1. Two-Stage Parsing
Chains are built in two phases to handle circular dependencies:
- **Phase 1**: Register chain IDs (creates placeholder chains)
- **Phase 2**: Build complete condition trees with EL parsing

#### 2. EL Expression Language
Uses QLExpress to parse declarative workflows. Example operators:
- **THEN(a, b, c)**: Sequential execution
- **WHEN(a, b, c)**: Parallel execution
- **IF(condition, THEN(x), ELSE(y))**: Conditional branching
- **SWITCH(selector).to(a, b, c)**: Multi-way branching
- **FOR(count).DO(loop)**: Loop execution
- **RETRY(node).times(3).forException()**: Retry mechanism
- **CATCH(node).DO(handler)**: Exception handling

Rules are defined in XML/JSON/YML:
```xml
<chain name="myChain">
    THEN(a, WHEN(b, c).maxWaitSeconds(5), IF(e, f, g));
</chain>
```

#### 3. Component Types
All extend `NodeComponent` but have specialized behaviors:
- **NodeComponent**: Standard synchronous component (`process()` method)
- **NodeBooleanComponent**: Returns boolean for IF conditions (`processBoolean()`)
- **NodeSwitchComponent**: Returns string for SWITCH routing (`processSwitch()`)
- **NodeIteratorComponent**: Provides iteration logic for ITERATOR construct
- **NodeForComponent**: Controls FOR loop behavior
- **ScriptComponent**: Script-based components (Groovy, JS, Python, etc.)

#### 4. Slot-Based Context
Thread-safe execution context:
- `DataBus.offerSlot(chainId)` acquires a slot from pool
- Slot contains execution metadata, context beans, and step tracking
- `DataBus.releaseSlot(slotIndex)` returns slot to pool
- Pool size configurable via `slotSize` property

#### 5. Parse Mode Strategies
Three modes (`ParseModeEnum`):
- **PARSE_ALL_ON_START**: Parse all chains at startup (default)
- **PARSE_ONE_ON_FIRST_EXEC**: Lazy parse each chain on first use (faster startup)
- **PARSE_ALL_ON_FIRST_EXEC**: Parse all chains on first any chain execution

### Module Structure

#### Core Modules
- **liteflow-core**: Core engine (FlowExecutor, FlowBus, DataBus, Slot, Condition system, component model)
- **liteflow-el-builder**: Programmatic chain builder API using QLExpress

#### Rule Source Plugins (6 implementations in `liteflow-rule-plugin/`)
- **liteflow-rule-zk**: ZooKeeper configuration source
- **liteflow-rule-sql**: SQL database configuration source
- **liteflow-rule-nacos**: Nacos configuration center
- **liteflow-rule-etcd**: etcd configuration source
- **liteflow-rule-apollo**: Apollo configuration center
- **liteflow-rule-redis**: Redis configuration source

#### Script Plugins (10 languages in `liteflow-script-plugin/`)
- **liteflow-script-groovy**: Groovy scripting
- **liteflow-script-javascript**: Rhino JavaScript (JSR223)
- **liteflow-script-graaljs**: GraalVM JavaScript
- **liteflow-script-qlexpress**: Alibaba QLExpress
- **liteflow-script-python**: Jython (Python on JVM)
- **liteflow-script-lua**: LuaJ
- **liteflow-script-aviator**: Aviator expression language
- **liteflow-script-java**: Janino (Java compiler)
- **liteflow-script-javax**: JSR223 standard Java compiler
- **liteflow-script-kotlin**: Kotlin scripting

#### Framework Integration
- **liteflow-spring**: Spring framework integration (component scanning, bean lifecycle, AOP)
- **liteflow-spring-boot-starter**: Spring Boot auto-configuration with `@ConfigurationProperties`
- **liteflow-solon-plugin**: Solon framework integration (lightweight alternative to Spring)

#### Testing Infrastructure
30+ test modules in `liteflow-testcase-el/` organized by:
1. **Framework**: springboot, springnative, solon, nospring
2. **Config Sources**: zk, nacos, etcd, apollo, redis, sql
3. **Scripts**: One module per language + multi-language scenarios
4. **Features**: builder, declare, routechain, monitoring, timeout, etc.

Test pattern:
```java
@SpringBootTest
@TestPropertySource(value = "classpath:/application.properties")
public class MyTest {
    @Resource private FlowExecutor flowExecutor;

    @Test
    public void test() {
        LiteflowResponse response = flowExecutor.execute2Resp("chainId", "arg");
        Assertions.assertTrue(response.isSuccess());
    }
}
```

### Important Design Patterns

#### SPI Pattern for Extensibility
Used extensively for:
- `ContextAware`: Framework abstraction (Spring vs non-Spring)
- `PathContentParser`: Custom file path resolution
- `CmpAroundAspect`: Global component lifecycle hooks
- `DeclComponentParser`: Declarative component parsing
- Script executors for each language

#### Lifecycle Hooks
Multiple extension points:
- `PostProcessChainBuildLifeCycle`: Before/after chain building
- `PostProcessNodeBuildLifeCycle`: Before/after node building
- `PostProcessChainExecuteLifeCycle`: Before/after chain execution
- `PostProcessFlowExecuteLifeCycle`: Before/after flow execution

#### Rollback Mechanism
Components implement `rollback()` for automatic rollback on failure (executed in reverse order via `executeSteps` deque).

#### Hot Reload Support
- `MonitorFile` watches rule files for changes
- `reloadRule()` refreshes without restart
- Copy-on-write collections (unless `fastLoad=true`) prevent concurrent modification

#### Metadata Caching
- `FlowBus` uses CopyOnWriteHashMap (or regular HashMap with `fastLoad`)
- EL MD5 caching for expression reuse
- Chain caching configurable (`chainCacheEnabled`, `chainCacheCapacity`)

### Critical Configuration (`LiteflowConfig`)
- **ruleSource**: Rule file locations (supports XML, JSON, YML)
- **parseMode**: Parsing strategy (affects startup performance)
- **slotSize**: Context slot pool size
- **enableMonitorFile**: Hot reload of rule files
- **supportMultipleType**: Mix XML/JSON/YML rules
- **whenMaxWaitTime**: Timeout for WHEN parallel execution
- **fastLoad**: Disable CopyOnWrite for faster startup
- **enableVirtualThread**: Use virtual threads (JDK 21+)

### Key File Locations
- Core engine: `liteflow-core/src/main/java/com/yomahub/liteflow/flow/`
- FlowExecutor: `liteflow-core/src/main/java/com/yomahub/liteflow/core/FlowExecutor.java`
- FlowBus: `liteflow-core/src/main/java/com/yomahub/liteflow/flow/FlowBus.java`
- Component base: `liteflow-core/src/main/java/com/yomahub/liteflow/core/NodeComponent.java`
- Condition system: `liteflow-core/src/main/java/com/yomahub/liteflow/flow/element/condition/`
- EL parser: `liteflow-core/src/main/java/com/yomahub/liteflow/parser/el/`

### Important Conventions
- **Naming**: Components identified by `nodeId`, chains by `chainId`
- **Thread Safety**: Extensive ThreadLocal and concurrent collections
- **Fail-Fast**: Validation at parse time with detailed error messages
- **Fluent APIs**: Builder pattern for chain construction (EL builder)
- **Namespaces**: Chains can be organized into namespaces
- **Versioning**: Uses `${revision}` placeholder (currently 2.15.1) via flatten-maven-plugin
