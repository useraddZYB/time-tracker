package com.yidian.serving.index.track.util;

import com.yidian.serving.index.track.TimeTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * 个性化配置
 *
 * @author zyb
 * @Date 2018/6/28 下午7:38
 **/
public class TrackConfig {
    private static final Logger logger = LoggerFactory.getLogger(TrackConfig.class);

    private List<Integer> slowLevel;
    private Set<String> hotStep;          // 尽量别重名，重名的话默认只打印一个
    private Integer minCost;              // 耗时>=minCost ? 打印耗时追踪栈明细 : 不打印耗时追踪栈

    // 默认的"慢操作"级别
    public static final List<Integer> DEFAULT_SLOW_LEVEL = new ArrayList<>(Arrays.asList(
            0, 10, 30, 50, 100, 200, 300, 500, 800, 1000, 1500, 2000, 4000)
    );
    public static boolean forbid        = false;   // 1，对外开关；2，对内使用：减少对业务代码的性能风险
    public static boolean forbidByError = false;   // 对内使用


    /** construct */
    private TrackConfig() {}

    /**
     * 构造器
     * @return this
     */
    public static TrackConfig newBuilder() {
        return new TrackConfig();
    }

    /**
     * 自定义慢操作，会覆盖默认值 TimeTracker.DEFAULT_SLOW_LEVEL
     *
     * @param slowLevel 慢操作
     * @return this
     */
    public TrackConfig slowLevel(Integer... slowLevel) {
        this.slowLevel = new ArrayList<>(Arrays.asList(slowLevel));
        return this;
    }

    /**
     * 定义：需要统计的慢操作步骤名
     *
     * @param hotStep 步骤名
     * @return this
     */
    public TrackConfig hotStep(String... hotStep) {
        this.hotStep = new HashSet<>(Arrays.asList(hotStep));
        return this;
    }

    /**
     * 耗时>=minCost ? 打印耗时追踪栈明细 : 不打印耗时追踪栈
     *
     * @param minCost 耗时>=minCost ? 打印耗时追踪栈明细 : 不打印耗时追踪栈
     * @return this
     */
    public TrackConfig minCost(int minCost) {
        this.minCost = minCost;
        return this;
    }

    /**
     * new
     * @return this
     */
    public TrackConfig build() {
        return this;
    }


    public List<Integer> getSlowLevel() {
        return slowLevel;
    }
    public Set<String> getHotStep() {
        return hotStep;
    }
    public Integer getMinCost() {
        return minCost;
    }

    /**
     * 全局开关设置：如果追踪器有性能问题，则可以关闭
     * @param forbid 开关
     */
    private static void _setForbid(boolean forbid) {
        TrackConfig.forbid = forbid;
    }

    /**
     * 内部开关
     * @param forbidByError 开关
     */
    public static void _setForbidByError(boolean forbidByError) {
        TrackConfig.forbidByError = forbidByError;
    }

    /**
     * toString
     * @return str
     */
    @Override
    public String toString() {
        return "TrackConfig{" +
                "slowLevel=" + slowLevel +
                ", hotStep=" + hotStep +
                ", minCost=" + minCost +
                '}';
    }


    private static volatile boolean initAuto = false;
    /**
     * 定时更新全局开关
     *
     * @param periodSecond 刷新
     * @param callable     任务内容
     */
    public static void _autoSwitchForbid(int periodSecond, Callable<Boolean> callable) {
        if(initAuto) return;

        ScheduledExecutorService sPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private int count;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r,  "TrackConfig-Thread-" + (++count));
                t.setDaemon(true);
                return t;
            }
        });

        sPool.scheduleAtFixedRate(() -> {
            try {
                Boolean forbidConfig = callable.call();
                if(null!=forbidConfig && forbid!=forbidConfig.booleanValue()) {
                    forbid = forbidConfig.booleanValue();
                    logger.info("autoSwitchForbid success, change forbid to be = {}", forbid);
                }
            } catch (Exception e) {
                TimeTracker.TrackExceptionHandler.timeTracker(e);
            }
        }, 20, periodSecond, TimeUnit.SECONDS);

        initAuto = true;
    }



}
