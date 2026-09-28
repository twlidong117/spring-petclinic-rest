# 第二十五步：更新名称非法时服务无调用

## 日期、基准与范围

- 2026-09-28（Asia/Shanghai），来源：Codex 实际核验。开始时工作区干净，已读取规则、进度与第二十四步收尾。
- PR #24 实查 MERGED、目标 dev，合并提交 7352b170eb0c63ca15e92a8ecb7c71117ba4d7a4；fetch 后确认 origin/dev 包含该提交，从该提交创建 codex-step-25-update-pettype-validation。
- 实际核验现有 Maven Wrapper 使用 Maven 3.9.16、Java 21.0.12.1，MAVEN_USER_HOME 为 D:\derek\maven\_cache；未安装或升级。
- 只增强既有 testUpdatePetTypeError 和学习文档，沿用现有导入；未改业务、接口约束、生成源码、依赖或配置，未提交推送。

## 目标与概念

原测试使用空名称，仅检查 400。本步直接提供独立请求正文，补充结构化错误响应及 ClinicService 无调用检查，确认错误字段和服务调用边界。

- 字段校验：检查请求对象的属性是否满足声明的规则。已有生成 PettypesApi.updatePetType 的请求对象使用 @Valid @RequestBody，PetTypeDto.getName 声明 @NotNull @Size(min=1,max=80)。生成文件已读取，不直接修改。
- 空字符串不是 null，但长度为 0，不满足最小长度 1。
- MethodArgumentNotValidException：本例请求对象字段校验失败时的异常类型；项目异常处理器将其转换为 400 和字段错误列表。
- application/problem+json：结构化错误响应的内容类型，测试通过 APPLICATION_PROBLEM_JSON 常量检查兼容性。
- schemaValidationErrors：本项目错误正文中的字段错误列表。本步检查只有一项、field 为 name、rejectedValue 为真实空字符串，而不是字符串 "null"。
- verifyNoInteractions：检查服务替身没有任何方法调用。本例包含查找和保存均未调用；与上步合法请求查找一次后返回 404 的情况不同。

## 完整相关测试

沿用已有测试类初始化和导入：clinicService 由 MockitoBean 替换为服务替身；mockMvc 配置控制器及 ExceptionControllerAdvice；WithMockUser 提供测试用户。put、status、content、jsonPath、verifyNoInteractions、MediaType 均已导入。以下为完整方法而非独立测试类。

```java
    @Test
    @WithMockUser(roles="VET_ADMIN")
    void testUpdatePetTypeError() throws Exception {
        this.mockMvc.perform(put("/api/pettypes/1")
                .content("{\"id\":1,\"name\":\"\"}")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("MethodArgumentNotValidException"))
            .andExpect(jsonPath("$.schemaValidationErrors.length()").value(1))
            .andExpect(jsonPath("$.schemaValidationErrors[0].field").value("name"))
            .andExpect(jsonPath("$.schemaValidationErrors[0].rejectedValue").value(""));

        verifyNoInteractions(this.clinicService);
    }
```

## 执行证据与验收（来源：Codex）

```powershell
./mvnw.cmd -o '-Dtest=PetTypeRestControllerV1Tests#testUpdatePetTypeSuccess+testUpdatePetTypeError+testUpdatePetTypeNotFound' test
```

- 2026-09-28 21:41 至 21:42 实际运行；Maven 退出状态 0，BUILD SUCCESS。离线使用已有依赖，不跳过检查。
- 3 项执行，失败、错误、跳过均为 0；三个指定方法均已核验。报告修改时间 21:42:20.7757626（+08:00），晚于本次开始。准确起止时间见 run.json。
- 原始证据目录：D:\derek\maven\_cache\learning-evidence\step25-update-validation-20260928-214156，含 test.log、run.json 和本次 XML 测试报告。
- 实际通过的检查：400、错误内容类型、正文 status=400、title=MethodArgumentNotValidException、一个 name 字段错误、被拒绝值为空字符串、ClinicService 无任何调用；成功更新及目标不存在两个案例也通过。
- 结合生成接口和约束可解释为请求字段校验阶段拒绝，尚未执行控制器内的查找保存流程；本次没有对控制器方法入口做独立调用计数。
- 测试启动连接 jdbc:hsqldb:mem:petclinic 内存数据库；请求的服务为替身，不能说整个测试没有数据库连接。服务无调用也不证明真实数据库端到端行为。
- 未运行全量测试或独立应用，未扩展到其他非法输入，未运行先查找后返回同样错误的故障版本。实现与定向验证完成，差异检查通过。

## 官方资料与面试问答

