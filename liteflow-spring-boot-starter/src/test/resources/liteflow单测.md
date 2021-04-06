### 先请认真阅读单测用例的说明

> 说明：
>
> 1.因为liteflow现在有xml，json，yml三种配置方式，每个场景如果都测试3种配置场景，那冗余有点多。目前有专门测试解析器的测试用例，其余的用例都统一基于xml场景。
>
> 2.liteflow使用上也分3种场景，无spring环境，spring环境，springboot环境。大多数情况下都会对3种场景进行测试，但是因为无spring环境限制比较多，使用场景也比较少。所以有些测试用例也就跳过了这种环境。



### 测试用例列表

- [x] aop的测试
  - [x] springboot下的AspectJ
  - [ ] spring环境下的aop
  - [x] springboot下的global AOP

- [x] 异常的测试
  - [x] Liteflow内部各种CheckedException的测试

- [x] 解析器测试
  - [x] Xml方式在无spring环境下测试
  - [x] Xml方式在spring环境下测试
  - [x] Xml方式在springboot环境下测试
  - [x] Json方式在无spring环境下测试
  - [x] Json方式在spring环境下测试
  - [x] Json方式在springboot环境下测试
  - [x] Yml方式在无spring环境下测试
  - [x] Yml方式在spring环境下测试
  - [x] Yml方式在springboot环境下测试

- [ ] 参数测试(只测到参数是不是被总的LiteFlowConfig加载到即可)
  - [ ] 非spring环境下的参数测试，必要参数测试，非必须参数的默认值测试。
  - [ ] spring环境下的参数测试，必要参数测试，非必须参数的默认值测试。
  - [ ] springboot环境下的参数测试，必要参数测试，非必须参数的默认值测试。
- [ ] zk配置源的功能测试(zk请自己本地安装提供)
  - [ ] spring环境下的zk配置源功能测试
  - [ ] springboot环境下的zk配置源功能测试
- [ ] 自定义源的功能测试
  - [ ] spring环境下的自定义配置源功能测试
  - [ ] springboot环境下的自定义配置源功能测试
- [ ] 组件功能点测试(基于springboot环境即可)
  - [ ] isAccess方法的功能测试
  - [ ] 组件抛错的功能点测试
  - [ ] isContinueOnError方法的功能点测试
  - [ ] isEnd方法和this.setIsEnd(true)的功能点测试
- [ ] 条件组件功能点测试(基于springboot环境)
  - [ ] 条件组件的功能点测试
- [ ] 显式子流程测试(基于springboot环境)
  - [ ] 子流程功能点测试，是否能进入子流程
  - [ ] 多个子流程是否能串联衔接
  - [ ] 同名节点和同名chain是否chain为优先级最高
  - [ ] 多个子流程配置顺序是否和最终执行结果无关(这个需要结合xml，json，yml来测，分3种配置方式，因为这个和parser有一定的关系)
- [ ] 隐式子流程测试(基于springboot环境)
  - [ ] 隐式子流程的功能点测试
  - [ ] 多个隐式子流程是否能共享同一个上下文
- [ ] when condition下的线程池功能测试(基于springboot环境)
  - [ ] 线程池的基本功能点测试
  - [ ] 线程池满了情况下基于errorResume参数的功能点测试
- [ ] when并行组功能测试(基于springboot环境)
  - [ ] 默认不配参数情况下并行组的功能点测试
  - [ ] 配置相同并行组情况下的功能点测试
  - [ ] 配置不同并行组情况下的功能点测试

