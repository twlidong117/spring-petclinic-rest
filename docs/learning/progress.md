# 学习进度

## 当前状态摘要（2026-09-14 第八步）

- 当前阶段：第四步首次离线编译与第五步离线编译复验均已通过；第七步离线 `test` 在测试运行前因缺少 Surefire JUnit Platform provider 失败；第八步重新确认缺失并进行一次标准缓存解析，但 Maven Central 域名解析失败，缓存与最终离线测试验收均未完成。自动化测试通过仍未得到证明。
- 学习者回答：已能区分编译成功与应用启动成功；关于 `BUILD SUCCESS` 与测试的原始回答部分正确，ChatGPT 已纠正讲解，但纠正后的判断尚未由学习者单独复核。已能判断 `release 17` 下不能使用 Java 21 才正式支持的语法，并在讲解后区分构建工具运行版本与编译目标。学习者能解释 Repository 替身数据不证明真实数据库记录存在，也能识别只断言姓氏可能放过固定姓氏错误；异常测试中曾提出用 `doThrow` 检查，ChatGPT 已纠正为设置 Repository 未找到、调用真实 Service 并用 `assertThrows` 检查。该教学示例未进入仓库也未执行，纠正后的异常测试理解尚未单独复核。网络故障、生成源码流程、测试缓存范围及测试证据分析的独立解释能力仍待验证。
- 外部补充证据：学习者提供的 2026-09-14 ChatGPT GitHub 远程提交比较显示，`dev` 为 `52975c6e0ce2a49fe2c7d08cbe4ff1937b45d3a1`，PR #4 分支为 `be6a999d769ce8d414095a7b3df91e046781687d`，共同祖先是该 `dev` 提交，PR 分支当时领先 1 个提交、落后 0 个提交。这是注明日期和来源的历史快照，不是当前实时状态；此前 Codex 离线环境无法核验远程关系的历史事实继续保留。
- Codex 第七步实际验证：在提交 `25229c6daed4b232dc3bf8d743a52dd9073c0324` 上执行 `./mvnw --offline --batch-mode --no-transfer-progress test`；主代码与 22 个测试源文件成功编译，Surefire 因本地缺少 `surefire-junit-platform:3.5.6` 以 Maven 状态 `1` 终止，未实际运行测试，也未生成 `target/surefire-reports`。测试数量、失败、错误及跳过数量均不可得，不能记为全零通过。
- Codex 第八步实际验证：本地 `HEAD` 正是 PR #6 合并提交 `25ac9cfab7214b9b87e446accf0e8c1e1031acb0`，实际 Maven 仓库为 `/root/.m2/repository`，provider 的 POM/JAR 仍不存在。执行联网 `dependency:get` 并要求传递解析时，读取 Maven Central 上的 provider POM 因 `repo.maven.apache.org` 域名解析失败，Maven 状态为 `1`；仅新增无效的 `.lastUpdated` 失败标记。未重复执行已知仍缺 provider 的离线测试，测试仍未实际执行，数量均不可得，`target/surefire-reports` 未生成。
- 当前边界：自动化测试通过、应用启动、接口、数据库、打包及远程最新 `dev` 仍未验证；缓存下载失败和第七步 provider 解析失败均不证明测试代码有缺陷。第八步记录：[`08-test-cache-preparation.md`](./08-test-cache-preparation.md)

## 唯一下一步

**恢复可信制品解析后补齐缓存并复验同一离线测试命令**。先让现有可信网络/代理链路能够解析并访问 `repo.maven.apache.org:443`，或通过组织现有可信 Maven 镜像提供 `org.apache.maven.surefire:surefire-junit-platform:3.5.6` 及其传递依赖；用第八步的 `dependency:get` 命令确认解析完整后，执行 `./mvnw --offline --batch-mode --no-transfer-progress test`。不得跳过测试或扩大为 `verify`。

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

## 第三步当时的下一步建议

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

## 第四步当时的下一步建议

**执行现有自动化测试**。应作为下一独立学习步骤进行；本次不自动运行测试或启动应用。

## 第五步：主代码离线编译复验

- 日期：2026-09-13（Etc/UTC）
- 状态：离线主代码编译复验已完成并通过。
- 记录：[`05-main-code-compile-revalidation.md`](./05-main-code-compile-revalidation.md)
- Codex 本次实际验证：执行前 `target` 不存在；Java 运行时与编译器均为 `21.0.2`，Wrapper 使用 Maven `3.9.16`；`./mvnw --offline --batch-mode --no-transfer-progress compile` 编译 115 个源文件，执行 OpenAPI 与 MapStruct 代码生成，产生 126 个 `.class` 文件，并以状态 `0` 输出 `BUILD SUCCESS`。
- 验收结论：当前提交的主代码在本地缓存和 Java 21.0.2 环境下成功编译为 Java 17 目标字节码。
- 尚未验证：自动化测试、应用启动、接口、数据库、打包，以及当前提交与远程 `dev` 的关系；不得由编译成功推导这些事项成功。
- 个人能力：此前练习及本次编译证据分析练习尚未收到学习者回答，继续标记为“待验证”。

## 第五步当时的下一步建议

**由学习者先完成第五步记录中的练习**；之后可把“执行现有自动化测试”作为新的独立学习步骤。本次不自动运行测试或启动应用。
