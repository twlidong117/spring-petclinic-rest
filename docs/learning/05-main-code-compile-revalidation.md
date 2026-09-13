# 第五步：主代码离线编译复验

检查日期：2026-09-13（Etc/UTC）

## 本次唯一目标与边界

本次只验证当前项目的主代码能否编译。遵循已有离线要求，使用 Maven Wrapper 执行到 `compile` 生命周期阶段；不运行测试、不启动应用、不执行 `clean`，也不修改业务代码、依赖版本或运行配置。

## 编译与其他验证的区别

- **编译**把 `.java` 源文件转换为 Java 虚拟机可加载的 `.class` 字节码，并检查语法、类型和依赖引用。
- 编译成功不等于测试通过：测试还需要编译并执行测试代码、断言和可能的测试环境。
- 编译成功不等于应用启动成功：启动还涉及 Spring Bean 装配、配置、端口、数据库和其他运行时条件。

## 开始前的环境与现场

### Codex 实际验证

- 当前为具名分支 `work`，`HEAD` 是 `52975c6e0ce2a49fe2c7d08cbe4ff1937b45d3a1`，开始时工作区干净。
- 本地没有远程配置和 `dev` 引用，无法用提交祖先关系确认当前提交是否基于远程 `dev`。本次保留现场，没有重置、清理、推送或合并。
- 已读取根目录 `AGENTS.md`、总进度和此前四步记录；此前记录要求后续构建保持离线。
- `java -version` 和 `javac -version` 均成功，版本为 `21.0.2`。
- `./mvnw -version` 成功，Wrapper 实际运行 Apache Maven `3.9.16`，使用 Java `21.0.2`。
- Wrapper 配置指定 Maven `3.9.16`；`pom.xml` 的 Maven Enforcer 配置值是 `3.9.9`。Wrapper 实际版本满足该最低要求。
- 离线解析有效属性成功：`${java.version}` 为 `21.0.2`，`${maven.compiler.release}` 为 `17`。
- 编译前 `target` 不存在，所以本次产物不是从旧编译目录统计所得。本次仍严格没有执行 `clean`。

### 基准关系的未验证事项

分支名不能证明提交关系。由于缺少可信的本地或远程 `dev` 引用，本次仍不能确认 `HEAD` 与远程 `dev` 的关系；这不阻止按用户要求在保留现场的前提下验证当前检出代码，但该限制必须继续保留在记录中。

## 构建配置与代码生成要求

执行 `compile` 会按 Maven 生命周期先执行较早阶段中绑定的目标：

1. Maven Enforcer Plugin 检查 Java 与 Maven 版本。
2. OpenAPI Generator Maven Plugin `7.25.0` 根据 `src/main/resources/openapi.yml` 生成 API 接口和数据传输对象源码。
3. Build Helper Maven Plugin 将 `target/generated-sources/openapi/src/main/java` 加入主源码目录。
4. Spring Boot Maven Plugin 生成 `META-INF/build-info.properties`，Resources Plugin 复制主资源。
5. Maven Compiler Plugin 编译手写源码与 OpenAPI 生成源码；MapStruct `1.6.3` 注解处理器同时生成 Mapper 实现并编译。

**注解处理器**是在编译期间读取 Java 注解并生成或检查代码的工具。本项目用它把 Mapper 接口转换为具体实现类。

## 实际编译命令与结果

```bash
./mvnw --offline --batch-mode --no-transfer-progress compile
```

- `--offline`：只使用本地 Maven 缓存，不联网下载。
- `--batch-mode`：使用非交互模式。
- `--no-transfer-progress`：隐藏传输进度，不跳过构建步骤。
- `compile`：执行生命周期到主代码编译，不进入测试阶段。

输出通过 `tee` 保存到仓库外的 `/tmp/current-main-compile.log`，退出状态读取 Bash 的 `PIPESTATUS[0]`，确保记录的是 Maven 而非 `tee` 的状态。

### 关键日志

```text
Rule 0: org.apache.maven.enforcer.rules.version.RequireJavaVersion passed
Rule 0: org.apache.maven.enforcer.rules.version.RequireMavenVersion passed
--- openapi-generator:7.25.0:generate (default) @ spring-petclinic-rest ---
Source directory: .../target/generated-sources/openapi/src/main/java added.
Compiling 115 source files with javac [debug parameters release 17] to target/classes
BUILD SUCCESS
Total time:  12.882 s
__MAVEN_EXIT_STATUS__=0
```

`JpaPetRepositoryImpl.java uses unchecked or unsafe operations` 是未检查类型操作警告。本次编译仍成功；依照任务边界没有修改业务代码或编译参数来处理它。

## 代码生成与编译产物证据

- OpenAPI Generator 日志明确显示 `dryRun=false`，并写出 `OwnerDto.java`、`OwnersApi.java` 等文件。
- `target/generated-sources/openapi/src/main/java` 中有 28 个 `.java` 文件：API 包 11 个，数据传输对象包 17 个。
- `target/generated-sources/annotations` 中有 7 个 `*MapperImpl.java`，包括 `OwnerMapperImpl.java`；它们是 MapStruct 注解处理器在本轮编译期间生成的。
- `target/classes` 中有 126 个 `.class` 文件，包括 `PetClinicApplication.class`、OpenAPI 生成类型和 7 个 Mapper 实现类。
- 代表性生成源码、类文件和构建信息的修改时间均位于本轮构建时间 `23:47:18Z` 至 `23:47:26Z`，与编译结束时间一致。
- `target/classes/META-INF/build-info.properties` 包含 `build.java.release=17`。
- `javap -verbose` 检查 `PetClinicApplication.class` 得到 `major version: 61`，与 Java 17 目标字节码一致。
- `.gitignore` 忽略 `/target`，编译完成后构建产物没有成为 Git 待提交修改。

