# 第四步：首次离线编译

检查日期：2026-09-13（Etc/UTC）

## 本次唯一目标与边界

本次只使用 Maven Wrapper 在离线模式下执行主代码编译，验证本地缓存是否足够、项目主代码是否能在 Java 21 环境中完成编译，以及构建配置中的代码生成是否实际执行。不运行测试、不启动应用、不执行 `clean`，也不修改业务代码、依赖版本或运行配置。

## 开始前的基准与现场

### Codex 实际验证

- 开始时当前分支为 `work`，工作区干净，`HEAD` 为 `fbf49d0aaa055e4cd1c15e68c55742e542967b86`。
- 提交图显示该提交合并了前两步学习记录，并包含此前用户提供的基准提交 `4cd8e1b0cd42578e882247d8801f6be5d402f118`。
- 本地没有远程配置和 `dev` 引用，因此本次离线环境不能独立核验远程 `dev` 的最新提交；分支关系没有仅凭名称作出判断。
- 从上述干净提交创建独立分支 `learning/04-first-offline-compile`，没有强制重置、清理或覆盖文件。
- 执行前 `target` 目录不存在。本次没有执行 `clean`，但也没有复用现场中的旧 `target/classes` 或旧生成代码。

### 用户提供的上一任务结果

- `./mvnw -version` 退出状态为 `0`。
- Maven 实际版本为 `3.9.16`，与 Wrapper 配置一致；Maven 使用 Java `21.0.2`。
- Maven Wrapper 下载阻塞已不再复现。
- 当时尚未证明项目可以编译、测试通过或应用启动，且上一任务没有修改文件或创建合并请求。

以上内容按“用户提供的上一任务结果”记录，不改写第二、三步中 Codex 当时实际观察到的下载失败历史。

## 新概念

- **编译（compile）**：把 `.java` Java 源代码转换为 `.class` Java 字节码。Java 字节码是供 Java 虚拟机加载和执行的指令格式。
- **Maven 构建生命周期（build lifecycle）**：Maven 按固定顺序组织项目处理工作的阶段。执行 `compile` 会先执行它之前的阶段，而不是只调用编译器。
- **离线模式（offline mode）**：要求 Maven 只使用本地仓库已有的父配置、插件和依赖，不联网解析缺失内容。缓存缺失时应直接失败。
- **生成代码（generated code）**：由构建工具根据接口定义或注解自动创建的源代码。本项目会根据 OpenAPI 文件生成接口与数据传输对象，并根据 MapStruct Mapper 接口生成映射实现。
- **注解处理器（annotation processor）**：编译期间读取 Java 注解并生成或检查代码的工具。本项目的 MapStruct 处理器会生成 Mapper 实现类。
- **类文件主版本号（class-file major version）**：`.class` 文件头部记录的字节码版本标识。本次应用入口类为 `61`，对应 Java 17 字节码。

## `compile` 前后的关键阶段与生成代码

根据 `pom.xml` 和本次构建输出，执行到 `compile` 时包含以下关键工作：

1. `validate` 阶段运行 Maven Enforcer Plugin，检查 Java 与 Maven 版本。
2. JaCoCo 的 `prepare-agent` 目标设置覆盖率代理参数；本次不执行测试，因此没有据此生成或验证测试覆盖率。
3. `generate-sources` 阶段运行 OpenAPI Generator Maven Plugin `7.25.0`，读取 `src/main/resources/openapi.yml`，以 Spring 服务端接口模式生成：
   - `org.springframework.samples.petclinic.rest.api` 包中的 API 接口及辅助类；
   - `org.springframework.samples.petclinic.rest.dto` 包中的数据传输对象，名称以 `Dto` 结尾。
4. Build Helper Maven Plugin 把 `target/generated-sources/openapi/src/main/java` 加入主源码目录。
5. Spring Boot Maven Plugin 生成 `META-INF/build-info.properties`，资源插件复制主资源。
6. `compile` 阶段运行 Maven Compiler Plugin，把手写主源码、OpenAPI 生成源码一起编译；MapStruct `1.6.3` 注解处理器同时在 `target/generated-sources/annotations` 生成 Mapper 实现源码并编译。

## 实际命令与退出状态

用户给出的选项使用了排版长横线 `–`。实际命令行使用两个 ASCII 短横线 `--`，否则 Maven 无法识别这些选项。

```bash
./mvnw --offline --batch-mode --no-transfer-progress compile
```

