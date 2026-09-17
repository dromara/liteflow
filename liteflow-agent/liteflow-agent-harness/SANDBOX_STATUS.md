# 会话容器状态查询

`AgentSandboxStatusService` 提供按运行命名空间、用户和会话隔离的只读查询。

```java
var service = new AgentSandboxStatusService(config.getApplicationName());
AgentSandboxStatus status = service.getStatus(conversationId);
```

服务读取当前 JVM 中 `SessionSandboxRegistry` 管理的沙箱，支持 `SESSION_IDLE` 和共享存储模式下的 `PER_CALL`。它不初始化 Agent、不创建或恢复容器、不读写会话存储，也不获取任务持有的工作区锁。HTTP 调用方负责先校验当前用户有权访问该会话。

查询通过与 AgentScope 相同的 Docker CLI 和 Docker context 执行只读 `docker inspect`，最长等待 3 秒，只提取容器 ID、名称、镜像和状态。它不依赖 `Sandbox.isRunning()`：该标记表示 SDK 工作区生命周期，保存快照后可能为 `false`，而物理容器仍在运行。

| 字段 | 含义 |
| --- | --- |
| `scope` | 固定为 `PROCESS_LOCAL`，仅观察当前应用实例注册的沙箱 |
| `state` | `RUNNING`、`STOPPED`、`PAUSED`、`RESTARTING`、`CREATED`、`REMOVING`、`DEAD` 等实际 Docker 状态 |
| `containerId`、`containerName`、`image` | 当前句柄关联的容器信息；`UNKNOWN` 或 `NOT_FOUND` 时 ID 仅用于识别待查询对象，不表示容器存在 |
| `busy` | 当前会话是否正在使用此沙箱，独立于 Docker 状态 |
| `lastActiveAt` | 最近开始或结束调用的时间，Unix 毫秒时间戳 |
| `checkedAt` | 本次查询完成时间，Unix 毫秒时间戳 |
| `message` | 查询失败或状态发生并发变化时的简短原因，不包含 Docker 原始输出 |

特殊状态：

- `NOT_ALLOCATED`：当前实例没有注册该会话的沙箱，包括尚未执行、空闲回收或实例重启后。它不表示其他实例也没有容器，不扫描整个 Docker 主机，也不从历史恢复记录推断容器仍在运行。
- `STARTING`：当前调用正在准备沙箱，还没有容器 ID。
- `NOT_FOUND`：Docker 明确报告句柄记录的容器不存在。
- `UNKNOWN`：Docker 不可用、权限不足、查询超时、返回异常，或查询过程中容器归属发生变化。

读取不会延长空闲回收时间。注册记录随回收、取消清理、会话删除和运行时关闭而移除。无共享存储的旧 `PER_CALL` 路径和自定义文件系统不由此注册表观察；这些路径没有注册记录时返回 `NOT_ALLOCATED`。

多实例部署应将查询路由到执行该会话的实例；本服务不提供跨实例容器发现。
