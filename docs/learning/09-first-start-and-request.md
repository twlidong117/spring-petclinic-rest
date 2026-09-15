# 第九步：应用首次启动与最小请求验证

日期：2026-09-16（Asia/Shanghai）。

## 唯一目标与范围

在已合并第八步记录的 dev 基准上，使用项目默认配置独立启动应用，核对真实监听端口，并验证健康检查和一个业务读取请求。验收后停止本次启动的进程。本次不修改业务代码、测试代码、依赖版本或项目运行配置，不执行打包、部署、外部数据库连接或业务写入，也不进入下一学习步骤。

## 开始前的基准与环境

来源：Codex 本次实际核验。

- GitHub 显示 PR #8 已合并到 dev，合并提交为 757f73a87178208ef9eb13659f986dc6d67f734f，合并时间为 2026-09-15T23:42:03Z（北京时间 2026-09-16 07:42:03）。
- 开始时工作区干净，位于上一工作分支 codex-step-08-windows-cache，HEAD 为 289a913e57d129e8094efc19fc7c13f3f5310cc4。
- 获取远程 dev 后，确认其包含上述合并提交；从 origin/dev 创建 codex-step-09-first-start。新分支起始 HEAD 为该合并提交，祖先检查通过；没有强制重置或合并旧工作分支。
- Java 为 Temurin 21.0.12.1，Maven Wrapper 使用 Maven 3.9.16；缓存仍在 D:\derek\maven\_cache。
- 默认激活 h2,spring-data-jpa，使用 jdbc:h2:mem:petclinic；数据库结构和示例数据由项目初始化脚本提供。
- 默认端口 9966，请求上下文路径 /petclinic；项目既有 petclinic.security.enable=false，本次没有改变认证设置。
- 开始时 9966 无监听进程，未发现 Spring、端口或数据库相关环境变量覆盖。

## 本次操作与实际证据

### 1. 独立启动

```powershell
.\mvnw.cmd --offline --batch-mode --no-transfer-progress spring-boot:run
```

使用既有缓存启动，未额外补缓存。启动目标会自然处理前置生成和编译任务；没有重新运行 test，也不把第八步 237 个测试的结果当成本轮测试证据。

运行日志实际显示：

- PetClinicApplication 使用 Java 21.0.12.1，应用进程编号 20984。
- 激活配置为 h2、spring-data-jpa。
- 实际数据库连接为 jdbc:h2:mem:petclinic。
- Tomcat 在 9966 端口启动，上下文路径为 /petclinic。
- Started PetClinicApplication in 4.013 seconds。

操作系统端口检查显示 9966 处于监听状态，所属进程为 20984，与应用启动日志一致。这是实际进程与端口证据，不仅是配置文件中声明了一个端口。

### 2. 健康检查

```text
GET http://127.0.0.1:9966/petclinic/actuator/health
```

- 实际响应状态码：200。
- 媒体类型：application/vnd.spring-boot.actuator.v3+json。
- 响应正文：{"groups":["liveness","readiness"],"status":"UP"}。

首次检查脚本遇到 PowerShell 将该媒体类型的响应内容作为字节数组返回，直接转换导致脚本的健康断言失败；当时应用响应已为 200。这是检查脚本的解码问题，不是应用健康失败。按 UTF-8 解码后重新读取健康接口，确认 200 与 UP，应用代码和配置均未修改。原始检查记录与正确解码结果分别保留。

### 3. 最小业务读取请求

```text
GET http://127.0.0.1:9966/petclinic/api/pettypes
```

- 请求时间：2026-09-16T07:45:38+08:00。
- 实际响应状态码：200。
- 媒体类型：application/json。
- 响应包含 6 条数据：bird(5)、cat(1)、dog(2)、hamster(6)、lizard(3)、snake(4)。
- 数据名称与 src/main/resources/db/h2/data.sql 的 6 条宠物类型初始化记录一致。
- 这是从独立于应用的客户端发送的真实本机网络请求，不是测试进程中的模拟请求；没有创建、修改或删除业务记录。

### 4. 验收后主动停止

核对应用主类、进程创建时间、父进程及启动参数文件中的项目路径后，Codex 仅终止本次启动的应用进程 20984；随后确认 9966 的监听数量为 0。

Spring Boot 使用独立子进程运行应用。由于本次通过操作系统主动终止该子进程，Maven 随后报告 Process terminated with exit code: -1，并以状态 1 输出 BUILD FAILURE。这是验收后的终止结果，不能写成 Maven 正常退出，也不能用它抹去停止前已经获得的启动和请求成功证据；本次没有验证优雅关闭流程。

## 验收结论

**本步骤通过**：在本机默认 h2,spring-data-jpa 配置下，应用已实际独立启动、连接 H2 内存数据库、监听 9966，并成功响应健康检查及宠物类型读取请求。验收后应用已停止，不保持后台运行。

证据边界：只覆盖本机默认配置与这两个读取接口；其他业务接口、认证模式、外部数据库、其他机器访问、打包部署和优雅关闭均未验证。不得把内存数据库启动成功扩大为所有目标运行环境均可用。

## 本地证据

统一目录：D:\derek\maven\_cache\learning-evidence\step09-first-start-20260916。

- 01-application-run.log：完整启动日志及验收后主动终止产生的 Maven 状态。
- http-results.json：首次两个请求的原始采集结果，健康正文最初为字节数组。
- health-response-decoded.json：正确解码后的健康响应。
- pettypes-response.json：实际业务响应。
- verified-results.json：完成解码、状态、数据和进程核验后的汇总。
- application-process.json、listener.json：应用进程及监听端口。
- stop-result.json：主动停止原因和端口释放结果。

原始日志和响应留在本机 D 盘，提交仅包含学习文档。

## 新概念与面试题

1. **监听端口是什么？** 应用在指定的网络入口等待请求。配置写着 9966 不代表已经监听，须结合操作系统实际监听状态与对应进程核对。
2. **为什么看到 Started 还不够？** 启动日志只证明当时完成启动流程；后续可能退出或请求地址、路由等仍不正确，因此还需核对监听进程和真实请求响应。
3. **健康状态 UP 是否证明所有业务接口正确？** 不能，它反映已配置健康检查项的状态。还应选择具体业务请求核对状态码、响应结构和内容；本次仅验证宠物类型读取。
4. **测试通过与独立启动验证有什么不同？** 测试可能使用模拟请求及测试配置；本次运行真实应用进程，用客户端经本机网络访问实际监听端口。
5. **最后出现 BUILD FAILURE 是否必然说明启动失败？** 不能脱离时间顺序和原因判断。本轮先取得启动和请求成功证据，随后主动终止子进程，Maven 才报告非零退出。

## 学习者练习与能力边界

本轮执行由 Codex 完成，尚未收到学习者对本步证据链的解释，因此独立分析本步启动和请求证据的能力标记为待验证。

请说明 Started 日志、9966 监听进程、健康接口 200/UP、宠物类型接口 200/6 条数据分别证明什么，以及为何只有 Started 日志不够。可结合主动终止后的 Maven 状态解释如何区分运行验收和结束方式。

## 官方文档

- [Spring Boot：使用 Maven 运行应用](https://docs.spring.io/spring-boot/maven-plugin/run.html)
- [Spring Boot：管理与健康端点](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)

## 唯一下一步建议

先由学习者完成上述本步证据分析练习，再选择下一项独立学习任务。本次不自动扩展到其他接口或部署。
