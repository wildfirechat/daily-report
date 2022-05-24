package cn.wildfirechat.app;


import cn.wildfirechat.app.jpa.*;
import cn.wildfirechat.app.pojo.ConfigData;
import cn.wildfirechat.app.pojo.PojoReport;
import cn.wildfirechat.app.shiro.AuthCodeToken;
import cn.wildfirechat.app.tools.RateLimiter;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.messagecontentbuilder.RichNotificationContentBuilder;
import cn.wildfirechat.pojos.MessagePayload;
import cn.wildfirechat.pojos.OutputApplicationConfigData;
import cn.wildfirechat.pojos.SendMessageResult;
import cn.wildfirechat.sdk.ChannelServiceApi;
import cn.wildfirechat.sdk.RobotService;
import cn.wildfirechat.sdk.model.IMResult;
import com.google.common.base.Joiner;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.h2.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static cn.wildfirechat.app.RestResult.RestCode.*;

@org.springframework.stereotype.Service
public class ServiceImpl implements Service {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);

    @Value("${im.url}")
    private String mImUrl;

    @Value("${application.id}")
    private String mApplicationId;

    @Value("${application.secret}")
    private String mApplicationSecret;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    private ChannelServiceApi mChannelServiceApi;
    private RobotService mRobotService;

    private RateLimiter rateLimiter;

    @PostConstruct
    private void init() {
        mChannelServiceApi = new ChannelServiceApi(mImUrl, mApplicationId, mApplicationSecret);
        mRobotService = new RobotService(mImUrl, mApplicationId, mApplicationSecret);
        rateLimiter = new RateLimiter(60, 200);
    }

    @Bean
    public ChannelServiceApi getChannelServiceApi() {
        return this.mChannelServiceApi;
    }

    public RestResult login(HttpServletResponse httpResponse, String account, String password) {
        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(account, password);

        try {
            subject.login(token);
        } catch (UnknownAccountException uae) {
            return RestResult.error(ERROR_CODE_ACCOUNT_NOT_EXIST);
        } catch (IncorrectCredentialsException ice) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT);
        } catch (LockedAccountException lae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT);
        } catch (ExcessiveAttemptsException eae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT);
        } catch (AuthenticationException ae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT);
        }

        if (subject.isAuthenticated()) {
            long timeout = subject.getSession().getTimeout();
            LOG.info("Login success " + timeout);
        } else {
            token.clear();
            return RestResult.error(RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT);
        }

        Object sessionId = subject.getSession().getId();
        httpResponse.setHeader("authToken", sessionId.toString());

        return RestResult.ok(null);
    }

    @Override
    public RestResult clientLogin(HttpServletResponse httpResponse, String appId, int appType, String authcode) {
        Subject subject = SecurityUtils.getSubject();
        AuthCodeToken token = new AuthCodeToken(appId, appType, authcode);

        try {
            subject.login(token);
        } catch (UnknownAccountException uae) {
            return RestResult.error(ERROR_CODE_ACCOUNT_NOT_EXIST);
        } catch (IncorrectCredentialsException ice) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT);
        } catch (LockedAccountException lae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT);
        } catch (ExcessiveAttemptsException eae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT);
        } catch (AuthenticationException ae) {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT);
        }

        if (subject.isAuthenticated()) {
            long timeout = subject.getSession().getTimeout();
            LOG.info("Login success " + timeout);
        } else {
            return RestResult.error(RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT);
        }

        Object sessionId = subject.getSession().getId();
        httpResponse.setHeader("authToken", sessionId.toString());

        return RestResult.ok(subject.getPrincipal());
    }

    @Override
    public RestResult updatePassword(String oldPassword, String newPassword) {
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            return RestResult.error(ERROR_NOT_LOGIN);
        }
        String account = (String) subject.getPrincipal();
        Optional<User> optionalUser = userRepository.findByAccount(account);
        if (!optionalUser.isPresent()) {
            return RestResult.error(ERROR_CODE_ACCOUNT_NOT_EXIST);
        }

        User user = optionalUser.get();
        String md5 = new Base64().encodeToString(DigestUtils.getDigest("MD5").digest((oldPassword + user.getSalt()).getBytes(StandardCharsets.UTF_8)));
        if (!md5.equals(user.getPasswordMd5())) {
            return RestResult.error(ERROR_CODE_PASSWORD_INCORRECT);
        }

        String newMd5 = new Base64().encodeToString(DigestUtils.getDigest("MD5").digest((newPassword + user.getSalt()).getBytes(StandardCharsets.UTF_8)));
        user.setPasswordMd5(newMd5);
        userRepository.save(user);

        return RestResult.ok(null);
    }

    public String getUserId() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            return (String) subject.getPrincipal();
        }
        return null;
    }
    public String getDisplayName() {
        Subject subject = SecurityUtils.getSubject();
        return (String)subject.getSession().getAttribute("displayName");
    }
    @Override
    public RestResult getAccount() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            return RestResult.ok(subject.getPrincipal());
        }
        return RestResult.error(ERROR_NOT_EXIST);
    }

    @Override
    public RestResult favApplication(String targetId) {
        return RestResult.ok(null);
    }

    @Override
    public RestResult createReport(PojoReport report) {
        report.userId = getUserId();
        report.day = getToday();

        boolean isExist = reportRepository.findById(new UserDayId(report.userId, report.day)).isPresent();
        reportRepository.save(convertReport(report));

        String messageContent;
        String sendName = report.userId;
        String displayName = getDisplayName();
        if(!StringUtils.isNullOrEmpty(displayName)) {
            sendName = displayName;
        }
        if (isExist) {
            messageContent = sendName + " 提交了工作日志，快来看看吧";
        } else {
            messageContent = sendName + " 更新了工作日志，快来看看吧";
        }
        List<String> reportTo = new ArrayList<>(report.reportTo);
        reportTo.add(getUserId());
        RichNotificationContentBuilder builder = RichNotificationContentBuilder.newBuilder("日报提交通知", messageContent, "https://report.wildfirechat.cn/report.html?day=" + report.day)
                .exName("日报小助手")
                .addItem("员工", sendName, "#173177")
                .addItem("标题", report.title, "#173177")
                .addItem("内容摘要", report.content.length() > 40 ? report.content.substring(0, 40)+"..." : report.content, "#173177")
                .addItem("明日计划", report.tomorrowPlan.length() > 40 ? report.tomorrowPlan.substring(0, 40)+"..." : report.tomorrowPlan, "#173177");
        if(!StringUtils.isNullOrEmpty(report.requirement)) {
            builder.addItem("需要协助", report.requirement.length() > 40 ? report.requirement.substring(0, 40)+"..." : report.requirement, "#173177");
        }
        taskExecutor.submit(() -> {
            sendTextMessage(reportTo, builder.build());
            sendTextMessage(report.ccTo, builder.build());
        });

        return RestResult.ok(null);
    }

    @Override
    public RestResult deleteReport(long day) {
        if (day <= 0) {
            day = getToday();
        }
        reportRepository.deleteById(new UserDayId(getUserId(), day));
        return RestResult.ok(null);
    }

    @Override
    public RestResult getReport(long day) {
        if(day <= 0) {
            day = getToday();
        }
        Optional<Report> reportOptional = reportRepository.findById(new UserDayId(getUserId(), day));
        return reportOptional.map(report -> RestResult.ok(convertReport(report))).orElseGet(() -> RestResult.error(ERROR_NOT_EXIST));

    }

    @Override
    public RestResult getReportList(int count, int offset) {
        List<PojoReport> out = new ArrayList<>();
        List<Report> list = reportRepository.getUserReports(getUserId(), count, offset);
        if(list != null) {
            list.forEach(report -> out.add(convertReport(report)));
        }
        return RestResult.ok(out);
    }

    @Override
    public RestResult getConfigData() {
        OutputApplicationConfigData outputApplicationConfigData = mRobotService.getApplicationSignature();
        ConfigData configData = new ConfigData();
        configData.appId = outputApplicationConfigData.getAppId();
        configData.appType = outputApplicationConfigData.getAppType();
        configData.nonceStr = outputApplicationConfigData.getNonceStr();
        configData.timestamp = outputApplicationConfigData.getTimestamp();
        configData.signature = outputApplicationConfigData.getSignature();
        return RestResult.ok(configData);
    }

    private long getToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.clear(Calendar.HOUR);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.SECOND);
        calendar.clear(Calendar.MILLISECOND);
        return calendar.getTimeInMillis();
    }

    private Report convertReport(PojoReport report) {
        Report r = new Report();
        r.userId = report.userId;

        r.day = report.day;
        r.title = report.title;
        r.content = report.content;
        r.tomorrowPlan = report.tomorrowPlan;
        r.requirement = report.requirement;

        if (report.reportTo != null)
            r.reportTo = Joiner.on(",").join(report.reportTo);
        if (report.ccTo != null)
            r.ccTo = Joiner.on(",").join(report.ccTo);

        if (report.reportToGroup != null)
            r.reportToGroup = Joiner.on(",").join(report.reportToGroup);

        if (report.ccToGroup != null)
            r.ccToGroup = Joiner.on(",").join(report.ccToGroup);
        return r;
    }

    private PojoReport convertReport(Report report) {
        PojoReport r = new PojoReport();
        r.userId = report.userId;

        r.day = report.day;
        r.title = report.title;
        r.content = report.content;
        r.tomorrowPlan = report.tomorrowPlan;
        r.requirement = report.requirement;

        if (!StringUtils.isNullOrEmpty(report.reportTo))
            r.reportTo = Arrays.asList(report.reportTo.split(","));

        if (!StringUtils.isNullOrEmpty(report.ccTo))
            r.ccTo = Arrays.asList(report.ccTo.split(","));

        if (!StringUtils.isNullOrEmpty(report.reportToGroup))
            r.reportToGroup = Arrays.asList(report.reportToGroup.split(","));

        if (!StringUtils.isNullOrEmpty(report.ccToGroup))
            r.ccToGroup = Arrays.asList(report.ccToGroup.split(","));
        return r;
    }

    private void sendTextMessage(List<String> toUsers, MessagePayload payload) {
        if (toUsers == null || toUsers.isEmpty()) {
            return;
        }

        try {
            IMResult<SendMessageResult> resultSendMessage = mChannelServiceApi.sendMessage(0, toUsers, payload);
            if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                LOG.info("send message success");
            } else {
                LOG.error("send message error {}", resultSendMessage != null ? resultSendMessage.getErrorCode().code : "unknown");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("send message error {}", e.getLocalizedMessage());
        }

    }
}
