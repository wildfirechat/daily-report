package cn.wildfirechat.app;


import cn.wildfirechat.app.pojo.PojoReport;

import javax.servlet.http.HttpServletResponse;

public interface Service {
    RestResult login(HttpServletResponse response, String account, String password);
    RestResult clientLogin(HttpServletResponse response, String appId, int appType, String authcode);
    RestResult updatePassword(String oldPassword, String newPassword);
    RestResult getAccount();
    RestResult favApplication(String targetId);
    RestResult createReport(PojoReport report);
    RestResult deleteReport(long day);
    RestResult getReport(long day);
    RestResult getReportList(int count, int offset);
    RestResult getConfigData();
}
