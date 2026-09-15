# 第八步续作：Windows 本地缓存准备与离线测试复验

日期：2026-09-15（Asia/Shanghai）。本记录为新增证据，不改写云端失败历史。

## 目标与边界

继续第八步：恢复可信依赖解析，补齐现有测试所需缓存，再执行原离线测试命令。缓存与日志统一使用 D 盘。不修改业务代码、测试代码、依赖版本或项目构建配置，不跳过测试，不执行 clean、verify、打包、部署或单独启动应用。

## 开始前的实际核验

来源：本次 Codex 在 Windows 本地执行。

- 起始分支为 dev，HEAD 为 ff28614b6a23a1301b68ae58e1a5a7d3c9a87018，工作区干净。
- 通过只读 git ls-remote 实时查询，远程 dev 与该 HEAD 一致。
- 从 dev 创建独立分支 codex/step-08-windows-cache；提交祖先检查成功。
- Maven Wrapper 实际运行 Maven 3.9.16，Java 为 Temurin 21.0.12.1。
- MAVEN_USER_HOME 为 D:\derek\maven\_cache；用户 settings.xml 指定 localRepository 为 D:/derek/maven/_cache/repository。
- 原 C:\Users\derek\.m2 为指向 D 盘缓存根目录的目录链接；Maven 临时目录为 D:\derek\maven\_cache\tmp。
- 开始时本地 repository 为空、项目 target 不存在。因此不能沿用云端“只缺一个 provider”的缓存前提。
- 测试默认配置为 hsqldb,spring-data-jpa；服务测试显式选择 hsqldb 或 h2，源码中的连接为内存数据库。本轮未发现 Spring 或数据库相关环境变量覆盖。此处为配置核对，不单独构成实际数据库连接证据。
- 下载的 spring-boot-starter-parent:4.1.1 声明 java.version=17，maven.compiler.release 引用该属性；Java 21 的工具运行版本与 Java 17 编译目标需区分。云端曾记录有效 java.version=21.0.2，该历史保留，不套用于当前本地配置。

## 标准解析已取得的证据

第一条命令：

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress dependency:get '-Dartifact=org.apache.maven.surefire:surefire-junit-platform:3.5.6' '-Dtransitive=true'
```

- dependency:get 实际使用 Maven Dependency Plugin 3.10.0。
- 日志确认解析指定 provider 及传递依赖，BUILD SUCCESS，Maven 自身退出状态为 0。
- 命令结束时间为 2026-09-15T23:32:52+08:00，用时 4 分 6 秒。
- Windows 本次标准解析成功；云端 2026-09-14 域名解析失败仍是当时的真实结果。
- 本条命令只证明依赖解析成功，没有执行测试。

为补齐空缓存下的项目和编译插件依赖，接着执行测试前编译阶段，并显式解析测试执行插件：

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress test-compile org.apache.maven.plugins:maven-dependency-plugin:3.10.0:get '-Dartifact=org.apache.maven.plugins:maven-surefire-plugin:3.5.6' '-Dtransitive=true'
```

这属于第八步的缓存准备，test-compile 会自然执行主代码和测试源码编译，不运行测试，也没有传入跳过测试参数。最终验收仍以原定离线 test 的实际结果为准。

## 新增学习者证据

来源：本次对话中学习者本人回答。

1. 学习者能解释 wrapper 保存 Maven 发行包缓存、repository 保存项目依赖和插件、tmp 保存 Maven 临时文件。这三个目录用途的概念区分已有独立回答证据，不扩大为已掌握独立配置或故障排查。
2. 对“dependency:get 的 BUILD SUCCESS 是否证明测试通过”，学习者回答：“不能，只能说明本次mvn命令执行完毕。命令中不一定会进行test阶段。还需要查看具体单测执行结果”。
3. Codex 纠正：BUILD SUCCESS 表示请求的目标成功完成，不只是执行完毕；本条 dependency:get 不执行测试阶段。最终应结合执行、失败、错误、跳过数量以及 Maven 退出状态。
4. 能区分本次依赖解析成功与测试通过，已有学习者回答证据；“成功完成”的精确表述、完整测试报告分析和独立故障排查仍需复核。本项目还包含加载 Spring 上下文和内存数据库的测试，不能笼统称为全部都是隔离的单元测试。

## 官方文档

