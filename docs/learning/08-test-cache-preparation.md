# 第八步：补齐测试缓存并离线复验

记录日期：2026-09-14（Etc/UTC）

## 本次唯一目标与边界

本次只为现有 Maven `test` 补齐可信本地缓存，并在缓存完整后使用原离线命令复验。允许缓存准备阶段从 Maven Central 或既有可信镜像解析必要构件，但不修改业务代码、测试代码、依赖版本、构建配置或安全策略，不跳过测试，不执行 `clean`、`verify`、打包、部署或单独启动应用。

## 开始前的现场与基准

### Codex 实际验证

- 已阅读根目录 `AGENTS.md`、`docs/learning/progress.md`、第七步记录、`pom.xml`、目标测试和应用/测试配置。
- 开始时工作区干净，当前 `HEAD` 为题目给出的 PR #6 合并提交 `25ac9cfab7214b9b87e446accf0e8c1e1031acb0`；`git merge-base --is-ancestor 25ac9cfab7214b9b87e446accf0e8c1e1031acb0 HEAD` 返回 `0`，所以本地已确认当前代码包含该提交。
- 仓库没有 Git 远程配置，因而没有实时核对远程最新 `dev`；这不影响上述本地提交关系事实，也不能据此声称本地就是远程最新状态。
- 已从该本地基准创建独立分支 `learn/step-8-offline-tests`，没有重置、清理、覆盖或合并现场。
- 环境为 Linux `amd64`、Java `21.0.2`、Maven Wrapper `3.9.16`；实际 Maven 本地仓库由离线 `help:evaluate` 成功求值得到 `/root/.m2/repository`。

## 工具职责与有效测试配置

1. **Maven Surefire Plugin**：绑定 Maven `test` 阶段，负责建立测试进程、发现测试、运行测试并生成汇总与报告；当前继承得到的版本是 `3.5.6`。
2. **JUnit Platform provider（JUnit 平台提供器）**：Surefire 与 JUnit 测试引擎之间的适配组件，使 Surefire 能通过 JUnit Platform 发现并调用 JUnit Jupiter 测试。本次已知缺失构件是 `org.apache.maven.surefire:surefire-junit-platform:3.5.6`。
3. **本地 Maven 缓存**：保存 Maven 已解析的插件、依赖、项目对象模型描述文件、来源标记及校验信息；离线模式只读这些已缓存内容。插件本体存在不代表动态选择的 provider 及其传递依赖也存在。
4. **传递依赖**：目标构件自身继续声明并需要的依赖。只手工放入一个 JAR 不能证明完整解析成功，因此本轮使用 Maven 标准解析目标并要求解析传递依赖。

测试配置检查显示 `src/test/resources/application.properties` 默认激活 `hsqldb,spring-data-jpa`，对应连接为 `jdbc:hsqldb:mem:petclinic`，即测试进程内的内存数据库；环境中没有发现 Spring 或数据库连接覆盖变量。由代码与配置可判断当前计划不会指向真实业务数据库，但由于测试未运行，这不是运行时连接证据。

## 缓存检查与准备结果

### 准备前检查

- `/root/.m2/repository/org/apache/maven/surefire/surefire-junit-platform/3.5.6` 不存在，POM 和 JAR 都未缓存。
- `/root/.m2/repository/org/apache/maven/surefire/` 下已有 Surefire `3.5.6` 的插件公共组件，但没有目标 provider。
- `target` 不存在，因此没有需要与本轮区分的旧 Surefire 报告。

### 实际解析命令与来源

使用项目已缓存的 Maven Dependency Plugin `3.10.0` 执行：

```bash
./mvnw --batch-mode --no-transfer-progress dependency:get \
  -Dartifact=org.apache.maven.surefire:surefire-junit-platform:3.5.6 \
  -Dtransitive=true
```

Maven 按当前有效仓库配置尝试从 Maven Central `https://repo.maven.apache.org/maven2` 读取 provider POM。命令通过启用 shell `pipefail` 并读取 `PIPESTATUS[0]` 记录，确认 **Maven 自身退出状态为 `1`**；仓库外临时日志没有纳入 Git。

### 首个有诊断价值的阻塞

解析在读取 `surefire-junit-platform:3.5.6` 的 POM 描述文件时失败：

```text
repo.maven.apache.org: Temporary failure in name resolution
```

`getent ahosts repo.maven.apache.org` 返回 `2`，`getent ahosts proxy` 返回 `0`；Maven 的 `.lastUpdated` 也记录同一 Central 域名解析错误。环境和用户级 Maven 设置均指向既有 `proxy:8080`。这些事实证明当前可信下载链路未能取得构件，但不足以进一步断定问题发生在代理实现还是其上游域名解析。

本次缓存变化只有 Maven Resolver 自动写入的 `surefire-junit-platform-3.5.6.pom.lastUpdated` 失败标记；它不是有效 POM、JAR、成功标记或可供离线使用的构件。没有伪造缓存元数据，也没有反复请求或关闭证书检查、更换未知下载源。

## 离线测试验收

缓存准备未完成后，没有重复执行已知必然缺少同一 provider 的离线测试命令。这样避免无新处理依据的重试；第七步已经保留了该准确失败证据。

- **最终离线验收命令是否执行：否。** 原因是前置缓存仍缺失，而不是测试代码失败。
- **测试是否实际执行：否。** 没有进入测试发现、初始化或方法运行。
- **执行数、失败数、错误数、跳过数：均不可得。** 不能填写为 `0/0/0/0`。
- **本轮测试报告：未生成。** `target/surefire-reports/` 不存在。
- **剩余缺失组件：**`org.apache.maven.surefire:surefire-junit-platform:3.5.6` 的有效 POM、JAR 及尚未完成解析的必要传递依赖。

