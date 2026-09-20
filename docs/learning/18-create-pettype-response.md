# 第十八步：新增成功测试的保存参数与响应验证

## 日期、基准与范围

- 日期：2026-09-20（Asia/Shanghai）。GitHub 核验 PR #17 为 MERGED，目标 dev；获取 origin/dev 后确认包含合并提交 27f1ee4cb9f898c02a44f716751e620a36d2138b，并从该提交创建 codex-step-18-create-pettype-response。起始工作区干净。
- 当前 Java 21.0.12.1、Maven Wrapper 使用 Maven 3.9.16，MAVEN_USER_HOME 为 D:\derek\maven\_cache。沿用用户环境配置，未安装或升级环境、依赖。
- 本步只增强 PetTypeRestControllerV1Tests.testCreatePetTypeSuccess，并定向执行它与原有 testCreatePetTypeError；不修改业务代码、运行配置和失败测试，不提交或推送。

## 一、为什么增强原测试

原测试仅检查 201 状态，无法发现返回名称或编号错误、Location 错误等问题。本步使用固定请求内容 {"name":"fish"}，直接表达接口输入；由真实 Spring 请求处理和 Mapper 完成对象转换。

断言是自动检查实际结果是否符合预期；任一必要断言失败，都不能认定测试通过。本次检查：

| 检查位置 | 预期 | 能证明的范围 |
| --- | --- | --- |
| 服务替身刚收到实体时 | name 为 fish，id 为 null | 请求转换后的实体字段符合本例要求 |
| 服务替身行为 | 将该实体 id 设为 7 | 仅模拟保存后拥有编号，不是数据库生成编号 |
| 响应 | 201，内容类型为 application/json | 本次模拟请求的状态与内容类型 |
| 响应正文 | id 为 7，name 为 fish | 控制器和真实 Mapper 使用保存模拟后的实体构造响应 |
| Location 响应头 | /api/pettypes/7 | 当前代码产生了预期地址文本，不证明该地址在外部部署下可访问 |
| 服务调用验证 | savePetType 恰好调用一次 | 本请求确实将实体交给服务替身一次，不证明真实落表 |

## 二、新写法说明

- Mockito 是创建和检查测试替身的工具。`doAnswer` 为替身的方法设置自定义行为：替身方法被调用时才执行大括号内的代码。
- `invocation -> { ... }` 是 Java 的匿名函数写法；这里 invocation 表示这一次方法调用。`invocation.getArgument(0)` 取第一个实参，即控制器交给服务的 PetType 实体。
- 先用 `assertEquals` 检查名称、用 `assertNull` 检查初始编号，再调用 `setId(7)`。检查必须在赋值前执行，才能验证服务收到实体时编号为空。
- `any(PetType.class)` 匹配非 null 的 PetType 参数；字段是否正确由回调内的断言检查。
- `savePetType` 是没有返回值的方法，回调的 `return null` 满足回调形式要求，并不代表保存返回了 null 实体。
- `verify(clinicService).savePetType(...)` 默认检查恰好一次调用。即使预设了替身行为，也仍须检查调用实际发生。
- `jsonPath("$.id")` 表示从响应正文根对象读取 id 字段；Location 是提供资源地址的响应头。

## 三、实际执行与证据

Codex 执行以下 Maven Wrapper 命令；`-o` 表示离线使用已有缓存，`-Dtest` 选择两个测试方法，没有跳过检查：

```powershell
./mvnw.cmd -o '-Dtest=PetTypeRestControllerV1Tests#testCreatePetTypeSuccess+testCreatePetTypeError' test
```