- `--offline`：只使用本地缓存，不联网解析。
- `--batch-mode`：以非交互方式执行。
- `--no-transfer-progress`：减少依赖传输进度输出；它不改变构建逻辑。
- `compile`：执行生命周期到主代码编译阶段，不执行测试阶段。

命令输出通过 `tee` 保存到仓库外的 `/tmp/first-offline-compile.log`，退出状态取 Bash 的 `PIPESTATUS[0]`，即管道左侧 Maven 命令的真实状态，而不是日志保存命令的状态。

### 关键输出

```text
Rule 0: org.apache.maven.enforcer.rules.version.RequireJavaVersion passed
Rule 0: org.apache.maven.enforcer.rules.version.RequireMavenVersion passed
Source directory: .../target/generated-sources/openapi/src/main/java added.
Compiling 115 source files with javac [debug parameters release 17] to target/classes
BUILD SUCCESS
Total time:  13.119 s
__MAVEN_EXIT_STATUS__=0
```

编译器还报告 `JpaPetRepositoryImpl.java uses unchecked or unsafe operations`。这是未检查类型操作警告；它没有导致本次编译失败。若要分析警告细节，需要在后续独立任务中使用编译器建议的检查选项，本次不修改配置。

## 产物检查

编译完成后实际检查得到：

- `target/generated-sources/openapi/src/main/java` 中有 28 个 `.java` 文件，包括 `OwnersApi.java`、`OwnerV2Api.java`、`OwnerDto.java` 等 API 和 DTO 源码。
- `target/generated-sources/annotations` 中有 7 个 MapStruct 实现源码，包括 `OwnerMapperImpl.java`、`PetMapperImpl.java` 和 `VisitMapperImpl.java`。
- `target/classes` 中有 126 个 `.class` 文件，其中 API 包 11 个、DTO 包 17 个；也包含 `PetClinicApplication.class` 和 7 个 Mapper 实现类。
- `javap -verbose target/classes/org/springframework/samples/petclinic/PetClinicApplication.class` 显示 `major version: 61`；生成的 `META-INF/build-info.properties` 显示 `build.java.release=17`。
- 编译后的 `git status --short --branch` 没有文件变化，说明 `target` 没有进入 Git 待提交列表。

## 验收结论与证据分类

### 1. Codex 本次实际执行并验证

- Maven Wrapper 能够在离线模式进入构建生命周期。
- 当前本地缓存足以解析本次 `compile` 所需的父配置、构建插件和主代码依赖。
- Java 21.0.2 环境中的 Maven 编译器实际编译 115 个源文件，目标为 Java 17 字节码，Maven 退出状态为 `0` 并输出 `BUILD SUCCESS`。
- OpenAPI 与 MapStruct 生成步骤实际运行，生成源码和对应 `.class` 文件实际存在。
- 执行前 `target` 不存在，因此本次没有复用已有编译类文件；但因为遵守要求未执行 `clean`，不把命令描述成 `clean compile`。

### 2. 用户提供的证据

- 上一任务已经验证 Wrapper 使用 Maven `3.9.16` 和 Java `21.0.2`，且 Wrapper 下载阻塞不再复现。
- 学习者尚未回答第三步的 Wrapper 下载链路分析练习，也尚未提供能够证明其本人掌握本次编译流程的回答。

### 3. 推断

- 类文件主版本号 `61`、编译日志的 `release 17` 与构建信息中的 `build.java.release=17` 相互一致，说明本次产物面向 Java 17 字节码平台。这不表示 Maven 运行时是 Java 17；上一任务提供的 Maven 运行时是 Java 21.0.2。

### 4. 尚未验证

- 未运行单元测试或集成测试，不能声称自动化测试通过。
- 未启动 Spring Boot 应用，不能声称应用能够启动、端口可访问或接口正常。
- 未部署或连接外部数据库。
- 没有验证打包、容器镜像、代码质量报告或测试覆盖率。
- 本次离线环境没有远程 `dev` 引用，未独立确认远程 `dev` 的最新提交。
- Codex 的成功操作不能自动证明学习者已经掌握离线编译、生命周期或代码生成。

## 分层结论

