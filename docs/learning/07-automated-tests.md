# 第七步：执行现有自动化测试

记录日期：2026-09-14（Etc/UTC）

## 本次唯一目标与边界

本次只分析现有测试、离线执行 Maven `test` 并记录结果；不执行 `clean`、`verify`、打包、部署或应用单独启动，不修改业务代码、测试代码、依赖版本及构建配置，也不以跳过测试或放宽断言规避失败。

## 开始前的现场与基准

### Codex 本次实际验证

- 已读取根目录 `AGENTS.md`、`docs/learning/progress.md`、第六步记录、`pom.xml`、测试源码及测试资源配置。
- 开始时工作区干净，当前 `HEAD` 为已知 PR #5 合并提交 `25229c6daed4b232dc3bf8d743a52dd9073c0324`；`git merge-base --is-ancestor 25229c6daed4b232dc3bf8d743a52dd9073c0324 HEAD` 返回 `0`。因此本地已确认代码包含该提交。
- 仓库没有配置 Git 远程。对题目所给 GitHub 地址执行只读 `git ls-remote ... refs/heads/dev` 时，代理返回 `CONNECT tunnel failed, response 403`，命令状态为 `128`；所以没有实时确认远程 `dev` 的最新提交。
- 已从该本地基准创建独立分支 `learning/07-automated-tests`；没有重置、清理、覆盖或合并现场。

## 测试方案分析

### 工具及职责

1. **JUnit Jupiter**：JUnit 5 的测试编程模型与引擎；源码中的 `@Test` 标记测试方法。
2. **Maven Surefire Plugin**：Maven 在 `test` 阶段发现并运行测试的插件。有效 POM 显示版本为 `3.5.6`，使用 JUnit Platform provider 承接 JUnit Jupiter 测试。
3. **Spring Boot Test**：`spring-boot-starter-test` 提供 Spring 测试上下文、常用测试库和自动配置支持；带 `@SpringBootTest` 的测试会加载 Spring 应用上下文。
4. **Mockito 与 Spring Security Test**：Mockito 用模拟对象隔离服务依赖；Spring Security Test 提供认证等安全测试支持。控制器测试结合 `@MockitoBean` 和 MockMvc 在进程内模拟 HTTP 请求，不需要监听真实网络端口。
5. **AssertJ**：提供流式断言，例如 `assertThat(...).isEqualTo(...)`。
6. **JaCoCo**：在测试进程上挂载代理并收集覆盖率执行数据。本项目的覆盖率 `check` 和 `report` 没有绑定到本次所到达的 `test` 阶段，不能把本轮当成覆盖率阈值验收。

工具名称来自 `pom.xml`、测试源码、有效 POM 与本轮 Maven 日志；不能仅凭测试文件名判定测试性质。

### 测试代码实际涉及的范围

- `ValidatorTests`、`PetAgeValidatorTest` 等直接构造对象或校验器，不加载完整 Spring 上下文。
- 控制器测试使用 `@SpringBootTest`、MockMvc 和模拟服务依赖，会加载 Spring 上下文并在进程内验证请求与响应。
- 服务测试继承抽象测试用例，使用 `@SpringBootTest` 与显式 profile 运行真实仓库/服务逻辑；这类测试会初始化测试数据库，不能仅称为隔离的单元测试。
- `SpringConfigTests` 检查 Spring 配置。是否执行及结果必须以 Surefire 报告为准，而不能仅由源码存在推断。

### 数据库、外部服务与环境变量

- `src/test/resources/application.properties` 默认激活 `hsqldb,spring-data-jpa`，并从仓库内 `db/${spring.sql.init.platform}/schema.sql` 与 `data.sql` 初始化数据。
- 服务测试显式选择的数据库 profile 是 `hsqldb` 或 `h2`；相应 JDBC URL 是 `jdbc:hsqldb:mem:petclinic` 或 `jdbc:h2:mem:petclinic...`，均为测试进程内的内存数据库。
- 本轮检查没有发现测试激活 `mysql` 或 `postgres` profile，也没有发现测试所需的必填环境变量或外部服务。因此依据现有配置推断，计划中的测试不会操作外部业务数据库。由于测试最终未运行，该判断是代码与配置分析结果，不是运行时连接证据。

### 命令覆盖与排除项

计划且实际调用：

```bash
./mvnw --offline --batch-mode --no-transfer-progress test
```

