# 第十四步：验证真实服务的未找到异常转换

## 基准、目标与环境

- 日期：2026-09-16（Asia/Shanghai）。
- PR #13 已合并。Codex 获取最新 origin/dev 并核验提交关系，从 `e911f6d408d14d65d5440ddf0bba91f8eb18febe` 创建独立分支 `codex-step-14-service-not-found`，起始工作区干净。
- 本步目标：用 Repository 测试替身抛出指定查询异常，调用真实 ClinicServiceImpl，验证返回 null。
- 环境核验：Java 21.0.12.1，Wrapper 使用 Maven 3.9.16，发行包、依赖仓库与临时文件配置均位于 D:/derek/maven/_cache 下。本轮使用离线缓存。

## 现有测试为什么不足以证明这件事

控制器未找到测试预设 ClinicService 返回 null，未执行真实服务的异常处理。

现有 `AbstractClinicServiceTests.shouldDeletePetType()` 在查询抛出异常时由测试代码自己捕获并将变量设为 null。因此该测试的空值断言不能单独区分“服务返回 null”与“服务抛出异常、测试将变量改为 null”。本步不修改该历史测试，新增聚焦异常转换的测试。

## 新增测试及执行链路

文件：`src/test/java/org/springframework/samples/petclinic/service/clinicService/ClinicServicePetTypeNotFoundTests.java`。

- `@ExtendWith(MockitoExtension.class)` 让 JUnit 测试框架使用 Mockito 扩展，初始化本测试中的替身。
- `@Mock` 创建 Repository 测试替身；六个 Repository 依赖通过构造方法传入服务。本步只预设 PetTypeRepository 的查询行为。
- 每个测试前通过 `new ClinicServiceImpl(...)` 创建真实服务对象，没有将服务设置为替身。
- 没有加载 Spring 测试上下文；因此也未验证事务代理等 Spring 容器行为。

```text
测试调用真实 findPetTypeById(999)
  → 真实 findEntityById(...)
  → Repository 替身的 findById(999) 抛出指定异常
  → 真实服务捕获异常并返回 null
  → 测试检查返回值，并验证查询参数
```

核心测试之一：

```java
given(petTypeRepository.findById(999))
    .willThrow(new ObjectRetrievalFailureException(PetType.class, 999));

PetType result = clinicService.findPetTypeById(999);

assertThat(result).isNull();
verify(petTypeRepository).findById(999);
```

`willThrow(...)` 预设替身被调用时抛出异常；`isNull()` 检查真实服务的返回值；`verify(...)` 检查替身方法以该编号被调用一次，不查询真实数据库。

第二个测试预设 `EmptyResultDataAccessException(1)`，表示预期取得一条结果但查询为空。两个异常类型与当前服务捕获范围对应。

测试本身没有 catch 异常。如果服务没有捕获对应异常，调用无法正常返回，测试会报错，不会靠测试自己赋值 null 而通过。这是基于代码结构的判断；本轮未删除服务捕获代码来制造失败。

## 实际验证

```powershell
./mvnw.cmd --offline --batch-mode --no-transfer-progress '-Dtest=ClinicServicePetTypeNotFoundTests' test
```

- 2026-09-16 21:41 执行，Maven 日志总耗时 11.789 秒。
- 实际执行 2 个测试，失败 0、错误 0、跳过 0；BUILD SUCCESS，Maven 退出状态 0。
- 报告名称和时间已独立核验，报告晚于命令开始时间；只统计本轮新测试类，不混入历史报告。
- 两个实际方法为 `shouldReturnNullWhenPetTypeRetrievalFails` 和 `shouldReturnNullWhenPetTypeQueryHasNoResult`。
- 证据保存在 `D:/derek/maven/_cache/learning-evidence/step14-service-not-found-20260916-214106`，含命令结果、日志、验证汇总和报告副本。

## 事实、推断与尚未验证事项

- Codex 实际验证：两个 Repository 模拟异常都经真实服务处理后返回 null，且对应编号查询调用已验证；测试代码由 Codex 编写和运行。
- 本步测试未连接真实数据库，也没有控制器或真实网络请求，不能证明真实数据库中缺少编号 999，不能单独证明外部接口返回 404。
- 两个用例只覆盖指定异常的转换，未验证正常查询、所有其他异常是否正确传播、全量测试、事务或应用启动。尤其不能由两项通过推断“其他异常绝不会被吞掉”。
- 预设异常确实在测试运行中抛出并被处理，但这不是实际数据库故障。没有修改服务代码或实际演示漏掉 catch 后的失败结果。
- 验收结论：本步测试实现与定向执行通过；个人能力待本步练习回答验证。此前学习者回答保留原有范围，不因 Codex 执行成功而扩展能力结论。

## 官方文档

- [Mockito：行为预设与调用验证](https://site.mockito.org/javadoc/current/org/mockito/BDDMockito.html)（该官方页面标注版本 2.2.7，仅参考基础机制，不代表本项目依赖版本）。
- [JUnit：断言](https://docs.junit.org/6.0.3/writing-tests/assertions.html)（本例具体断言使用项目已有的 AssertJ 风格）。

## 面试题与答案

1. **要验证服务逻辑，应该替换哪一层？** 保留真实服务，把其 Repository 依赖设置为替身，以便控制查询返回或异常。
2. **为什么测试不能自行捕获异常再把变量设为 null？** 那样即使服务漏掉异常处理，测试也可能通过，无法证明 null 是服务返回的。
3. **verify 检查到 findById(999)，能否证明查过真实数据库？** 不能，它验证的是对替身的调用及参数，真实数据库访问仍需另行验证。

## 当次练习与唯一下一步

为了验证真实服务把查询异常转换成 null，应让 Service 还是 Repository 成为测试替身？如果真实服务没有捕获异常，直接调用服务后检查返回值为 null 的测试会通过吗？请说明原因。

本步等待学习者回答并记录，不自动扩大测试范围，不提交、推送或进入后续学习步骤。

## 2026-09-16 术语讲解后复核与收尾（增量证据）

- 过程：学习者先询问“应该让哪一层成为测试替身”是什么意思。Codex 解释本步用可控制的 Repository 替身抛出异常，保留真实 Service 执行异常处理；直接预设 Service 返回 null 无法验证服务逻辑。
- 学习者随后回答：“让Repository做替身。如果真实服务没有捕获异常，这个测试不会通过。单元测试代码执行时抛错。”
- 复核结果：正确选择 Repository 作为替身，并正确判断真实服务不捕获预设异常时测试不会通过。更具体地说，异常会在调用服务这一行向外传播，后面的返回值断言不会执行。
- 证据边界：这是术语讲解及给出示例后的正确复述与判断，不记为无提示独立设计能力。没有实际删除服务捕获逻辑或运行失败版本；本步实际运行结果仍是此前两个测试通过，本轮未重跑。
- 验收结论：第十四步测试实现、定向运行和基础概念复核收尾完成。测试由 Codex 编写与执行，学习者独立编写、执行及排查失败仍待实践验证。
- 本轮只更新学习记录，未修改测试或业务代码，未提交或推送。
- 下一独立步骤建议：补充一个不应被吞掉的异常场景，先由学习者判断应断言 null 还是异常向外传播，再基于现有捕获范围编写并定向验证。本次不执行该步骤。
