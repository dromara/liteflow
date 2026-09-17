# 统一 Agent 与工作区

Agent 组件统一继承 `com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent`，并依赖 `liteflow-agent-harness`。原 `AgentComponent`、普通 `AgentRuntime` 和独立的 `WorkspaceBackend` 已移除。模型、工具、Skill、会话状态、流式事件、结构化输出与人工确认沿用共享基础层。

## 配置职责

| 配置 | 职责 |
| --- | --- |
| `liteflow.agent.skills.path` | Skill 来源，可以是 classpath 或宿主机文件目录 |
| `liteflow.agent.harness.filesystem-backend` | 选择 `GUARDED_LOCAL`、`DOCKER` 或自定义执行后端 |
| `liteflow.agent.harness.local.workspace-root` | 本地会话执行目录的宿主机根目录，仅本地模式使用 |
| `liteflow.agent.harness.docker.workspace-root` | 容器中的会话执行目录，仅 Docker 模式使用 |
| `liteflow.agent.session-store.type` | JSON、MySQL 或 Redis 持久化 |
| `liteflow.agent.session-store.json-root` | JSON 会话状态目录 |
| `liteflow.agent.session-store.json-workspace-root` | 本地工作区记录、记忆与归档的持久化目录；省略时为 `json-root/workspace` |

MySQL／Redis 的会话、记忆、工作区文件及附件仍由现有存储适配器管理。JSON 的执行目录与持久化目录分别配置；Docker JSON 的业务文件快照仍由 `harness.docker.snapshot-root` 保存。切换执行后端不改变 Skill 来源及持久化配置的含义。

## 本地执行

```yaml
liteflow:
  agent:
    skills:
      enabled: true
      path: classpath:agent/skills
    session-store:
      type: JSON
      json-root: ./data/agent-state
      json-workspace-root: ./data/workspace-records
    harness:
      filesystem-backend: GUARDED_LOCAL
      local:
        workspace-root: ./data/execution
```

实际会话命令目录为 `harness.local.workspace-root/<应用名>/<原始会话ID>/`，按应用和会话隔离。命令在该目录执行，相对路径属于当前会话。命令前恢复业务文件，命令后回写创建、修改和删除；执行缓存可从持久化存储重建。

## Docker 执行

```yaml
liteflow:
  agent:
    skills:
      enabled: true
      path: classpath:agent/skills
    session-store:
      type: MYSQL
      mysql:
        jdbc-url: jdbc:mysql://localhost:3306/liteflow_agent
        username: liteflow
        password: ${MYSQL_PASSWORD}
        database-name: liteflow_agent
        create-if-not-exist: true
    harness:
      filesystem-backend: DOCKER
      docker:
        image: liteflow-agent-sandbox:node22
        workspace-root: /workspace
```

Docker 不读取 `harness.local.workspace-root`。MySQL／Redis 模式所需的宿主机暂存目录由框架创建和回收，不作为用户执行工作区，也不再复制本地执行根目录中的内容。JSON 模式的宿主机持久化目录由 `session-store.json-workspace-root` 定义。

## 同一会话文件约定

两种模式都在当前会话工作区提供完整的技能文件：

```text
.skills-cache/<来源>/<技能名>/SKILL.md
.skills-cache/<来源>/<技能名>/scripts/...
.skills-cache/<来源>/<技能名>/references/...
```

`load_skill_through_path` 返回的 `Files root` 是工作区相对路径。`read_file` 和 `execute` 使用同一路径；Skill 不依赖宿主机绝对路径或容器绝对路径。`SKILL.md` 的元数据与正文都会落盘，脚本、文本和二进制附件一起准备。资源写入前校验路径，不设置文件大小上限。

技能来源是静态资源的权威来源。每次调用按当前可见技能更新会话文件，移除上次投放而本次不再可见的资源，不删除业务文件或其他会话的技能。框架内部记忆和状态仍通过对应工具访问，不需要把数据库记录全部暴露给 Shell。

## 从旧配置迁移

- `agent.workspace.root` 用作本地执行位置时，改为 `agent.harness.local.workspace-root`。
- 旧 JSON 工作区目录要同时配置为 `agent.session-store.json-workspace-root`，以继续读取既有记忆、归档和工作区文件。
- 工作区目录自动创建，文件大小上限配置已移除。
- 本地执行不再需要单独的信任开关，由执行后端配置决定运行位置。
- `agent.workspace.backend` 不再使用，统一由 `agent.harness.filesystem-backend` 选择后端。
- 原 `customizeAgent(ReActAgent.Builder)` 改为 `customizeHarness(HarnessAgent.Builder)`，在收到的 builder 上修改并返回同一对象。
- `enableWorkspaceFileTools()` 已移除，文件工具由 Harness 统一装配；`enableShellTool()` 继续控制命令工具，默认开启。
- 旧普通组件的工具名应迁移到 Harness 的 `read_file`、`write_file`、`list_files`、`edit_file` 和 `execute`。

升级后需重新构建应用。删除的 Java API 和旧的顶层工作区配置不提供另一套运行时兼容入口。
