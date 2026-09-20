# 第十九步：新增字段校验错误与服务无调用验证

## 日期、基准和范围

- 2026-09-20（Asia/Shanghai）。核验 PR #18 为 MERGED、目标 dev；获取 origin/dev 后核验包含合并提交 2442751f38cf5e2d88ab9fba9e879775f20513f8，从该提交创建 codex-step-19-create-pettype-validation，起始工作区干净。
- 环境核验：Java 21.0.12.1，Maven Wrapper 使用 Maven 3.9.16，MAVEN_USER_HOME 为 D:\derek\maven\_cache；沿用用户环境设置，未安装或升级依赖与环境。
- 仅增强 PetTypeRestControllerV1Tests.testCreatePetTypeError 并增加 verifyNoInteractions 导入；业务代码、运行配置与成功测试不变，未提交或推送。

## 为什么改动

原失败测试仅断言 400，不能单独证明具体错误字段或保存服务没有被调用。现改为直接发送 {"name":null}，核对结构化错误响应和服务调用边界。null 表示没有值，不是空字符串，也不是请求字符串 "null"。

## 关键流程与概念

1. Spring 将请求内容反序列化为 PetTypeFieldsDto，字段约束 @NotNull 拒绝 name 为 null。校验失败发生在正常进入新增控制器方法体之前。
2. 本项目 ExceptionControllerAdvice.handleMethodArgumentNotValidException 构造 400 错误响应。MethodArgumentNotValidException 是请求方法参数校验失败的异常名称。
3. application/problem+json 是本例结构化错误响应的内容类型。schemaValidationErrors 是字段错误列表；[0] 表示第一项；field 为字段名，rejectedValue 为被拒绝的值，defaultMessage 为校验说明。
4. 处理器使用 Objects.toString(value, "null")，所以请求 name 的 null 被转换为响应 rejectedValue 的字符串 "null"。两者类型不同。
5. 测试检查错误列表只有一项、字段为 name、rejectedValue 为字符串 "null"、说明非空；不固定校验说明的语言文本，不声称检查了所有错误字段。
6. verifyNoInteractions(clinicService) 检查服务替身没有任何方法调用，比仅检查 savePetType 未调用更严格。本例请求在字段校验阶段被拒绝，适合这一预期。它不意味着整个测试环境没有连接数据库。

## 完整测试方法

```java
@Test
@WithMockUser(roles="VET_ADMIN")
void testCreatePetTypeError() throws Exception {
    this.mockMvc.perform(post("/api/pettypes")
            .content("{\"name\":null}")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("MethodArgumentNotValidException"))
        .andExpect(jsonPath("$.schemaValidationErrors.length()").value(1))
        .andExpect(jsonPath("$.schemaValidationErrors[0].field").value("name"))
        .andExpect(jsonPath("$.schemaValidationErrors[0].rejectedValue").value("null"))
        .andExpect(jsonPath("$.schemaValidationErrors[0].defaultMessage").isNotEmpty());

    verifyNoInteractions(this.clinicService);
}
```

新增导入为 `import static org.mockito.Mockito.verifyNoInteractions;`。方法依赖现有测试类初始化的 MockMvc（模拟处理请求的测试工具）和 ClinicService 替身；不是单独可运行的完整测试类。手机阅读时直接在聊天展示完整方法及必要说明。

## 实际执行证据与验收

Codex 使用现有缓存离线执行：

```powershell
./mvnw.cmd -o '-Dtest=PetTypeRestControllerV1Tests#testCreatePetTypeSuccess+testCreatePetTypeError' test
```

