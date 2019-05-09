package com.yidian.serving.index.track;

import com.yidian.serving.index.track.util.TrackConfig;

import java.util.ArrayList;
import java.util.List;

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
