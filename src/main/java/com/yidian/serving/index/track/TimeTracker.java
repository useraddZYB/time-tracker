package com.yidian.serving.index.track;

import com.alibaba.fastjson.JSONObject;
import com.yidian.serving.index.track.util.TrackConfig;
import com.yidian.serving.index.track.util.StringUtill;
import com.yidian.serving.index.track.util.TrackWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * "跟踪器"：用作进程内链路耗时分析
 *
 * 注意：目前不支持多线程追踪；异步线程可以"从头"开始追踪（重新begin()）
 *
 * 用法：
 * 示例见 TestTimeTracker.main()
 *
 * @author  zyb
 * @Date 2018/6/19 下午7:40
 **/
public class TimeTracker {
    private static final Logger logger = LoggerFactory.getLogger(TimeTracker.class);

    private static final String STEP_MAIN = "main";




    //============================================ 必选API ================================================

    /**
     * 启动跟踪器，在顶层函数入口调用
     *
     * eg:
     *
     *         TimeTracker.begin();
     *
     *         List<Long> ids = queryId();
     *         TimeTracker.step("queryId");
     *
     *         String type = "news";
     *         queryObj(ids, type);
     *         TimeTracker.step("queryObj", "ids={}, type={}", ids, type);
     *
     *         // do filter
     *         Thread.sleep(50);
     *         TimeTracker.step("filter");
     *
     *         // 打印
     *         System.out.println(TimeTracker.getReport("sessionId={}", System.currentTimeMillis()));
     *
     */
    public static void begin() {
        if(forbidLight()) return;

        try {
            Container.clear();
            Container.begin();

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }
    }

    /**
     * 打点
     *
     * eg:
     * TimeTracker.step("queryId");
     *
     * @param step 步骤名
     */
    public static void step(String step) {
        if(forbid()) return;

        try {
            if(StringUtill.isBlank(step)) return;

            // 设置耗时：追加到对应父节点上
            TrackNode.newBuilder()
                    .step(step)
                    .cost(Container.costThenRestart())
                    .parent(Container.currentMethod())
                    .buld();

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }
    }

    /**
     * 打点
     *
     * eg:
     * TimeTracker.step("queryObj", "ids={}, type={}", ids, type);
     *
     * @param step       步骤名
     * @param formatDesc 步骤更多描述，支持{}格式化占位符
     * @param valueDesc  替换{}占位符的真实值
     */
    public static void step(String step, String formatDesc, Object... valueDesc) {
        if(forbid()) return;

        try {
            if(StringUtill.isAnyBlank(step, formatDesc)) return ;

            // 设置耗时：追加到对应父节点上
            TrackNode.newBuilder()
                    .step(step)
                    .cost(Container.costThenRestart())
                    .parent(Container.currentMethod())
                    .stepDesc(StringUtill.format(formatDesc, valueDesc))
                    .buld();

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }
    }


    /**
     * 当前函数体结束：有begin()的函数，需执行end();
     */
    public static void end() {
        if(forbid()) return;

        try {
            Container.end();

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }
    }





    //============================================ 可选API ================================================

    /**
     *
     * @param config 自定义"慢操作"级别、火焰图等
     */
    public static void config(TrackConfig config) {
        if(forbidLight()) return;

        try {
            Container.config(config);

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }
    }

    /**
     * 跟踪器，在内部函数入口调用
     * 内部函数：指的是在该函数体内，会调用一到多个内部也需要计时的别的函数
     *
     * eg:
     * TimeTracker.begin("queryId");   // "一级"内部函数
     *
     * TimeTracker.begin("queryId.queryIdFromMongo");   // "二级"内部函数
     *
     */
    public static void begin(String method) {
        if(forbid()) return;

        try {
            Container.beginInnerMethod(method, null, null);

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }
    }

    /**
     * 跟踪器，在内部函数入口调用
     * 内部函数：指的是在该函数体内，会调用一到多个内部也需要计时的别的函数
     *
     */
    public static void begin(String method, String formatDesc, Object... valueDesc) {
        if(forbid()) return;

        try {
            Container.beginInnerMethod(method, formatDesc, valueDesc);

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }
    }


