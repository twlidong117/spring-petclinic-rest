# 第二十六步：删除宠物类型流程阅读

## 日期、基准与范围

- 2026-09-29（Asia/Shanghai），来源：Codex 实际命令和源码读取。已读取规则与学习记录，第二十五步问答完成，开始时工作区干净。
- PR #25 实查 MERGED、目标 dev，合并提交 767ae51865ac7ab41e032c09918ffa9dc5e47f0c。fetch 后确认 origin/dev 包含该提交，从该提交创建 codex-step-26-delete-pettype-flow。
- 核对 PowerShell 工作环境、分支、提交与文件状态。本步只阅读，不运行 Java/Maven 版本检查、构建、测试或应用，不把历史环境或报告冒记为今日运行。
- 只新建本步文档并增量更新进度，不改业务代码、测试、依赖、约束或配置，不提交推送。

## 源码事实与新概念

阅读 PetTypeRestControllerV1.deletePetType、ClinicServiceImpl.deletePetType/findPetTypeById、openapi.yml 删除定义、现有两个删除测试及服务替身声明。已有生成 PettypesApi.deletePetType 也已读取，仅作源码参考，不代表本次成功生成。

- DELETE：用于请求删除目标资源的请求方法；本例路径 /api/pettypes/{petTypeId} 提供目标编号。
- 控制器只有 Integer petTypeId 参数；接口定义未声明删除请求正文。测试虽发送对象正文，但该方法不从正文读取名称或编号。不能将此推广为所有删除接口都禁止或忽略正文。
- 按路径编号查找，返回 null 则立即返回 404，不继续删除。
- 找到时将查到的 PetType 实体交给 clinicService.deletePetType，调用正常返回后构造 204 响应，没有提供正文。未设置名称，也未调用 savePetType。
- 实际服务将实体交给 petTypeRepository.delete。Repository 是负责访问持久化数据的组件；本轮未执行真实数据删除。
- @Transactional：声明事务边界。事务将一组数据操作作为一个整体管理提交与回滚；具体生效和回滚条件需结合运行配置及异常规则。本步只识别注解，不测试事务行为，不把注解存在等同于实际删除或回滚成功。
- @PreAuthorize 声明调用前的角色权限要求，@Override 表示实现接口方法；本步不验证权限边界。
- 接口定义写成功 200/304，控制器与成功测试使用 204；仅记录差异，不在本步修复。

## 完整控制器方法

依赖已有 clinicService；沿用 PreAuthorize、jakarta.transaction.Transactional、ResponseEntity、HttpStatus、PetType、PetTypeDto 导入及现有控制器初始化，不是独立程序。

```java
    @PreAuthorize("hasRole(@roles.VET_ADMIN)")
    @Transactional
    @Override
    public ResponseEntity<PetTypeDto> deletePetType(Integer petTypeId) {
        PetType petType = this.clinicService.findPetTypeById(petTypeId);
        if (petType == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        this.clinicService.deletePetType(petType);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
```

## 现有成功测试与证据范围

使用已有 Test、WithMockUser、ObjectMapper、PetType、MediaType、given、delete、status 导入。每个测试前 petTypes.get(0) 初始化为 id=1、name=cat；ClinicService 为 MockitoBean 替身，mockMvc 使用已有控制器和异常处理器配置。

```java
    @Test
    @WithMockUser(roles="VET_ADMIN")
    void testDeletePetTypeSuccess() throws Exception {
        PetType newPetType = petTypes.get(0);
        ObjectMapper mapper = new ObjectMapper();
        String newPetTypeAsJSON = mapper.writeValueAsString(newPetType);
        given(this.clinicService.findPetTypeById(1)).willReturn(petTypes.get(0));
        this.mockMvc.perform(delete("/api/pettypes/1")
            .content(newPetTypeAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNoContent());
    }
```