- 执行时间：2026-09-20 07:23:04 至 07:23:26（Asia/Shanghai）。
- 结果：Tests run 2、Failures 0、Errors 0、Skipped 0，BUILD SUCCESS，Maven 自身退出状态 0。
- 核验报告只含 testCreatePetTypeSuccess、testCreatePetTypeError；报告最后修改时间 07:23:26，晚于本次开始时间。没有将其他旧报告计入本次结果。
- 证据目录：D:\derek\maven\_cache\learning-evidence\step18-create-pettype-20260920-072304，保存 test.log、run.json 和本测试类的原始报告。
- 日志实际显示测试上下文连接 jdbc:hsqldb:mem:petclinic 内存数据库，但本新增请求的 ClinicService 是替身，没有执行真实服务保存链路。不能将“服务替身”表述为“整个测试没有连接数据库”。
- 原有失败测试本轮返回 400；日志记录 name 的 NotNull 校验失败。该测试仍只断言状态，本步未增强失败用例的字段错误或服务未调用检查。
- 日志中的框架警告不等于测试失败；以上结果由退出状态、测试统计、报告方法名及时间共同确认。

## 四、验收与能力边界

- Codex 实际验证：增强后的成功测试和原有失败测试均通过；新增保存前参数、响应字段、Location 和调用次数检查已实际执行。
- 本步尚未收到学习者练习回答，个人能力为待验证。代码实现、执行和报告核验均由 Codex 完成，不证明学习者能独立编写或排障。
- 未验证：真实新增落表、数据库生成编号、事务提交、外部网络访问及 Location 在 /petclinic 部署前缀下的可访问性。未运行全量测试，未单独启动应用。
- 若把替身编号改为 8、仍期待响应编号 7，按代码推断测试应失败；本轮未实际执行这一修改版本，不记为已观察到的故障。

## 五、官方资料与面试题

