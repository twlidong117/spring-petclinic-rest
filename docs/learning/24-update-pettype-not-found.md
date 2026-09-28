# 第二十四步：更新目标不存在时不保存

## 日期、基准与范围

- 2026-09-28（Asia/Shanghai）。来源：Codex 实际命令检查。读取 AGENTS.md、进度与第二十三步收尾，开始时工作区干净。
- PR #23 实查 MERGED、目标 dev，合并提交 57bd4183b865dd7e2961947e862943a216bc4d8d。fetch 后确认 origin/dev 包含该提交，从该提交创建独立分支 codex-step-24-update-pettype-not-found。
- 已有 Maven Wrapper 实查 Maven 3.9.16、Java 21.0.12.1，MAVEN_USER_HOME 为 D:\derek\maven\_cache。未安装或升级环境、依赖。
- 仅新增 testUpdatePetTypeNotFound 与 never 静态导入，并增量更新学习文档；不改业务代码、接口约束或配置，不提交或推送。

## 目标与知识点

控制器 updatePetType 先调用 findPetTypeById；结果为 null 则 return 404，后面的 setName 和 savePetType 不再执行。本步用合法请求进入这个分支，避免字段校验先返回 400。

- 404：本例表示待更新目标未找到；400：已有非法名称测试中的请求错误。不能只看到失败响应就认为覆盖了同一个分支。
- given(...).willReturn(null)：明确规定服务替身查找编号 999 时返回空值，不是实际查询数据库。
- verify(service).findPetTypeById(999)：检查以这个编号查找恰好一次，可防止仅凭替身默认空返回掩盖查错编号或绕过查找。
- never()：指定匹配的调用次数为零，只约束这里的保存方法，不禁止合法的查找调用。
- any()：参数匹配器，即指定检查覆盖哪些参数的工具。这里覆盖任何参数，包括 null；若误调用 savePetType(null)，也在禁止范围。any(PetType.class) 不匹配 null，所以此处选择 any()。
- 响应检查与调用检查分工：404 验证返回结果，never 验证没有保存调用；两者一起检查。

## 完整新增测试

依赖现有测试类初始化：clinicService 为 MockitoBean 服务替身，mockMvc 使用已配置的控制器与异常处理器，WithMockUser 提供测试用户。无需真实数据库中存在或不存在编号 999。

新增导入 import static org.mockito.Mockito.never;。given、verify、any、put、status、MediaType、Test、WithMockUser 均沿用已有导入；以下不是独立测试类。

```java
    @Test
    @WithMockUser(roles="VET_ADMIN")
    void testUpdatePetTypeNotFound() throws Exception {
        given(this.clinicService.findPetTypeById(999)).willReturn(null);

        this.mockMvc.perform(put("/api/pettypes/999")
                .content("{\"id\":999,\"name\":\"dog I\"}")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(this.clinicService).findPetTypeById(999);
        verify(this.clinicService, never()).savePetType(any());
    }
```

## 实际验证与验收（来源：Codex）

```powershell
./mvnw.cmd -o '-Dtest=PetTypeRestControllerV1Tests#testUpdatePetTypeSuccess+testUpdatePetTypeError+testUpdatePetTypeNotFound' test
```

- 2026-09-28 21:27 执行，Maven 退出状态 0，BUILD SUCCESS。-o 使用现有本地依赖，未跳过检查。
- 3 项执行，失败 0、错误 0、跳过 0；已核验上述三个方法均在本次报告中。报告修改时间为 21:27:59.3359512（+08:00），晚于本次启动；准确起止时间见 run.json。
- 原始证据：D:\derek\maven\_cache\learning-evidence\step24-update-not-found-20260928-212734，含 test.log、run.json 及本次 XML 报告。
- 新测试实际验证 404、findPetTypeById(999) 一次、savePetType 零次。此前成功更新及非法名称更新两个案例本轮也通过。
- 日志显示启动连接 jdbc:hsqldb:mem:petclinic 内存数据库，但请求的服务是替身；未证明真实数据库不存在编号 999，也未验证真实服务的查找链路或写入。
- 实现与三个定向验证完成；未跑全量测试或独立应用，未运行先保存再返回 404、替换 verifyNoInteractions 等假设故障版本。差异检查通过。