    /**
     * 获取追踪报告
     * @return node
     */
    public static TrackReport getReport() {
        if(forbid()) return TrackReport.newNull();

        TrackReport report = null;
        try {
            report = new TrackReport(Container.getReportTree(), Container.getConfig());
            clear();

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }

        return report;
    }

    /**
     * 获取追踪报告：json格式的字符串
     *
     * eg:
     * TimeTracker.getReport("sessionId={}", System.currentTimeMillis())
     *
     * @param reportDesc 本次接口描述
     * @param value      值
     * @return           json
     */
    public static TrackReport getReport(String reportDesc, Object... value) {
        if(forbid()) return TrackReport.newNull();

        TrackReport report = null;
        try {
            report = getReport();
            if(null==report || null==report.getReport()) {
                clear();
                return null;
            }

            report.getReport().stepDesc("slowLevel=" + report.getReport().innerGetSlowLevel() + "; " + StringUtill.format(reportDesc, value));
            clear();

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }

        return report;
    }

    /**
     * 结束追踪器：清理相关数据
     */
    public static void clear() {
        if(forbid()) return;

        try {
            Container.clearAll();

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }
    }

    /**
     * 重置计时器：重新开始计时，为了更准确的记录"step"的耗时
     * 此函数为可选，由于每次调用step()函数会自动重置计时器
     *
     * eg:
     *         Thread.sleep(400L);
     *         TimeTracker.resetWatch();
     *         ids = addRec(ids);
     *         TimeTracker.step("addRec");
     */
    public static void resetWatch() {
        if(forbid()) return;

        try {
            Container.resetWatch();

        } catch (Throwable e) {
            TrackExceptionHandler.timeTracker(e);
        }
    }





    //============================================ 特殊或工具代码 ==========================================


    /**
     * 全面判断
     * @return bool
     */
    private static boolean forbid() {
        return forbidLight() || Container.notBegin();
    }

    /**
     * 轻量级判断
     * @return bool
     */
    private static boolean forbidLight() {
        return TrackConfig.forbidByError || TrackConfig.forbid;
    }

    /**
     *
     * @param config 配置
     * @return slowLevel
     */
    protected static List<Integer> getSlowLevel(TrackConfig config) {
        List<Integer> slowLevel = TrackConfig.DEFAULT_SLOW_LEVEL;
        if(null!=config && null!=config.getSlowLevel() && config.getSlowLevel().size()>0) {
            slowLevel = config.getSlowLevel();
        }

        return slowLevel;
    }





    //============================================ 内部类 ==========================================


    /**
     * 线程容器
     *
     * 具体内部实现：
     *
     * 存储：
     * 采用三个ThreadLocal变量，分别存储每个方法的计时器、当前方法、自定义的慢操作级别
     *
     * 清理：
     * 1，每次调用结果（耗时追踪报表）函数，如getReport()函数时会自动清理本地线程对象
     * 2，或者手工调用clear()函数，清理线程对象
     *
     */
    private static class Container {

        private static final ThreadLocal<Map<TrackNode, TrackWatch>> method2Watch = new ThreadLocal<>();
        private static final ThreadLocal<TrackNode> currentMethod = new ThreadLocal<>();
        private static final ThreadLocal<TrackConfig> config = new ThreadLocal<>();


        /**
         * 配置
         * @param config 配置
         */
        static void config(TrackConfig config) {
            if(null != config) {
                Container.config.set(config);
            }
        }

        /**
         * begin
         */
        static void begin() {
            TrackNode main = TrackNode.newBuilder().step(STEP_MAIN).stepDesc("").buld();
            Container.currentMethod.set(main);

            Map<TrackNode, TrackWatch> ps2W = new HashMap<>();
            TrackWatch watch = TrackWatch.start();
            ps2W.put(main, watch);
            Container.method2Watch.set(ps2W);
        }

        /**
         *
         * @param method "方法名"标示
         */
        static void beginInnerMethod(String method, String formatDesc, Object... valueDesc) {
            if(notBegin()) return ;

            TrackNode methodNew = TrackNode.newBuilder()
                    .step(method)
                    .stepDesc(StringUtill.format(formatDesc, valueDesc))
                    .parent(Container.currentMethod.get())
                    .buld();

            Container.currentMethod.set(methodNew);
            Container.method2Watch.get().put(methodNew, TrackWatch.start());
        }

