package com.yidian.serving.index.track;

import com.yidian.serving.index.track.util.TrackConfig;
import com.yidian.serving.index.track.util.StringUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * "性能"追踪：原始追踪数据打印器
 *
 * @author zyb
 * @Date 2018/6/26 上午9:57
 **/
public class TrackReporter {
    private static final Logger logger = LoggerFactory.getLogger(TrackReporter.class);


    private static final String PRE_ACS  = "a-c-s";    // 耗时：tp99 slowLevel   api-cost-slowLevel
    private static final String PRE_ASCS = "as-c-s";   // 耗时：火焰图 热点代码片段 api-step-cost-slowLevel

    private static boolean forbid = false;  // 内部使用：减少对业务代码的性能风险


    /**
     * 打印 "全量" || "满足最小耗时条件" 的追踪明细 + 打印"基础"统计所需原始数据
     *
     * @param api    接口名，顶层函数名标记
     * @param report TimeTracker.getReport...()
     */
    public static void logReportStat(String api, TrackReport report) {
        logReportStat(api, report, true, false);
    }

    /**
     * 打印"全量" || "满足最小耗时条件" 的追踪明细 + 打印"自定义"统计所需原始数据
     *
     * @param api       接口名，顶层函数名标记
     * @param report    TimeTracker.getReport...()
     * @param tp99      tp99，自定义是否打印tp99所需数据
     * @param hotStep   热点代码片段，自定义是否
     */
    public static void logReportStat(String api, TrackReport report, boolean tp99, boolean hotStep) {
        if(forbid) return;

        try {
            if(illegal(api, report)) return;

            // 热点代码，需要借助TP99的日志，做总请求条数的计算；且tp99和slowLevel只有一行短的日志，比hotStep日志量少很多，性能损失可以忽略
            if(hotStep) {
                tp99 = true;
            }

            // 1，打印report
            logReport(api, report);

            // 2，打印tp99及slowLevel
            if(tp99) {
                logger.debug(PRE_ACS + ";{};{};{}", api, report.getReport().getCost(), report.getReport().innerGetSlowLevel());
            }

            // 3，打印hotStep (热点片段 / 火焰图)
            boolean configHotStep = (null!=report.getConfig() && null!=report.getConfig().getHotStep()
                    && report.getConfig().getHotStep().size()>0);

            if(hotStep && configHotStep) {
                TrackReporter.hotStep(api, report.getReport(), report.getConfig(), TimeTracker.getSlowLevel(report.getConfig()));
            }

        } catch (Throwable e) {
            TimeTracker.TrackExceptionHandler.trackReporter(e);
        }
    }

    /**
     * 打印 "全量" || "满足最小耗时条件" 的追踪明细
     * 不打印统计数据
     *
     * @param api     接口名，顶层函数名标记
     * @param report  TimeTracker.getReport...()
     */
    public static void logReport(String api, TrackReport report) {
        if(forbid) return;

        try {
            if(illegal(api, report)) return;

            if(null==report.getConfig() || null==report.getConfig().getMinCost()
                    || report.getReport().getCost()>=report.getConfig().getMinCost()) {

                logger.debug("api={}, report={}", api, report.toJson());
            }

        } catch (Throwable e) {
            TimeTracker.TrackExceptionHandler.trackReporter(e);
        }
    }



    /**
     * 内部使用
     * 全局开关设置：如果追踪上报器本身或函数有问题（多次异常等），则内部自动关闭上报器
     *
     * @param forbid 开关
     */
    protected static void _setForbid(boolean forbid) {
        TrackReporter.forbid = forbid;
    }


    //================================================== tools =======================================================



    /**
     * 校验
     *
     * @param api    接口名，顶层函数名标记
     * @param report TimeTracker.getReport...()
     * @return       校验失败 ? true : false
     */
    private static boolean illegal(String api, TrackReport report) {
        return StringUtill.isBlank(api) || null==report
                || null==report.getReport() || report.getReport().iAmNull();
    }

    /**
     * "火焰图"统计，所需原始数据
     *  打印叶子节点
     *
     * @param api    接口名，顶层函数名标记
     * @param node   TimeTracker.getReport...()，递归对象
     * @param config 配置
     */
    private static void hotStep(String api, TimeTracker.TrackNode node, TrackConfig config,
                                List<Integer> slowLevelConfig) {

        List<TimeTracker.TrackNode> children = node.getChildren();
        // 没有孩子的节点，就是叶子，打印
        if(null==children || children.size()==0) {
            if(config.getHotStep().contains(node.getStep())) {
                logHotStep(api, node, slowLevelConfig);
            }
        }
        // 有孩子，就是父亲，继续往下递归找叶子
        else {
            for(TimeTracker.TrackNode nodeNew : children) {
                if(config.getHotStep().contains(node.getStep())) {
                    logHotStep(api, node, slowLevelConfig);
                }

                hotStep(api, nodeNew, config, slowLevelConfig);
            }
        }
    }

    /**
     *
     * @param api
     * @param node
     * @param slowLevelConfig
     */
    private static void logHotStep(String api, TimeTracker.TrackNode node, List<Integer> slowLevelConfig) {
        int slowLevel = 0;
        for(int i=slowLevelConfig.size()-1; i>=0; i--) {
            if(node.getCost() >= slowLevelConfig.get(i)) {
                slowLevel = slowLevelConfig.get(i);
                break;
            }
        }
        logger.debug(PRE_ASCS + ";{};{};{}", api+"-"+node.getStep(), node.getCost(), slowLevel);
    }


}
