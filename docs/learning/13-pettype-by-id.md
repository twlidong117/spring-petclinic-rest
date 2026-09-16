# 第十三步：按编号查询宠物类型

## 日期、基准与验收范围

- 日期：2026-09-16（Asia/Shanghai）。
- PR #12 已合并；本轮获取 origin/dev，并核验合并提交 `d533efea44661450b8ce968b3dcf4693ad0f7980` 的祖先关系，从该提交创建 `codex-step-13-pettype-by-id`，开始时工作区干净。
- 目标：阅读按编号查询的参数传递、成功与未找到分支，并与现有控制器测试对照。
- Codex 实际验证：读取接口描述、已有生成接口、控制器、服务与辅助方法、数据访问接口及控制器测试。仅更新学习文档，未修改业务或测试代码，未构建、运行测试或启动应用。
- 验收：源码阅读完成；学习者本步解释与场景判断待回答验证。第十二步三个列表测试的通过结果不能当作本步按编号测试的新执行证据。

## 一、路径中的编号如何进入方法

路径参数是地址路径中的可变部分。`/petclinic/api/pettypes/1` 对应 `/pettypes/{petTypeId}` 中的编号 1；`/petclinic` 为应用前缀，`/api` 为控制器路径。

`src/main/resources/openapi.yml` 描述该 GET 操作。已有生成接口 `target/generated-sources/openapi/src/main/java/org/springframework/samples/petclinic/rest/api/PettypesApi.java` 声明路径与参数：

```java
@PathVariable("petTypeId") Integer petTypeId
```

`@PathVariable` 将路径中的值交给方法参数；Spring 将文本形式的编号转换成 Integer 整数对象。控制器覆盖接口方法，因此进入 `PetTypeRestControllerV1.getPetType(...)`。本步仅阅读已有生成文件，没有重新生成，也没有验证非法编号、类型转换失败或最小值校验的实际响应。

## 二、编号传递与服务处理

```text
请求 /petclinic/api/pettypes/1
  → 路径参数 petTypeId = 1
  → getPetType(1)
  → clinicService.findPetTypeById(1)
  → petTypeRepository.findById(1)
  ← 单个 PetType 或未找到结果
```

服务实现为：

```java
return findEntityById(() -> petTypeRepository.findById(petTypeId));
```

`() -> ...` 是 Lambda 表达式，即可以传给其他方法的一小段行为；这里把查询操作交给 `findEntityById`。`Supplier<T>` 表示一个提供结果的操作，调用其 `get()` 才执行所提供的查询。本步只需要理解这段代码包装了查询，不扩展学习泛型或函数式编程。

辅助方法执行查询并直接返回结果；只捕获 `ObjectRetrievalFailureException` 和 `EmptyResultDataAccessException`，然后返回 `null`。`null` 表示没有对象。如果数据访问本身返回 `null`，该方法也会原样返回。不能推断所有数据库异常都会变成 `null` 或 404；其他异常不在这个捕获范围内。

这条数据访问接口声明为 `PetType findById(int id)`，返回单个实体对象，而非列表；本步不虚构具体数据库查询语句或运行轨迹。

## 三、谁决定 200 与 404

`PetTypeRestControllerV1.getPetType()`：

```java
PetType petType = this.clinicService.findPetTypeById(petTypeId);
if (petType == null) {
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
}
return new ResponseEntity<>(petTypeMapper.toPetTypeDto(petType), HttpStatus.OK);
```

- 非 null：Mapper 将实体转换为对外数据对象，控制器返回 200，由消息转换器写出单个 JSON 对象。
- null：控制器直接返回 404，此分支不调用 Mapper，也没有设置业务响应体。
- 数据访问层处理查询，服务返回或统一部分未找到结果，控制器决定这里的响应状态；数据库本身不向客户端发送该 404。

成功响应形状是对象，例如 `{"id":1,"name":"cat"}`，而列表读取返回数组。两者对应的字段断言分别是 `$.name` 和 `$.[0].name`。

## 四、现有测试的证据边界

`PetTypeRestControllerV1Tests` 中：

