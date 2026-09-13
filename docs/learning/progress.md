# 学习进度

## 第一步：项目初步认识

- 状态：已在此前 ChatGPT 会话中通过远程阅读完成初步认识。
- 边界：这不是在当前 Codex 环境完成的构建、测试或应用启动；不得据此宣称项目可运行。

## 第二步：检查代码基准和构建环境

- 日期：2026-09-10（Etc/UTC）
- 状态：检查已完成，环境验收存在明确阻塞。
- 记录：[`02-environment-check.md`](./02-environment-check.md)
- 已确认：当前为干净的具名分支 `work`，起始提交 `4cd8e1b0cd42578e882247d8801f6be5d402f118`；Git `2.43.0`、Java 运行时及编译器 `21.0.2`；项目 Maven Enforcer 配置值 `3.9.9`，Wrapper 指定 Maven `3.9.16`。
- 尚未确认：因为本地无远程/`dev` 引用且 GitHub 查询被代理以 403 拒绝，无法判断当前提交是否基于远程 `dev`；父 POM 未缓存且网络不可用，未在当前环境展开继承的 `${java.version}`；未执行构建、测试或启动。
- 阻塞：`./mvnw -version` 因无法解析 `repo.maven.apache.org` 以状态 `1` 失败。

## 第二步当时的下一步

**解决已经确认的具体环境问题**：恢复 Maven Central 访问或可信地预置 Wrapper 指定的 Maven `3.9.16`，重试 `./mvnw -version`；恢复题目 GitHub 仓库只读访问并通过提交关系核对 `dev` 基准。不要执行强制重置、清理或覆盖已有修改。

## 第三步：定位 Maven Wrapper 下载访问问题

- 日期：2026-09-11（Etc/UTC）
- 状态：诊断及最小修复尝试已完成，Wrapper 验收仍被明确的网络策略阻塞。
- 记录：[`03-maven-wrapper-access.md`](./03-maven-wrapper-access.md)
- 已确认：Wrapper 下载地址指定 Maven `3.9.16`；本机域名解析失败，已配置的代理对该目标返回 `403`；标准 Wrapper 缓存及常见本地目录没有同版本发行包；`./mvnw -version` 在下载阶段以状态 `1` 失败；当前 Java 运行时为 `21.0.2`。
- 尚未确认：Maven `3.9.16` 的实际启动、继承的 `java.version`、最终编译目标版本；未执行完整构建、测试、应用启动或数据库部署。
- 个人能力：待学习者回答第三步记录中的独立故障分析练习后验证。

## 下一步

**复验当前阻塞**：允许代理访问 `repo.maven.apache.org:443`，或通过受信任制品缓存提供配置指定的 Maven `3.9.16`，然后运行 `./mvnw -version`。Wrapper 成功后再查询继承的 Java 与编译属性；在两项均成功前，不进入“首次构建”。

## 第四步：首次离线编译

- 日期：2026-09-13（Etc/UTC）
- 状态：离线主代码编译已完成并通过。
- 记录：[`04-first-offline-compile.md`](./04-first-offline-compile.md)
- 用户提供的上一任务结果：`./mvnw -version` 退出状态为 `0`；Wrapper 使用 Maven `3.9.16` 和 Java `21.0.2`；此前下载阻塞不再复现。该补充不覆盖第二、三步当时的失败历史。
- Codex 本次实际验证：开始时 `target` 不存在；`./mvnw --offline --batch-mode --no-transfer-progress compile` 编译 115 个源文件，生成 OpenAPI 与 MapStruct 源码及 126 个 `.class` 文件，输出 `BUILD SUCCESS`，Maven 真实退出状态为 `0`。
- 验收结论：本地缓存足够完成本次 `compile`，主代码在 Java 21.0.2 构建环境中成功编译为 Java 17 目标字节码；Wrapper 可用、缓存足够和主代码编译成功均已有对应证据。
- 尚未验证：自动化测试、应用启动、接口、外部数据库、打包和远程 `dev` 最新提交；不得从编译成功推导这些项目成功。
- 个人能力：第三步练习仍未回答；第四步新增编译运行时、目标版本与生命周期范围练习。收到学习者回答前均为待验证。

## 第四步后的下一步

**执行现有自动化测试**。应作为下一独立学习步骤进行；本次不自动运行测试或启动应用。

## 第五步：主代码离线编译复验

- 日期：2026-09-13（Etc/UTC）
- 状态：离线主代码编译复验已完成并通过。
- 记录：[`05-main-code-compile-revalidation.md`](./05-main-code-compile-revalidation.md)
- Codex 本次实际验证：执行前 `target` 不存在；Java 运行时与编译器均为 `21.0.2`，Wrapper 使用 Maven `3.9.16`；`./mvnw --offline --batch-mode --no-transfer-progress compile` 编译 115 个源文件，执行 OpenAPI 与 MapStruct 代码生成，产生 126 个 `.class` 文件，并以状态 `0` 输出 `BUILD SUCCESS`。
- 验收结论：当前提交的主代码在本地缓存和 Java 21.0.2 环境下成功编译为 Java 17 目标字节码。
- 尚未验证：自动化测试、应用启动、接口、数据库、打包，以及当前提交与远程 `dev` 的关系；不得由编译成功推导这些事项成功。
- 个人能力：此前练习及本次编译证据分析练习尚未收到学习者回答，继续标记为“待验证”。

## 第五步后的下一步

**由学习者先完成第五步记录中的练习**；之后可把“执行现有自动化测试”作为新的独立学习步骤。本次不自动运行测试或启动应用。