- [Maven Dependency Plugin：dependency:get](https://maven.apache.org/plugins/maven-dependency-plugin/get-mojo.html)
- [Maven 构建生命周期](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [Maven Wrapper 缓存位置](https://maven.apache.org/tools/wrapper/#specifying-maven-distribution-base-path)

## 知识点与面试题

1. **为什么迁移到 Windows 后不能只补云端缺少的一个组件？** 本地依赖缓存与云端缓存独立，本轮 repository 开始为空，还需要父配置、项目依赖、编译插件和测试执行所需构件。
2. **dependency:get 输出 BUILD SUCCESS 能否证明测试通过？** 不能，它只证明该次依赖解析目标成功；必须执行测试并检查报告和退出状态。
3. **为什么 test-compile 成功也不能证明测试通过？** 它只运行到测试源码编译，测试发现、上下文初始化和断言执行在之后。
4. **为什么最终仍需离线 test？** 它验证当前缓存足以支撑本项目本次测试流程，并取得测试执行证据；不能据此证明所有后续阶段的缓存完整。

## 学习者练习

结合本轮最终日志，指出测试是否实际执行，列出支持判断的执行数、失败数、错误数、跳过数和退出状态；如果没有测试报告，解释阻塞发生在哪一阶段。收到具体回答前，完整报告分析能力仍标记为待验证。

## 本轮执行结果与最终验收

### 缓存准备

- 测试前编译及 Surefire 插件解析成功，Maven 退出状态 0；结束于 2026-09-15T23:45:00+08:00，用时 11 分 2 秒。
- 日志确认 115 个主源码文件和 22 个测试源码文件编译成功，均为 release 17。它们属于本轮缓存准备和测试流程，不另记为新的独立编译学习步骤。
- 缓存中存在 surefire-junit-platform:3.5.6 的有效描述文件、程序包及标准解析元数据。

### 第一次离线验收：发现额外缓存缺口

```powershell
.\mvnw.cmd --offline --batch-mode --no-transfer-progress test
```

- 结束于 2026-09-15T23:46:08+08:00，用时 9.031 秒，Maven 退出状态 1。
- 主代码和测试源码编译通过，Surefire 自动选择 JUnitPlatformProvider。
- 测试实际运行前发现缺少 org.junit.platform:junit-platform-launcher:6.0.3；当时没有测试执行结果，不能将数量记为全零通过。
- 这次失败属于新增缓存缺口，不是测试断言失败。历史日志独立保留。

依据真实日志执行标准补齐：

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress dependency:get '-Dartifact=org.junit.platform:junit-platform-launcher:6.0.3' '-Dtransitive=true'
```

解析成功，Maven 退出状态 0，结束于 2026-09-15T23:46:38+08:00。版本来自本地实际测试执行器请求，没有修改项目声明，也没有照搬云端旧测试框架版本。

### 第二次离线验收：通过

再次执行完全相同的离线 test 命令：

- 结束时间：2026-09-15T23:47:28+08:00。
- 用时：34.634 秒。
- Maven 自身退出状态：0。
- 日志：BUILD SUCCESS。
- 测试实际执行数：237。
- 断言失败数：0。
- 执行错误数：0。
- 跳过数：0。
- Surefire 生成 19 份 TEST-*.xml 测试报告；Codex 独立汇总所有报告的数字，与控制台汇总完全一致。

因此，第八步的“补齐当前测试缓存并完成离线 test 验收”已在 Windows 本地通过。

### 真实样例与数据库证据

- ValidatorTests.shouldNotValidateWhenFirstNameEmpty 本轮已实际执行并通过，报告用时 0.701 秒。它准备空 firstName 的 Person，调用真实校验器，再检查违反项数量、属性路径和消息。
- 服务测试报告包含 ClinicServiceH2JdbcTests、ClinicServiceHsqlJdbcTests 等，运行日志显示 jdbc:h2:mem:petclinic 和 jdbc:hsqldb:mem:petclinic。本轮已有内存数据库使用的运行时证据，超出先前仅静态分析配置的证据范围。
- 测试中加载 Spring 上下文，不等于已单独启动可供外部访问的应用；外部接口访问、外部数据库、打包和部署仍未验证。
- 19 份报告与 22 个测试源文件不是矛盾：测试源文件还包含抽象基类，文件数不等于实际执行的测试类数或测试方法数。

## 本地证据位置

统一位于 D:\derek\maven\_cache\learning-evidence\step08-windows-20260915，不加入 Git：

| 文件 | 内容 |
| --- | --- |
| 01-provider-get.log | 原缺失 provider 及传递依赖解析成功 |
| 02-test-cache-prepare.log | 测试前编译与 Surefire 插件解析成功 |
| 03-offline-test.log | 第一次离线验收，缺 launcher，退出 1 |
| 04-launcher-get.log | 补齐真实缺失 launcher，退出 0 |
| 05-offline-test-retry.log | 最终离线测试 237/0/0/0，退出 0 |
| test-summary.json、test-suites.csv | 从报告重新计算的总数与分组明细 |
| surefire-reports/ | 19 份原始测试报告备份 |

Maven 退出状态是在每次原生命令完成后立即读取的 LASTEXITCODE，没有使用日志读取命令的退出状态代替它。原始日志中的部分中文编译器说明存在终端编码显示问题；关键阶段、数字、英文结果及机器可读测试报告已交叉核对，没有为此修改项目编码或编译配置。

## 当前结论与唯一下一步建议

第八步执行验收已完成；这不能自动证明学习者已掌握依赖故障排查或完整测试报告分析。先由学习者解释本轮 237/0/0/0、退出状态 0 以及测试报告分别支持什么结论，完成本页练习。应用首次启动应作为后续独立学习步骤，在新的明确请求下进行，本次不执行。
