# 第二十步：新增名称长度边界验证

## 日期、基准与范围

- 2026-09-20（Asia/Shanghai）。PR #19 已核验为 MERGED、目标 dev；获取 origin/dev 后核验包含合并提交 a4c8682e0003beff799c08298390edeff57956d5，从该提交创建 codex-step-20-pettype-name-length。开始时工作区干净。
- 已核验 Java 21.0.12.1、Maven Wrapper 使用 Maven 3.9.16，MAVEN_USER_HOME 为 D:\derek\maven\_cache；沿用用户环境变量，不安装或升级 Java、依赖和配置。
- 在 PetTypeRestControllerV1Tests 新增两组参数化测试及两个导入；已有测试和业务代码不变。只验证名称长度边界，不提交或推送。

## 规则、概念与测试设计

接口定义 openapi.yml 的 PetTypeFields.name 长度范围为 1 至 80；生成字段约束为 @NotNull 与 @Size(min = 1, max = 80)。Size 包含上下边界，单独不拒绝 null；NotNull 拒绝 null，但不会单独拒绝空字符串。

边界值测试是检查允许范围的端点及紧邻范围外的值，以发现“少接受一个”或“多接受一个”的错误。本步用字母 a 重复生成名称，避免混入特殊字符处理。

| 输入 | 本轮实际结果 | 服务验证 |
| --- | --- | --- |
| 空字符串，长度 0 | 400；字段 name；日志显示 Size | 无任何调用 |
| 一个 a，长度 1 | 201；响应名称保持原值，编号为模拟的 7 | 保存恰好调用一次 |
| 80 个 a | 201；响应名称保持原值，编号为模拟的 7 | 保存恰好调用一次 |
| 81 个 a | 400；字段 name；日志显示 Size | 无任何调用 |
| null（原测试） | 400；日志显示 NotNull | 无任何调用 |

- 参数化测试：同一个测试方法带入不同参数，分别执行和报告。@ParameterizedTest 替代普通 @Test；@ValueSource(ints = {1, 80}) 依次提供整数 1 和 80。
- length 是本次执行收到的长度；"a".repeat(length) 重复字母 a，重复 0 次得到空字符串。用固定字母拼接请求仅适用于这里的受控数据，不推广为任意文本的序列化方案。
- 每次参数执行都运行现有初始化逻辑，服务替身由测试框架重置；不是在一个请求中同时验证两个名称。
- 接受组沿用服务替身设置编号 7，验证保存前名称与空编号、响应名称和编号、保存一次；编号不是数据库生成的证据。
- 拒绝组验证 400、错误内容类型、唯一字段错误、字段 name 与被拒绝的原名称，再检查服务无调用。Size 的具体错误代码由本轮日志佐证，不声称正文断言直接检查了约束代码。

## 本步新增完整代码

