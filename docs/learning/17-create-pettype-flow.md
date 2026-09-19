# 第十七步：新增宠物类型的请求、校验与响应

## 日期、基准与范围

- 日期：2026-09-19（Asia/Shanghai）。
- PR #16 已合并，获取 origin/dev 并核验祖先关系，从提交 `d2410c24ed73ecffc3fe041058904c7c4f471c0c` 创建 `codex-step-17-create-pettype-flow`，起始工作区干净。
- 本步只阅读新增接口及现有测试，更新学习文档；未修改业务代码、测试、依赖或运行配置，未构建、运行测试或发出新增请求。
- Codex 阅读了控制器、服务、Mapper、编号生成配置、接口描述及已有生成接口和数据传输对象；读取已有生成文件不代表本步重新生成或构建成功。

## 一、请求体与请求对象

目标接口为 `POST /petclinic/api/pettypes`。POST 在本接口用于创建资源。请求体是请求携带的数据内容，例如：

```json
{"name":"fish"}
```

`src/main/resources/openapi.yml` 中的 PetTypeFields 定义可编辑字段 name。已有生成接口 PettypesApi 的新增方法参数为：

```java
@Valid @RequestBody PetTypeFieldsDto petTypeFieldsDto
```

`@RequestBody` 指示 Spring 通过消息转换器把请求文本转换为 Java 对象，这一过程叫反序列化；`@Valid` 触发对象字段校验。它们声明在控制器所实现的生成接口中。

新增使用的数据传输对象 PetTypeFieldsDto 只有 name，响应使用 PetTypeDto，包括 name 与 id。不能将请求对象与实体、响应对象混为一谈。

## 二、name 字段约束

生成请求对象的 name getter 上标注：

```java
@NotNull
@Size(min = 1, max = 80)
```

- `@NotNull`：值不能是 null。
- `@Size`：这里检查字符串长度为 1 至 80，包含边界；单独使用 Size 不拒绝 null，因此两者职责不同。
- 按这两项约束推断：null 不满足 NotNull；空字符串不满足最小长度；81 个普通字符超过最大长度；一个普通空格长度为 1，不会仅因这两项约束被拒绝。不能擅自说已有“禁止纯空白”规则。
- 以上为源码和约束语义分析，本步没有实际提交这些输入来验证响应。

对于请求对象字段校验失败，Spring 的请求参数处理会在正常进入控制器方法体前拒绝请求；本项目 `ExceptionControllerAdvice.handleMethodArgumentNotValidException()` 构造 400 错误响应。这是按现有源码与框架机制解释的流程，本步未采集新的运行证据。

## 三、通过校验后的写入流程

```text
JSON 请求体
  → Spring 反序列化为 PetTypeFieldsDto 并校验
  → PetTypeRestControllerV1.addPetType(...)
  → PetTypeMapper.toPetType(...) 创建实体并复制 name
  → ClinicServiceImpl.savePetType(type)
  → PetTypeRepository.save(type)
  → 控制器构造 Location，Mapper 转成 PetTypeDto
  → 返回 201 和响应对象，由消息转换器生成文本
```

Mapper 的请求对象转换方法标注 `@Mapping(target = "id", ignore = true)`，已有生成实现创建 PetType 并设置 name，不生成编号、不写数据库。服务的 savePetType 方法调用 Repository.save，保存职责属于数据访问流程。

实体基类 BaseEntity 使用 `@GeneratedValue(strategy = GenerationType.IDENTITY)`，声明编号由持久化保存时的数据库身份列机制生成。此为配置事实，本步没有实际插入来验证编号或事务行为。

控制器在保存调用正常返回后构造 `201 Created`，表示创建成功的响应状态。Location 是响应头，用于提供新资源地址。本项目代码按实体编号构造 `/api/pettypes/{id}`；若保存后编号为 7，代码会构造 `/api/pettypes/7`。这是基于编号假设的代码示例，并非已创建的真实记录。

