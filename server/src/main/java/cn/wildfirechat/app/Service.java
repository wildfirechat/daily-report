package cn.wildfirechat.app;

import cn.wildfirechat.app.pojo.PojoReport;

import javax.servlet.http.HttpServletResponse;

public interface Service {
    RestResult login(HttpServletResponse response, String account, String password);
    RestResult clientLogin(HttpServletResponse response, String appId, int appType, String authcode);
    RestResult updatePassword(String oldPassword, String newPassword);
    RestResult logout();
    RestResult getAccount();
    RestResult favApplication(String targetId);
    RestResult createReport(PojoReport report);
    RestResult deleteReport(long day);
    RestResult getReport(String userId, long day);
    RestResult getReportList(int count, int offset);
    RestResult getReportPage(int count, int offset, String keyword, Long startDay, Long endDay);
    RestResult getReceivedReports(int count, int offset, String keyword, Long startDay, Long endDay);
    RestResult getReportFeed(int count, String keyword, Long startDay, Long endDay, Long cursorDay);
    RestResult getReceivedFeed(int count, String keyword, Long startDay, Long endDay, Long cursorDay, Long cursorUpdatedAt, String cursorUserId);
    RestResult getReportStats();
    RestResult getConfigData();
}
