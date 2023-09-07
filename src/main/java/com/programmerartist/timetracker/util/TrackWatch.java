package com.programmerartist.timetracker.util;

import java.util.concurrent.TimeUnit;

/**
 * 函数分阶段代码 和 函数整体 耗时
 *
 * copy from Metrics Watch：不想新增metrics的依赖，所以拷贝代码过来了
 *
 * 计时器：目标为了替换掉guava Stopwatch工具
 *  *
 *  * 原因一：由于Stopwatch的API设计不太好：
 *  * 1，太啰嗦：无默认TimeUnit
 *  * 2，不好理解：reset stop 分不清
 *  * 3，容易用错：reset stop start 三者搭配很容易用错
 *  *
 *  * 原因二：函数分阶段代码 和 函数整体 耗时，必须要生成至少两个 Stopwatch
 *  *
 *  * 基于上述两原因，导致历史代码出现：
 *  * 一个方法里生成了多个Stopwatch（由于难用怕用错，索性每次重新生成）
 *
 * @Author 程序员Artist
 * @Date 2018/7/19 下午7:51
 *
 **/
public class TrackWatch {

    // 内部实现：用于记录"函数内分阶段程序"耗时
    private long lastTime;
    // 内部实现：用于记录"函数整体"耗时
    private long totalLastTime;

    /**
     * private
     *
     * @param lastTime      代码片段计时器
     * @param totalLastTime 函数计时器
     */
    private TrackWatch(long lastTime, long totalLastTime) {
        this.lastTime = lastTime;
        this.totalLastTime = totalLastTime;
    }

    /**
     * 生成计时器，并开始计时
     *
     * @return this
     */
    public static TrackWatch start() {
        long now = System.currentTimeMillis();
        return new TrackWatch(now, now);
    }

    /**
     * 耗时：毫秒
     *
     * @return 毫秒
     */
    public long cost() {
        return System.currentTimeMillis() - lastTime;
    }

    /**
     * 耗时
     *
     * @param timeUnit 单位
     * @return 耗时
     */
    public long cost(TimeUnit timeUnit) {
        return TimeUnit.MICROSECONDS.convert(System.currentTimeMillis() - lastTime, timeUnit);
    }

    /**
     * 耗时，然后重置计时器：毫秒
     *
     * @return 耗时
     */
    public long costAndReStart() {
        long cost = this.cost();
        this.reStart();
        return cost;
    }

    /**
     * 耗时
     *
     * @param timeUnit 单位
     * @return 耗时
     */
    public long costAndReStart(TimeUnit timeUnit) {
        long cost = this.cost(timeUnit);
        this.reStart();
        return cost;
    }

    /**
     * 重新开始计时
     */
    public void reStart() {
        lastTime = System.currentTimeMillis();
    }

    /**
     * 函数整体耗时：毫秒
     *
     * @return 耗时
     */
    public long totalCost() {
        return System.currentTimeMillis() - totalLastTime;
    }

    /**
     * 函数整体耗时
     *
     * @param timeUnit 单位
     * @return 耗时
     */
    public long totalCost(TimeUnit timeUnit) {
        return TimeUnit.MICROSECONDS.convert(System.currentTimeMillis() - totalLastTime, timeUnit);
    }
}