## 验收结论与证据分类

### Codex 本次实际验证的结果

1. Maven Wrapper、Java 编译器以及 `compile` 所需的离线缓存可用。
2. 环境约束、OpenAPI 代码生成、生成源码目录注册、资源处理、构建信息生成和 Java 编译均实际执行。
3. Maven 实际编译 115 个源文件，生成 126 个类文件，退出状态为 `0` 并输出 `BUILD SUCCESS`。
4. **明确结论：当前项目主代码编译成功。**

### 推断

编译日志中的 `release 17`、构建信息中的 `build.java.release=17` 和类文件主版本号 `61` 相互一致，因此可判断本轮产物面向 Java 17 平台；运行 Maven 的 Java 版本仍是 `21.0.2`。

### 尚未验证

- 没有运行单元测试或集成测试，不能声称测试通过。
- 没有启动 Spring Boot 应用，不能声称应用可启动、接口可访问或 Spring Bean 能正确装配。
- 没有验证数据库连接、外部服务、打包、容器镜像或部署。
- 没有确认远程 `dev` 的最新提交，也没有确认当前提交与该远程基准的祖先关系。
- 本次成功操作是 Codex 的环境证据，不能自动证明学习者已经掌握相关知识或能独立完成操作。

## 本次修改

- 新增本记录，保存真实编译证据、结论和学习材料。
- 增量更新 `docs/learning/progress.md`，索引本轮结果。
- 未修改业务代码、依赖版本或运行配置；`target` 产物被 Git 忽略，不纳入提交。

## 关键知识点

1. Maven `compile` 是生命周期阶段，会先执行绑定到更早阶段的必要插件目标，不只是直接调用 `javac`。
2. 离线编译成功只证明本地缓存满足本次 `compile`；测试或打包可能需要额外依赖和插件。
3. Java 21 可以运行构建工具并由编译器生成 Java 17 目标字节码；构建运行时版本与产物目标版本是不同概念。
4. 生成源码也是主代码编译输入。日志、源文件、对应类文件和时间信息共同构成比“目录存在”更可靠的执行证据。
5. Maven 输出 `BUILD SUCCESS` 和进程退出状态 `0` 应结合判断；经管道保存日志时要读取 Maven 自身状态。

## 官方文档

- [Apache Maven：构建生命周期简介](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Apache Maven：命令行选项（包括离线模式）](https://maven.apache.org/ref/current/maven-embedder/cli.html)
- [Maven Compiler Plugin：设置 `release`](https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-release.html)
- [OpenAPI Generator Maven Plugin](https://openapi-generator.tech/docs/plugins/)
- [MapStruct：注解处理器配置](https://mapstruct.org/documentation/stable/reference/html/#setup)
- [Java 虚拟机规范：类文件格式](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html)

## 面试题及答案

### 1. `mvn compile` 与直接执行 `javac` 有什么区别？

**答案：** `javac` 主要负责把指定 Java 源码编译成字节码；`mvn compile` 会读取项目模型和依赖，并按 Maven 生命周期执行到 `compile`，所以还可能先运行环境约束、代码生成、资源复制等插件目标。本项目本轮先生成 OpenAPI 源码，再由编译器共同编译。

### 2. 为什么 Java 21.0.2 环境可以产生 Java 17 字节码？

**答案：** Java 21.0.2 是运行 Maven 和编译器的开发工具版本；`release 17` 是编译目标设置，它限制平台接口并生成适用于 Java 17 的字节码。本轮类文件主版本号 `61` 是 Java 17 字节码证据。

### 3. `BUILD SUCCESS` 能否证明测试通过？

**答案：** 不能。本轮目标是 `compile`，它位于测试编译和测试执行阶段之前。只有实际进入对应测试阶段、看到测试结果并获得成功退出状态，才能声称该次测试通过。

### 4. 如何证明代码生成在本轮实际执行，而不是只看到旧目录？

**答案：** 应组合多项证据：执行前生成目录不存在；日志显示生成插件及具体写文件动作；生成源码和对应类文件存在；修改时间与本轮构建时间一致。本轮四类证据均已取得。

### 5. 为什么离线 `compile` 成功不能证明离线 `test` 一定成功？

**答案：** `test` 会使用测试源码、测试范围依赖和更后阶段的插件，这些内容可能未缓存，也可能存在编译错误或断言失败。因此本轮只证明 `compile` 范围内的缓存和主代码有效。

## 学习者练习（能力待验证）

请学习者依据本记录，用自己的话完成两项内容：

1. 画出本项目从 `openapi.yml` 到 `OwnersApi.java` 再到 `OwnersApi.class` 的最短流程，并指出每一步由哪个 Maven 插件负责。
2. 解释为什么 `Java 21.0.2`、`release 17` 和 `major version: 61` 可以同时成立，再说明还缺少哪类证据才能声称“测试通过”。

在收到学习者的回答或独立操作证据前，“能独立解释 Maven 生命周期、代码生成和编译目标”的个人能力保持为**待验证**。

## 下一步建议

本轮目标已经完成。学习者应先完成上面的练习；之后可把“执行现有自动化测试”作为下一独立步骤。本轮到此停止，不自动运行测试、启动应用或进入下一学习步骤。
