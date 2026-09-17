# Agent 命令执行与默认配置

统一入口 `HarnessAgentComponent` 的 `enableShellTool()` 均默认返回 `true`，不需要覆盖方法来开启命令工具。需要关闭某个组件时覆盖：

```java
@Override
protected boolean enableShellTool() {
    return false;
}
```

| 组件／后端 | 默认工具 | 执行位置 |
| --- | --- | --- |
| 本地 Harness | `execute` | `harness.local.workspace-root` 下的会话目录 |
| Docker Harness | `execute` | 容器中的 `harness.docker.workspace-root` |
| CUSTOM Harness | 后端支持命令时提供 `execute` | 自定义文件系统决定 |

Docker 不再自动注册宿主机的 `execute_shell_command`。显式返回 `false` 会禁止当前 Harness 组件的内置命令工具；不会禁止文件读写工具或操作系统本身的进程启动能力。

## 不填写命令白名单也可使用

`harness.shell.mode` 默认是 `WHITELIST`，`harness.shell.whitelist` 默认包含以下命令：

- Shell、解释器和构建工具：`sh`、`bash`、`python`、`python3`、`node`、`git`、`npm`、`npx`、`java`、`javac`、`mvn`、`gradle`。
- 文件操作与查找：`mkdir`、`cp`、`mv`、`rm`、`touch`、`chmod`、`ls`、`find`、`tree`、`stat`、`file`、`basename`、`dirname`、`pwd`、`which`。
- 文本处理与输出：`cat`、`head`、`tail`、`grep`、`sed`、`awk`、`wc`、`sort`、`uniq`、`cut`、`tr`、`diff`、`echo`、`printf`、`expr`。
- 系统信息：`date`、`whoami`、`hostname`、`uname`、`env`、`df`、`du`、`ps`。
- 哈希、JSON 和网络：`md5sum`、`sha256sum`、`jq`、`curl`、`wget`。

只需保留当前后端需要的目录配置。例如本地 Harness：

```yaml
liteflow:
  agent:
    harness:
      filesystem-backend: GUARDED_LOCAL
      local:
        workspace-root: ./data/agent-workspaces
```

本地模式直接使用宿主机环境，Docker 模式使用镜像中安装的 Python／Node 等解释器。

需要缩小或替换命令列表时配置：

```yaml
liteflow:
  agent:
    shell:
      whitelist: [python3, node, pwd]
      timeout: 60s
```

用户填写的列表替换默认列表；不填写则使用默认值。显式空列表、空命令或 `mode: DISABLED` 与开启命令工具冲突，会在构建时被拒绝。需要关闭工具请让 `enableShellTool()` 返回 `false`。

## 本地 Harness 工作目录

命令在配置的 `harness.local.workspace-root` 下运行，各应用和会话拥有独立固定目录：

```text
harness.local.workspace-root/
  应用名/
    原始会话ID/
      input.json
      report.csv
```

应用名与会话 ID 使用明文，必须是合法的单层目录名；包含路径分隔符或路径跳转的 ID 会被拒绝。同一会话后续命令以及应用重启后使用同一路径；运行 `pwd` 可以查看实际目录。目录不再随每条命令结束而删除。`working_directory` 可选择该会话目录下已有的子目录。

## 文件与持久化

执行目录是会话文件的本地副本。每条命令执行前，框架从当前存储刷新文件并清除已在存储中删除的文件；执行后把新增、修改和删除写回原存储。普通文件工具、脚本和 `deliver_artifact` 因此共享同一份会话数据。

- MySQL／Redis 是权威存储，本地执行目录可以由数据库重新恢复。
- JSON 模式通过 `session-store.json-workspace-root` 保存和恢复工作区记录，不改变已有 JSON 会话地址。
- 不同会话使用不同目录，同一会话的执行与回写沿用 Harness 工作区锁。
- 本机缓存中的空目录和执行位在连续命令间保留，但不作为跨机器恢复的数据。跨机器执行 Shell 脚本可使用 `sh script.sh`。
- 手工修改执行缓存不会自动同步到数据库，下次刷新以存储内容为准。请通过文件工具或 `execute` 修改需要持久化的文件。

同步失败或线程取消时，执行目录及内的 `.agentscope/execution.pending` 标记会保留，后续命令会明确报错，不会静默覆盖尚未保存的输出。恢复这些文件后，可移除对应标记再重试。正常非零退出和超时仍会同步有效文件。

## 命令策略

本地和 Docker Harness 均使用 `harness.shell.whitelist`。本地根据宿主机平台选择 AgentScope 命令校验器，Docker 使用 POSIX 命令校验器。白名单检查命令执行程序，并限制顶层管道、分号、换行等多命令拼接。多步操作应写成脚本，由已允许的解释器执行；允许 `sh`／`python3`／`node` 意味着允许它们执行脚本，白名单不是操作系统沙箱。

本地 `working_directory` 作为进程目录单独处理；Docker 在校验原始命令后，将工作目录安全引用并交给容器执行。Harness 的 `harness.shell.timeout` 默认 1 分钟，是服务端上限，模型的 `timeout` 参数只能缩短它；Docker 超时以秒为单位，配置应至少为 1 秒。输出最多捕获 100,000 字节，并继续排空管道以避免阻塞。文件回写不设置大小上限，符号链接和特殊文件不回写。

命令继承宿主机应用进程的系统权限、环境变量与 `PATH`。Linux／macOS 使用 `sh -c`，Windows 使用 `cmd.exe /c`。解释器需预先安装；进程环境和 `cd` 不跨调用保留，长期后台服务不受支持。

## 从旧版本迁移

组件入口与配置迁移见 [统一工作区说明](../WORKSPACE.md)。

- 默认关闭改为默认开启。原先依赖默认关闭、且不需要命令的组件，请显式返回 `false`；需要命令的组件请保留后端所需的目录配置。
- Docker 现在也由 `enableShellTool()` 控制；原先 Docker 组件若覆盖了 `false`，此次更新后会关闭容器命令工具，删除该覆盖即可使用新默认值。
- 移除旧的 `harness.local-shell-enabled` 配置，不再需要两套开启入口。
- 默认白名单现在也应用到 Docker。顶层多命令拼接会被校验器拒绝，多步任务应使用脚本；`sh`、Python、Node 已在默认列表中。

## 验证

```bash
mvn -pl liteflow-testcase-el/liteflow-testcase-el-agent-harness,liteflow-testcase-el/liteflow-testcase-el-agent-core -am test \
  -DskipTests=false \
  -Dtest=SandboxShellToolTest,HarnessLocalShellTest,LocalExecutionFilesystemTest,GuardedLocalFilesystemTest,HarnessConfigTest,HarnessAgentComponentTest,HarnessMemoryModeTest,ToolkitRuntimeTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

示例项目的 `scripts/web_local_acceptance.py` 使用本地模拟模型，通过真实 HTTP 验证 Python、Node、固定工作目录、附件下载和重启恢复。MySQL／Redis 验收还会删除本地副本，确认可从数据库恢复。