源码使用以 /api 开始的路径，没有自动拼接应用前缀 /petclinic；本步不宣称该 Location 在实际部署下已验证可访问，也不修改这一设计。

## 四、现有测试与尚未证明的事

`PetTypeRestControllerV1Tests.testCreatePetTypeSuccess()` 通过真实 Mapper 构造请求内容，模拟 POST，只有 `status().isCreated()` 断言。ClinicService 是替身，未设置真实保存或生成编号，因此不能由这条断言证明真实数据库插入、编号生成、响应字段及 Location 正确。源码中也未显式验证 savePetType 的调用参数。

`testCreatePetTypeError()` 将 name 设为 null，使用 PetTypeDto 序列化构造请求，期望 400；新增接口实际接收 PetTypeFieldsDto。它只检查状态码，没有检查具体字段错误详情或服务未被调用，不能由状态码断言单独证明失败的精确来源。

以上均为现有测试源码阅读，本步没有执行测试。此前四个按编号查询测试不覆盖本新增请求，不能当成本步新增接口运行证据。

## 五、验收、官方文档和面试题

源码阅读与记录完成；个人理解待练习回答。没有实际创建资源、校验数据库写入或重跑测试；未提交或推送。

- [Spring：RequestBody 与 Valid](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)
- [Jakarta Validation：NotNull](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/notnull)
- [Jakarta Validation：Size](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/size)

1. **RequestBody 和 Valid 各做什么？** 前者绑定请求体并触发反序列化，后者触发对象约束校验；请求解析成功不等于字段一定合规。
2. **Mapper 会生成编号并写入数据库吗？** 本例不会，它创建实体并复制 name；实际保存和编号生成属于持久化流程。
3. **控制器替身测试得到 201 能证明数据库插入成功吗？** 不能。服务被替换，状态断言只验证测试环境下的响应状态，没有证明真实数据库写入。

## 六、当次练习与唯一下一步

请说明：（1）收到 `{"name":"fish"}` 后，谁把 JSON 转为 Java 对象，谁把请求对象转换为实体，谁负责实际保存？（2）name 为 null 时，按当前字段约束应被哪条规则拒绝，正常情况下是否应进入保存逻辑？（3）为什么现有成功测试只检查 201 还不够证明数据库新增成功？

当前等待学习者回答并增量记录，不自动执行新增请求或进入测试修改步骤。

## 七、首次练习回答与复核（2026-09-19）

### 学习者提供的原回答

> 1、Spring把输入json字符串反序列化为Java对象，mapper把dto转为实体，服务层调用Repository进行实际数据库保存落表。2、@NonNull约束。不应进入保存逻辑。3、测试使用mock类，没有连接真实数据库

### 复核与知识点

1. 第一题正确：Spring 通过消息转换器完成反序列化；Mapper 把数据传输对象转为实体；真实服务调用 Repository 完成保存流程。基本职责已有回答证据，不证明独立实现或实际执行过数据库写入。
2. 第二题流程判断正确，注解应为 `@NotNull`，不是 `@NonNull`。已有生成 PetTypeFieldsDto 的 name getter 标注 `@NotNull @Size(min = 1, max = 80)`。按源码与框架校验机制分析，null 应在正常进入控制器方法体前被拒绝，不进入保存逻辑；本轮没有实际发送请求。
3. 第三题识别了 mock（测试替身，即测试中替代真实组件的对象），但“没有连接真实数据库”不能仅凭服务替身推得。源码明确 `@MockitoBean` 替换 ClinicService；本请求没有执行真实服务到 Repository 的保存链路。测试启动是否连接数据库是另一件事；即使连接了数据库，仅检查 201 也没有验证新增记录。

### 证据与验收

