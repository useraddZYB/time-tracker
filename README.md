# 零，程两大核心痛点？
1， 日常维护  
1.1 在线系统通常运行的好好的，但是总是每天偶尔被反馈几次问题：没结果、超时、结果错误、异常  
1.2 遇到上述问题，多数情况你都拿不到任何参数信息，即使拿到的参数，甚至拿到了完全的url接口，等你去请求的时候偏偏就是正常的，这就是所谓的“不能复现”；更严重的问题是没有“犯罪现场”，很被动。  
1.3 极端情况下，后续可以长期十天半个月没有复现，但是雷就在那里

2， 性能调优（耗时调优）  
2.1 接口很慢，逻辑极其冗长，如何下手  
2.2 是逻辑不合理，还是特殊参数变量导致的高耗时，不知道  
2.3 每个方法都上耗时打点监控行不行？方法有n多调用方，到底是都慢，还是部分慢，咋区分？  
2.4 定位到方法了，头部方法内部的逻辑（代码片段）也很冗长，如何下手？重构拆分太危险，不重构不知道哪慢  

```
“接口url给我下”
“什么没有接口，那我咋查”
“我试了下是好的啊，你等下次复现吧”
不能复现！！！
不了了之！！！
“日志里没打印异常啊”
“x，日志刷满了，这咋查啊”
“x，这么复杂的老代码，如何下手优化啊”
“x，直接重构吧，大不了xx”

古今中外，多少人痛心疾首，花费大量时间查问题而没有时间打游戏看小说写代码
小弟江湖游走七八载，识得论文几篇，总结落地
```

## 答案在下面！！！
## 答案在下面！！！
## 答案在下面！！！

# 一，time-tracker简介（以下简称TT）
针对进程内对外接口级别的，完整进程内逻辑调用链路的：代码片段耗时统计、串联debug信息、可查历史或线上接口问题   

即做两件事：  
1 进程内每个接口级别的，代码逻辑片段耗时统计分析；   
2 接口explain，可以理解为自动串联好了方法片段调用关系链路（作用上接近mysql explain分析的意思）   
 
源码极其简单，依赖极少的第三方包，查历史慢请求、异常请求、在线查问题十分方便；当需要优化耗时的时候，可以使用自带的统计脚本统计代码片段级别的耗时分布  

**time-tracker**在实际公司高并发系统内稳定运行一年多，无问题无bug，使用效果极佳  
友商（朋友的公司）在推进使用中  
咨询方式：钉钉号 najq2k3，微信号 zybpf806119623  

### 所以，能说人话了吗，能干啥？
* 想要查问题：“你用TT；耗时分析是赠送的”
* 想要优化性能：“你用TT；explain是赠送的”
* 又想查问题又想要优化性能：“恭喜你，答对了！！！”  

# 二，先看东西（耗时分析 & debug历史或实时）

### 1.1 关于代码片段的耗时统计  

<img src="https://trello-attachments.s3.amazonaws.com/5b3212b3f0e8f4a9560720e6/5b2a2ad29e9c614a034bb5bc/2e7a105390b645c52edee25ea4cf8747/tt_report.jpg" width="475" height="492"/>  

### 1.2 关于代码片段链路  （json格式天然的树状结构契合方法调用，截图未格式化）