- [Spring：测试响应状态、响应头和内容](https://docs.spring.io/spring-framework/reference/testing/mockmvc/hamcrest/expectations.html)
- [Mockito：替身行为与调用验证](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html)

1. **为什么替身要设置编号？** 控制器保存调用后读取同一个实体编号构造响应；设置 7 是为测试提供受控的保存后状态，不是真实编号生成。
2. **检查 201 之外还应检查什么？** 本例检查保存前实体字段、保存调用次数、响应编号和名称，以及 Location，避免状态正确但数据或地址错误。
3. **verify 保存方法调用一次能证明落表吗？** 不能，它只验证服务替身被调用一次；真实保存流程被替换，数据库新增仍需另外的验证。

## 六、当次练习与唯一下一步

请用自己的话回答：

1. 本测试的编号 7 是谁设置的？为什么不能据此证明数据库自动生成了编号？
2. 如果仅把 `petType.setId(7)` 改成 `petType.setId(8)`，保留所有响应断言，哪条响应断言会最先失败，为什么？请按测试中的断言顺序分析。
3. `verify(clinicService).savePetType(...)` 与检查响应中的 id，各自检查什么？

当前停在第十八步练习，收到学习者回答后增量复核，不自动开始下一步或提交。

## 七、首次回答复核与手机阅读适配（2026-09-20）

### 学习者提供

> 编号7来自服务mock，没有连接实际数据库。doAnwser检查7失败。type名称和type ID。在ChatGPT iOS客户端查看java代码非常困难，找一个其他方案。

### 复核与验收

1. 编号来自服务替身的判断正确，准确说是测试通过 doAnswer 中的 setId(7) 设置。“没有连接实际数据库”需纠正：本步日志已证明测试启动连接内存数据库；本请求没有执行真实服务保存链路，不能证明真实新增。
2. 方法名为 doAnswer。它在本例设置替身调用行为，内部先检查 name 为 fish、id 为 null，再设置编号，没有检查“编号必须为 7”。仅把设置值改为 8，按源码顺序推断最先失败的是 Location 响应头断言：实际 /api/pettypes/8，预期 /api/pettypes/7。后面的正文 id 检查在该失败后不再执行。本轮没有实际运行改为 8 的版本。
3. “type名称和type ID”没有准确区分两项职责：verify(...).savePetType(any(PetType.class)) 检查保存方法对匹配参数恰好调用一次；正文 id 断言检查返回给调用者的数据编号为 7。保存前的名称和空编号由 doAnswer 内的另外两条断言检查。

Codex 本轮实际核对现有测试顺序、工作区及学习记录，仅增量更新文档，未改测试或业务代码，未重跑、提交或推送。编号来源已有回答证据，数据库边界、断言顺序和调用验证职责仍待纠正后复核，独立编写及排障仍待验证。

### 手机阅读方式与当前唯一练习

用户明确表示手机查看 Java 文件困难。后续练习在聊天中直接提供所需几行代码或中文执行顺序，一次聚焦一个问题，不要求先打开仓库文件；不因此将代码阅读障碍当成已证明的能力不足。

本轮手机练习：假设替身设置编号 8，响应检查顺序为“状态 201 → 内容类型 → 地址必须 /api/pettypes/7 → 正文编号必须 7”。请说明最先在哪一项失败、实际地址是什么。先完成这一题，再复核其他待确认概念。

### 面试问答与官方资料

1. **doAnswer 是否等于检查编号为 7？** 不是；它设置替身行为，里面可以包含断言，也可以修改对象，本例 setId(7) 是赋值。
2. **连续 andExpect 中一项失败后会怎样？** 本例停止后续检查；地址断言排在正文编号之前。[Spring 官方说明](https://docs.spring.io/spring-framework/reference/testing/mockmvc/hamcrest/expectations.html)
3. **verify 和响应断言有什么区别？** 前者检查组件调用，本例默认一次；后者检查返回的数据。[Mockito 官方文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html)

## 八、查看完整方法后的顺序判断复核（2026-09-20）

- 学习者先要求将测试用例完整代码贴入聊天，Codex 已展示完整成功测试方法、相关导入及中文注释；后续继续在聊天直接提供必要代码，不依赖仓库访问。
- 学习者原回答：`4，实际值是8`。
- 复核：正文 id 为 8 的判断正确，但最先失败的是第③项 Location。控制器根据编号构造实际地址 /api/pettypes/8，而第③项仍期待 /api/pettypes/7；因此在到达第④项正文编号断言前就已失败。
- 核心概念：值会不匹配与某条断言最先被执行并失败是两件事。连续 andExpect 按顺序执行，前一项失败会阻止后续断言执行，末尾 verify 也不会到达。
- 证据边界：Codex 本轮读取源码核对顺序，仅追加学习文档，没有实际运行编号改为 8 的版本；上述失败位置仍为代码推断。原测试的通过证据不变。未修改代码、提交或推送。
- 能力状态：编号变化已有回答证据；断言执行顺序仍待纠正后验证，其他待复核项保留，不标记第十八步问答完成。
- 当前唯一练习：在编号设为 8 的假设下，如果同时把第③项地址预期改为 /api/pettypes/8，但第④项仍期待正文 id 为 7，那么最先失败的是哪项？预期值与实际值分别是什么？只需在聊天回答。

### 面试问答与官方资料

1. **为什么原假设先失败在地址检查？** 地址断言先执行，实际 /api/pettypes/8 与预期 /api/pettypes/7 不同。
2. **正文编号是否仍为 8？** 是；只是正文编号断言尚未执行就因前项失败退出。
3. **前面的 andExpect 失败后，末尾 verify 会执行吗？** 本例不会，失败中断了该测试方法的后续正常执行。

官方资料：[Spring 连续响应断言的执行行为](https://docs.spring.io/spring-framework/reference/testing/mockmvc/hamcrest/expectations.html)。

## 九、断言顺序纠正后回答（2026-09-20）

### 学习者原回答

> 明白了。这个条件下最先失败的是4，资源地址已经是8，测试用例修改了资源地址也是8就不会失败了

### 复核与证据边界

- 回答正确：在替身编号与 Location 预期均改为 8 的假设下，第③项通过，第④项最先失败。学习者正确解释地址的实际值与预期一致；结合上一回答“实际值是8”，已有讲解后判断顺序的证据。Codex 补充第④项预期编号为 7、实际为 8，不将该补充冒记为本轮学习者原话。
- 此次为讲解后的概念复核，不证明独立编写测试或分析任意失败。原回答和纠正过程保留。
- Codex 本轮读取当前记录和工作区，仅更新学习文档；未修改或运行假设版本，未重跑已有测试、提交或推送。最初两项测试通过证据保留原范围。
- 尚待确认：verify 的调用验证职责与响应字段断言的区别；测试启动连接数据库与请求执行真实保存链路的区别。第十八步尚未全部收尾。

### 当前唯一练习（直接在聊天展示）

```java
verify(this.clinicService)
    .savePetType(any(PetType.class));
```

请说明这段代码检查的是“保存方法被调用的次数”，还是“返回的宠物类型名称和编号”？如果检查通过，能否证明数据库新增成功，为什么？此处 any(PetType.class) 表示匹配 PetType 类型的非 null 参数。

### 面试问答与官方资料

1. **地址实际值和预期都为 /api/pettypes/8 时，地址断言会失败吗？** 不会，两者一致。
2. **后续正文编号仍期待 7、实际为 8，会怎样？** 在前面检查通过的前提下，这条正文断言失败。
3. **响应字段正确就能代替保存调用验证吗？** 不能；响应断言检查返回内容，调用验证检查组件交互，两者关注不同事实。

官方资料沿用：[Spring 响应断言](https://docs.spring.io/spring-framework/reference/testing/mockmvc/hamcrest/expectations.html)、[Mockito 调用验证](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html)。

## 十、调用验证回答与第十八步收尾（2026-09-20）

### 学习者原回答

> 保存方法被调用的次数。不能证明，因为没有执行真实的保存流程

### 复核与验收

- 回答正确：学习者能区分调用次数验证与返回字段检查，并说明没有执行真实保存流程，所以不能证明数据库新增。Codex 补充本例 verify 默认要求恰好一次；学习者原话没有明确次数，不将补充记为独立回答。
- 结合此前回答，第十八步实现、定向验证与基础问答复核完成：编号来自服务替身、纠正后的断言顺序判断、调用验证职责与真实保存证据边界均已有基础回答证据。
- 保留首次“没有连接实际数据库”、doAnswer 误解、最先失败位置误判及纠正过程；本次为讲解和提示后的复核，不升级为无提示独立掌握。独立代码阅读、编写测试、排障与真实写入验证仍待验证。
- Codex 本轮核对最新学习记录和工作区，仅增量更新两份文档，未修改测试或业务代码、重跑测试、提交或推送。
- 测试证据仍为本日 07:23 的两项测试通过，失败、错误、跳过均为 0，Maven 退出状态 0；本轮不记作再次执行。假设编号改成 8 的版本从未实际运行。

### 知识点、官方资料与面试问答

1. **verify 默认检查什么？** 本例检查服务替身的 savePetType 对匹配参数恰好调用一次，不检查响应字段，也不证明真实落表。
2. **连续响应断言哪条先失败？** 按书写顺序，第一个实际值与预期不符的断言先失败；本例随后断言和末尾 verify 不再执行。
3. **测试连接数据库且返回 201，能否证明本次新增成功？** 不能；本请求服务是替身，编号由测试设置，真实保存流程没有执行。

官方资料：[Spring 响应断言](https://docs.spring.io/spring-framework/reference/testing/mockmvc/hamcrest/expectations.html)、[Mockito 调用验证](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html)。

### 下一步建议（未执行）

下一独立步骤可增强 name 为 null 的失败测试，检查具体字段错误和保存服务未被调用；继续在聊天直接提供完整相关测试方法及必要上下文，适配手机阅读。本轮停在第十八步收尾，不自动进入下一步或提交。
