# 第二步：检查代码基准和构建环境

检查日期：2026-09-10（Etc/UTC）

## 本次目标与边界

本次只核对 Git 代码基准、项目声明的构建要求和当前工具版本，不执行完整构建、测试或应用启动，不安装或升级 Java，也不修改业务代码、依赖版本或运行配置。

## 新名词

- **工作区（working tree）**：当前检出的、可以直接查看和编辑的文件集合。工作区干净表示相对当前提交没有尚未提交的文件变化，但不代表项目能构建或运行。
- **提交（commit）**：Git 保存的一次文件快照及其历史关系；提交散列值用于唯一标识它。
- **远程仓库（remote repository）**：通常位于代码托管服务上的 Git 仓库。本地远程配置只是地址别名；仍需成功访问并比较提交才能确认关系。
- **Maven**：Java 项目的构建与依赖管理工具，读取 `pom.xml` 描述项目和构建规则。
- **Maven Wrapper**：项目携带的 Maven 启动脚本（本项目为 `./mvnw`），它按项目配置下载并运行约定的 Maven 版本，使不同环境尽量一致。
- **Java Development Kit（Java 开发工具包）**：包含 Java 运行时和 `javac` 编译器的开发工具集合。
- **Java Virtual Machine（Java 虚拟机）**：执行 Java 字节码的运行环境；`java -version` 展示本次实际运行它的版本。
- **持续集成（Continuous Integration）**：代码变更后由自动化工作流执行构建和检查；本项目的 GitHub Actions 工作流选用 Java 17。
- **父 POM（父项目对象模型）**：Maven 允许 `pom.xml` 从父配置继承属性和插件设置。本项目的 `${java.version}` 来自 Spring Boot 父 POM，而不是在当前 `pom.xml` 直接声明。
- **Maven Enforcer Plugin（Maven 约束检查插件）**：在构建期间检查 Java、Maven 等环境条件，不满足规则时使构建失败。
- **退出状态（exit status）**：命令返回给操作系统的数字；通常 `0` 表示成功，非 `0` 表示失败。

## 代码基准检查

### 操作与实际结果

| 命令 | 关键输出 | 退出状态 | 含义 |
| --- | --- | ---: | --- |
| `pwd` | `/workspace/spring-petclinic-rest` | 0 | 当前操作目录是目标仓库目录。 |
| `find .. -name AGENTS.md -print` | 无输出 | 0 | 检查前仓库及其相邻作用域没有既有 `AGENTS.md`；本次新增根目录规则文件。 |
| `find docs -maxdepth 3 -type f -print` | 无输出 | 0 | 检查前没有已有学习记录，因此没有覆盖历史文件。 |
| `git remote -v` | 无输出 | 0 | 本地仓库没有配置远程地址；不能由本地配置确认题目给定远程。 |
| `git branch --show-current` | `work` | 0 | 当前附着在具名分支 `work`，不是分离头指针状态。分支名本身不能证明是否基于 `dev`。 |
| `git rev-parse HEAD` | `4cd8e1b0cd42578e882247d8801f6be5d402f118` | 0 | 当前提交已被精确记录。 |
| `git status --short --branch` | `## work` | 0 | 检查开始时工作区干净；这不表示构建或启动成功。 |
| `git branch -avv` | 仅有 `work` 指向 `4cd8e1b` | 0 | 本地没有可用于比较的 `dev` 分支或远程跟踪引用。 |
| `git ls-remote https://github.com/twlidong117/spring-petclinic-rest.git refs/heads/dev` | `CONNECT tunnel failed, response 403` | 128 | 网络代理拒绝访问，未取得远程 `dev` 的提交值。 |

### 基准结论

**实际验证的事实：** 当前在具名分支 `work`，起始提交为 `4cd8e1b0cd42578e882247d8801f6be5d402f118`，检查开始时没有未提交修改；本地无远程配置，也无 `dev` 引用。题目给出的远程查询因代理返回 403 而失败。

