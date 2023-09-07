package com.programmerartist.timetracker;

import com.programmerartist.timetracker.util.TrackConfig;

/**
 * 耗时分布报告
 *
 * @author 程序员Artist
 * @Date 2018/6/29 上午10:37
 **/
public class TrackReport {

    private TimeTracker.TrackNode report;
    private TrackConfig config;

    /**
     * 构造
     */
    public TrackReport() {
    }
    public TrackReport(TimeTracker.TrackNode report, TrackConfig config) {
        this.report = report;
        this.config = config;
    }

    public TimeTracker.TrackNode getReport() {
        return report;
    }
    public TrackConfig getConfig() {
        return config;
    }

    /**
     * 内部容错
     *
     * @return 空对象
     */
    protected static TrackReport newNull() {
        return new TrackReport(TimeTracker.TrackNode.newNull(), null);
    }


    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return this.toJson();
    }

    /**
     *
     * @return
     */
    public String toJson() {
        return null!=report ? report.toJson() : "";
    }

}