        /**
         * 执行end表示：当前方法执行结束，需要回退到上一级主调函数，即更新 Container.currentMethod
         */
        static void end() {
            if(notBegin()) return ;

            TrackNode curMethod = currentMethod();
            TrackWatch watch = currentWatch();

            // 退回到最顶层方法的时候，就不再回退了
            if(null==curMethod || STEP_MAIN.equals(curMethod.getStep())) {
                return;
            }

            if(null != watch) {
                curMethod.cost(watch.totalCost());
            }
            Container.currentMethod.set(curMethod.innerGetParent());
            // 退回上一层需要执行一次重置计时
            resetWatch();
        }

        /**
         *
         * @return 耗时
         */
        static long costThenRestart() {
            TrackWatch watch = Container.currentWatch();
            if(null == watch) {
                return -1;   // 错误处理
            }

            long cost = watch.cost();
            watch.reStart();

            return cost;
        }

        /**
         *
         * @return 当前函数计数器
         */
        private static TrackWatch currentWatch() {
            if(notBegin()) return null;

            return Container.method2Watch.get().get(Container.currentMethod.get());
        }

        /**
         *
         * @return 当前函数
         */
        static TrackNode currentMethod() {
            return checkNull(Container.currentMethod) ? null : Container.currentMethod.get();
        }

        /**
         * 重新计时
         */
        static void resetWatch() {
            if(notBegin()) return;

            TrackWatch watch = Container.currentWatch();
            if(null != watch) {
                watch.reStart();
            }
        }

        /**
         * 可以重复调用
         */
        static void clear() {
            if(null != method2Watch.get()) {
                method2Watch.get().clear();
                method2Watch.remove();
            }

            currentMethod.remove();
        }

        /**
         *
         */
        static void clearAll() {
            clear();
            config.remove();
        }

        /**
         * 获取当前配置
         * @return 配置
         */
        static TrackConfig getConfig() {
            return checkNull(Container.config) ? null : Container.config.get();
        }

        /**
         *
         * @return 串起来各node
         */
        static TrackNode getReportTree() {
            Container.assertNull(Container.currentMethod, Container.method2Watch);
            TrackWatch watchMain = Container.currentWatch();
            if(null == watchMain) {
                throw new RuntimeException("[TimeTracker] can not find main watch");
            }

            // 1，设置root节点
            // 1.1，判断慢操作级别
            long mainCost = watchMain.totalCost();
            int slowLevel = 0;
            List<Integer> slowLevelConfig = TimeTracker.getSlowLevel(Container.getConfig());
            for(int i=slowLevelConfig.size()-1; i>=0; i--) {
                if(mainCost >= slowLevelConfig.get(i)) {
                    slowLevel = slowLevelConfig.get(i);
                    break;
                }
            }

            // 1.2 更新耗时和描述
            TrackNode main = Container.currentMethod.get();
            main.cost(mainCost).slowLevel(slowLevel);

            return main;
        }

        /**
         *
         * @param threadLocal 变量
         */
        private static void assertNull(ThreadLocal... threadLocal) {
            for(ThreadLocal tl : threadLocal) {
                if(null == tl.get()) {
                    throw new RuntimeException("[TimeTracker] your action use a null obj; please invoke TimeTracker.begin() before your action, or, your has invoked TimeTracker.getReport()/clear()");
                }
            }
        }

        /**
         *
         * @return
         */
        private static boolean notBegin() {
            return checkNull(Container.currentMethod, Container.method2Watch);
        }

        /**
         *
         * @param threadLocal 变量
         * @return bool
         */
        private static boolean checkNull(ThreadLocal... threadLocal) {
            for(ThreadLocal tl : threadLocal) {
                if(null == tl.get()) {
                    return true;
                }
            }

            return false;
        }

    }

    /**
     * 片段节点
     *
     * @author zyb
     * @Date 2018/6/20 上午11:04
     **/
    protected static class TrackNode {

        private String step;                  // 必选：当前步骤名
        private long cost;                    // 必选：当前步骤耗时
        private String stepDesc;              // 可选：当前步骤附加描述；root节点会系统自动置顶slowLevel属性到描述里
        private List<TrackNode> children;     // 可选：当前步骤的子步骤（子函数）