新增导入：

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
```

以下方法放在现有测试类内，依赖已有初始化及导入；聊天中同样直接展示，不要求手机打开仓库。

```java
    @ParameterizedTest(name = "accept name length {0}")
    @ValueSource(ints = {1, 80})
    @WithMockUser(roles = "VET_ADMIN")
    void testCreatePetTypeNameLengthAccepted(int length) throws Exception {
        String name = "a".repeat(length);
        doAnswer(invocation -> {
            PetType petType = invocation.getArgument(0);
            assertEquals(name, petType.getName());
            assertNull(petType.getId());
            petType.setId(7);
            return null;
        }).when(this.clinicService).savePetType(any(PetType.class));

        this.mockMvc.perform(post("/api/pettypes")
                .content("{\"name\":\"" + name + "\"}")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.id").value(7));

        verify(this.clinicService).savePetType(any(PetType.class));
    }

    @ParameterizedTest(name = "reject name length {0}")
    @ValueSource(ints = {0, 81})
    @WithMockUser(roles = "VET_ADMIN")
    void testCreatePetTypeNameLengthRejected(int length) throws Exception {
        String name = "a".repeat(length);
        this.mockMvc.perform(post("/api/pettypes")
                .content("{\"name\":\"" + name + "\"}")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.schemaValidationErrors.length()").value(1))
            .andExpect(jsonPath("$.schemaValidationErrors[0].field").value("name"))
            .andExpect(jsonPath("$.schemaValidationErrors[0].rejectedValue").value(name));

        verifyNoInteractions(this.clinicService);
    }
```

## 实际执行与证据

Codex 使用现有 Wrapper 离线执行，现有依赖已支持参数化测试，没有修改依赖：

```powershell
./mvnw.cmd -o '-Dtest=PetTypeRestControllerV1Tests#testCreatePetTypeSuccess+testCreatePetTypeError+testCreatePetTypeNameLengthAccepted+testCreatePetTypeNameLengthRejected' test
```

- 执行开始 2026-09-20 22:18:27，结束 22:19:14。Maven 退出状态 0，BUILD SUCCESS。
- 实际执行 6 项，失败 0、错误 0、跳过 0：接受组两次、拒绝组两次、原成功和 null 失败各一次。
- 报告包含 Accepted(int)[1]/[2]、Rejected(int)[1]/[2] 及两个原测试方法；报告修改时间 22:19:14，晚于本次开始，未混入历史报告。
- 证据目录：D:\derek\maven\_cache\learning-evidence\step20-name-length-20260920-221826，保留 test.log、run.json 及本测试类原始报告。
- 本轮日志确认空字符串与 81 个 a 触发 Size，null 触发 NotNull。测试启动连接 jdbc:hsqldb:mem:petclinic 内存数据库，但本请求的服务为替身，接受组没有执行真实保存链路。
- 验收：第二十步实现和六项定向验证通过，学习者基础理解待回答验证。代码、运行及报告检查均由 Codex 完成。
- 未验证：真实数据库新增、任意字符或纯空白输入、完整权限链路、全量测试、独立应用启动及外部网络访问。不能因四个边界通过就宣称所有输入均已验证。

## 官方资料与面试问答

- [Jakarta Validation：Size](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/size)
- [JUnit：参数化测试](https://docs.junit.org/6.1.3/writing-tests/parameterized-classes-and-tests.html)

1. **为什么选 0、1、80、81？** 分别检查下界外、下界、上界、上界外，能发现边界接受或拒绝错误。
2. **null 与空字符串违反同一条规则吗？** 本例 null 违反 NotNull，空字符串长度为 0，违反 Size；Size 单独接受 null。
3. **两个参数化方法为什么产生四次执行？** 每个方法收到两组参数，各执行两次；再加原有两个测试，本轮共六次执行。

## 当前唯一练习

当前约束是 @NotNull 和 @Size(min = 1, max = 80)。假设只删除 @NotNull，name 为 null 与 name 为 ""，哪个仍会被这条 Size 约束拒绝，为什么？只分析字段约束，不推断删除后整个请求最终会返回什么。故障版本未实际修改或运行。

本次停在第二十步练习；收到学习者回答后增量复核，不自动继续下一步、提交或推送。
## 练习回答与第二十步收尾（2026-09-21）

### 学习者原回答

> name=“”会被拒绝。长度为0

### 复核、验收与能力边界

- 回答正确：学习者指出空字符串会被拒绝并给出长度为 0 的原因，符合 Size(min = 1, max = 80) 的最小长度要求。
- Codex 补充：Size 单独接受 null，因此需要 NotNull 另外约束 null；这是对规则的补充说明，不将学习者未明确展开的内容记为独立表述，也不推断删除注解后整个请求最终状态。
- 第二十步实现、定向验证和当次基础练习复核完成。已有讲解后识别空字符串长度约束的回答证据；独立解释参数化执行机制、编写边界测试、分析任意输入与排障仍待验证。
- Codex 本轮核对工作区及本地 origin/dev 祖先关系；仍在 codex-step-20-pettype-name-length，HEAD 为 a4c8682e0003beff799c08298390edeff57956d5。未获取远端新状态，不声称远端今日没有变化。
- 本轮仅增量更新学习文档，未修改代码、构建、重跑测试、提交或推送。六项测试通过是 2026-09-20 的运行证据，不改写成 2026-09-21 重跑；删除 NotNull 的假设版本未实际修改或运行。

### 知识点、官方资料与面试問答

1. **空字符串为何被 Size(min = 1, max = 80) 拒绝？** 长度为 0，小于最小值 1。
2. **Size 单独拒绝 null 吗？** 不拒绝；本例由 NotNull 负责。
3. **四个长度边界通过是否证明任意输入都正确？** 不能，只证明本次选择的输入与检查通过，其他字符、空白及真实保存等仍需相应验证。

官方资料：[Jakarta Validation：Size](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/size)、[JUnit 参数化测试](https://docs.junit.org/6.1.3/writing-tests/parameterized-classes-and-tests.html)。

### 下一步建议（未执行）

后续独立步骤可检查一个普通空格是否通过现有字段约束，帮助区分 null、空字符串与纯空白；先核对现有规则，不擅自新增业务约束。继续在聊天提供完整相关测试和必要说明。本轮停在第二十步收尾，等待用户安排，不自动提交或开始下一步。
