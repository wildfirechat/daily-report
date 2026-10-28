package cn.wildfirechat.app;

import cn.wildfirechat.app.pojo.ClientLoginRequest;
import cn.wildfirechat.app.pojo.LoginRequest;
import cn.wildfirechat.app.pojo.PojoReport;
import cn.wildfirechat.app.pojo.UpdatePasswordRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RequestMapping(value = "/api/")
@RestController
public class ApiController {
    @Autowired
    private Service mService;

    @PostMapping(value = "login", produces = "application/json;charset=UTF-8")
    public Object login(@RequestBody LoginRequest request, HttpServletResponse response) {
        return mService.login(response, request.getAccount(), request.getPassword());
    }

    @PostMapping(value = "update_pwd", produces = "application/json;charset=UTF-8")
    public Object updatePwd(@RequestBody UpdatePasswordRequest request) {
        return mService.updatePassword(request.oldPassword, request.newPassword);
    }

    @GetMapping(value = "account", produces = "application/json;charset=UTF-8")
    public Object getAccount() {
        return mService.getAccount();
    }

    @PostMapping(value = "user/login", produces = "application/json;charset=UTF-8")
    public Object userLogin(@RequestBody ClientLoginRequest request, HttpServletResponse response) {
        return mService.clientLogin(response, request.getAppId(), request.getAppType(), request.getAuthCode());
    }

    @PutMapping(value = "user/report", produces = "application/json;charset=UTF-8")
    public Object createReport(@RequestBody PojoReport report) {
        return mService.createReport(report);
    }

    @GetMapping(value = "user/report/{userId}/{day}", produces = "application/json;charset=UTF-8")
    public Object getReport(@PathVariable("userId") String userId, @PathVariable("day") long day) {
        return mService.getReport(userId, day);
    }

    @DeleteMapping(value = "user/report/{day}", produces = "application/json;charset=UTF-8")
    public Object deleteReport(@PathVariable("day") long day) {
        return mService.deleteReport(day);
    }

    @GetMapping(value = "user/report_list", produces = "application/json;charset=UTF-8")
    public Object getReportList(@RequestParam(defaultValue = "20") int count,
                                @RequestParam(defaultValue = "0") int offset,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Long startDay,
                                @RequestParam(required = false) Long endDay,
                                @RequestParam(defaultValue = "false") boolean paged) {
        if (paged) {
            return mService.getReportPage(count, offset, keyword, startDay, endDay);
        }
        return mService.getReportList(count, offset);
    }

    @GetMapping(value = "user/received_list", produces = "application/json;charset=UTF-8")
    public Object getReceivedReports(@RequestParam(defaultValue = "20") int count,
                                     @RequestParam(defaultValue = "0") int offset,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Long startDay,
                                     @RequestParam(required = false) Long endDay) {
        return mService.getReceivedReports(count, offset, keyword, startDay, endDay);
    }

    @GetMapping(value = "user/report_feed", produces = "application/json;charset=UTF-8")
    public Object getReportFeed(@RequestParam(defaultValue = "10") int count,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Long startDay,
                                @RequestParam(required = false) Long endDay,
                                @RequestParam(required = false) Long cursorDay) {
        return mService.getReportFeed(count, keyword, startDay, endDay, cursorDay);
    }

    @GetMapping(value = "user/received_feed", produces = "application/json;charset=UTF-8")
    public Object getReceivedFeed(@RequestParam(defaultValue = "10") int count,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Long startDay,
                                  @RequestParam(required = false) Long endDay,
                                  @RequestParam(required = false) Long cursorDay,
                                  @RequestParam(required = false) String cursorUserId) {
        return mService.getReceivedFeed(count, keyword, startDay, endDay, cursorDay, cursorUserId);
    }

    @GetMapping(value = "user/report_stats", produces = "application/json;charset=UTF-8")
    public Object getReportStats() {
        return mService.getReportStats();
    }

    @GetMapping(value = "user/config", produces = "application/json;charset=UTF-8")
    public Object getConfigData() {
        return mService.getConfigData();
    }
}
