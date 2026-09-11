# 第三步：定位 Maven Wrapper 下载访问问题

检查日期：2026-09-11（Etc/UTC）

## 本次唯一目标与边界

本次只诊断 Maven Wrapper 下载访问问题，尝试让 `./mvnw -version` 成功，并在成功后核实继承的 Java 版本要求。不执行完整构建、测试、应用启动或数据库部署，不更改依赖、Java、Wrapper 或 Maven 版本，也不关闭证书验证。

## 开始前的基准与现场

### Codex 实际验证

- 当前目录为 `/workspace/spring-petclinic-rest`，开始时 `HEAD` 为 `fa6a744cecc3da0d53f5ddcefe0efe3fb145614c`，工作区干净。
- 本地没有远程配置或 `dev` 引用；`git merge-base --is-ancestor 4cd8e1b0cd42578e882247d8801f6be5d402f118 HEAD` 退出状态为 `0`，证明该基准提交是当前提交的祖先。
- `git log` 显示 `fa6a744` 是合并请求 #1 的本地合并提交，历史包含上一任务的 `AGENTS.md`、`docs/learning/02-environment-check.md` 和 `docs/learning/progress.md`。
- 从干净现场创建并使用独立分支 `learning/03-maven-wrapper-access`，未重置、清理或覆盖文件。

### 学习者提供的外部证据

学习者说明合并请求 #1 的目标分支为 `dev`，其基准提交为 `4cd8e1b0cd42578e882247d8801f6be5d402f118`。本环境没有成功联网读取该合并请求，因此不能把这项外部证据描述为 Codex 独立网络核验；结合上述本地提交关系，它支持当前现场包含已合入 `dev` 的上一任务内容。

## 新概念

- **域名解析（Domain Name System，域名系统解析）**：把 `repo.maven.apache.org` 这样的主机名转换成网络地址。若失败，客户端尚未连接到目标下载服务器。
- **代理（proxy）**：代表客户端访问外部网络的中间服务。代理可转发请求，也可依据网络策略拒绝目标；`403` 表示请求被理解但被拒绝。
- **下载地址**：Wrapper 配置中发行包的完整网络位置。网络可达并不必然表示路径存在，需收到服务器响应才能区分成功与 `404`（资源不存在）。
- **缓存（cache）**：本地保存的下载包和解压结果。有效缓存可避免联网；残缺下载或异常解压通常发生在已取得数据之后。
- **退出状态（exit status）**：命令结束时返回的数字，通常 `0` 表示成功，非 `0` 表示失败。关键判断必须同时结合输出和退出状态。
- **编译目标版本**：编译器生成的字节码所面向的 Java 平台版本；它与运行 Maven 的 Java 版本不是同一个概念。

## Wrapper 配置事实

`.mvn/wrapper/maven-wrapper.properties` 的实际内容指定：

- Wrapper 组件版本：`3.3.4`；
- 发行类型：`bin`；
- Maven 完整下载地址：`https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip`；
- Maven 版本：`3.9.16`。

`wrapperVersion=3.3.4` 是启动和安装 Maven 的 Wrapper 组件版本，不是 Maven 本身的版本。

## 分层诊断证据

代理检查仅记录变量是否设置、协议、主机、端口及是否含凭据，不输出完整地址、密码或令牌。

| 层次与命令 | 必要输出 | 退出状态 | 事实与含义 |
| --- | --- | ---: | --- |
| `getent ahosts repo.maven.apache.org` | 无网络地址 | 2 | 当前主机未解析出目标地址；失败发生在连接目标服务器之前。 |
| 脱敏检查 `HTTP_PROXY` / `HTTPS_PROXY` | `http`、主机 `proxy`、端口 `8080`、无凭据 | 0 | 命令行环境配置了代理；`NO_PROXY` 不包含目标域名。 |
| `curl --head` 请求配置中的精确地址 | `CONNECT tunnel failed, response 403`，代理地址 `172.31.5.253` | 56 | `curl` 到达代理，但代理拒绝建立到目标加密网站的隧道；不能据此声称整个环境完全断网。 |
| 查找 `~/.m2/wrapper/dists` | 未发现 `3.9.16` | 1（目录不存在） | 标准 Wrapper 缓存没有可复用的同版本发行包；不是已确认的缓存解压故障。 |
| 在 `/opt`、`/usr/local`、`/root/.m2`、`/tmp` 查找同版本包或可执行文件 | 无候选文件 | 0 | 常见本地目录中没有可信的 Maven `3.9.16` 候选。 |
| 请求 Apache 官方归档中的同版本包 | `CONNECT tunnel failed, response 403` | 56 | 替代官方入口也被同一代理策略拒绝，没有重复请求原失败地址。 |
| `java -version` | OpenJDK `21.0.2` | 0 | 当前执行 Java 命令的运行时是 Java 21。 |
| `./mvnw -version` | `UnknownHostException: repo.maven.apache.org`，堆栈位于 `DefaultDownloader.download` | 1 | Wrapper JAR 已启动，但 Maven 发行包下载尚未完成，Maven `3.9.16` 没有实际启动。 |