**验收结论：当前不能确认代码基于远程 `dev`。** 这不是因为分支名叫 `work`，而是缺少远程 `dev` 的提交引用，无法运行 `git merge-base --is-ancestor <dev> HEAD` 之类的提交关系检查。

**未验证事项：** `4cd8e1b` 是否等于远程 `dev`，或其历史是否包含远程 `dev`。网络恢复后的最小核对方式是先只获取远程引用，再比较提交关系；不得因此强制重置或清理现场。

## 项目配置证据

1. `pom.xml` 使用 `org.springframework.boot:spring-boot-starter-parent:4.1.1`，并以 `<relativePath/>` 要求从 Maven 仓库解析父 POM。
2. 当前 `pom.xml` 没有直接定义 `<java.version>`；Enforcer 的 `requireJavaVersion` 使用继承的 `${java.version}`，范围是 `[${java.version},)`，即最低版本为该继承值。
3. 三个必要构建工作流 `.github/workflows/docker-build.yml`、`.github/workflows/maven-build-master.yml` 和 `.github/workflows/maven-build-pull-request.yml` 均设置 Java 17；`.github/workflows/newman-pipeline.yml` 的应用检查也设置 Java 17。因此仓库自动化以 Java 17 为明确构建基线。
4. 父 POM 不在本地 Maven 缓存，且网络不可用，本次无法生成有效 POM 来展开 `${java.version}`。因此“父 POM 中 `${java.version}` 的实际值为 17”与“Java 17 是 Enforcer 的精确下限”尚未在当前环境直接解析验证，不能只凭经验补写为事实。
5. `pom.xml` 将 `<maven.version>` 定义为 `3.9.9`，Enforcer 的 `requireMavenVersion` 使用该值；这是项目构建检查使用的 Maven 要求。
6. `.mvn/wrapper/maven-wrapper.properties` 的 `distributionUrl` 明确指定 Apache Maven `3.9.16`；`wrapperVersion=3.3.4` 是 Wrapper 组件自身版本，不是 Maven 版本。
7. `readme.md` 的本地运行示例使用 `./mvnw spring-boot:run`，说明仓库推荐使用 Wrapper；这只是使用说明，不代表本次已启动应用。

## 环境命令与关键输出

以下命令均逐条执行并记录状态：

### `git --version`

```text
git version 2.43.0
exit=0
```

`0` 表示 Git 可执行；版本为 `2.43.0`。

### `java -version`

```text
openjdk version "21.0.2" 2024-01-16
OpenJDK Runtime Environment (build 21.0.2+13-58)
OpenJDK 64-Bit Server VM (build 21.0.2+13-58, mixed mode, sharing)
exit=0
```

`java` 是运行入口。输出证明当前实际 Java 运行时为 64 位 OpenJDK `21.0.2`，不能据此声称项目已经编译。

### `javac -version`

```text
javac 21.0.2
exit=0
```

`javac` 是 Java 源代码编译器。它与运行时同为 `21.0.2`，表明当前 Java 开发工具链内部版本一致。

### `./mvnw -version`

```text
Exception in thread "main" java.net.UnknownHostException: repo.maven.apache.org
exit=1
```

Wrapper 脚本和其 JAR 已经开始运行，但在下载配置指定的 Maven `3.9.16` 时无法解析 Maven Central 主机名。非零状态 `1` 表示命令失败，所以 **Maven Wrapper 本次不能正常完成执行**。

为区分 Wrapper 与系统工具，另行执行了 `mvn -version`（不是构建）：系统 Maven `3.9.10` 可执行，使用 Java `21.0.2`，退出状态为 `0`。这不能替代对 Wrapper `3.9.16` 的验收，也没有用于绕过失败执行构建。

## 版本对照

