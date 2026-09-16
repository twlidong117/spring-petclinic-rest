# 第十步：追踪宠物类型读取请求

## 日期、目标与基准

- 日期：2026-09-16（Asia/Shanghai）。
- 目标：沿第九步已验证的 `GET /petclinic/api/pettypes`，理解请求入口、服务、数据访问与响应转换；本步只阅读这一条链路。
- Codex 核验：PR #9 已合并至 `dev`，合并提交为 `238ae93d70ae9ab426228e890ff2f4d23292d0e9`；获取远程 `dev` 后确认提交关系，从该基准创建 `codex-step-10-pettype-read-flow`，起始工作区干净。
- 未修改业务代码、依赖或运行配置；本步未执行构建、测试、启动或网络业务请求。

## 一、请求如何找到方法

完整路径由三部分组成：

- `/petclinic`：应用配置的访问前缀。
- `/api`：`PetTypeRestControllerV1` 上的 `@RequestMapping("api")`。
- `/pettypes`：生成接口 `PettypesApi` 中 `listPetTypes()` 对应的 GET 路径。

控制器实现 `PettypesApi` 并覆盖 `listPetTypes()`；正常调用进入控制器的实现，不是使用生成接口中的示例响应。

源码入口：`src/main/resources/openapi.yml`、`src/main/java/org/springframework/samples/petclinic/rest/controller/v1/PetTypeRestControllerV1.java`。本机已存在的生成接口位于 `target/generated-sources/openapi/src/main/java/org/springframework/samples/petclinic/rest/api/PettypesApi.java`，本步只读取，没有重新生成。

## 二、请求与数据的流向

```text
GET /petclinic/api/pettypes
  → PetTypeRestControllerV1.listPetTypes()
  → ClinicServiceImpl.findAllPetTypes()
  → PetTypeRepository.findAll()
  → Spring Data 提供的数据访问实现 → types 表
  ← PetType 集合 ← 服务原样返回
  → 控制器判断是否为空
      空：404，无业务响应体
      非空：PetTypeMapper.toPetTypeDtos(...) → PetTypeDto 列表
  → ResponseEntity（响应状态和响应体）
  → Spring 的消息转换器 → JSON 响应
```

这里的箭头是根据源码和框架机制整理的调用关系，不是本步采集的运行时调用轨迹。

### 控制器：接收请求并选择响应

控制器（Controller）是处理外部请求、组织响应的类。核心实现：

```java
List<PetType> petTypes = new ArrayList<>(this.clinicService.findAllPetTypes());
if (petTypes.isEmpty()) {
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
}
return new ResponseEntity<>(petTypeMapper.toPetTypeDtos(petTypes), HttpStatus.OK);
```

`new ArrayList<>(...)` 把返回的集合复制为列表，不创建数据库记录。当前项目约定空集合返回 404，非空返回 200；不能推广为所有列表接口都应如此设计。

### 服务：本方法只委托查询

服务（Service）承接业务处理。本例 `ClinicServiceImpl.findAllPetTypes()` 的方法体只有 `return petTypeRepository.findAll();`，没有额外过滤或排序。方法标注 `@Transactional(readOnly = true)`，表达只读事务意图；本步不展开事务机制，也不把它当成绝对禁止写入的保证。

### 数据访问：接口不等于没有实现

数据访问接口（Repository）描述如何读取或保存数据。`PetTypeRepository` 声明 `findAll()`；`spring-data-jpa` 配置下，`SpringDataPetTypeRepository` 同时继承该接口和 Spring Data 的 `Repository`。

Spring Data JPA 是基于 Java 对象与数据库映射机制提供数据访问能力的框架。本例标准 `findAll()` 由框架提供实现，不必在项目里手写同名方法。`SpringDataPetTypeRepositoryImpl` 的自定义内容是删除操作，不是本次列表读取的实现。

实体（Entity）是映射数据库记录的 Java 对象。`PetType` 的 `@Table(name="types")` 指向 `types` 表；`name` 来自 `NamedEntity`，`id` 来自 `BaseEntity`。本步没有采集实际数据库查询语句，也不根据第九步的返回顺序保证未来查询顺序。

### 转换：实体、对外对象和文本各有用途

数据传输对象（DTO）用于表达接口对外传输的数据，本例为 `PetTypeDto`。`PetTypeMapper` 把 `PetType` 转成 `PetTypeDto`；本机已有的 MapStruct 生成实现逐项复制 `name`、`id`。

MapStruct 是在编译期间生成对象转换代码的工具。本步读取已有的 `target/generated-sources/annotations/.../PetTypeMapperImpl.java`，未重新编译生成它。

JSON 是用字段和值表示数据的文本格式，例如 `{"id":1,"name":"cat"}`。Mapper 只转换 Java 对象，不查询数据库，也不把对象写成 JSON 文本。控制器使用 `ResponseEntity` 表达状态和响应体，之后 Spring 的消息转换器负责写出响应文本。

