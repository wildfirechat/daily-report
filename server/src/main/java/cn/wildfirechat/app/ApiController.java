package cn.wildfirechat.app;

import cn.wildfirechat.app.pojo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RequestMapping(value = "/api/")
@RestController
public class ApiController {
    private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);
    @Autowired
    private Service mService;

    /*
    管理后台登陆
     */
    @PostMapping(value = "login", produces = "application/json;charset=UTF-8")
    public Object login(@RequestBody LoginRequest request, HttpServletResponse response) {
        return mService.login(response, request.getAccount(), request.getPassword());
    }

    /*
    管理后台修改密码
     */
    @PostMapping(value = "update_pwd", produces = "application/json;charset=UTF-8")
    public Object updatePwd(@RequestBody UpdatePasswordRequest request) {
        return mService.updatePassword(request.oldPassword, request.newPassword);
    }

    /*
    获取当前用户ID，管理后台和客户端都可以使用
     */
    @GetMapping(value = "account", produces = "application/json;charset=UTF-8")
    public Object getAccount() {
        return mService.getAccount();
    }

    /*
    客户端登陆
     */
    @PostMapping(value = "user/login", produces = "application/json;charset=UTF-8")
    public Object userLogin(@RequestBody ClientLoginRequest request, HttpServletResponse response) {
        return mService.clientLogin(response, request.getAppId(), request.getAppType(), request.getAuthCode());
    }

    /*
    创建报告
     */
    @PutMapping(value = "user/report", produces = "application/json;charset=UTF-8")
    public Object createReport(@RequestBody PojoReport report) {
        return mService.createReport(report);
    }

    /*
    删除报告
     */
    @GetMapping(value = "user/report/{day}", produces = "application/json;charset=UTF-8")
    public Object getReport(@PathVariable("day") long day) {
        return mService.getReport(day);
    }

    /*
    删除报告
     */
    @DeleteMapping(value = "user/report/{day}", produces = "application/json;charset=UTF-8")
    public Object deleteReport(@PathVariable("day") long day) {
        return mService.deleteReport(day);
    }

    /*
    获取当前用户的报告列表
     */
    @GetMapping(value = "user/report_list", produces = "application/json;charset=UTF-8")
    public Object getReportList(@RequestBody PojoCountOffset countOffset) {
        return mService.getReportList(countOffset.count, countOffset.offset);
    }

    /*
    获取config的信息
     */
    @GetMapping(value = "user/config", produces = "application/json;charset=UTF-8")
    public Object getConfigData() {
        return mService.getConfigData();
    }
}