| 项目 | 项目要求或仓库基线 | Wrapper 指定 | 当前环境实际值 | 本次判断 |
| --- | --- | --- | --- | --- |
| Java | 工作流明确使用 17；Enforcer 要求不低于父 POM 的 `${java.version}`，但继承值本次未能在本地展开 | 不适用 | `java` 与 `javac` 均为 `21.0.2` | 实际版本高于工作流基线；精确 Enforcer 下限仍待父 POM解析验证。 |
| Maven | `pom.xml` 的 Enforcer 使用 `maven.version=3.9.9` | `3.9.16` | Wrapper 未下载成功；系统 `mvn` 为 `3.9.10` | 系统版本满足项目声明值，但指定 Wrapper 尚不可用。 |
| Maven Wrapper | 不适用 | Wrapper 组件 `3.3.4` | 启动后下载失败 | 未通过。 |

## 验收结论

1. **当前代码是否可以确认基于 `dev`？** 不可以。已知当前是 `work@4cd8e1b`，但本地无远程和 `dev` 引用，远程查询又被 403 阻断，无法做提交关系判断。
2. **项目要求哪些 Java 和 Maven 版本，证据在哪里？** Java：仓库构建工作流统一选用 17，`pom.xml` Enforcer 的精确最低值继承自父 POM但本次未展开；Maven：`pom.xml` 以 `maven.version=3.9.9` 做 Enforcer 检查，Wrapper 配置指定实际启动 Maven `3.9.16`。
3. **当前环境实际使用哪些版本？** Git `2.43.0`；Java 运行时与编译器 `21.0.2`；系统 Maven `3.9.10` 使用 Java `21.0.2`；项目 Wrapper 的 Maven 没有成功启动。
4. **Maven Wrapper 是否可以正常执行？** 不可以。本次因 `UnknownHostException: repo.maven.apache.org` 以状态 `1` 退出。
5. **是否具备进入首次构建的条件？** 暂不具备。必须先让 Wrapper 能取得或找到其指定的 Maven `3.9.16` 并使 `./mvnw -version` 成功；同时应恢复对题目远程仓库的只读访问以确认 `dev` 提交关系。即使处理后，依赖能否完整解析、源码能否编译、测试能否通过以及应用能否启动仍都尚未验证。

### 已确认的阻塞与最小处理方案

- **阻塞：** 当前网络无法访问 `repo.maven.apache.org`，Wrapper 无法取得 Maven `3.9.16`。
- **最小处理：** 修复域名解析/代理访问，或由可信来源预置 Wrapper 配置中同一 Maven `3.9.16` 发行包，然后仅重试 `./mvnw -version`。不要升级依赖、改 Wrapper 版本或跳过检查。
- **基准核对：** 恢复 GitHub 只读访问后取得题目仓库的 `dev` 引用，再用提交祖先关系判断；不要按分支名猜测，更不要强制重置。

## 知识点

1. Git 分支名是可移动标签；“基于 `dev`”应通过提交图和祖先关系证明。
2. 工作区干净只说明没有相对当前提交的文件变化，不等价于依赖可下载、构建成功或服务可运行。
3. `java` 检查运行时，`javac` 检查编译器；两者都成功才说明基本开发工具可用。
4. 项目要求、Wrapper 选定版本、机器上的系统 Maven 是三种不同事实。优先 Wrapper 能提高构建复现性。
5. Maven 子项目会从父 POM 继承属性。无法解析父 POM 时，应把精确继承值标为未验证，而不是猜测。
6. `./mvnw -version` 也可能首次下载 Maven；脚本能够启动不等于 Wrapper 已经成功执行到输出 Maven 版本。

## 官方文档

