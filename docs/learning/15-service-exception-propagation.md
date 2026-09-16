# 第十五步：验证资源故障异常向外传播

## 基准与本步目标

- 日期：2026-09-17（Asia/Shanghai）。
- 用户表示 PR 已提交；Codex 实际查询 PR #14 状态为 MERGED，合并提交 `cb048d3bcbf91a66fb8b24dcb1c1c3dbeedea549`。获取 origin/dev 并核验祖先关系后，从该提交创建 `codex-step-15-service-exception-propagation`，起始工作区干净。
- 目标：为现有服务测试增加一个不属于“未找到”的异常场景，验证其不会被转换为 null。
- 环境：Java 21.0.12.1、Wrapper 使用 Maven 3.9.16；发行包、依赖仓库和临时目录沿用 D:/derek/maven/_cache 配置。未升级依赖或修改配置。

## 为什么要补充这个测试

当前服务只捕获 `ObjectRetrievalFailureException` 和 `EmptyResultDataAccessException`。第十四步的两个测试证明这两种异常转换为 null，但不能发现有人扩大捕获范围、把资源故障也转换为 null 的问题。

本步选用 `DataAccessResourceFailureException`，表示数据访问资源故障，例如无法连接数据库；它不属于上述两种捕获类型。资源不可用与查询没有记录是不同情况。本步只验证这一选定异常，不能由此推断所有其他异常均正确传播。

## 新增代码与概念

在 `ClinicServicePetTypeNotFoundTests` 中新增 `shouldPropagatePetTypeResourceFailure()`，保留原有两个测试：

```java
DataAccessResourceFailureException failure =
    new DataAccessResourceFailureException("Database resource unavailable");
given(petTypeRepository.findById(999)).willThrow(failure);

DataAccessResourceFailureException actual = assertThrows(
    DataAccessResourceFailureException.class,
    () -> clinicService.findPetTypeById(999));

assertThat(actual).isSameAs(failure);
verify(petTypeRepository).findById(999);
```

- 仍由 Repository 替身预设异常，Service 是通过构造方法创建的真实对象。
- 异常传播指异常没有在当前方法中被处理，继续交给调用者处理；这里真实服务将异常传到测试中的 `assertThrows`。
- `assertThrows` 执行给定操作，检查是否抛出指定类型或其子类型的异常，并返回捕获到的异常对象。没有异常或异常类型不匹配时，断言失败。
- `() -> clinicService.findPetTypeById(999)` 把待执行调用交给断言；异常在断言执行该调用时产生。
- `isSameAs(failure)` 检查对象身份，要求收到的就是替身原本抛出的那个对象，而不是另建或包装的异常。
- `verify(...)` 检查以编号 999 调用查询一次，不执行真实数据库查询。

第十四步期望服务正常返回 null；本步期望服务抛出资源故障异常。因此抛出符合预期的异常在本测试中可以是成功行为，不能笼统认为“抛异常就代表测试失败”。

## Codex 实际执行证据

```powershell
./mvnw.cmd --offline --batch-mode --no-transfer-progress '-Dtest=ClinicServicePetTypeNotFoundTests' test
```

- 执行时间：2026-09-17 07:35 至 07:36（Asia/Shanghai），日志总耗时 11.342 秒。
- 本轮实际执行 3 个测试，失败 0、错误 0、跳过 0；BUILD SUCCESS，Maven 退出状态 0。
- 已读取新生成报告，确认修改时间晚于命令开始时间，方法名为两个原有 null 场景及新增资源故障传播场景；未汇总其他历史报告。
- 证据目录：`D:/derek/maven/_cache/learning-evidence/step15-exception-propagation-20260917-073554`，包含运行日志、命令结果、报告副本和独立验证汇总。
- 最终测试差异为新增一个测试方法及两个导入，原有两项测试和服务实现未修改；`git diff --check` 通过。

## 验收结论与边界

本步实现与定向验证通过：原有两项异常转换行为保持通过，选定资源故障异常原样传出且被异常断言捕获。未加载 Spring 测试上下文或连接真实数据库，不是实际数据库故障；未验证外部响应状态、事务代理、全量测试或独立应用启动。

若服务把该异常转换为 null，新增断言应失败，这是基于断言机制的判断。本轮没有修改服务来制造这种回归，也没有实际执行失败版本。

## 学习者证据与练习

- 本步先询问：资源故障不属于服务捕获的两种异常时，应返回 null 还是向外传播，为什么？
- 等待期间完成基准、类型和环境核对；尚未收到回答后，按源码规定的向外传播预期实现测试，并给出 assertThrows 讲解。
- 本步个人理解待回答验证；测试由 Codex 编写和运行，不能推定学习者已独立掌握。第十四步已记录的讲解后回答保留原有范围。
- 待回答练习：服务为何不把本例资源故障转换为 null？为什么本轮抛出了异常，测试仍然能通过？如果服务错误地捕获它并返回 null，哪个断言会失败？

## 官方文档与面试题

- [Spring：DataAccessResourceFailureException](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/dao/DataAccessResourceFailureException.html)
- [JUnit 6.0.3：异常断言](https://docs.junit.org/6.0.3/writing-tests/assertions.html)

1. **assertThrows 与直接调用服务后检查 null 有何不同？** 前者要求调用抛出预期类型的异常，后者要求调用正常返回且结果为空。
2. **为什么不能把所有数据库异常都转成 null？** 可能将资源故障伪装成没有记录，使调用者无法区分故障与正常查询结果。
3. **为何还检查异常对象相同？** 类型检查只验证类型，对象身份检查进一步证明收到的是原异常，没有被替换或包装。

## 当前唯一下一步

等待学习者完成本步异常断言练习并增量记录，本次不自动扩大测试范围、不提交推送或进入下一学习步骤。

## 2026-09-17 练习复核与本步收尾（增量证据）

- 学习者原文：“这个异常是预期中的资源故障异常，测试正常捕获，所以通过了。assertThrow这条会失败”。
- 复核结果：正确理解预期异常被异常断言捕获时可以通过，以及服务错误返回 null、未抛出异常时异常断言失败。方法名纠正为 `assertThrows`，保留原始拼写证据，不据此认定已能独立写出可执行语法。
- 精确边界：捕获预期类型意味着异常类型断言通过；本测试还检查异常对象身份与 Repository 调用，全部检查通过才是整个测试通过。
- 验收结论：第十五步实现、三个定向测试及基础问答收尾完成。上述属于讲解后的概念复核；具体异常继承与捕获匹配的独立分析、异常对象身份断言的独立解释、测试编写和故障定位仍待验证。
- 本轮仅更新学习文档，未修改测试或业务代码，未重跑测试，未提交或推送。错误地返回 null 的场景仍未实际注入和运行。
- 下一独立步骤建议：跟踪服务向外传播的异常如何由控制器异常处理器转换为响应，并用服务替身触发同类异常，定向检查当前项目的错误响应。本次不执行该步骤，也不把服务测试通过当成外部响应已验证。