| 能力层次 | 当前结论 | 证据来源 |
| --- | --- | --- |
| Wrapper 可用 | 已验证 | 用户提供的上一任务结果：Maven `3.9.16`、Java `21.0.2`、退出状态 `0` |
| 离线依赖缓存足够 | 对本次 `compile` 已验证 | 本次离线命令完成，退出状态 `0` |
| 主代码编译成功 | 已验证 | 编译 115 个源文件、生成 126 个类文件、`BUILD SUCCESS` |
| 测试通过 | 未验证 | 本次没有运行测试阶段 |
| 应用启动成功 | 未验证 | 本次没有运行应用 |

## 知识点

1. Maven 的 `compile` 是生命周期阶段，会触发绑定在更早阶段的插件目标，所以代码生成和资源处理可以先于 Java 编译发生。
2. 离线编译成功证明当前缓存满足这一次命令，但不能推导出所有测试、打包或其他插件所需依赖也都已缓存。
3. “使用 Java 21 运行 Maven”和“生成 Java 17 字节码”可以同时成立：前者描述构建工具的运行环境，后者描述编译产物目标。
4. 生成源码是实际编译输入的一部分；若 OpenAPI 生成未执行，手写控制器与 Mapper 对 API/DTO 类型的引用将无法解析。
5. 管道中应读取 Maven 子命令的退出状态，不能用 `tee` 的状态代替构建状态。

## 官方文档

- [Apache Maven：构建生命周期简介](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Apache Maven：离线命令行选项](https://maven.apache.org/ref/current/maven-embedder/cli.html)
- [Maven Compiler Plugin：`release` 选项](https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-release.html)
- [OpenAPI Generator Maven Plugin](https://openapi-generator.tech/docs/plugins/)
- [MapStruct：注解处理器安装](https://mapstruct.org/documentation/stable/reference/html/#setup)
- [Java 虚拟机规范：ClassFile 格式](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html)

## 面试题及答案

### 1. `mvn compile` 是否只执行 Java 编译器？

**答案：**不是。Maven 会按生命周期顺序执行直到 `compile` 的所有相关阶段，并调用绑定到这些阶段的插件目标。本项目在编译前还会检查版本、生成 OpenAPI 源码、加入生成源码目录、生成构建信息并复制资源。

### 2. 离线 `compile` 成功能否证明离线 `test` 一定成功？

**答案：**不能。`test` 会进入更后的生命周期阶段，可能需要测试范围依赖、测试编译插件或其他尚未缓存的组件；同时测试代码本身也可能编译失败或断言失败。本次只证明 `compile` 所需缓存足够。

### 3. 为什么 Maven 使用 Java 21，却显示 `release 17`？

**答案：**Java 21 是运行 Maven 和 `javac` 的 Java 开发工具包版本；`release 17` 是编译器的目标平台设置，限制可用的 Java 平台接口并生成面向 Java 17 的字节码。本次类文件主版本号 `61` 与 Java 17 目标一致。

### 4. OpenAPI 和 MapStruct 分别生成什么？

**答案：**OpenAPI Generator 根据 `openapi.yml` 生成 REST API 接口和 DTO；MapStruct 注解处理器根据 Mapper 接口生成对象映射实现类。两类生成代码都成为本次主代码编译输入。

### 5. 为什么通过 `tee` 保存日志时要检查 `PIPESTATUS[0]`？

**答案：**管道包含 Maven 和 `tee` 两个命令。默认取得的可能是最后一个命令 `tee` 的状态，即使 Maven 失败，`tee` 仍可能成功写日志。`PIPESTATUS[0]` 对应管道第一个 Maven 命令的真实退出状态。

## 尚待学习者回答的练习

### 上一步练习（继续保留，个人能力仍待验证）

请只依据第三步的真实输出解释：`./mvnw -version` 的 `UnknownHostException` 与 `curl` 经代理得到的 `403` 分别发生在哪个阶段、各自证据是什么，以及下一项最小检查是什么。不要提出升级 Maven、关闭证书验证或直接执行完整构建。

### 本次练习（不提供答案）

请依据本次真实输出回答：为什么“Java 21.0.2 环境中编译成功”“`release 17`”“类文件主版本号 `61`”并不矛盾？再说明为什么本次离线 `compile` 成功仍不能证明离线 `test` 所需缓存足够。请分别引用一条本次输出作为证据。

在收到并检查学习者的回答前，“能独立解释构建运行时、编译目标、类文件版本及生命周期范围”的个人能力保持为**待验证**。

## 下一步建议

本次目标已经完成。下一独立步骤建议为：**执行现有自动化测试**。本次到此停止，不自动进入测试或应用启动。