- Maven Wrapper 固定构建工具版本；`--offline` 只允许使用本地缓存；`--batch-mode` 禁用交互；`--no-transfer-progress` 减少传输进度输出；`test` 自然执行生成源码、主代码编译、测试资源处理、测试源码编译及测试阶段。
- 命令不会运行 `src/test/jmeter` 的 JMeter 性能脚本或 `src/test/postman` 的 Postman 接口集合，也不进入 Failsafe 的 `integration-test`/`verify` 阶段，不单独启动应用、不打包、不部署、不验证外部数据库。

## 初学者阅读样例

- 路径：`src/test/java/org/springframework/samples/petclinic/model/ValidatorTests.java`
- 类：`ValidatorTests`
- 方法：`shouldNotValidateWhenFirstNameEmpty`
- 最少上下文：`Person.firstName` 声明了 `@NotEmpty`；`createValidator()` 创建并初始化 Bean Validation 校验器。

```java
@Test
void shouldNotValidateWhenFirstNameEmpty() {

    LocaleContextHolder.setLocale(Locale.ENGLISH);
    Person person = new Person();
    person.setFirstName("");
    person.setLastName("smith");

    Validator validator = createValidator();
    Set<ConstraintViolation<Person>> constraintViolations = validator.validate(person);

    assertThat(constraintViolations.size()).isEqualTo(1);
    ConstraintViolation<Person> violation = constraintViolations.iterator().next();
    assertThat(violation.getPropertyPath().toString()).isEqualTo("firstName");
    assertThat(violation.getMessage()).isEqualTo("must not be empty");
}
```

练习：请用自己的话说明它准备了什么、执行了什么、验证了什么。本轮阻塞发生在测试发现/运行之前，所以这只是已阅读的真实样例，**不能声称该方法已经执行**；学习者解释能力仍待验证。

## 实际执行证据与故障定位

### 环境与命令

- 执行时间：2026-09-14 12:11 UTC。
- 系统：Linux `amd64`；Java `21.0.2`；Maven Wrapper 实际使用 Apache Maven `3.9.16`。
- 被验证提交：`25229c6daed4b232dc3bf8d743a52dd9073c0324`。
- 分支：`learning/07-automated-tests`。
- 命令：`./mvnw --offline --batch-mode --no-transfer-progress test`。
- 为同时观察输出与保留日志，执行时开启 shell `pipefail` 并通过 `tee` 写入仓库外的 `/tmp/petclinic-step-07-test.log`；立即读取管道首项状态，确认 **Maven 自身退出状态为 `1`**。临时日志没有加入 Git。

### 到达的阶段

Maven 已完成或到达以下关键步骤：

1. Java 与 Maven 版本规则检查通过；
2. JaCoCo `prepare-agent` 已设置测试进程参数；
3. OpenAPI 生成和主代码编译完成，编译了 115 个主源文件；
4. 测试资源已复制，`testCompile` 编译了 22 个测试源文件；
5. Surefire `3.5.6` 进入 `test` 目标并自动选择 `JUnitPlatformProvider`；
6. 在测试发现和运行前解析 provider 失败，构建终止。

测试命令自然发生的前序编译是本轮测试生命周期的一部分，不另记为第三次独立编译复验。

### 首个有诊断价值的错误

```text
The following artifacts could not be resolved:
org.apache.maven.surefire:surefire-junit-platform:jar:3.5.6 (absent):
Cannot access central (...) in offline mode and the artifact ... has not been downloaded from it before.
```

分类为 **本地依赖或插件缓存缺失**，不是主代码/测试代码编译失败、测试环境初始化失败、测试运行错误或断言失败。缺少的是 Surefire 用来连接 JUnit Platform 的 provider 构件；`--offline` 正确地阻止 Maven 联网获取它。本轮没有以联网、修改依赖或更换命令扩展处理，也没有对同一阻塞反复重试。

### 测试执行与报告验收

- 执行前 `target/surefire-reports` 不存在；失败后该目录仍不存在。
- 日志没有任何 `Tests run:` 汇总；Surefire 在 provider 解析阶段终止。
- **是否真正执行测试：否。** 测试源码成功编译不等于测试方法已运行。
- **测试数量、失败、错误、跳过：均不可得，而不是 `0/0/0/0`。** 没有进入测试运行阶段，不能把“没有报告”表述为零个测试且通过。
- **本轮报告位置：没有生成 Surefire 测试报告。** 正常预期位置是 `target/surefire-reports/`，但本轮不存在；`target/jacoco.exec` 即使出现也不是测试结果报告，不能替代 Surefire 汇总。
- 阅读样例 `ValidatorTests.shouldNotValidateWhenFirstNameEmpty` 没有实际执行，因此没有该方法的通过/失败结果。