### 故障定位

**实际验证：**主机自身无法解析目标域名；支持代理的 `curl` 到达代理后被 `403` 拒绝；Java Wrapper 的下载器则以 `UnknownHostException` 失败。故障明确发生在 Maven 发行包下载前的名称解析或代理访问阶段，早于缓存写入、解压和 Maven 启动。

**推断：**Java Wrapper 没有从普通的 `HTTP_PROXY` / `HTTPS_PROXY` 环境变量获得可用代理路径，因而表现为直接解析失败；这是依据两种客户端的不同行为作出的推断，未检查代理产品内部配置，不能视为已证明的根因。

**未验证：**由于请求未通过代理，本地命令尚未从源站收到状态码，不能仅凭本次网络结果证明下载路径存在或不存在；也没有证据表明下载成功后会发生缓存或解压异常。

## 最小修复尝试与结果

优先查找当前环境中的可信同版本缓存，但未找到 Maven `3.9.16`。随后仅检查一次 Apache 官方归档的同版本发行包入口，也被代理拒绝。由于没有可用发行包，本次未手工制造 Wrapper 缓存、未换成其他 Maven 版本、未关闭安全检查，也未使用系统 `mvn` 冒充 Wrapper 成功。

### 当前需要的最小环境操作

允许当前执行环境经其代理访问 `repo.maven.apache.org:443`，用途是取得 Wrapper 配置指定的 Maven `3.9.16` 发行包以及后续解析父配置。若组织策略不允许直连，可由管理员从原始配置地址取得同一发行版，放入组织认可的可信制品缓存。当前环境无法验证所用代理产品及其设置界面名称，因此不编造具体界面入口。

调整后首先复验：

```bash
./mvnw -version
```

必须同时满足退出状态为 `0`、输出 Maven `3.9.16`，且其 Java runtime 为 `21.0.2`（或另一符合学习目标的 Java 21 运行时），才能判定 Wrapper 验收成功。

## Java 三类版本的当前结论

1. **项目最低运行或构建要求**：项目 Enforcer 使用从 Spring Boot 父 POM 继承的 `${java.version}` 作为最低 Java 值。本次父配置未成功解析，因此精确值仍待验证。
2. **编译目标版本**：决定生成字节码面向哪个 Java 平台，可能来自父 POM 的编译器配置。本次未得到有效 POM，因此仍待验证。
3. **当前执行 Maven 的 Java 版本**：是 Maven 进程实际运行在哪个 Java 运行时。当前只能证明 `java -version` 是 `21.0.2`；由于 Maven 没有启动，尚不能从 `./mvnw -version` 的输出完成这一项验收。

这三者分别描述项目规则、构建产物兼容目标和当前工具进程环境，不能相互替代。Java 21 符合本学习计划目标，但在 Maven 实际启动和父配置解析前，不能声称项目版本要求已全部验收。

## 验收结论与证据分类

### 1. Codex 已执行并验证

- 当前提交包含上一学习任务文件，且用户给出的旧基准提交是当前 `HEAD` 的祖先。
- Wrapper 配置指定 Maven `3.9.16` 及上述完整下载地址。
- 当前 Java 命令使用 OpenJDK `21.0.2`。
- 域名解析失败，代理对目标返回 `403`，且没有发现可用的本地同版本缓存。
- `./mvnw -version` 退出状态为 `1`，没有启动 Maven。
- 未执行完整构建、测试、应用启动或数据库部署。

