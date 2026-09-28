# 第二十三步：验证更新时的保存参数与调用

## 日期、基准和范围

- 2026-09-27（Asia/Shanghai）。用户告知 PR 已合入并要求继续；Codex 实查 PR #22 为 MERGED、目标 dev，合并提交 8046d4c17df296ac3d033a71507d05ebea1fed8a。
- 开始时工作区干净；读取 AGENTS.md、进度和第二十二步收尾。fetch 后核验 dev 包含该合并提交，再从 origin/dev 建立 codex-step-23-update-pettype-save，HEAD 为上述提交。
- 实际通过已有 Maven Wrapper 核验 Maven 3.9.16、Java 21.0.12.1，MAVEN_USER_HOME 为 D:\derek\maven\_cache。未安装或升级 Java、依赖或工具。
- 只增强 testUpdatePetTypeSuccess 并增加 assertSame 静态导入，另更新本步与进度文档。未改业务代码、接口约束或运行配置，未提交或推送。

## 问题和知识点

旧测试把 petTypes.get(1) 同时用作请求数据来源和查找替身的返回对象，在请求前就 setName("dog I")。后续读取新名称不足以证明控制器完成了修改。

新测试保持查找返回实体的初始名称为 dog，另用独立的 JSON 请求正文传入 dog I。JSON 是用键和值表达数据的文本格式。

- 断言：检查实际结果是否符合预期，不符合就使测试失败。
- assertSame：检查两个引用指向同一个对象；不仅是字段值相同。本步明确验证当前“修改并保存查到的实体”的流程，不表示所有实现都必须使用同一个对象。
- doAnswer：设置替身方法被调用时执行的回调。本例在保存调用发生的当刻，读取参数并检查对象身份、编号和名称；不主动改名、不执行真实保存。
- invocation.getArgument(0)：取本次保存调用的第一个参数，序号从 0 开始。
- verify：此处检查匹配的保存调用恰好一次，防止保存调用根本没有发生时回调中的断言被全部绕过。
- 后续 GET 仍读取服务替身返回的同一个对象，用于检查响应；不是重新查询真实数据库的证据。

## 完整相关测试

使用现有测试类：ClinicService 由 MockitoBean 替换为服务替身，MockMvc 负责模拟请求；每次测试前 petTypes.get(1) 初始化为 id=2、name=dog，控制器与映射器沿用现有初始化。

新增导入：import static org.junit.jupiter.api.Assertions.assertSame;。其余 assertEquals、given、doAnswer、verify、any、put/get、status/content/jsonPath、MediaType、Test、WithMockUser 和 PetType 均使用已有导入。以下不是独立测试类。

```java
    @Test
    @WithMockUser(roles="VET_ADMIN")
    void testUpdatePetTypeSuccess() throws Exception {
        PetType currentPetType = petTypes.get(1);
        given(this.clinicService.findPetTypeById(2)).willReturn(currentPetType);
        assertEquals("dog", currentPetType.getName());

        doAnswer(invocation -> {
            PetType savedPetType = invocation.getArgument(0);
            assertSame(currentPetType, savedPetType);
            assertEquals(2, savedPetType.getId());
            assertEquals("dog I", savedPetType.getName());
            return null;
        }).when(this.clinicService).savePetType(any(PetType.class));

        this.mockMvc.perform(put("/api/pettypes/2")
                .content("{\"id\":2,\"name\":\"dog I\"}")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().contentType("application/json"))
            .andExpect(status().isNoContent());

        verify(this.clinicService).savePetType(any(PetType.class));

        this.mockMvc.perform(get("/api/pettypes/2")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.name").value("dog I"));
    }
```

## 实际执行证据（来源：Codex）

命令：

```powershell
./mvnw.cmd -o '-Dtest=PetTypeRestControllerV1Tests#testUpdatePetTypeSuccess+testUpdatePetTypeError' test
```

-o 表示离线使用已有本地依赖；没有跳过检查。

- 2026-09-27 12:26 开始，报告生成于 12:26:52（+08:00），晚于本次启动；精确起止时间见 run.json。
- Maven 退出状态 0，BUILD SUCCESS；2 项执行，失败 0、错误 0、跳过 0。报告已核验 testUpdatePetTypeSuccess 与 testUpdatePetTypeError 两个方法。
- 原始证据目录：D:\derek\maven\_cache\learning-evidence\step23-update-save-20260927-122625，含 test.log、run.json 和本次原始 XML 测试报告。
- 实际验证：保存替身收到同一个已有实体，id=2、name=dog I；保存恰好一次，更新响应状态 204，随后 GET 返回编号 2 和新名称；原非法空字符串更新测试仍通过（400）。
- 日志显示测试启动连接 jdbc:hsqldb:mem:petclinic；本请求服务为替身，不能据此证明真实数据库更新。
- 未验证：全量测试、独立应用启动、真实数据库写入、地址与正文编号不一致的请求、200/204 契约修复。未运行删除 setName 或 savePetType 的故障版本。

