# 第十六步：资源故障如何成为控制器错误响应

## 基准、范围与环境

- 日期：2026-09-17（Asia/Shanghai）。
- PR #15 已确认合并。获取 origin/dev 并核验祖先关系，从提交 `b451acab99cfe98a13d82c8b1e70bf6074c06b74` 创建 `codex-step-16-controller-error-response`，起始工作区干净。
- 本步目标：用 Service 替身抛出资源故障异常，验证真实控制器与异常处理器形成的响应。
- 环境核验：Java 21.0.12.1、Wrapper 使用 Maven 3.9.16；发行包、依赖仓库与临时目录仍配置在 D:/derek/maven/_cache 下。
- 只新增一个控制器测试及学习记录，没有修改业务代码、依赖或运行配置。

## 一、从异常到响应

异常处理器是专门接收请求处理过程中的异常、组织错误响应的组件。本项目 `ExceptionControllerAdvice` 标注 `@ControllerAdvice`，其中 `@ExceptionHandler(Exception.class)` 的 `handleGeneralException(...)` 处理本次资源故障异常。

```text
MockMvc 模拟 GET /api/pettypes/999
  → 真实 PetTypeRestControllerV1.getPetType(999)
  → ClinicService 替身抛出 DataAccessResourceFailureException
  → 控制器调用服务这一行异常退出，未进入 null 判断或 Mapper 调用
  → Spring 将异常交给 ExceptionControllerAdvice.handleGeneralException(...)
  → 记录错误日志，构造状态 500 的 ProblemDetail 响应
  → 消息转换器生成响应文本
  → 测试检查状态、内容类型与响应字段
```

`ProblemDetail` 是 Spring 用于描述错误的结构化对象。本项目处理器设置状态、异常类名标题、通用说明、时间戳以及校验错误集合；此结构由异常处理器构造，不是 PetTypeMapper 转换而来。

正常返回 null 和抛出异常不同：前者能执行控制器后面的空值判断并返回 404；后者使服务调用无法正常返回，控制流程进入异常处理机制。本次 500 由异常处理器选择，不是数据库直接返回，也不是 null 分支返回。

## 二、测试修改与断言

在 `PetTypeRestControllerV1Tests` 新增 `testGetPetTypeResourceFailure()`。现有准备代码已通过 `standaloneSetup(...)` 使用真实控制器，并通过 `setControllerAdvice(new ExceptionControllerAdvice())` 显式注册异常处理器；`ClinicService` 为 `@MockitoBean` 替身。

预设服务按编号查询抛出资源故障异常，然后通过 MockMvc 执行请求，检查：

- 响应状态为 500。
- 内容类型兼容 `application/problem+json`（表示 JSON 格式的错误详情）。
- 响应体 `status` 为 500、`title` 为 `DataAccessResourceFailureException`。
- `detail` 等于项目现有通用说明 `An unexpected error occurred while processing your request`，而非异常原始消息 `Database resource unavailable`。
- `timestamp` 存在，`schemaValidationErrors` 为空。

本例检查通用 detail，不代表对整个响应进行了完整信息泄露审查；title 仍按现有实现包含异常类名。时间戳只断言存在，没有验证精确时间格式或时间范围。本步未修改这些响应设计。

## 三、实际执行与证据

```powershell
./mvnw.cmd --offline --batch-mode --no-transfer-progress '-Dtest=PetTypeRestControllerV1Tests#testGetPetType*' test
```

- 2026-09-17 07:53 执行，日志总耗时 20.366 秒。
- 实际执行 4 个测试，失败 0、错误 0、跳过 0；BUILD SUCCESS，Maven 退出状态 0。
- 本轮方法：`testGetPetTypeSuccessAsOwnerAdmin`、`testGetPetTypeSuccessAsVetAdmin`、`testGetPetTypeNotFound`、`testGetPetTypeResourceFailure`。未选择列表读取或其他操作。
- 已独立核对新报告修改时间晚于命令开始时间、方法名及统计，未混入历史报告。`git diff --check` 通过。
- 证据目录：`D:/derek/maven/_cache/learning-evidence/step16-controller-error-20260917-075330`，含日志、命令结果、报告副本和验证汇总。
- 日志实际记录 `ERROR ExceptionControllerAdvice - Unexpected error at GET /api/pettypes/999`，这是本测试预设异常触发处理器日志的结果；不能单凭 ERROR 字样判断测试失败。

## 四、验收与证据边界