### 2. 学习者已提供的证据

- 学习者提供合并请求 #1 的目标分支和基准提交信息；它已经作为注明来源的外部补充证据加入第二步记录。
- 学习者尚未提交能证明其本人掌握 Maven Wrapper 下载链路分析的回答或操作证据。

### 3. 尚待验证

- Wrapper Maven `3.9.16` 成功启动及其实际 Java runtime。
- 父 POM 继承的最终 `java.version`、编译目标版本和精确最低构建要求。
- 学习者本人能否依据证据区分名称解析、代理拒绝、地址不存在以及缓存/解压异常。

## 知识点

1. 下载链路应逐层检查：配置地址、名称解析、代理或连接、服务器响应、落盘缓存、校验与解压、程序启动。
2. `UnknownHostException` 指向名称解析阶段；代理的 `403` 指向代理策略阶段。不同客户端可能因代理配置机制不同而表现不同。
3. 系统 Maven 成功不能证明 Wrapper 成功；Wrapper 的价值之一是固定项目使用的 Maven 发行版本。
4. 没有收到源站响应时，不应把网络访问失败误写成下载地址 `404`，也不应推断缓存已经损坏。
5. 当前 Java、项目最低 Java 和编译目标 Java 是三项独立证据。

## 官方文档

- [Apache Maven Wrapper 官方文档](https://maven.apache.org/wrapper/)
- [Apache Maven：POM 简介与继承](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html)
- [Apache Maven Compiler Plugin：`release` 选项](https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-release.html)
- [Apache Maven Enforcer Plugin：Require Java Version](https://maven.apache.org/enforcer/enforcer-rules/requireJavaVersion.html)
- [Java 21 `UnknownHostException` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/UnknownHostException.html)

## 面试题及答案

### 1. `UnknownHostException` 说明下载进行到了哪个阶段？

**答案：**客户端试图把主机名解析为网络地址，但未得到可用结果。它发生在成功连接目标服务器之前，因此单凭该异常不能判断远端文件是否存在，也不能证明缓存或解压有问题。

### 2. 为什么 `curl` 的代理 `403` 与 Wrapper 的 `UnknownHostException` 可以同时出现？

**答案：**不同程序读取代理配置的方式可能不同。`curl` 使用环境代理并到达代理，代理拒绝隧道；Java Wrapper 可能没有使用这些环境变量，转而直接解析目标域名并失败。后半部分仍需检查 Java 代理配置才能从合理推断升级为事实。

### 3. 为什么系统 `mvn -version` 成功不能替代 `./mvnw -version`？

**答案：**系统 Maven 由机器安装，版本可能与仓库约定不同；Wrapper 按仓库配置选择 Maven。本项目要求验收的是 Wrapper 指定的 `3.9.16`，系统 Maven 成功既不证明该发行包已下载，也不证明 Wrapper 缓存和启动链路正常。

### 4. 项目最低 Java、编译目标 Java、运行 Maven 的 Java 有何区别？

**答案：**最低 Java 是项目或构建规则允许的下限；编译目标决定生成字节码面向的平台；运行 Maven 的 Java 是当前构建工具进程实际使用的运行时。三者可能不同，必须分别从有效配置和命令输出验证。

## 个人故障分析练习（等待学习者回答，不提供标准答案）

请只依据本次真实输出回答：

> `./mvnw -version` 在 `DefaultDownloader.download` 中抛出 `UnknownHostException: repo.maven.apache.org`；`curl` 通过 `proxy:8080` 请求相同地址时收到 `CONNECT tunnel failed, response 403`；标准 Wrapper 缓存中没有 Maven `3.9.16`。请解释错误分别发生在哪个阶段、每个判断的证据是什么，以及你会执行的下一项**最小检查**是什么。不要提出升级 Maven、关闭证书验证或直接执行完整构建。

在收到并检查学习者的实际回答前，“能独立定位 Maven Wrapper 下载故障”的个人能力保持为**待验证**。

## 下一步建议

当前仍受阻。完成上述最小网络策略调整后，只运行 `./mvnw -version` 复验；成功后再通过父配置或 Maven 属性查询核实 `java.version` 和编译目标。两项均验证成功后的下一独立步骤才是“首次构建”，本次不自动开始。
