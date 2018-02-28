## 架构设计

![architecture_image](images/architecture.png)

组件式流程引擎架构设计
Handler Unit：我们想象成每一个业务都是一个业务组件，每一个业务组件就是一个handlerUnit（处理单元）
EPU：这里的epu对应的就是我们的执行器，用来统筹并处理handlerUnit。相当于计算机的CPU
Event Bus：事件总线，用来指定下一个命令是什么，该如何去执行处理单元。这里的时间总线由我们的配置构成
Data Bus：数据总线，用来存储整个调用链里数据。每一个请求生成一个数据槽。一个数据里最多有1024个数据槽。