- 成功测试仅检查 204，未验证删除调用或参数；不存在测试设置 findPetTypeById(999) 返回 null 并检查 404，未检查禁止删除。
- 请求使用服务替身，现有状态断言不能证明数据库记录已删除。本轮没有运行这两个测试，不能宣称今日通过。
- 若仅遗漏控制器的删除调用但仍返回原 204，从当前成功测试源码推断它不能发现遗漏；故障版本未实际执行。本轮也未修改测试补此检查。

## 验收与官方资料

完成仓库状态核对与删除流程阅读，学习记录差异检查通过。无新运行证据；业务方法、测试和真实删除均未执行。学习者本步能力待回答验证，独立编写与排障待验证。

- [Spring ResponseEntity](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)：返回状态、头及正文。
- [Jakarta Transactional](https://jakarta.ee/specifications/transactions/2.0/apidocs/jakarta/transaction/transactional)：声明事务边界。只引用基本概念，不当作项目版本或运行证据。

## 面试问答

1. 为什么删除前先查找？定位实体并区分目标不存在的 404 分支。
2. 删除方法的参数来自哪里？该控制器从请求路径获取编号，删除服务接收查到的实体。
3. 返回 204 能证明真实删除吗？单靠状态断言不能；尤其本测试服务是替身，没有执行真实服务删除链路。

## 当前唯一练习

假设查找仍返回 id=1 的实体，仅删除控制器中的 this.clinicService.deletePetType(petType) 这一行，保留 204 返回。上面的现有成功测试能发现这个遗漏吗？如不能，应补哪类服务调用检查？只分析，不改代码。

收到回答后增量复核；本轮停在第二十六步，不自动补测试、提交或进入下一步。

## 练习回答与第二十六步收尾（2026-09-29）

### 学习者原回答（来源：本轮聊天）

> 不能。需要补充删除服务调用检查

### 复核与能力边界

- 回答正确：学习者识别现有成功测试不能发现遗漏删除调用，并指出需要补充删除服务调用检查。
- Codex 补充具体检查方向：验证 deletePetType 收到查到的实体且调用恰好一次。参数与次数的具体要求由助手补充，不记为学习者已独立写出验证代码。
- 第二十六步源码阅读与基础练习复核完成。已有完整代码和讲解后识别状态断言不足的回答证据；独立验证代码编写、参数匹配选择、实际删除及排障能力仍待验证。
- 遗漏删除的假设版本未实际修改或运行，结论仍是源码推断；本轮没有删除测试运行证据。

### 本轮实际操作（来源：Codex）

- 已读取规则、进度和分步记录，核对分支 codex-step-26-delete-pettype-flow；HEAD 与本地 origin/dev 同为 767ae51865ac7ab41e032c09918ffa9dc5e47f0c，祖先检查退出状态 0。
- 当前已有修改仅两份学习文档，本轮增量追加问答，保留历史；未 fetch、查询远端、修改业务或测试、构建、运行测试、提交或推送。

### 知识点、官方资料与面试问答

1. 只检查 204 能发现删除调用遗漏吗？不能，错误实现仍可返回相同状态。
2. 应补什么检查？删除服务调用检查，进一步核对参数和恰好一次的调用次数。
3. 验证服务替身删除调用成功能证明数据库已删除吗？不能，仍需真实持久化流程的执行证据。

官方资料：[Mockito 调用验证](https://site.mockito.org/javadoc/current/org/mockito/Mockito.html)、[Spring ResponseEntity](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)。用于基础语义，不作为项目运行或版本证据。

### 下一步建议（未执行）

后续独立步骤可增强现有删除成功测试，检查删除服务收到查到的实体且调用一次。本轮停在第二十六步收尾，等待用户安排，不自动补测试或提交。


## 提交前格式检查补记（2026-09-29）

- 暂存后 git diff --cached --check 首次发现新文档中复制的测试代码含空格后接制表符的缩进。此前 git diff --check 未覆盖尚未跟踪的新文档，不能将此前通过扩大为新文档已检查通过。
- 本轮仅将文档中的制表符替换为空格，不改测试源码或语义；首次失败时尚未创建提交或推送。修正后再次执行暂存差异检查，通过后才继续提交。