- 两个 `testGetPetTypeSuccessAs...` 方法预设服务查询 1 返回测试准备的 cat 对象，执行 `/api/pettypes/1` 并检查 200、内容类型、id=1、name=cat。
- `testGetPetTypeNotFound()` 预设服务查询 999 返回 null，执行 `/api/pettypes/999` 并检查 404。

这里服务为测试替身。因此未找到测试检查的是“控制器接到 null 后返回 404”，没有执行真实 `ClinicServiceImpl.findEntityById()`，不能证明该测试覆盖了服务捕获数据访问异常的逻辑，也不能证明数据库确实没有编号 999。

本步没有重新执行这些测试，以上是代码规定的条件和断言，不是新采集的运行结果。也未验证外部网络请求、安全链路或所有异常响应。

## 五、官方文档与面试题

- [Spring：请求映射和路径参数](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)
- [Spring：ResponseEntity](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)

1. **地址中的编号如何进入控制器？** 路径模板捕获 petTypeId，`@PathVariable` 绑定到参数，并由 Spring 转换为参数要求的整数类型。
2. **数据库未找到时，谁决定本例的 404？** 当服务返回 null 时，控制器的空值分支构造状态为 404 的响应。
3. **控制器的未找到测试是否覆盖服务异常转换？** 没有。它预设服务替身返回 null，不会执行真实服务的查询及异常捕获代码。

## 六、当次练习与唯一下一步

对于 `/petclinic/api/pettypes/999`，假设服务正常返回 null，请说明：（1）999 如何传到查询方法？（2）谁返回 404，是否还调用 Mapper？（3）为什么现有控制器未找到测试不能证明真实数据库没有编号 999？

这是代码分析练习，不是已发出的真实请求。当前等待学习者回答并增量记录，不自动修改测试或进入其他步骤。

## 2026-09-16 首次练习回答与纠正（增量证据）

- 学习者原文：“通过PathVariable注解绑定到controller层方法入参petTypeId。controller返回404，调用Mapper进行实际数据库查询。因为测试走的是mock服务，没有真实查询数据库。”
- 正确部分：能说明路径参数绑定到控制器入参、由控制器返回 404，以及当前服务替身测试没有执行真实查询链路。
- 尚不完整：已说明路径进入控制器，但尚未完整复述控制器把编号交给服务、服务再调用数据访问接口的传递顺序。
- 需纠正：本例 PetTypeMapper 只将实体转换为对外数据对象，不查询数据库。真实查询由服务调用 PetTypeRepository.findById(...) 完成；控制器收到 null 后立即 return 404，该方法到此结束，后面的 Mapper 调用不会执行。
- 证据边界：保留第十步曾正确指出 Mapper 复制字段的回答，同时记录本步再次混淆查询与映射职责的事实；不能据此前回答推定本场景已掌握。纠正后的职责区分及提前返回理解仍待复核。
- 本轮只核对源码和增量更新文档，未运行测试或启动，未修改业务代码。
- 当次追问：服务返回 null 后，为什么不会执行 Mapper？若查到对象，Repository 与 Mapper 各负责什么？

## 2026-09-16 纠正后复核与本步收尾（增量证据）

- 学习者原文：“服务返回null后，controller直接返回404响应，所以不执行mapper。Repository执行实际数据库查询，Mapper负责把entity转换为dto”。
- 复核结果：正确解释本例 null 分支提前返回后不执行 Mapper，并正确区分 Repository 的数据查询职责与 Mapper 的实体到数据传输对象转换职责。
- 与首次回答合并判断：路径绑定、控制器决定 404、服务替身测试的数据证据边界已有回答证据；本次新增的是纠正后对提前返回和对象转换职责的正确说明。
- 验收结论：第十三步源码阅读与基础概念问答收尾完成。保留首次误解及纠正过程；不能将提示后的正确回答等同于稳定独立掌握。完整编号传递链路的无提示复述、独立源码定位、测试编写与实际排障仍待验证。
- 本轮只更新学习记录，未执行构建、测试或启动，未修改业务代码，未提交或推送。
- 下一独立步骤建议：聚焦真实服务的未找到处理，先检查现有服务测试，再通过 Repository 测试替身设置查询异常，验证真实 ClinicServiceImpl 将指定异常转换为 null；由学习者解释它与控制器测试的区别。本次不执行该步骤。