- [Git：远程仓库](https://git-scm.com/book/zh/v2/Git-%E5%9F%BA%E7%A1%80-%E8%BF%9C%E7%A8%8B%E4%BB%93%E5%BA%93%E7%9A%84%E4%BD%BF%E7%94%A8)
- [Git：分支简介](https://git-scm.com/book/zh/v2/Git-%E5%88%86%E6%94%AF-%E5%88%86%E6%94%AF%E7%AE%80%E4%BB%8B)
- [Apache Maven Wrapper 官方文档](https://maven.apache.org/wrapper/)
- [Apache Maven POM 参考](https://maven.apache.org/pom.html)
- [Apache Maven Enforcer Plugin：Require Java Version](https://maven.apache.org/enforcer/enforcer-rules/requireJavaVersion.html)
- [Apache Maven Enforcer Plugin：Require Maven Version](https://maven.apache.org/enforcer/enforcer-rules/requireMavenVersion.html)
- [Java `java` 命令官方文档](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html)
- [Java `javac` 命令官方文档](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html)
- [GitHub Actions：使用 Maven 构建和测试 Java](https://docs.github.com/en/actions/use-cases-and-examples/building-and-testing/building-and-testing-java-with-maven)

## 面试题及答案

### 1. 为什么不能根据当前分支名 `work` 判断它不是基于 `dev`？

**答案：** 分支名只是指向某个提交的引用，临时分支可以从 `dev` 的某个提交创建后取任何名字。应先取得可信的 `dev` 引用，再检查 `dev` 提交是否是当前 `HEAD` 的祖先；如果是，当前历史包含该基准。若引用缺失，就只能说关系未确认。

### 2. `java -version` 与 `javac -version` 分别验证什么？

**答案：** `java -version` 验证执行字节码的 Java 运行时；`javac -version` 验证把 Java 源代码编译为字节码的编译器。只存在运行时可能足以运行已有程序，却不一定能编译项目。

### 3. 为什么项目已有系统 Maven，还要优先使用 `./mvnw`？

**答案：** 系统 Maven 版本由机器决定，不同开发者可能不同。Wrapper 把下载地址和 Maven 版本记录在仓库中，使本地和持续集成尽量使用同一版本，提高可复现性。本项目系统 Maven 是 `3.9.10`，但 Wrapper 明确指定 `3.9.16`。

### 4. `pom.xml` 中没有 `<java.version>`，为什么构建仍可能使用它？

**答案：** Maven 支持父 POM 继承。当前项目声明 Spring Boot 父 POM，子 POM 中的 `${java.version}` 可以由父配置提供。要得到准确最终值，应解析父 POM或查看 Maven 生成的有效 POM，不能凭经验猜测。

### 5. `./mvnw -version` 下载失败是否证明代码有问题？

**答案：** 不证明。本次错误发生在构建工具下载阶段，`UnknownHostException` 表示主机名无法解析。源码尚未编译、测试或运行，所以代码质量和运行结果均未验证。

### 6. 工作区干净是否等于项目可以构建？

**答案：** 不等于。工作区干净只是 Git 状态；构建还依赖正确的 Java/Maven、父 POM和依赖可访问、源码可编译及测试可通过等条件。

## 下一步

**解决已经确认的具体环境问题**：恢复 Maven Central 的域名解析/网络访问或可信地预置 Maven `3.9.16`，使 `./mvnw -version` 成功；同时恢复题目 GitHub 仓库的只读访问并核对 `dev` 提交关系。完成后停止，再单独安排首次构建步骤。

## 2026-09-11 外部补充证据（不改写原结论）

学习者在第三步任务开始时提供：合并请求 #1 的目标分支为 `dev`，基准提交为 `4cd8e1b0cd42578e882247d8801f6be5d402f118`，与本记录当时的起始提交相同。这是**学习者提供的外部证据**，补充支持第二步工作基于当时的 `dev` 基准。

本补充不把第二步的现场检查改写为成功：第二步执行时本地没有远程或 `dev` 引用，`git ls-remote` 仍确实因代理返回 `403` 而失败，因此当时无法在该环境独立核验合并请求的目标分支和远程引用。