        private TrackNode parent;             // 不做输出：父亲，只用于内部处理
        private Integer slowLevel;            // 不做输出：慢操作级别，只用于内部处理

        private static final long NULL_COST = -99;  // 容错

        private TrackNode() {}


        /**
         *
         * @return 构造器
         */
        static TrackNode newBuilder() {
            return new TrackNode();
        }

        /**
         *
         * @return null
         */
        static TrackNode newNull() {
            return TrackNode.newBuilder().cost(NULL_COST).buld();
        }

        /**
         *
         * @return this
         */
        TrackNode buld() {
            return this;
        }



        //========================================== Setter ==============================================

        /**
         *
         * @param step 步骤名
         * @return this
         */
        TrackNode step(String step) {
            this.step = step;
            return this;
        }
        /**
         *
         * @param cost 耗时
         * @return this
         */
        TrackNode cost(long cost) {
            this.cost = cost;
            return this;
        }
        /**
         *
         * @param stepDesc 描述
         * @return this
         */
        TrackNode stepDesc(String stepDesc) {
            this.stepDesc = stepDesc;
            return this;
        }
        /**
         *
         * @param children 孩子
         * @return this
         */
        TrackNode children(List<TrackNode> children) {
            this.children = children;
            return this;
        }
        /**
         *
         * @param child 孩子
         * @return this
         */
        TrackNode addChild(TrackNode child) {
            if(null == this.children) {
                this.children = new ArrayList<>();
            }
            this.children.add(child);
            return this;
        }
        /**
         *
         * @param parent 父母
         * @return this
         */
        TrackNode parent(TrackNode parent) {
            this.parent = parent;
            if(null != parent) {
                parent.addChild(this);
            }
            return this;
        }
        /**
         *
         * @param slowLevel 慢操作级别
         * @return this
         */
        TrackNode slowLevel(int slowLevel) {
            this.slowLevel = slowLevel;
            return this;
        }


        //========================================== Getter ==============================================

        public String getStep() {
            return step;
        }
        public long getCost() {
            return cost;
        }
        public String getStepDesc() {
            return stepDesc;
        }
        public List<TrackNode> getChildren() {
            return children;
        }

        public TrackNode innerGetParent() {
            return parent;
        }
        public Integer innerGetSlowLevel() {
            return slowLevel;
        }

        /**
         *
         * @return toString
         */
        @Override
        public String toString() {
            return this.toJson();
        }

        /**
         *
         * @return toJson
         */
        String toJson() {
            return iAmNull() ? "" :JSONObject.toJSONString(this);
        }

        /**
         *
         * @return 是否为空
         */
        protected boolean iAmNull() {
            return NULL_COST == this.cost;
        }

    }

    /**
     * 追踪器本身异常，处理类
     * 降低侵入性，追踪器本身或使用有误造成异常，则停止追踪器
     *
     * @author zyb
     * @Date 2018/6/26 下午12:12
     **/
    public static class TrackExceptionHandler {

        private static final AtomicInteger error_times_tt = new AtomicInteger(0);
        private static final AtomicInteger error_times_tr = new AtomicInteger(0);

        private static final int ERROR_TIMES_MAX_TT = 20;
        private static final int ERROR_TIMES_MAX_TR = 20;

        /**
         * 异常出现多次 ? 关闭"相关系统"开关 : 输出异常日志并记录异常发生次数
         *
         * @param e 异常
         */
        public static void timeTracker(Throwable e) {
            if(error_times_tt.get() > ERROR_TIMES_MAX_TT) {
                TrackConfig._setForbidByError(true);
            }else {
                int errorTimes = error_times_tt.incrementAndGet();
                logger.error("type=timeTracker, errorTimes=" + errorTimes, e);
            }
        }

        /**
         * 异常出现多次 ? 关闭"相关系统"开关 : 输出异常日志并记录异常发生次数
         *
         * @param e 异常
         */
        public static void trackReporter(Throwable e) {
            if(error_times_tr.get() > ERROR_TIMES_MAX_TR) {
                TrackReporter._setForbid(true);
            }else {
                int errorTimes = error_times_tr.incrementAndGet();
                logger.error("type=trackReporter, errorTimes=" + errorTimes, e);
            }
        }
    }

}