本步新增错误响应测试及三个既有按编号查询测试均通过，证明当前模拟请求处理环境下，选定服务异常被转换为符合断言的 500 响应。

本测试类加载 Spring 测试上下文，日志显示 HSQLDB 组件初始化；但当前请求调用的 Service 为替身，不经过真实服务和 Repository 查询。这是主动模拟的资源故障，不是观察到真实数据库故障。

MockMvc 使用进程内请求和响应对象，未执行真实网络请求，未启动独立监听服务。也未验证完整认证授权、所有异常类型或全量测试。

第十五步验证真实服务传播异常，第十六步验证服务替身抛异常后的响应；它们是两个不同层次的测试，不能拼称一次贯穿真实数据库、服务和外部请求的验证。本步没有重跑第十五步的服务测试。

测试代码由 Codex 编写并执行。尚未收到本步练习回答，个人解释能力待验证；历史学习者证据保持原范围。

## 五、官方文档与面试题

- [Spring：ExceptionHandler 异常处理](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)
- [Spring：ProblemDetail 错误响应](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)

1. **服务返回 null 与抛异常有什么区别？** 返回 null 后控制器继续执行空值判断；抛异常时调用无法正常返回，由匹配的异常处理机制接手。
2. **本次 500 由谁决定？** 当前项目的 `ExceptionControllerAdvice.handleGeneralException` 构造 500 响应，消息转换器负责输出文本。
3. **错误响应为 500，为什么测试仍通过？** 本测试预期这个模拟故障产生 500，且所有响应断言均满足；测试通过不等于业务请求成功。

## 六、当次练习与唯一下一步

Service 正常返回 null 与抛出资源故障异常，为什么不能都走控制器的 404 分支？这次是谁生成 500 响应？如果模拟请求得到预期 500，能否证明真实数据库发生了故障？

当前等待学习者回答并增量记录。本次不提交、推送或自动进入下一学习步骤。

## 2026-09-19 首次练习回答与细化（增量证据）

- 来源：学习者本轮回答：“1、服务返回null表示未找到资源，代码业务逻辑正常执行，符合404语义。抛出异常时代码业务逻辑无法正常执行，已经转入异常处理流程，不符合404语义。2、Controller层生成。3、不能”。
- 正确部分：在本例服务约定下，null 代表未找到，控制器继续空值判断并返回 404；抛出本次资源故障异常则转入异常处理流程。正确判断模拟响应不能证明真实数据库故障。
- 需要限定：null 的含义取决于方法约定，不普遍等于资源不存在；异常也不普遍排除 404，仍需看异常含义及处理规则。本例是资源故障而不是资源不存在，且当前处理器将它映射为 500。
- 需要精确定位：“Controller层”方向接近，但本次响应不是由 getPetType 的正常分支生成，而是由 `ExceptionControllerAdvice.handleGeneralException()` 构造，再由消息转换器输出文本。
- 第三问仅回答“不能”，结论正确；本次尚未独立说明 Service 替身预设抛异常、没有经过真实查询这一原因，不能把未表达的理由记为已验证。
- 当前结论：部分概念已有回答证据；异常映射规则及具体处理器定位待细化后复核。本轮仅更新文档，未修改测试或业务代码，未重跑测试或启动；2026-09-17 四个测试通过仍为历史执行证据。
- 当次追问：请说出本次生成 500 的具体方法，并说明为什么该测试不能证明真实数据库发生故障。

## 2026-09-19 复核收尾与提交准备（增量证据）

- 学习者复核回答：“`ExceptionControllerAdvice.handleGeneralException()` 这个方法。本测试中的异常是service代理抛出，并没有查询真实数据库”。
- 复核结果：正确定位生成本次 500 的具体处理方法，并正确解释异常为测试预设、没有经过真实数据库查询。术语细化为 Service 测试替身（mock），不将其笼统称为代理。
- 验收结论：第十六步测试实现、四项定向验证及基础问答收尾完成。保留首次回答、补充讲解和复核过程；精确方法定位属于讲解后复核，不推定独立源码定位或测试设计能力。任意异常与状态码规则的独立分析仍待验证。
- 本次按用户要求整理提交并创建面向 dev 的合并请求。2026-09-17 四个测试通过后测试代码未变，本轮核对历史报告，不将其记为 2026-09-19 重跑。
- 下一独立步骤建议：阅读新增宠物类型的请求体、字段校验和成功响应，先定位现有控制器及测试，再安排一个小练习。本次不执行该步骤。
