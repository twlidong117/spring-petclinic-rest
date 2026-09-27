# 第二十一步：普通空格名称的现有行为

## 日期、基准与范围

- 2026-09-21（Asia/Shanghai）。PR #20 已核验为 MERGED，目标 dev；获取 origin/dev 并确认包含合并提交 2d887b7a825d0a147740951344828d6749a51aff，从该提交创建 codex-step-21-pettype-space-name。开始时工作区干净。
- 已核验 Java 21.0.12.1、Maven Wrapper 使用 Maven 3.9.16，MAVEN_USER_HOME 为 D:\derek\maven\_cache；未安装或升级环境、依赖。
- 仅新增 testCreatePetTypeSingleSpaceNameAccepted 测试与学习记录，不改变业务代码、字段约束、运行配置或已有测试；未提交或推送。

## 概念与预期

- null：没有字符串值，由 NotNull 拒绝。
- ""：存在字符串，长度为 0，由 Size(min = 1, max = 80) 拒绝。
- " "：包含一个普通空格，长度为 1，既不是 null，也未越过长度边界；当前生成 PetTypeFieldsDto 仅有 NotNull 与 Size，没有 NotBlank。
- 纯空白指字符串只有空白字符。本步实际只验证一个普通空格，不推广为任意空白字符组合均已验证。
- NotBlank 要求非 null 且至少有一个非空白字符。它与长度限制职责不同；本步只解释，不添加该注解或修改接口契约。是否禁止纯空白应由明确业务需求决定。

## 完整新增测试

测试检查服务刚收到的实体名称为一个空格、编号为空，再模拟保存后编号 7，检查 201、原样返回的名称和编号，以及保存调用一次。服务前和响应后的精确字符串检查可以发现本例处理链路将空格改为空字符串的变化。

以下方法使用现有测试类初始化和导入，不是独立可运行的完整测试类；聊天中直接展示，适配手机阅读。

```java
    @Test
    @WithMockUser(roles = "VET_ADMIN")
    void testCreatePetTypeSingleSpaceNameAccepted() throws Exception {
        doAnswer(invocation -> {
            PetType petType = invocation.getArgument(0);
            assertEquals(" ", petType.getName());
            assertNull(petType.getId());
            // Simulate saving without changing the supplied name.
            petType.setId(7);
            return null;
        }).when(this.clinicService).savePetType(any(PetType.class));

        this.mockMvc.perform(post("/api/pettypes")
                .content("{\"name\":\" \"}")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(" "))
            .andExpect(jsonPath("$.id").value(7));

        verify(this.clinicService).savePetType(any(PetType.class));
    }
```

## 执行证据与验收

Codex 使用现有 Wrapper 离线执行新增用例及原有六个新增案例：

```powershell
./mvnw.cmd -o '-Dtest=PetTypeRestControllerV1Tests#testCreatePetTypeSuccess+testCreatePetTypeError+testCreatePetTypeNameLengthAccepted+testCreatePetTypeNameLengthRejected+testCreatePetTypeSingleSpaceNameAccepted' test
```

- 本轮实际执行时间：2026-09-21 07:39:16 至 07:39:38；Maven 退出状态 0，BUILD SUCCESS。
- 7 项执行，失败 0、错误 0、跳过 0：新空格用例一次、长度参数化测试四次、原成功和 null 失败各一次。
- 新报告核验包含上述七次执行，修改时间 07:39:38，晚于本次开始，未计入历史报告。
- 证据目录：D:\derek\maven\_cache\learning-evidence\step21-space-name-20260921-073916，保留 test.log、run.json 和测试类原始报告。
- 实际验证：普通空格请求返回 201，服务收到和响应返回的名称均为一个空格，保存服务替身调用一次，编号为测试设置的 7。当前测试环境确实接受这一输入。
- 日志显示测试启动连接 jdbc:hsqldb:mem:petclinic 内存数据库，但请求服务为替身，不证明真实数据库保存普通空格名称；也不证明业务上允许空白是合理需求。
- 实现和七项定向验证完成，本步个人理解仍待回答验证。实现与执行均由 Codex 完成，不推定独立实践能力。
- 未验证：其他空白字符、真实数据库插入、全量测试、独立应用启动与外部请求；没有实际添加 NotBlank 或运行改约束后的版本。

## 官方资料与面试问答

- [Jakarta Validation：Size](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/size)
- [Jakarta Validation：NotBlank](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/notblank)

1. **为什么空字符串失败，一个普通空格却通过？** 空字符串长度为 0；普通空格长度为 1 且不是 null，满足当前两项约束。
2. **NotBlank 能否代替最大长度限制？** 不能，它要求至少一个非空白字符，最大长度仍需另外约束。
3. **这个测试通过能证明数据库保存了空格名称吗？** 不能，服务为替身；它验证请求处理、映射、响应和服务调用，不验证真实写入。

## 当前唯一练习

现有 name 约束为 NotNull 与 Size(min = 1, max = 80)。假设未来业务要求名称至少包含一个非空白字符：仅把 Size 的 min 从 1 改成 2，能彻底禁止纯空白名称吗？请用一个具体字符串说明原因。只分析规则，不执行约束修改。

本轮停在第二十一步练习，收到回答后增量复核；不自动提交或进入下一步。
## 练习回答与第二十一步收尾（2026-09-21）

### 学习者原回答

> 不能，可以输入连续空格的字符串，这样它的长度就可能大于size注解中min的限制

### 复核与验收

- 回答正确：学习者能说明连续空格可以满足最小长度，因此提高 min 不能彻底禁止纯空白名称。
- Codex 补充精确边界与例子：Size 的上下界均包含；在 min = 2、max = 80 的假设下，两个普通空格 "  " 长度为 2，等于下限也能通过。三个空格同样能通过；超过最大长度仍会被拒绝。具体例子与边界补充不冒记为学习者原话。
- 第二十一步实现、七项定向验证与基础练习复核完成。已有讲解后区分长度限制与纯空白限制的回答证据，不推定独立修改校验规则、设计测试或排障能力。
- Codex 本轮核对工作区及本地 origin/dev 祖先关系，HEAD 仍为 2d887b7a825d0a147740951344828d6749a51aff；未获取远端新状态。仅增量更新两份学习文档，未修改代码或业务规则，未重跑测试、提交或推送。
- 运行证据仍为本日 07:39 七项测试通过，不记作本轮再次执行。min = 2 与多个空格的假设未实际修改或运行；原有单个普通空格测试才是本步运行证据。

### 知识点、官方资料与面试问答

1. **提高 min 能禁止纯空白吗？** 不能，只要空格数量落在长度范围内，仍能通过 Size。
2. **长度必须大于 min 吗？** 不必，等于 min 即可；max 同样包含在允许范围内。
3. **NotBlank 与 Size 各负责什么？** NotBlank 要求非 null 且至少有一个非空白字符；Size 限制长度，不能相互替代全部职责。

官方资料：[Size](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/size)、[NotBlank](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/notblank)。

### 下一步建议（未执行）

可在后续独立步骤阅读更新宠物类型的查找、修改与保存流程，对比新增请求；是否禁止纯空白须有明确业务需求，不能因本次学习擅自改变契约。继续在聊天展示完整相关代码。本轮停在第二十一步收尾，等待用户安排，不自动提交或进入下一步。