## 官方资料及面试题

- [JUnit Assertions](https://docs.junit.org/5.12.2/api/org.junit.jupiter.api/org/junit/jupiter/api/Assertions.html)：assertSame 检查同一对象，assertEquals 检查预期值。页面用于方法语义，不当作项目依赖版本证据。
- [Mockito 官方文档](https://site.mockito.org/javadoc/current/org/mockito/Mockito.html)：doAnswer 回调与 verify 默认一次。该官方页面标为旧版，仅用于这两个基础方法语义，不指导升级。

1. 为什么不能在发送更新请求前就修改查找返回对象？那会提前满足新名称条件，降低测试发现控制器遗漏改名的能力。
2. assertSame 与 assertEquals 有何区别？前者检查是否同一个对象；后者用于检查值是否符合预期。本步分别检查实体身份与编号、名称。
3. 保存回调里已经有断言，为何还检查 verify？没有保存调用时回调不会执行；verify 可发现未调用或调用次数不正确。

## 验收、能力与当前唯一练习

- Codex 完成实现与两项定向验证，文档与代码差异检查通过。实现和运行不代表学习者已掌握。
- 学习者本步暂无回答或操作证据；独立理解、修改测试和排障待验证。第二十二步已验证能力保持原范围。
- 练习：假设控制器保留 setName 和 204 返回，但误删 savePetType 调用。上述新测试最先会在哪个检查处失败？为什么 doAnswer 内的名称断言不能发现这个遗漏？只分析，不实际修改业务代码。
- 收到回答后增量记录原回答与复核；本轮停在第二十三步练习，不自动提交、创建合并请求或继续下一步骤。

## 练习回答与第二十三步收尾（2026-09-27）

### 学习者原回答（来源：本轮聊天）

> 第4步verify检查失败。doAnswer只比对了currentPetType和savedPetType是否一致，而且触发时机是主流程走到调用savePetType调用时。现在删除了savePetType调用，所以不会触发doAnswer

### 复核与能力边界

- 核心判断正确：保留改名和原响应、仅遗漏保存调用的假设下，回调不执行，第④步 verify 发现保存调用为 0 而非预期 1 次，测试在后续 GET 前失败。
- 需纠正的表述：doAnswer 回调并非“只比对了 currentPetType 和 savedPetType 是否一致”；它依次执行 assertSame 检查同一对象、assertEquals 检查编号 2、assertEquals 检查名称 dog I。这是 Codex 补充，不记为学习者已独立解释完整断言职责。
- 基础练习复核完成：学习者在完整代码和讲解后能定位遗漏保存的失败检查，并解释回调因未调用而不触发。回调内三项断言职责的独立区分、独立测试编写和真实故障排查仍待验证。
- 本题为源码推断，未实际删除 savePetType 或运行故障版本；不能当作实际失败日志或数据库证据。

### 本轮实际操作（来源：Codex）

- 已读取规则和既有记录；当前目录与分支符合本步，HEAD 和本地 origin/dev 均为 8046d4c17df296ac3d033a71507d05ebea1fed8a，祖先检查退出状态为 0。
- 保留已有一个测试文件与两份文档的修改，本轮仅增量更新学习记录；未 fetch、查询远端新状态、改代码、重跑测试、提交或推送。
- 两项通过仍为本日 12:26 的执行证据，不记作本轮重新执行。

### 知识点、官方资料与面试问答

1. 配置 doAnswer 会立即执行回调吗？不会，此处在匹配的保存替身调用发生时才执行。
2. 回调内检查什么？同一实体对象、编号为 2、名称为 dog I；对象身份与字段值分别检查。
3. verify 默认检查什么？本例匹配的保存调用恰好一次，0 次或多次均不满足。

官方资料：[Mockito](https://site.mockito.org/javadoc/current/org/mockito/Mockito.html)、[JUnit Assertions](https://docs.junit.org/5.12.2/api/org.junit.jupiter.api/org/junit/jupiter/api/Assertions.html)。仅用于基础方法语义，不作为项目版本证明。

### 下一步建议（未执行）

后续独立步骤可为更新时查找不到实体补充测试，检查返回 404 且未调用保存。本轮停在第二十三步收尾，等待用户安排，不自动提交或进入下一步。