## 官方资料与面试问答

- [Mockito 官方文档](https://site.mockito.org/javadoc/current/org/mockito/Mockito.html)：verify 默认一次，never 等价于 times(0)。
- [ArgumentMatchers 官方文档](https://site.mockito.org/javadoc/current/org/mockito/ArgumentMatchers.html)：any() 与 any(Class) 的空值匹配区别。
- 以上页面标为旧版，引用基础方法语义，不作为项目实际版本或升级建议。

1. 为什么请求名称不能用空字符串？会先触发字段错误，无法针对查找后不存在的分支验证 404。
2. 只检查 404 足够吗？不足以确认没有误调用保存，因此另检查保存零次。
3. 为什么显式检查 findPetTypeById(999)？确保实际查找了目标编号，而不是仅因其他未设置的替身调用默认返回 null 就得到 404。

## 个人能力与唯一练习

- 学习者尚未提供本步回答，个人理解、测试编写和排障能力待验证；Codex 实现与执行不自动转为学习者能力。
- 练习：若把最后一行 verify(this.clinicService, never()).savePetType(any()) 换成 verifyNoInteractions(this.clinicService)，当前正确实现的测试还能通过吗？请指出决定结果的服务方法调用。只分析，不实际修改测试。
- 下一步：收到回答后追加原回答和复核。本轮停在第二十四步练习，不自动提交或继续下一步。

## 练习回答与第二十四步收尾（2026-09-28）

### 学习者原回答（来源：本轮聊天）

> 不能。findPetTypeById方法正常调用了一次

### 复核与验收

- 回答正确：学习者准确判断替换为 verifyNoInteractions 后不能通过，并定位实际已发生的 findPetTypeById 调用。
- Codex 补充：本例具体为 findPetTypeById(999) 一次；never 只要求指定的保存调用为零，verifyNoInteractions 则要求整个服务替身没有任何方法调用。即使前一行 verify 已验证查找调用，也不会消除调用历史。此补充不记作学习者原回答。
- 第二十四步实现、三项定向验证与基础练习复核完成。已有完整代码和讲解后区分本题检查范围的回答证据；独立测试编写、调用验证的其他场景及真实排障仍待验证。
- 替换检查的版本未实际执行，本题结论是源码与方法语义推断，不是实际失败运行证据。

### 本轮实际操作（来源：Codex）

- 已读取规则与记录并核对现场：当前分支 codex-step-24-update-pettype-not-found，HEAD 和本地 origin/dev 同为 57bd4183b865dd7e2961947e862943a216bc4d8d，祖先检查退出状态 0。
- 已有修改为一个测试文件及两份学习文档，本轮保留现场，仅增量更新两份文档；未 fetch、重新查询远端、改代码、重跑测试、提交或推送。
- 三项测试通过仍为本日 21:27 的执行记录，不冒记为本轮再次运行。

### 知识点、官方资料与面试问答

1. never 与 verifyNoInteractions 的范围有什么区别？前者检查指定方法及匹配参数的调用为零，后者检查整个替身无任何方法调用。
2. 查找不存在的目标是否意味着服务无调用？不是，本例已经调用查找方法，只是结果为 null。
3. 用 verify 验证过查找调用后，verifyNoInteractions 能通过吗？不能，验证不会清除已经发生的调用。

官方资料：[Mockito 文档](https://site.mockito.org/javadoc/current/org/mockito/Mockito.html)、[ArgumentMatchers 文档](https://site.mockito.org/javadoc/current/org/mockito/ArgumentMatchers.html)。用于调用验证与匹配器基础语义，不作为项目版本证据。

### 下一步建议（未执行）

后续独立步骤可增强更新请求字段非法的测试，验证返回 400 且查找、保存服务均未调用，对比本步查找后返回 404 的流程。本轮停在第二十四步收尾，等待用户安排，不自动提交或进入下一步。
