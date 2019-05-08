# time-tracker
针对进程内对外接口级别的，完整进程内逻辑调用链路的：代码片段耗时统计、串联debug信息、可查历史或线上接口问题

# 耗时分析

## 一，TimeTracker 耗时追踪器

### 先看东西
<img src="https://trello-attachments.s3.amazonaws.com/5b3212b3f0e8f4a9560720e6/5b2a2ad29e9c614a034bb5bc/2e7a105390b645c52edee25ea4cf8747/tt_report.jpg" width="475" height="492"/>

### 为什么

> 1，我们要干啥？  
> > 知己知彼  
> > 知己：知道自己自身的整体系统延迟性能情况，知道自己的函数片段有哪些是瓶颈，有超时风险  
> > 知彼：知道我们依赖的外部服务、sdk、存储，这些系统的延迟情况，是否有瓶颈，反馈出去
> > 
> > 最后，逐个解决  
>    
> 2，怎么做？已有的grafana监控、haproxy监控，平均耗时和tp99不够用吗？  
> > 对于性能优化接近极致的系统来说，够用  
> > 但是对于很多性能或延迟处于早期，待大范围优化的系统来说，不够用，平均和tp99太粗了   
> > 我们需要更细致的tp30、tp50...，以及耗时范围段的占比统计，这样能正确反映出系统整体的延迟情况
> 
> 3，有了更细致的tp及耗时段分布，够了吗？  
> > 不够  
> > 由于我们处在一个复杂的分布式系统架构内，每一个服务都有错综复杂的对外部服务依赖  
> > 且，对于业务逻辑复杂的系统，光服务内部的自身函数片段也错综复杂  
> > 所以需要一个，能理清函数调用栈的统计系统  
> > 
> > 更进一步，对函数片段（调用栈+片段）做耗时分布统计；于此可以去解决 1 了

### maven引入

TimeTracker  

```
<dependency>  
	<groupId>com.yidian.serving.index</groupId>  
	<artifactId>index-util</artifactId>  
	<version>1.5.1</version>  
</dependency>
```  

### 集成步骤  

* 修改pom.xml，添加index-util依赖包；见上maven引入一节
* 修改log4j.properties，增加一个appendar；demo见依赖包里的log4j.pro.demo
* 修改代码，添加埋点；发布环境运行；埋点demo见依赖包里的test示例
* 执行shell统计脚本，需要提前将shell脚本手工copy或上传到服务器上；脚本在依赖包里
* 查看统计结果

### 原理简介：  

```
1，内部使用ThreadLocal类存储：片段栈TrackNode(步骤名、耗时、描述备注)、函数计时器(Watch)
2，step()计时；begin() end()控制函数计时器更新：每个函数包括子函数，用自己的计时器
2，调用TimeTracker的获取结果api，内部自动清理TrackNode等数据
3，使用TrackReporter的统计api，会将耗时明细及耗时统计日志输出到单独的日志文件里；减少日志侵入及方便查阅耗时日志
```

### 2.0 版本

```
开发中...  

1，“性能火焰图”：将耗时片段topN（即热点代码）统计显示出来
```

### 详细