## 验收结论

本轮已完成现有测试方案分析和一次合规的离线 `test` 调用，但**自动化测试通过的目标未达成**：测试代码编译成功后，因本地缺少 `surefire-junit-platform:3.5.6` 而在测试发现/运行前终止。结论仅限这次命令；应用启动、接口、真实 HTTP 服务、外部数据库、JMeter、Postman、打包、部署和远程 `dev` 最新状态仍未验证。

最小后续处理建议：在允许访问 Maven Central 的可信环境中，仅补齐项目有效 POM 所需的 `org.apache.maven.surefire:surefire-junit-platform:3.5.6` 及其传递依赖（或由可信制品缓存预置），然后在相同提交上重新执行同一离线 `test` 命令。不要通过跳过测试或直接改用更后生命周期掩盖该阻塞。

## 知识点

1. `testCompile` 成功只证明测试源码可以编译；Surefire provider 可用、测试发现、测试方法运行和断言通过是后续不同证据层级。
2. Maven 插件本体已缓存，不代表插件运行时动态选择的 provider 及其传递依赖也已缓存。
3. `BUILD FAILURE` 应结合失败阶段分类；本轮是测试运行基础设施解析失败，而不是已有测试用例失败。
4. 没有 Surefire 报告和 `Tests run:` 汇总时，测试计数是“不可得”，不能填写为全零通过。
5. `@SpringBootTest` 表示加载 Spring 应用上下文；使用内存数据库的服务测试会运行真实数据访问逻辑，不能仅按文件名笼统称为单元测试。

## 官方文档

- [Apache Maven：构建生命周期简介](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Apache Maven：离线模式设置](https://maven.apache.org/ref/current/maven-embedder/cli.html)
- [Maven Surefire Plugin：JUnit Platform Provider](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html)
- [JUnit 5 用户指南：运行测试](https://docs.junit.org/5.14.1/user-guide/running-tests.html)
- [Spring Boot：测试应用程序](https://docs.spring.io/spring-boot/reference/testing/index.html)
- [Spring Framework：MockMvc](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html)
- [JaCoCo Maven Plugin：`prepare-agent`](https://www.jacoco.org/jacoco/trunk/doc/prepare-agent-mojo.html)

## 面试题及答案

### 1. `testCompile` 成功能否证明测试通过？为什么？

**答案：**不能。它只证明测试源码及其编译期依赖可用。测试仍要经过测试引擎/provider 加载、发现测试、初始化环境、运行方法和执行断言；任一环节都可能失败。

### 2. Maven Surefire Plugin 与 JUnit Jupiter 分别负责什么？

**答案：**Surefire 是 Maven `test` 阶段的测试执行插件，负责建立测试进程、选择 provider、发现并汇总测试；JUnit Jupiter 提供 JUnit 5 的注解、编程模型和测试引擎。Surefire 通过 JUnit Platform provider 调用相应引擎。

### 3. 为什么 Maven 离线 `compile` 成功，离线 `test` 仍可能缺少构件？

**答案：**`test` 会使用主代码编译未必需要的测试依赖、测试编译步骤和测试插件运行时组件。本轮就是 Surefire 插件能够启动，但它动态需要的 JUnit Platform provider 未进入本地缓存。

### 4. 测试数量应该何时记为零，何时记为不可得？

**答案：**只有测试执行器完成发现/运行并明确报告发现零个测试时，才能记录“执行器报告 0 个”；若在执行器启动、provider 解析或环境初始化前中断且没有汇总，数量应写“不可得”。

### 5. `@SpringBootTest` 测试为什么不应仅凭文件名叫作单元测试？

**答案：**测试性质取决于实际边界。`@SpringBootTest` 会加载 Spring 应用上下文，服务测试还可能初始化内存数据库并运行真实仓库逻辑，协作组件范围明显大于只隔离一个类的测试；应结合注解、依赖替身和真实资源判断。

## 学习者练习（能力待验证）

请先回答阅读样例问题，再根据本轮日志解释：为什么“22 个测试源文件已编译”不能写成“22 个测试已运行”，以及缺失的 provider 位于测试流程的哪一层。当前只有 Codex 的执行证据和本文讲解；在收到学习者自己的回答或操作证据前，相关能力保持“待验证”。