- [Spring 请求正文与校验](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)：请求对象校验及 400。引用框架基本机制，不当作本项目版本证据。
- [Mockito 调用验证](https://site.mockito.org/javadoc/current/org/mockito/Mockito.html)：调用验证基础语义，官方旧版页面不作为升级建议。

1. 空字符串为何不由 NotNull 拒绝？它是存在的字符串值；本例由最小长度约束拒绝。
2. 更新返回 400 与上步 404 的流程有何区别？本例 400 发生在字段校验阶段，服务无调用；上步请求合法，查找返回 null 后才返回 404。
3. 只验证保存 never 是否足够证明服务无调用？不够，仍可能调用查找；verifyNoInteractions 检查整个服务替身无调用。

## 能力边界与唯一练习

- 学习者尚未提供本步回答，本步个人理解、独立测试编写和排障均待验证；Codex 实现与执行不代表学习者已掌握。
- 练习：假设错误实现先调用 findPetTypeById(1)，不调用保存，然后仍返回完全相同的 400 和字段错误正文。当前测试能通过吗？哪一行会发现问题？如果只改为检查保存 never，又能否发现这次多余查找？只分析，不改代码。
- 下一步：收到回答后增量记录原回答与复核。本轮停在第二十五步练习，不自动提交、创建合并请求或进入后续步骤。

## 首次练习回答与纠正（2026-09-28）

### 学习者原回答（来源：本轮聊天）

> 第一步失败。不能发现。

### 复核结论

- 第二部分正确：仅检查保存 never，不能发现多余的查找调用。学习者尚未展开说明原因，不推定已独立解释全部检查范围。
- 第一部分需纠正：题设保证仍返回相同的 400 和错误正文，第①步是发送请求，第②至④步响应检查都会通过。第⑤步 verifyNoInteractions(this.clinicService) 因 findPetTypeById(1) 已调用而失败。
- Codex 补充：错误调用发生在处理请求时，不等于测试在发送请求这一行就发现错误；失败位置取决于哪个检查不满足预期。响应检查和服务调用检查职责不同。
- 本题仍是源码推断，未执行假设故障版本。基础练习尚待纠正后复核，不标记本步理解已全部验证；独立测试编写、排障等仍待验证。

### 本轮操作与证据

- Codex 已读取规则与记录，核对当前分支 codex-step-25-update-pettype-validation，HEAD 与本地 origin/dev 同为 7352b170eb0c63ca15e92a8ecb7c71117ba4d7a4，祖先检查退出状态 0。
- 保留现有测试文件与两份文档改动，本轮仅增量更新学习文档；未 fetch、查询远端、修改代码、重跑测试、提交或推送。
- 三项通过仍为 2026-09-28 21:41 至 21:42 的执行证据，不记为本轮重新执行。

### 知识点、官方资料与面试问答

1. 多余调用发生时，测试一定立即失败吗？不一定；本题响应仍符合预期，最终服务调用检查才会报告失败。
2. verifyNoInteractions 为何失败？它要求服务替身完全无调用，但查找已发生一次。
3. 保存 never 为何发现不了？它只检查保存调用次数，保存确实为零，不约束查找。

官方资料：[Mockito 调用验证](https://site.mockito.org/javadoc/current/org/mockito/Mockito.html)、[Spring 请求正文校验](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)。

### 当前唯一复核练习

同一假设下：已调用 findPetTypeById(1)，没有保存，响应内容完全不变。检查 A 为 verifyNoInteractions(this.clinicService)，检查 B 为 verify(this.clinicService, never()).savePetType(any())。请分别判断 A、B 通过还是失败，并各用一句话说明检查范围。它们作为两种独立检查比较，不是前者失败后还会继续执行后者。只分析，不改代码；停在第二十五步复核。

## 纠正后回答与第二十五步收尾（2026-09-28）

### 学习者原回答（来源：本轮聊天）

> A失败，B通过。verifyNoInteractions会检查整个类的方法都没有调用，但是verify never只检查指定方法是否没有调用

### 复核与能力边界

- 判断正确：查找一次、保存零次时，A 的 verifyNoInteractions 失败，B 的保存 never 检查通过。学习者在纠正和代码提示后说明了两种检查的范围差异。
- Codex 精确补充：“整个类”应表述为传入的这个服务替身对象；不是检查该类的所有实例。verify(..., never()) 检查指定方法及匹配参数的调用为零。本例 any() 覆盖保存方法的任意参数。该精确限定由助手补充，不记为学习者原话。
- 第二十五步实现、三项定向验证与基础练习复核完成，保留首次失败位置误判与纠正过程。已有纠正后区分两种调用检查的回答证据；无提示独立定位完整测试最先失败位置、独立编写与排障仍待验证。
- 假设查找后返回同样错误的版本未实际执行；A/B 结果为代码和调用语义推断，不是运行日志。

### 本轮实际核对（来源：Codex）

- 已读取规则、进度与前次回答，核对分支 codex-step-25-update-pettype-validation；HEAD 与本地 origin/dev 同为 7352b170eb0c63ca15e92a8ecb7c71117ba4d7a4，祖先检查退出状态 0。
- 保留一个测试文件与两份文档的既有改动，仅增量记录本轮问答；未 fetch、查询远端新状态、修改代码、重跑、提交或推送。
- 三项通过仍为本日 21:41 至 21:42 执行证据，不冒记为本轮运行。

### 知识点、官方资料与面试问答

1. verifyNoInteractions 检查整个类吗？不是，检查传入的替身对象没有任何方法调用。
2. 保存 never 能发现查找调用吗？不能，它只检查指定保存方法及匹配参数的调用。
3. 响应完全符合预期时，测试还可能失败吗？可能，服务调用记录仍可能违反后续检查。

资料：[Mockito 调用验证](https://site.mockito.org/javadoc/current/org/mockito/Mockito.html)、[Spring 请求正文校验](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)。

### 下一步建议（未执行）

后续独立步骤可阅读删除宠物类型的查找与删除流程，对比本步更新的校验和目标不存在分支。本轮停在第二十五步收尾，等待用户安排，不自动进入下一步或提交。