* 统计分析是可选的，如果不需要统计tp99等耗时分布，则无须调整log4j及使用统计脚本，无须使用日志工具类做打点，正常输出日志即可：log.info("xx=" + xx + ", report=" + report);
* 单条耗时明细demo截图：  
<img src="https://trello-attachments.s3.amazonaws.com/5b3212b3f0e8f4a9560720e6/5b2a2ad29e9c614a034bb5bc/7ec9e6f534ae61be5c962be1f9f0fe53/tt_report_detail.jpg" width="412" height="652"/>
* 集成代码demo截图：  
<img src="https://trello-attachments.s3.amazonaws.com/5b3212b3f0e8f4a9560720e6/5b2a2ad29e9c614a034bb5bc/09623eb8334410d63d0960df6afe419a/tt_client.jpg" width="512" height="500"/>
* 脚本及log4j示例的路径截图：  
![](https://trello-attachments.s3.amazonaws.com/5b3212b3f0e8f4a9560720e6/5b2a2ad29e9c614a034bb5bc/e2509744728ca9f942b881b2e09ef7a2/tt_util.jpg)
* 脚本可以统计所有代码片段的耗时分布，观察具有耗时瓶颈的代码片段，即找到我们重头要优化的地方

  

## 二，与 Profiler 区别

Profiler  

```
<dependency>  
	<groupId>com.hipu.util</groupId>
  	<artifactId>utils</artifactId>
  	<version>1.0.43</version>
</dependency>
```

### 用途 / 共同点

> 1，代码级别耗时分析，不仅计算整个对外提供的接口（顶层函数）耗时；  
> 2，还可以进一步分析该函数内部代码片段以及“子函数”的“片段/步骤“耗时；  
> 3，用于分析整个函数内部各步骤的耗时，找出耗时较高的步骤，即找出性能瓶颈；   
> 4，定位在：为性能优化前的，必要的定位问题阶段。

### 区别

> 1，TimeTracker（简称TT），为统计tp99、耗时区间分布等整体统计设计（提供配套统计脚本） --> 更有用  
> 2，TT提供尽量简化的api，降低代码侵入性；输出单行json格式，Profiler为多行  --> 更好用  
> 3，TT定位于线上全量请求耗时分析及日志打印（提供配套日志打印api、统计脚本） --> 更全面  
> 4，TT提供自定义的耗时分布参数，供业务方使用，日志带耗时级别标识，方便grep定位 --> 个性化

### 区别明细及示例

> 1，耗时分布统计：统计脚本tt_stat.sh及log4j配置修改示例log4j.pro.demo在index-util项目内  
> 2，简化api：  
> > 步骤耗时打点：TimeTracker.step("步骤名");  
> > 步骤耗时打点，带描述：TimeTracker.step("步骤名", "ids={}, type={}", ids, type);  
> > 获取统计结果：TrackNode report = TimeTracker.getReport();   
> 
> 3，日志打印：TrackReporter.logReportStat("channelServlet", TimeTracker.getReport());  
> 4，自定义耗时分布：
> > 使用默认耗时分布：TimeTracker.begin();  
> > 自定义耗时分布：TimeTracker.begin(0, 30, 100);  
> > 日志级别，方便定位：cat tt_stat.log | grep "slowLevel=30"


## 三，与“火焰图”的区别

### 火焰图简介 

> 0，原理：
> > perf 命令（performance 的缩写）是 Linux 系统原生提供的性能分析工具，会返回 CPU 正在执行的函数名以及调用栈（stack）。通常，它的执行频率是 99Hz（每秒99次），如果99次都返回同一个函数名，那就说明 CPU 这一秒钟都在执行同一个函数，可能存在性能问题。  
> 
> 1，打日志：sudo perf record -F 99 -p 13204 -g -- sleep 30  
> > perf record 表示采集系统事件, 没有使用 -e 指定采集事件, 则默认采集 cycles(即 CPU clock 周期), -F 99 表示每秒 99 次, -p 13204 是进程号, 即对哪个进程进行分析, -g 表示记录调用栈, sleep 30 则是持续 30 秒.   
> 
> 2，统计：sudo perf report -n --stdio   
> >为了便于阅读, perf record 命令可以统计每个调用栈出现的百分比, 然后从高到低排列.  
> 
> 3，火焰图：  
> >perf script -i perf.data &> perf.unfold             // 生成折叠后的调用栈  
> >./stackcollapse-perf.pl perf.unfold &> perf.folded  // 生成火焰图  
> >./flamegraph.pl perf.folded > perf.svg              // 生成svg图  

### 区别  

> 1，火焰图统计的是短时间（如几十秒几分钟）的耗时，生成日志文件巨大；TT是长时间，每天的日志在1G左右  
> 2，火焰图不能支持备注描述的嵌入，比如想知道某一次的请求的某个函数或片段的参数情况  
> 3，火焰图的实现机制只能预估函数对于cpu的占用时间，不能准确统计函数的耗时  
> 4，火焰图只能统计“函数”级别，没法支持同一函数内的多个代码片段的耗时，比如一个rpc调用  
> 5，无法或极难定制化、改造、扩展等  
> 6，火焰图方案的优点是：图片漂亮直观、代码零侵入；但是零侵入对于复杂逻辑系统、分布式调用系统来说，无法做到更精细的统计  

### 在线服务debug增强
可以把链路结果追加到，已有系统的debug调试页，提高定位问题效率，使用效果良好

![](https://trello-attachments.s3.amazonaws.com/5be29a8c765178737b25f7a4/5b2a2ad29e9c614a034bb5bc/9dc3c8e2abfcf628908071f08780abc5/tt_debug.jpg)

####简单说几句，与阿里arthas的区别 

1，arthas功能更全面，代码无侵入性，但是在耗时调优这块，缺点是粒度过粗（方法）或过细（每一行代码），对参数、结果收集体验不好，其次是只能在线统计较短时间的请求，另外debug用户请求支持不够好


2，timetracker专注于耗时优化及debug辅助；  

耗时这块，由于基于日志输出，离线shell脚本统计，理论上可以统计任何时间段的代码片段，且统计不影响在线服务性能；  

其次debug这块，可以完全自定义需要带什么参数，以完整的用户请求（顶级接口）为链路路径，更符合debug需求；历史耗时链路“全量”保持在日志里，可以随时借助grep slowLevel=1000 查到极端耗时请求链接和当时的链路现场；debug功能可以长期在线支持；  

关于粒度，代码片段大小完全自己决定，粗细自己掌握  

缺点就是代码具有侵入性











  




