# 第十二步：增加列表数量断言并定向验证

## 基准与本步目标

- 日期：2026-09-16（Asia/Shanghai）。
- PR #11 已合并，Codex 获取最新 origin/dev 并核验合并提交 `a3063259b4cb1726e531577f19b4b63116d9bca1` 的祖先关系；从该基准创建 `codex-step-12-list-size-assertion`，起始工作区干净。
- 目标：把第十一步讨论的“列表数量恰好为 2”落实到现有测试，并只运行列表相关测试。
- 环境核验：Java 21.0.12.1、Wrapper 实际运行 Maven 3.9.16；发行包、依赖仓库、临时目录均使用 D:/derek/maven/_cache 下的配置。C:/Users/derek/.m2 仍为指向该目录的链接。

## 修改与知识点

仅在 `src/test/java/org/springframework/samples/petclinic/rest/controller/PetTypeRestControllerV1Tests.java` 的两个列表成功方法中各增加一行：

```java
.andExpect(jsonPath("$.length()").value(2))
```

对应方法：`testGetAllPetTypesSuccessAsOwnerAdmin()` 和 `testGetAllPetTypesSuccessAsVetAdmin()`。原有状态、内容类型、编号和名称断言均保留；业务代码、依赖及运行配置没有修改。

`$` 表示响应的整体；本例响应为数组，`length()` 取得元素数量，`value(2)` 要求它恰好为 2。检查前两项字段不能限制是否存在第三项，因此数量与字段断言互为补充。只检查数量也不能发现名称或编号错误。

定向测试是只选择与本次修改相关的测试执行。本轮选择方法名以 `testGetAllPetTypes` 开头的测试，覆盖两个成功场景和一个空集合场景，不代表重新执行全项目测试。

## Codex 实际执行与证据

命令（PowerShell，运行前已刷新用户的 JAVA_HOME、MAVEN_USER_HOME、MAVEN_OPTS）：

```powershell
./mvnw.cmd --offline --batch-mode --no-transfer-progress '-Dtest=PetTypeRestControllerV1Tests#testGetAllPetTypes*' test
```

- `--offline`：使用本地已有缓存，不联网解析依赖。
- `-Dtest=...`：按测试类及方法名模式选择测试；报告验证了本轮实际选择结果。
- `test`：执行 Maven 测试阶段及其前置构建阶段。
- 执行时间：2026-09-16 20:35 至 20:36（Asia/Shanghai）；日志显示总耗时 17.450 秒。
- 结果：Tests run 3，Failures 0，Errors 0，Skipped 0；BUILD SUCCESS，Maven 退出状态 0。
- 已独立读取本轮新生成报告，核对其修改时间晚于命令开始时间，且三个方法恰为两个成功方法和 `testGetAllPetTypesNotFound`。未汇总目录中其他历史报告。
- `git diff --check` 通过；测试源码差异仅新增两行数量断言。

证据目录：`D:/derek/maven/_cache/learning-evidence/step12-list-size-20260916-203544`，含 `maven-test.log`、`run-result.json`、`verified-results.json` 和本轮对应测试报告副本。

## 学习者回答与能力边界

- 本步练习：解释数量断言含义，以及“前两项仍是正确的 dog、snake，但额外多出第三项”时原字段断言与新增断言的结果。
- 学习者原文：“检查响应数组的长度是否等于2；字段断言正常通过，新增数量断言失败”。
- 复核：判断正确，数量与字段断言互补的概念已有针对给定场景的回答证据。
- 实际两行代码修改和命令执行由 Codex 完成；学习者独立编写可执行测试、运行命令与排查失败仍待实践验证。
- 额外第三项属于假设分析，本轮没有修改业务响应、没有注入该故障，也没有实际采集失败测试。

## 验收结论与范围

第十二步的数量断言修改、三个相关测试定向验证及基础练习完成。两个成功场景现在明确要求数量为 2；空集合 404 场景也在本轮选择范围内通过。

日志显示 Spring 测试上下文初始化了 HSQLDB 数据库相关组件，但控制器调用的 ClinicService 为测试替身，响应数据仍来自预设集合。不能据此证明真实查询返回 dog、snake。也不能把测试上下文启动当作应用独立监听端口或真实网络请求成功。

本轮没有运行全量测试、启动独立应用或验证所有认证授权行为；未提交、推送或创建合并请求。历史第八步、第九步运行证据仍属于各自历史执行。

## 官方文档

- [Spring：响应断言](https://docs.spring.io/spring-framework/reference/testing/mockmvc/hamcrest/expectations.html)
- [JsonPath：路径与函数](https://github.com/json-path/JsonPath#functions)
- [Maven Surefire：选择测试](https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html)

## 面试题与答案

1. **为什么已有前两项字段断言，还要增加数量断言？** 前两项正确不排除多返回记录；数量断言要求列表恰好两项。
2. **只检查数量等于 2，是否足够？** 不足够，两项的编号或名称仍可能错误，需保留内容断言。
3. **三个定向测试通过，能否宣布全部测试通过或应用可对外服务？** 不能。它只证明本轮选中的测试在当前环境通过，全量测试和真实网络启动需要各自证据。

## 唯一下一步建议

本步停止在已验证结果。下一独立学习步骤可阅读按编号查询宠物类型的成功与未找到分支，理解路径中的编号如何传入方法，以及服务未找到时如何形成响应；先核对源码再作判断。本次不自动进入该步骤。