![](https://trello-attachments.s3.amazonaws.com/5be29a8c765178737b25f7a4/5b2a2ad29e9c614a034bb5bc/9dc3c8e2abfcf628908071f08780abc5/tt_debug.jpg)

# 三，DEMO
#### 直接用，不需要db、不需要redis、更不需要kafka、elk，上来就直接用
#### 文档就这一页，api就七八个，学习成本almost为0
```
/**
 * 注意两点：
 * 1，接口最外层的begin使用无参数的begin()，即只有一个，之后的方法最开始使用带参数的begin(String functionName, 其他参数)
 * 2，方法的end请使用try finally结构放在finally里面，保证每个方法可以执行到end()；否则方法父子关系及方法耗时计算会不准确
 *
 * 说明：
 * 1，也可以将关键代码片段的catch到的异常摘要放进来，比如在catch里添加:
 * TimeTracker.step("enterCatch", "functionName={}, errorMsg={}", "queryIds", e.getMessage());
 * 2，可以自定义选择对接口链路上，哪些方法做 begin end，如果不关心的不重要的方法可以不做begin end
 * 3，jar包里的方法也可以加上begin end step埋点
 * 4，最烦人的耗时计算底层直接做了，可以不用再手工计时相减了
 * 5，step() 输出完全可以替换掉很多的原本的系统日志
 *
 * @author zyb
 * @Date 2018/6/20 下午4:05
 **/
public class TestTimeTracker {

    /**
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {

        TimeTracker.config(TrackConfig.newBuilder().minCost(5).hotStep("queryIdFromRedis").slowLevel(0, 500, 1000).build());
        TimeTracker.begin();   // *** 启动，注意接口最外层首次调用的是无参数的begin方法，内部方法不能再用

        try {
            List<Long> ids = queryId(); // *** 内部有详细打点

            String type = "news";
            queryObj(ids, type);
            TimeTracker.step("queryObj", "ids={}, type={}", ids, type); // *** 附带备注

            // 模拟 filter 逻辑
            Thread.sleep(50);
            TimeTracker.step("filter"); // *** 简单的片段计时
        } finally {
            // *** 打印报告
            TimeTracker.end();

            // 这里一般用作描述当前请求/接口，可以将http请求放进去，或者关键参数放进去，也可以将关键的结果值，比如物品数量放进去
            TrackReport report = TimeTracker.getReport("sessionId={}", System.currentTimeMillis());
            System.out.println(report);
            TrackReporter.logReportStat("channelServlet", report);
        }
    }
    /**
     * queryId系列方法
     *
     * @return
     * @throws Exception
     */
    private static List<Long> queryId() throws Exception {
        TimeTracker.begin("queryId");   // *** 函数内部需要分段计时的话，函数也需要begin(name)，注意内部方法需要使用带参数的begin(参数)

        List<Long> ids;
        try {
            ids = null;
            ids = queryIdFromRedis();
            TimeTracker.step("queryIdFromRedis");
            if(null == ids) {
                ids = queryIdFromMongo();
            }
        } finally {
            TimeTracker.end();   // *** 与begin()配套
        }

        return ids;
    }
    private static List<Long> queryIdFromRedis() throws Exception {
        Thread.sleep(200L);
        return null;
    }
    private static List<Long> queryIdFromMongo() throws Exception {
        TimeTracker.begin("queryIdFromMongo");

        List<Long> ids;
        try {
            Thread.sleep(300L);
            ids = new ArrayList<>();
            ids.add(1L);
            ids.add(2L);
            ids.add(3L);
            TimeTracker.step("newIds");

            Thread.sleep(400L);
            TimeTracker.resetWatch();
            ids = addRec(ids);
        } finally {
            TimeTracker.end();
        }

        /*List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(2L);
        ids.add(3L);
        ids = addRec(ids);*/

        return ids;
    }

    private static List<Long> addRec(List<Long> ids) throws Exception {
        TimeTracker.begin("addRec", "ids={}", ids);

        try {
            ids.add(9L);
            Thread.sleep(200L);
            TimeTracker.step("addRec1");

            Thread.sleep(400L);
            TimeTracker.step("addRec2");
        } finally {
            TimeTracker.end();
        }

        return ids;
    }
    /**
     * queryObj系列方法
     *
     * @param ids
     * @param type
     * @return
     * @throws Exception
     */
    private static List<Object> queryObj(List<Long> ids, String type) throws Exception {
        Thread.sleep(1200L);
        return new ArrayList<>();
    }
}
```

# 四，背景

### 1 为什么（关于耗时）

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

### 2 干嘛用（关于explain查问题）

* 线上反馈问题说我们服务返回数据有误，比如：超时、很慢、数据少、没数据等等，很难或没法收集到完整的请求参数，无法在线下重放测试
* 无法复现的线上问题，同样的请求一会正常一会不正常
* 接口逻辑复杂，日志打印太多，且跟别的请求日志混合交杂在一块，极难从日志中找出问题
* 线上偶尔有部分接口超时，导致上层服务收到空结果。从哪里能看到类似于mysql的慢查询日志呢，而且可以方便的获取完整的接口现场呢
* 性能调优，但是面对复杂的业务流程逻辑代码，不知道从何下手，不知道哪几块是瓶颈

### 3 TT如何解决上述问题

* 一次埋点接入，解决上述所有问题，首先，可实时或查看历史问题现场；其次，可以做性能调优的前期定位瓶颈代码片段的准备工作
* 线上线下各种环境直接接入，可一直在生产环节跑（耗时分析是离线手工触发去做的），查看就是json结果，无任何额外学习成本
* 缺点：少量侵入代码，但是不侵入逻辑，侵入风格几乎与打印日志一模一样，甚至比日志还简单，因为不需要额外计算耗时

### 4 关于粒度
```
分布式全链路 < 进程接口 < 进程接口以及内部n层方法链 < 方法+代码片段 < 每一行代码  
TT的粒度定位在 方法+代码片段（代码片段指的是一个方法体里的一段代码，一个方法可以有多个片段）
```

# 五，在线或离线explain

#### 在线：可以把链路结果追加到，已有系统的debug调试页，提高定位问题效率，使用效果极好
#### 离线：由于链路日志已经全量（或高于耗时阈值）输出在独立的日志文件中，所以可以查线上产品运营测试等反馈的问题，根据关键参数grep日志即可；
#### 也可以随时去看日志，找有没有慢操作、超时、异常请求，并根据查出的当时的逻辑链路定位问题

![](https://trello-attachments.s3.amazonaws.com/5be29a8c765178737b25f7a4/5b2a2ad29e9c614a034bb5bc/9dc3c8e2abfcf628908071f08780abc5/tt_debug.jpg)


# 六，集成使用

### 1 maven引入

TimeTracker  

```
<dependency>
    <groupId>com.yidian.serving.index</groupId>
    <artifactId>time-tracker</artifactId>
    <version>0.0.1</version>
</dependency>
```  

### 2 集成步骤  

* 下载源码，本地 mvn clean install 打包到本地仓库
* 修改业务系统pom.xml，添加time-tracker依赖包；见上maven引入一节（time-tracker本身只依赖了slf4j和fastjson）
* 修改log4j.properties，增加一个appendar；demo见依赖包里的log4j.pro.demo
* 修改代码，添加埋点；发布环境运行；埋点demo见依赖包里的test示例
* 执行shell统计脚本，需要提前将shell脚本手工copy或上传到服务器上；脚本在依赖包里
* 查看统计结果

# 七，原理

### 1 原理简介：  

```
1，内部使用ThreadLocal类存储：片段栈TrackNode(步骤名、耗时、描述备注)、函数计时器(Watch)
2，step()计时；begin() end()控制函数计时器更新：每个函数包括子函数，用自己的计时器
2，调用TimeTracker的获取结果api，内部自动清理TrackNode等数据
3，使用TrackReporter的统计api，会将耗时明细及耗时统计日志输出到单独的日志文件里；减少日志侵入及方便查阅耗时日志
```

### 2 后续版本

```
开发中...  

1，“性能火焰图”：将耗时片段topN（即热点代码）统计显示出来
```

### 3 详细

* 统计分析是可选的，如果不需要统计tp99等耗时分布，则无须调整log4j及使用统计脚本，无须使用日志工具类做打点，正常输出日志即可：log.info("xx=" + xx + ", report=" + report);
* 单条耗时明细demo截图：  
<img src="https://trello-attachments.s3.amazonaws.com/5b3212b3f0e8f4a9560720e6/5b2a2ad29e9c614a034bb5bc/7ec9e6f534ae61be5c962be1f9f0fe53/tt_report_detail.jpg" width="412" height="652"/>
* 集成代码demo截图：  
请参照源码中的test包下的示例demo类，添加了注释说明，end()埋点务必放在finally里
<img src="https://trello-attachments.s3.amazonaws.com/5b3212b3f0e8f4a9560720e6/5b2a2ad29e9c614a034bb5bc/09623eb8334410d63d0960df6afe419a/tt_client.jpg" width="512" height="500"/>
* 脚本可以统计所有代码片段的耗时分布，观察具有耗时瓶颈的代码片段，即找到我们重头要优化的地方

  
# 八，其他说明

### 1 用途 

> 1，代码级别耗时分析，不仅计算整个对外提供的接口（顶层函数）耗时；  
> 2，还可以进一步分析该函数内部代码片段以及“子函数”的“片段/步骤“耗时；  
> 3，用于分析整个函数内部各步骤的耗时，找出耗时较高的步骤，即找出性能瓶颈；   
> 4，定位在：首先，为性能优化前的，必要的定位问题阶段；其次，强大的在线离线debug问题功能

### 2 特点

> 1，TimeTracker（简称TT），为统计tp99、耗时区间分布等整体统计设计（提供配套统计脚本） --> 有用  
> 2，TT提供尽量简化的api，降低代码侵入性；输出单行json格式  --> 好用  
> 3，TT定位于线上全量请求耗时分析及日志打印（提供配套日志打印api、统计脚本） --> 全面  
> 4，TT提供自定义的耗时分布参数，供业务方使用，日志带耗时级别标识，方便grep定位 --> 个性化
> 5，埋点函数可以输出自定义参数列表，方法定位在线或追加离线历史接口问题，比如超时 异常 慢处理等  --> 实用

### 3 API介绍

> 1，耗时分布统计：统计脚本tt_stat.sh及log4j配置修改示例log4j.pro.demo在项目resource目录内  
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


# 九，与“火焰图”的区别

### 1 火焰图简介   
#### 以下四点是网上随便查的一段结果

> 0，原理：
> > perf 命令（performance 的缩写）是 Linux 系统原生提供的性能分析工具，会返回 CPU 正在执行的函数名以及调用栈（stack）。通常，它的执行频率是 99Hz（每秒99次），如果99次都返回同一个函数名，那就说明 CPU 这一秒钟都在执行同一个函数，可能存在性能问题。  
> 
> 1，打日志：sudo perf record -F 99 -p 13204 -g -- sleep 30   
> 
> 2，统计：sudo perf report -n --stdio   
> 
> 3，火焰图：根据命令分别生成折叠后的调用栈、火焰图、svg图

### 2 区别  

> 1，火焰图统计的是短时间（如几十秒几分钟）的耗时，生成日志文件巨大；TT是长时间，每天的日志根据业务来说在几G左右  
> 2，火焰图不能支持备注描述的嵌入，比如想知道某一次的请求的某个函数或片段的参数情况  
> 3，火焰图的实现机制只能预估函数对于cpu的占用时间，不能准确统计函数的耗时  
> 4，火焰图只能统计“函数”级别，没法支持同一函数内的多个代码片段的耗时，比如一个rpc调用  
> 5，无法或极难定制化、改造、扩展等  
> 6，火焰图方案的优点是：图片漂亮直观、代码零侵入；但是零侵入对于复杂逻辑系统、分布式调用系统来说，无法做到更精细的统计  


# 十，与阿里arthas的区别 

#### 1，arthas优缺点
功能更全面，代码无侵入性  
但是在耗时调优跟业务逻辑debug这块，缺点是粒度过粗（方法）或过细（每一行代码），对参数、局部变量、结果收集体验不好  
其次是只能在线统计较短时间的请求  
另外debug用户请求支持不够好，真正最需要的是进程内一个完整接口（底层函数）的完整片段逻辑的链路，而arthas只做到了单个方法级别，基本定位不了问题  arthas更适合处理极端性能或极端问题，对于通用的耗时和业务逻辑debug不太应手。


#### 2，timetracker专注于耗时优化及debug辅助；  

耗时这块，由于基于日志输出，离线shell脚本统计，理论上可以统计任何时间段的代码片段，且统计不影响在线服务性能；  

其次debug这块，可以完全自定义需要带什么参数，以完整的用户请求（顶级接口）为链路路径，更符合debug需求；历史耗时链路“全量”保持在日志里，可以随时借助grep slowLevel=1000 查到极端耗时请求链接和当时的链路现场；debug功能可以长期在线支持；  

关于粒度，代码片段大小完全自己决定，粗细自己掌握  
关于代码片段的上下文信息，可以在片段埋点api上自己随意添加，甚至可以加上异常摘要

#### 3，TT缺点
代码具有侵入性。辩解一下：等同于log4j的侵入型，可用tt代替log4j  
arthas虽然无侵入，但是具备两个问题：  
第一不能在线一直跑，会遗漏重要问题请求的现场  
第二不侵入就没办法获取当时逻辑内部关键方法局部变量值，而这对于查问题太重要了  

#### 4，个人对arthas的看法  
工具做的很多很全很厉害，非常极端对场景下大有用处；但是未能针对特定几大类痛点问题做出对应的完整的解决方案，是功能而不是工具利器











  