最小外部调整：允许现有可信网络/代理链路解析并访问 `repo.maven.apache.org:443`，或通过组织现有的可信 Maven 制品镜像提供该 provider 及传递依赖。调整后先重试上述 `dependency:get`；确认构件完整后执行：

```bash
./mvnw --offline --batch-mode --no-transfer-progress test
```

结论仅覆盖本次缓存准备和未能开始的测试验收；性能脚本、Postman 集合、外部数据库、应用启动、接口、打包和部署仍未验证。

## 真实测试样例核对

目标是 `src/test/java/org/springframework/samples/petclinic/model/ValidatorTests.java` 中的 `shouldNotValidateWhenFirstNameEmpty`：

- **准备**：设置英文区域，创建 `Person`，把 `firstName` 设为空字符串、`lastName` 设为 `smith`，并创建 Bean Validation 校验器。
- **执行**：调用 `validator.validate(person)` 收集约束违反结果。
- **断言**：要求恰有一个违反项，属性路径为 `firstName`，英文消息为 `must not be empty`。
- **本轮实际结果**：该方法未执行，因此没有通过或失败结果。

## 本次对话的增量学习证据

### 学习者提供的证据

- 学习者能解释：使用 Repository 替身返回数据，不能证明真实数据库中存在对应记录。
- 学习者能判断：只检查姓氏的断言，可能让返回固定姓氏的错误实现通过。
- 对“查不到记录时服务抛异常”，学习者提出“给 service 注入异常，用 `doThrow` 检查”。

### ChatGPT 纠正与边界

正确测试思路应是：为 Repository 替身设置“未找到”的返回结果，调用真实 Service，再用 `assertThrows` 检查预期异常。`doThrow` 用于配置替身在调用时抛异常，不是检查真实代码是否抛出预期异常。上述内容只是本次对话中的教学示例，不是仓库代码，也没有实际执行；异常测试相关理解经讲解后仍未单独复核，不能记录为已经掌握。

## 验收结论

本步骤完成了基准、环境、缓存位置、有效测试/数据库配置及缺失构件的重新核实，并进行了一次标准可信解析尝试。缓存补齐与离线自动化测试验收均因 Maven Central 域名解析失败而未完成。第七步“测试运行前缺 provider”的历史保持不变；本次没有测试报告，不能声称测试通过或失败。

## 知识点

1. `dependency:get` 通过 Maven Resolver 解析 POM、JAR 和传递依赖，比手工复制单个 JAR 更能保持本地仓库的一致性。
2. `.lastUpdated` 是一次解析失败的状态记录，不是构件已缓存成功的证据；离线构建仍需要有效 POM、JAR 及必要依赖。
3. 下载失败、测试基础设施解析失败、测试初始化错误、运行错误和断言失败是不同阶段，结论不能相互替代。
4. 只有测试执行器产生完整汇总或报告后，才能记录执行、失败、错误和跳过数量。
5. 配置分析可排除已知的真实数据库指向，但测试未运行时不能把分析提升为运行时连接事实。

## 官方文档

- [Apache Maven Dependency Plugin：`dependency:get`](https://maven.apache.org/plugins/maven-dependency-plugin/get-mojo.html)
- [Apache Maven：本地仓库](https://maven.apache.org/repositories/local.html)
- [Apache Maven：构建生命周期](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Maven Surefire Plugin：JUnit Platform Provider](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html)
- [JUnit 5 用户指南：运行测试](https://docs.junit.org/5.14.1/user-guide/running-tests.html)
- [Mockito：抛出异常](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#doThrow(java.lang.Throwable...))
- [JUnit Jupiter：`assertThrows`](https://docs.junit.org/5.14.1/api/org.junit.jupiter.api/org/junit/jupiter/api/Assertions.html#assertThrows(java.lang.Class,org.junit.jupiter.api.function.Executable))

## 面试题及答案

### 1. 为什么只复制 `surefire-junit-platform` 的 JAR 可能仍不能离线运行测试？

**答案：**Maven 还需要构件的 POM 来解析依赖关系，provider 自身也可能依赖其他构件；本地仓库还维护来源等状态。缺少任何必要传递依赖都可能在离线解析或运行时继续失败。

### 2. `.lastUpdated` 文件能否证明 Maven 构件下载成功？

**答案：**不能。它可能记录某个仓库的最近失败及错误信息。本轮只有 `.lastUpdated`，而没有有效 POM 和 JAR，恰好证明解析尝试失败而非缓存完成。

### 3. `BUILD FAILURE` 是否等同于测试用例失败？

**答案：**不等同。必须查看失败阶段。本轮在下载 provider 的 POM 时失败，最终离线测试甚至没有执行；断言失败则必须发生在测试方法实际运行之后。

### 4. `doThrow` 和 `assertThrows` 的职责有什么区别？

**答案：**`doThrow` 配置 Mockito 替身在特定调用时抛出异常；`assertThrows` 执行被测代码并检查它是否抛出指定类型。验证 Service 的“未找到”业务路径时，应配置 Repository 返回未找到，再调用真实 Service 并用 `assertThrows` 验证。

### 5. 为什么检查测试数据库配置后仍不能声称测试使用了内存数据库？

**答案：**配置只说明按当前静态条件预计会怎样运行；本轮测试未启动，缺少运行日志或上下文证据，因此只能记录“未发现真实数据库覆盖”和配置推断，不能记录实际连接事实。

## 学习者练习（能力待验证）

请用自己的话解释：为什么本轮生成 `.lastUpdated` 后仍不能执行离线测试，以及恢复可信网络后应先观察哪两类证据来确认缓存完整。收到学习者回答或操作证据前，此项能力标记为“待验证”。