## 三、证据与验收边界

- **Codex 本步实际验证**：远程合并状态、Git 提交关系、上述源码与已有生成源码；阅读控制器测试中非空返回 200、空集合返回 404 的断言。本步没有执行这些测试。
- **历史运行证据**：第九步实际启动应用，使用 `h2,spring-data-jpa` 配置，请求返回 200 和六条宠物类型数据，与初始化数据一致。详细证据保留在 `09-first-start-and-request.md`，不记为本步再次运行。
- **基于源码与官方机制的解释**：数据访问实现读取映射实体，服务返回集合，控制器决定状态并转换响应。本步没有逐方法调试或采集数据库查询轨迹。
- **学习者证据**：第九步已收到的回答保持原结论；尚未收到第十步调用链练习回答。
- **验收结论**：调用链阅读与记录完成；学习者独立解释数据来源、各层职责和分支结果的能力待验证。

## 四、官方文档

- [Spring Data：定义数据访问接口](https://docs.spring.io/spring-data/commons/reference/repositories/definition.html)：标准方法可转交框架提供的基础实现。
- [Spring：ResponseEntity](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)：状态、响应头、响应体以及消息转换器。
- [MapStruct 官方参考](https://mapstruct.org/documentation/stable/reference/html/)：编译期间生成对象映射实现。

## 五、面试题与答案

1. **控制器、服务、数据访问接口分别做什么？** 控制器处理请求与响应，服务承接业务处理，数据访问接口提供读取或保存数据的入口。本例服务只委托查询，不能虚构额外业务逻辑。
2. **为什么找不到手写的 `findAll()` 实现，仍然可以查询？** 当前使用 Spring Data，它为标准数据访问方法提供实现；接口声明的能力由框架在运行时接入。
3. **Mapper 是否查询数据库或生成 JSON？** 都不是。本例 Mapper 复制实体的字段到对外数据对象，查询由数据访问层承担，响应文本由 Spring 的消息转换器写出。
4. **控制器测试返回了 dog、snake，能否证明数据库存在这些记录？** 不能。这里的测试预先设置了服务替身的返回值，验证控制器响应行为，不是实际数据库记录的证据。本步也没有重新执行测试。

## 六、当次练习与下一步

请用自己的话说明：假设 `petTypeRepository.findAll()` 正常返回包含 `id=7、name=fish` 的一条记录，数据经过哪些方法变成响应？由谁决定状态码、谁复制字段、谁生成响应文本？如果返回空集合，流程在哪一处改变？

当前只等待本练习的回答并增量记录，不自动进入其他接口、代码修改或故障排查步骤。

## 2026-09-16 练习回答与纠正（增量证据）

- 来源：学习者本轮对话回答。原文为：“findallpettypets方法和listpettypes方法转成响应输出。controller层决定状态码，mapper层复制字段，service层生成响应文本。在controller层处理”。
- 已有回答证据：正确指出控制器决定状态码、Mapper 复制字段、空集合在控制器处理。
- 尚不完整：提及的方法对应 `findAllPetTypes()` 和 `listPetTypes()`，但未完整说明数据访问、对象转换与响应文本生成的顺序，也未明确空集合的具体状态码。
- 需纠正：服务层不生成响应文本；本例服务返回 `Collection<PetType>`。控制器调用 Mapper 获得 `List<PetTypeDto>` 并构造 `ResponseEntity`；Spring 的消息转换器随后把响应体对象写成 JSON 文本。
- 当前结论：部分职责区分已有回答证据；对象集合与响应文本的区别仍待学习者纠正后复核，不能仅凭讲解标记掌握。本轮只更新学习记录，未执行测试或启动。
- 当次追问：`findAllPetTypes()` 返回的是 Java 对象集合还是 JSON 文本？谁负责最终转换成 JSON？空集合时控制器返回什么状态码？

## 2026-09-16 纠正后复核与本步收尾（增量证据）

- 来源：学习者对三项追问的回答：“1，返回java对象集合。2，Spring消息转换器负责序列化，输出json文本。3，not found 404”。
- 复核结果：三项均正确。服务返回对象集合、消息转换器序列化为响应文本、当前控制器空集合返回 404 的概念区分已有纠正后回答证据。
- 与此前回答合并判断：控制器决定状态码、Mapper 复制字段的职责区分也已有回答证据。保留首次将响应文本生成归于服务的原始记录，不改写成当时已正确理解。
- 验收结论：第十步源码阅读与基础概念问答收尾完成。纠正后回答不等于独立完整追踪能力；未借助提示复述整条调用链、独立定位源码、调试和编写测试仍待验证。
- 本轮操作仅为学习文档增量更新，未重新运行测试或应用，未提交、推送或创建合并请求。
- 下一独立学习步骤建议：阅读本接口现有控制器测试，理解服务替身、模拟请求和响应断言分别验证什么，再由学习者解释为什么测试通过不证明真实数据库有对应记录。本次不执行该步骤。
