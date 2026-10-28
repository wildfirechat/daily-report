package cn.wildfirechat.app;

import cn.wildfirechat.app.pojo.ClientLoginRequest;
import cn.wildfirechat.app.pojo.LoginRequest;
import cn.wildfirechat.app.pojo.PojoReport;
import cn.wildfirechat.app.pojo.UpdatePasswordRequest;
import cn.wildfirechat.app.tools.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static cn.wildfirechat.app.RestResult.RestCode.ERROR_INVALID_PARAMETER;
import static cn.wildfirechat.app.RestResult.RestCode.ERROR_REQUEST_TOO_FREQUENT;

@RequestMapping(value = "/api/")
@RestController
public class ApiController {
    private static final int MAX_ACCOUNT_LENGTH = 64;
    private static final int MAX_PASSWORD_LENGTH = 256;
    private static final int MAX_AUTH_CODE_LENGTH = 512;

    private final RateLimiter loginIpLimiter = new RateLimiter(60, 30);
    private final RateLimiter loginAccountLimiter = new RateLimiter(300, 10);
    private final RateLimiter authCodeIpLimiter = new RateLimiter(60, 30);
    private final RateLimiter configIpLimiter = new RateLimiter(60, 60);

    @Autowired
    private Service mService;

    @PostMapping(value = "login", produces = "application/json;charset=UTF-8")
    public Object login(@RequestBody LoginRequest request, HttpServletResponse response,
                        HttpServletRequest httpRequest) {
        if (request == null || !validString(request.getAccount(), MAX_ACCOUNT_LENGTH)
            || request.getPassword() == null || request.getPassword().length() > MAX_PASSWORD_LENGTH) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        if (!allowLogin(httpRequest, request.getAccount())) {
            return RestResult.error(ERROR_REQUEST_TOO_FREQUENT);
        }
        return mService.login(response, request.getAccount(), request.getPassword());
    }

    @PostMapping(value = "update_pwd", produces = "application/json;charset=UTF-8")
    public Object updatePwd(@RequestBody UpdatePasswordRequest request) {
        if (request == null) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        return mService.updatePassword(request.oldPassword, request.newPassword);
    }

    @PostMapping(value = "logout", produces = "application/json;charset=UTF-8")
    public Object logout() {
        return mService.logout();
    }

    @GetMapping(value = "account", produces = "application/json;charset=UTF-8")
    public Object getAccount() {
        return mService.getAccount();
    }

    @PostMapping(value = "user/login", produces = "application/json;charset=UTF-8")
    public Object userLogin(@RequestBody ClientLoginRequest request, HttpServletResponse response,
                            HttpServletRequest httpRequest) {
        if (request == null || !validString(request.getAppId(), MAX_ACCOUNT_LENGTH)
            || !validString(request.getAuthCode(), MAX_AUTH_CODE_LENGTH)) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        if (!authCodeIpLimiter.isGranted(clientIp(httpRequest))) {
            return RestResult.error(ERROR_REQUEST_TOO_FREQUENT);
        }
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
    public Object getConfigData(HttpServletRequest request) {
        if (!configIpLimiter.isGranted(clientIp(request))) {
            return RestResult.error(ERROR_REQUEST_TOO_FREQUENT);
        }
        return mService.getConfigData();
    }

    private boolean allowLogin(HttpServletRequest request, String account) {
        String ip = clientIp(request);
        return loginIpLimiter.isGranted(ip)
            && loginAccountLimiter.isGranted(ip + ":" + account.toLowerCase());
    }

    private String clientIp(HttpServletRequest request) {
        return request == null || request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private boolean validString(String value, int maxLength) {
        return StringUtils.hasText(value) && value.length() <= maxLength;
    }
}
