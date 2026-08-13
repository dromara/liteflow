# `liteflow-testcase-el` 外历史测试审计

## 结论

完成 AgentScope 2 测试迁移后，排除 `.git`、`.worktrees`、`target` 和整个
`liteflow-testcase-el`，仓库中仍有 97 个 `src/test` 文件。它们全部位于
`liteflow-benchmark`，其中 79 个 Java 文件、18 个测试资源；没有发现其他生产模块测试。

AgentScope 2 升级基线 `2655a16b13d7fd95c48575c979cb5b3f45b84b7b` 中同样存在这 97 个
文件，且本次升级分支没有修改它们，因此可以确认它们与 AgentScope 2 升级无关。

## 明细

| benchmark 子模块 | 文件数 | 最早日期 | 最早提交 |
|---|---:|---|---|
| `liteflow-benchmark-common` | 10 | 2024-09-29 | `857a6d6ea` |
| `liteflow-benchmark-common-example` | 29 | 2025-09-04 | `09e3dcf6a` |
| `liteflow-benchmark-compile` | 24 | 2025-12-02 | `3b43111f1` |
| `liteflow-benchmark-script-groovy` | 5 | 2024-09-29 | `7c9bf1ebe` |
| `liteflow-benchmark-script-java` | 10 | 2024-09-29 | `7c9bf1ebe` |
| `liteflow-benchmark-script-javax` | 6 | 2024-09-26 | `9334b1e84` |
| `liteflow-benchmark-script-javax-pro` | 5 | 2025-01-16 | `5805a1e53` |
| `liteflow-benchmark-script-qlexpress` | 8 | 2025-12-05 | `6206697c8` |
| 合计 | 97 | — | — |

## 后续迁移风险

这些文件不是普通功能回归测试，而是 benchmark／JMH 相关工程内容。若后续集中迁移，需要
单独设计以下边界：

- JMH 编译、注解处理和运行 classpath；
- benchmark 资源的相对路径与 module base directory；
- script language 插件的独立依赖与启动成本；
- benchmark profile、运行入口和整体构建时间；
- 是否新建专用 benchmark testcase 子模块，而不是混入 Agent testcase。

## 当前状态

本报告只做只读审计。按照用户要求，在获得下一阶段明确确认前，没有移动、编辑或重新配置
这 97 个历史文件。

