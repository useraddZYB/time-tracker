package com.yidian.serving.index.track;

import com.yidian.serving.index.track.util.TrackConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zyb
 * @Date 2018/6/20 下午4:05
 **/
public class TestTimeTracker {

    /**
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        /*TrackConfig._autoSwitchForbid(1, () -> true);
        Thread.sleep(21000L);

        for(int i=0; i<5; i++) {
            Thread.sleep(2000);
            System.out.println("i get forbid=" + TrackConfig.forbid);
        }

        if(1==1) {
            return;
        }*/

        TimeTracker.config(TrackConfig.newBuilder().minCost(5).hotStep("queryIdFromRedis").slowLevel(0, 500, 1000).build());
        TimeTracker.begin();   // *** 启动

        List<Long> ids = queryId(); // *** 内部有详细打点

        String type = "news";
        queryObj(ids, type);
        TimeTracker.step("queryObj", "ids={}, type={}", ids, type); // *** 附带备注

        // 模拟 filter 逻辑
        Thread.sleep(50);
        TimeTracker.step("filter"); // *** 简单的片段计时

        // *** 打印报告
        TimeTracker.end();
        TrackReport report = TimeTracker.getReport("sessionId={}", System.currentTimeMillis());
        System.out.println(report);
        TrackReporter.logReportStat("channelServlet", report);
    }
    /**
     * queryId系列方法
     *
     * @return
     * @throws Exception
     */
    private static List<Long> queryId() throws Exception {
        TimeTracker.begin("queryId");   // *** 函数内部需要分段计时的话，函数也需要begin(name)

        List<Long> ids = null;
        ids = queryIdFromRedis();
        TimeTracker.step("queryIdFromRedis");
        if(null == ids) {
            ids = queryIdFromMongo();
        }

        TimeTracker.end();   // *** 与begin()配套
        return ids;
    }
    private static List<Long> queryIdFromRedis() throws Exception {
        Thread.sleep(200L);
        return null;
    }
    private static List<Long> queryIdFromMongo() throws Exception {
        /*TimeTracker.begin("queryIdFromMongo");

        Thread.sleep(300L);
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(2L);
        ids.add(3L);
        TimeTracker.step("newIds");

        Thread.sleep(400L);
        TimeTracker.resetWatch();
        ids = addRec(ids);

        TimeTracker.end();*/

        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(2L);
        ids.add(3L);
        ids = addRec(ids);

        return ids;
    }
    private static List<Long> addRec(List<Long> ids) throws Exception {
        TimeTracker.begin("addRec", "ids={}", ids);

        ids.add(9L);
        Thread.sleep(200L);
        TimeTracker.step("addRec1");

        Thread.sleep(400L);
        TimeTracker.step("addRec2");

        TimeTracker.end();
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