- 实际开始 2026-09-20 21:55:23，结束 21:55:46；Maven 退出状态 0，BUILD SUCCESS。
- 2 项执行，失败 0、错误 0、跳过 0。新报告方法名为 testCreatePetTypeSuccess 和 testCreatePetTypeError，修改时间 21:55:46，晚于本次开始，未混入旧报告。
- 证据目录：D:\derek\maven\_cache\learning-evidence\step19-create-pettype-20260920-215523，包含 test.log、run.json 与本测试类原始报告。
- 本轮日志实际记录 name 的 NotNull 错误；所有响应断言及服务无调用检查通过。日志也显示测试启动连接 jdbc:hsqldb:mem:petclinic 内存数据库；不能将服务无调用说成测试没有连接数据库。
- 验收：本步增强实现及两项定向验证通过。实现与执行均由 Codex 完成，尚无学习者本步回答，个人能力待验证。
- 未验证：其他非法输入（空字符串、超长名称等）、真实数据库写入、外部网络访问、独立应用启动和完整权限链路；未跑全量测试。没有实际运行“先保存再返回错误”的故障版本。

## 官方文档与面试问答

- [Spring 请求体反序列化与字段校验](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
- [Spring 响应断言](https://docs.spring.io/spring-framework/reference/testing/mockmvc/hamcrest/expectations.html)
- [Mockito 调用验证](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html)

1. **为什么检查 400 还不够？** 不同错误可能产生相同状态，需检查字段错误内容；响应错误也不能单独证明服务未被调用。
2. **verifyNoInteractions 检查什么？** 检查指定服务替身没有任何方法调用，本例包含没有保存调用；不检查数据库连接是否存在。
3. **响应 rejectedValue 为什么是字符串 "null"？** 本项目处理器把被拒绝的值转换为文本，null 被转换为字符串 "null"，请求的原字段值仍是 null。

## 当前唯一练习

假设出现一个错误实现：它先调用一次 clinicService.savePetType，之后仍返回与当前预期完全相同的 400 和字段错误内容。本测试会通过吗？如果不会，具体哪一行会发现问题？按手机阅读偏好在聊天中提供完整方法，一次只复核这一题。

本轮停在第十九步练习，不自动进入下一步、提交或推送。

## 练习回答与第十九步收尾（2026-09-20）

### 学习者原回答

> 不会。9会有问题

### 复核、验收与能力边界

- 回答正确：在已给出的完整测试方法及假设下，学习者正确判断测试不会通过，并定位第⑨行 verifyNoInteractions(this.clinicService)。
- Codex 补充原因：①至⑧响应检查虽全部通过，服务替身却已有一次保存调用，违反“没有任何方法调用”的预期。此原因由 Codex 解释，不冒记为学习者独立表述。
- 第十九步实现、两项定向验证及当次基础练习复核完成。学习者已有在讲解与完整代码提示下定位本例失败检查的证据，不推定独立编写测试、无提示解释任意调用验证或实际排障能力。
- Codex 本轮只核对记录与工作区并增量更新文档；未修改代码、重跑测试、提交或推送。运行证据仍为本日 21:55 两项测试通过，不记作本轮重跑。“先调用保存再返回错误”的故障版本未实际运行，失败位置属于代码分析。
- 其他非法输入、真实数据库写入、完整权限链路与外部请求验证仍未覆盖；其余个人实践能力继续待验证。

### 知识点、官方资料与面试问答

1. **响应符合预期，测试就一定通过吗？** 不一定，还需后续调用验证等检查全部通过。
2. **第⑨行为什么会失败？** 它要求服务替身没有任何调用，假设中的保存调用违反该要求。
3. **verifyNoInteractions 通过能证明环境没有数据库连接吗？** 不能，它只检查指定替身的调用情况；本步日志已证明测试启动连接内存数据库。

官方资料：[Spring 响应断言](https://docs.spring.io/spring-framework/reference/testing/mockmvc/hamcrest/expectations.html)、[Mockito 调用验证](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html)。

### 下一步建议（未执行）

下一独立步骤可验证 name 的长度边界，区分 null、空字符串与超长字符串的约束结果；按手机阅读偏好在聊天中提供完整相关测试及必要说明。本轮停在第十九步收尾，等待用户安排，不自动开始下一步或提交。