- Codex 实际验证：读取学习记录、已有生成字段注解与测试源码，核对官方文档。本轮只修改学习文档，未构建、运行测试、启动应用或实际新增，未提交或推送。
- 接手时实际确认：当前分支 codex-step-17-create-pettype-flow，HEAD 为 d2410c24ed73ecffc3fe041058904c7c4f471c0c，本地 dev 是其祖先；本地 dev 指针不等于 HEAD。本轮未更新远端。
- 学习者证据：原回答能说明转换与保存的基本分工，以及校验失败不应保存。
- 验收结论：首次回答复核完成；准确注解名称与测试证据边界仍待纠正后复述。独立源码定位、测试编写、真实写入及排障能力仍待验证，不能因助手解释而标记掌握。

### 官方文档与面试问答

- [Jakarta Validation：NotNull](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/notnull)
- [Spring：MockitoBean 与 MockitoSpyBean](https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-mockitobean.html)
- 请求体转换资料沿用第五节的 Spring RequestBody 链接。

1. **谁转换请求体，谁转换实体？** Spring 消息转换器完成请求体到 Java 对象的转换，本例 Mapper 完成请求对象到实体的转换。
2. **本例哪个注解拒绝 null，失败后应否保存？** `@NotNull`；正常校验流程拒绝请求，不进入保存逻辑。
3. **测试启动时连接了数据库，201 就能证明新增成功吗？** 不能。本例服务被替换，本请求未执行真实保存链路，状态断言也没有验证数据库记录。

### 当前唯一下一步

请学习者补充一句话：本例拒绝 null 的准确注解是什么；即使测试启动时连接了数据库，为什么现有成功测试仍不能证明新增落表？收到回答后再增量复核，不自动进入下一实践步骤。

## 八、纠正后回答与第十七步收尾（2026-09-19）

### 学习者提供的原回答

> 注解是NotNull，本例中ClicnicService被mock替身替换，所以请求没有执行真实保存链路。

### 复核、知识点与验收结论

- 回答正确：准确注解为 `@NotNull`；本请求调用服务替身，没有执行真实保存链路，因此 201 状态断言不能证明新增落表。代码名称拼写为 `ClinicService`，原回答中的 ClicnicService 按原样保留，不因此否定概念理解。
- 第十七步源码阅读与基础问答复核完成。结合首次回答，学习者已说明反序列化、对象映射、保存的分工，以及 null 校验失败不应保存；本次为讲解纠正后的复述证据，不升级为无提示独立掌握。
- 学习者提供的证据：本节原回答。独立定位源码、编写测试、验证真实数据库写入及排障能力仍待验证。
- Codex 本轮实际验证：读取最新学习记录与工作区状态，确认仍在 codex-step-17-create-pettype-flow，HEAD 为 d2410c24ed73ecffc3fe041058904c7c4f471c0c，且本地 dev 是其祖先；仅增量更新两份学习文档并检查改动。
- 本轮没有构建、运行测试、启动应用、执行新增请求或修改业务代码、测试、依赖、运行配置；没有提交或推送。历史源码阅读和测试证据仍保留原日期与范围。

### 官方文档与面试问答

- [Jakarta Validation：NotNull](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/notnull)
- [Spring：MockitoBean 与 MockitoSpyBean](https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-mockitobean.html)
- [Spring：RequestBody 与 Valid](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)

1. **请求内容到实体经过哪些转换？** Spring 消息转换器先将请求内容反序列化为请求对象，Mapper 再将请求对象转换为实体。
2. **name 为 null 时由谁拒绝，是否保存？** `@NotNull` 声明非 null 约束，在正常请求校验流程中失败，不进入保存逻辑。
3. **为什么本例 201 不证明数据库新增？** ClinicService 被替身替换，本请求不执行真实保存链路；状态断言也没有验证数据库记录。

### 下一步建议（未执行）

后续独立步骤可针对现有新增成功测试补充保存参数、响应字段和 Location 的验证，并继续区分控制器测试与真实数据库写入证据。本轮停在第十七步收尾，等待用户安排，不自动修改测试、提交或进入下一步。
