package cn.wildfirechat.app;

import cn.wildfirechat.app.jpa.OffsetPageRequest;
import cn.wildfirechat.app.jpa.Report;
import cn.wildfirechat.app.jpa.ReportRepository;
import cn.wildfirechat.app.jpa.ReportRecipient;
import cn.wildfirechat.app.jpa.ReportRecipientRepository;
import cn.wildfirechat.app.jpa.User;
import cn.wildfirechat.app.jpa.UserDayId;
import cn.wildfirechat.app.jpa.UserRepository;
import cn.wildfirechat.app.pojo.ConfigData;
import cn.wildfirechat.app.pojo.PojoReport;
import cn.wildfirechat.app.pojo.PojoReportPage;
import cn.wildfirechat.app.pojo.PojoReportStats;
import cn.wildfirechat.app.shiro.AuthCodeToken;
import cn.wildfirechat.app.shiro.DBSessionDao;
import cn.wildfirechat.app.shiro.PasswordHasher;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.messagecontentbuilder.RichNotificationContentBuilder;
import cn.wildfirechat.pojos.MessagePayload;
import cn.wildfirechat.pojos.OutputApplicationConfigData;
import cn.wildfirechat.pojos.SendMessageResult;
import cn.wildfirechat.sdk.ChannelServiceApi;
import cn.wildfirechat.sdk.RobotService;
import cn.wildfirechat.sdk.model.IMResult;
import com.google.common.base.Joiner;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static cn.wildfirechat.app.RestResult.RestCode.ERROR_CODE_ACCOUNT_NOT_EXIST;
import static cn.wildfirechat.app.RestResult.RestCode.ERROR_CODE_PASSWORD_INCORRECT;
import static cn.wildfirechat.app.RestResult.RestCode.ERROR_INVALID_PARAMETER;
import static cn.wildfirechat.app.RestResult.RestCode.ERROR_NOT_EXIST;
import static cn.wildfirechat.app.RestResult.RestCode.ERROR_NOT_LOGIN;
import static cn.wildfirechat.app.RestResult.RestCode.ERROR_NO_RIGHT;

@org.springframework.stereotype.Service
public class ServiceImpl implements Service {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_OFFSET = 10000;
    private static final int MAX_RECIPIENTS = 20;
    private static final int MAX_TEXT_LENGTH = 2048;
    private static final int MAX_USER_ID_LENGTH = 64;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72;

    @Value("${im.url}")
    private String mImUrl;

    @Value("${application.id}")
    private String mApplicationId;

    @Value("${application.secret}")
    private String mApplicationSecret;

    @Value("${report.base-url:https://report.wildfirechat.cn}")
    private String mReportBaseUrl;

    @Value("${report.notification-enabled:true}")
    private boolean mNotificationEnabled;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportRecipientRepository reportRecipientRepository;

    @Autowired
    private DBSessionDao dbSessionDao;

    @Autowired
    @Qualifier("reportNotificationExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    private ChannelServiceApi mChannelServiceApi;
    private RobotService mRobotService;

    @PostConstruct
    private void init() {
        mChannelServiceApi = new ChannelServiceApi(mImUrl, mApplicationId, mApplicationSecret);
        mRobotService = new RobotService(mImUrl, mApplicationId, mApplicationSecret);
        configureSdkHttpTimeouts();
    }

    /**
     * The bundled SDK creates an Apache HttpClient without connect/socket timeouts. Replace it
     * with a bounded client so an unavailable IM server cannot pin request threads indefinitely.
     */
    private void configureSdkHttpTimeouts() {
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(3000)
            .setConnectionRequestTimeout(2000)
            .setSocketTimeout(5000)
            .build();
        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .build();
        replaceSdkHttpClient(mChannelServiceApi, "channelHttpUtils", httpClient);
        replaceSdkHttpClient(mRobotService, "robotHttpUtils", httpClient);
    }

    private void replaceSdkHttpClient(Object service, String utilsFieldName, CloseableHttpClient httpClient) {
        try {
            Field utilsField = service.getClass().getDeclaredField(utilsFieldName);
            utilsField.setAccessible(true);
            Object httpUtils = utilsField.get(service);
            Field clientField = httpUtils.getClass().getDeclaredField("httpClient");
            clientField.setAccessible(true);
            clientField.set(httpUtils, httpClient);
        } catch (Exception e) {
            LOG.warn("Unable to configure SDK HTTP timeouts", e);
        }
    }

    @Bean
    public ChannelServiceApi getChannelServiceApi() {
        return this.mChannelServiceApi;
    }

    @Override
    public RestResult login(HttpServletResponse httpResponse, String account, String password) {
        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(account, password);

        try {
            subject.login(token);
        } catch (UnknownAccountException uae) {
            return RestResult.error(ERROR_CODE_ACCOUNT_NOT_EXIST);
        } catch (IncorrectCredentialsException | LockedAccountException | ExcessiveAttemptsException e) {
            return RestResult.error(ERROR_CODE_PASSWORD_INCORRECT);
        } catch (AuthenticationException ae) {
            return RestResult.error(ERROR_CODE_PASSWORD_INCORRECT);
        }

        if (!subject.isAuthenticated()) {
            token.clear();
            return RestResult.error(ERROR_CODE_PASSWORD_INCORRECT);
        }

        establishSession(subject);
        token.clear();
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
        } catch (IncorrectCredentialsException | LockedAccountException | ExcessiveAttemptsException e) {
            return RestResult.error(ERROR_CODE_PASSWORD_INCORRECT);
        } catch (AuthenticationException ae) {
            return RestResult.error(ERROR_CODE_PASSWORD_INCORRECT);
        }

        if (!subject.isAuthenticated()) {
            return RestResult.error(ERROR_CODE_PASSWORD_INCORRECT);
        }

        establishSession(subject);
        Object sessionId = subject.getSession().getId();
        httpResponse.setHeader("authToken", sessionId.toString());
        return RestResult.ok(subject.getPrincipal());
    }

    private void establishSession(Subject subject) {
        subject.getSession().setAttribute("userId", subject.getPrincipal());
        dbSessionDao.rotate(subject.getSession());
    }

    @Override
    @Transactional
    public RestResult updatePassword(String oldPassword, String newPassword) {
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            return RestResult.error(ERROR_NOT_LOGIN);
        }
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)
            || newPassword.length() < MIN_PASSWORD_LENGTH
            || newPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        String account = (String) subject.getPrincipal();
        Optional<User> optionalUser = userRepository.findByAccount(account);
        if (!optionalUser.isPresent()) {
            return RestResult.error(ERROR_CODE_ACCOUNT_NOT_EXIST);
        }

        User user = optionalUser.get();
        if (!PasswordHasher.matches(oldPassword, user.getPasswordMd5(), user.getSalt())) {
            return RestResult.error(ERROR_CODE_PASSWORD_INCORRECT);
        }

        user.setPasswordMd5(PasswordHasher.encode(newPassword));
        userRepository.save(user);

        // Password changes revoke every existing session, including the current one.
        dbSessionDao.deleteByUserId(account);
        subject.logout();
        return RestResult.ok(null);
    }

    @Override
    public RestResult logout() {
        Subject subject = SecurityUtils.getSubject();
        if (subject != null && subject.isAuthenticated()) {
            subject.logout();
        }
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
        if (!subject.isAuthenticated()) {
            return null;
        }
        return (String) subject.getSession().getAttribute("displayName");
    }

    @Override
    public RestResult getAccount() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            return RestResult.ok(subject.getPrincipal());
        }
        return RestResult.error(ERROR_NOT_LOGIN);
    }

    @Override
    public RestResult favApplication(String targetId) {
        return RestResult.ok(null);
    }

    @Override
    @Transactional
    public RestResult createReport(PojoReport report) {
        String userId = getUserId();
        if (!StringUtils.hasText(userId)) {
            return RestResult.error(ERROR_NOT_LOGIN);
        }
        RestResult validationError = validateAndNormalizeReport(report);
        if (validationError != null) {
            return validationError;
        }

        long now = System.currentTimeMillis();
        UserDayId id = new UserDayId(userId, report.day);
        Optional<Report> existing = reportRepository.findById(id);
        Report entity = existing.orElseGet(Report::new);
        boolean isUpdate = existing.isPresent();
        Long createdAt = entity.createdAt;
        copyReportFields(report, entity);
        entity.userId = userId;
        entity.day = report.day;
        entity.displayName = StringUtils.hasText(getDisplayName()) ? getDisplayName() : userId;
        entity.createdAt = createdAt == null ? now : createdAt;
        entity.updatedAt = now;
        reportRepository.save(entity);
        syncReportRecipients(entity);

        runAfterCommit(() -> notifyReport(entity, isUpdate));
        return RestResult.ok(convertReport(entity));
    }

    @Override
    @Transactional
    public RestResult deleteReport(long day) {
        String userId = getUserId();
        if (!StringUtils.hasText(userId)) {
            return RestResult.error(ERROR_NOT_LOGIN);
        }
        long reportDay = day <= 0 ? getToday() : normalizeDay(day);
        UserDayId id = new UserDayId(userId, reportDay);
        Optional<Report> report = reportRepository.findById(id);
        if (!report.isPresent()) {
            return RestResult.error(ERROR_NOT_EXIST);
        }
        if (!userId.equals(report.get().userId)) {
            return RestResult.error(ERROR_NO_RIGHT);
        }
        reportRecipientRepository.deleteReportRecipients(userId, reportDay);
        reportRepository.deleteById(id);
        return RestResult.ok(null);
    }

    @Override
    public RestResult getReport(String userId, long day) {
        String currentUserId = getUserId();
        if (!StringUtils.hasText(currentUserId)) {
            return RestResult.error(ERROR_NOT_LOGIN);
        }
        if (StringUtils.hasText(userId) && userId.length() > MAX_USER_ID_LENGTH) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        long reportDay = day <= 0 ? getToday() : normalizeDay(day);
        String reportUserId = StringUtils.hasText(userId) ? userId : currentUserId;
        Optional<Report> reportOptional = reportRepository.findById(new UserDayId(reportUserId, reportDay));
        if (!reportOptional.isPresent()) {
            // Return the same error for "not found" and "no permission" to avoid a report
            // existence oracle.
            return RestResult.error(ERROR_NOT_EXIST);
        }
        Report report = reportOptional.get();
        if (!canRead(report, currentUserId)) {
            return RestResult.error(ERROR_NOT_EXIST);
        }
        return RestResult.ok(convertReport(report));
    }

    @Override
    public RestResult getReportList(int count, int offset) {
        RestResult result = getReportPage(count, offset, null, null, null);
        if (result.getCode() != RestResult.RestCode.SUCCESS.code) {
            return result;
        }
        return RestResult.ok(((PojoReportPage) result.getResult()).items);
    }

    @Override
    public RestResult getReportPage(int count, int offset, String keyword, Long startDay, Long endDay) {
        String userId = getUserId();
        if (!StringUtils.hasText(userId)) {
            return RestResult.error(ERROR_NOT_LOGIN);
        }
        RestResult validationError = validateQuery(count, offset, keyword, startDay, endDay);
        if (validationError != null) {
            return validationError;
        }
        Page<Report> page = reportRepository.getUserReports(userId, searchPattern(keyword), startDay, endDay,
            new OffsetPageRequest(offset, count));
        return RestResult.ok(convertPage(page, count, offset));
    }

    @Override
    public RestResult getReceivedReports(int count, int offset, String keyword, Long startDay, Long endDay) {
        String userId = getUserId();
        if (!StringUtils.hasText(userId)) {
            return RestResult.error(ERROR_NOT_LOGIN);
        }
        RestResult validationError = validateQuery(count, offset, keyword, startDay, endDay);
        if (validationError != null) {
            return validationError;
        }
        Page<Report> page = reportRecipientRepository.getReceivedReports(userId, searchPattern(keyword),
            startDay, endDay, new OffsetPageRequest(offset, count));
        return RestResult.ok(convertPage(page, count, offset));
    }

    @Override
    public RestResult getReportFeed(int count, String keyword, Long startDay, Long endDay, Long cursorDay) {
        String userId = getUserId();
        if (!StringUtils.hasText(userId)) {
            return RestResult.error(ERROR_NOT_LOGIN);
        }
        RestResult validationError = validateQuery(count, 0, keyword, startDay, endDay);
        if (validationError != null) {
            return validationError;
        }
        Slice<Report> reports = reportRepository.getUserFeed(userId, searchPattern(keyword), startDay, endDay,
            cursorDay, PageRequest.of(0, count));
        return RestResult.ok(convertFeed(reports, count, false));
    }

    @Override
    public RestResult getReceivedFeed(int count, String keyword, Long startDay, Long endDay, Long cursorDay, String cursorUserId) {
        String userId = getUserId();
        if (!StringUtils.hasText(userId)) {
            return RestResult.error(ERROR_NOT_LOGIN);
        }
        RestResult validationError = validateQuery(count, 0, keyword, startDay, endDay);
        if (validationError != null || (cursorDay != null && !StringUtils.hasText(cursorUserId))
            || (cursorUserId != null && cursorUserId.length() > MAX_USER_ID_LENGTH)) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        Slice<Report> reports = reportRecipientRepository.getReceivedFeed(userId, searchPattern(keyword), startDay,
            endDay, cursorDay, cursorUserId, PageRequest.of(0, count));
        return RestResult.ok(convertFeed(reports, count, true));
    }

    @Override
    public RestResult getReportStats() {
        String userId = getUserId();
        if (!StringUtils.hasText(userId)) {
            return RestResult.error(ERROR_NOT_LOGIN);
        }
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        clearTime(monthStart);
        Calendar nextMonth = (Calendar) monthStart.clone();
        nextMonth.add(Calendar.MONTH, 1);
        PojoReportStats stats = new PojoReportStats(
            reportRepository.existsById(new UserDayId(userId, getToday())),
            reportRepository.countByUserId(userId),
            reportRepository.countByUserIdAndDayGreaterThanEqualAndDayLessThan(userId,
                monthStart.getTimeInMillis(), nextMonth.getTimeInMillis()),
            reportRecipientRepository.countByRecipientId(userId)
        );
        return RestResult.ok(stats);
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

    private RestResult validateAndNormalizeReport(PojoReport report) {
        if (report == null) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        report.day = report.day <= 0 ? getToday() : normalizeDay(report.day);
        if (report.day > getToday()) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        report.title = StringUtils.hasText(report.title) ? report.title.trim() : "日报";
        report.content = trim(report.content);
        report.tomorrowPlan = trim(report.tomorrowPlan);
        report.requirement = trim(report.requirement);
        if (!StringUtils.hasText(report.content) || report.title.length() > 64
            || report.content.length() > MAX_TEXT_LENGTH || report.tomorrowPlan.length() > MAX_TEXT_LENGTH
            || report.requirement.length() > MAX_TEXT_LENGTH) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        report.reportTo = cleanRecipients(report.reportTo);
        report.ccTo = cleanRecipients(report.ccTo);
        report.reportToGroup = cleanRecipients(report.reportToGroup);
        report.ccToGroup = cleanRecipients(report.ccToGroup);
        if (report.reportTo.isEmpty() || report.reportTo.size() > MAX_RECIPIENTS
            || report.ccTo.size() > MAX_RECIPIENTS || report.reportToGroup.size() > MAX_RECIPIENTS
            || report.ccToGroup.size() > MAX_RECIPIENTS) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        report.ccTo.removeAll(report.reportTo);
        report.ccToGroup.removeAll(report.reportToGroup);
        return null;
    }

    private RestResult validateQuery(int count, int offset, String keyword, Long startDay, Long endDay) {
        if (count < 1 || count > MAX_PAGE_SIZE || offset < 0 || offset > MAX_OFFSET
            || (keyword != null && keyword.length() > 100)) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        if (startDay != null && endDay != null && normalizeDay(startDay) > normalizeDay(endDay)) {
            return RestResult.error(ERROR_INVALID_PARAMETER);
        }
        return null;
    }

    private PojoReportPage convertPage(Page<Report> page, int count, int offset) {
        List<PojoReport> items = new ArrayList<>();
        page.getContent().forEach(report -> items.add(convertReport(report)));
        return new PojoReportPage(items, page.getTotalElements(), count, offset);
    }

    private PojoReportPage convertFeed(Slice<Report> reports, int count, boolean includeUserCursor) {
        List<PojoReport> items = new ArrayList<>();
        reports.getContent().forEach(report -> items.add(convertReport(report)));
        PojoReportPage result = new PojoReportPage(items, count, reports.hasNext());
        if (reports.hasNext() && !items.isEmpty()) {
            PojoReport last = items.get(items.size() - 1);
            result.nextCursorDay = last.day;
            if (includeUserCursor) {
                result.nextCursorUserId = last.userId;
            }
        }
        return result;
    }

    private void syncReportRecipients(Report report) {
        Set<String> recipients = new LinkedHashSet<>(splitRecipients(report.reportTo));
        recipients.addAll(splitRecipients(report.ccTo));
        reportRecipientRepository.deleteReportRecipients(report.userId, report.day);
        List<ReportRecipient> relations = new ArrayList<>();
        for (String recipient : recipients) {
            relations.add(new ReportRecipient(report.userId, report.day, recipient));
        }
        reportRecipientRepository.saveAll(relations);
    }

    private void runAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }

    private boolean canRead(Report report, String userId) {
        return userId.equals(report.userId)
            || reportRecipientRepository.existsByReportUserIdAndReportDayAndRecipientId(report.userId, report.day, userId)
            || containsRecipient(report.reportTo, userId) || containsRecipient(report.ccTo, userId);
    }

    private boolean containsRecipient(String recipients, String userId) {
        if (!StringUtils.hasText(recipients)) {
            return false;
        }
        for (String recipient : recipients.split(",")) {
            if (userId.equals(recipient.trim())) {
                return true;
            }
        }
        return false;
    }

    private List<String> cleanRecipients(List<String> recipients) {
        if (recipients == null) {
            return new ArrayList<>();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String recipient : recipients) {
            if (StringUtils.hasText(recipient)) {
                result.add(recipient.trim());
            }
        }
        return new ArrayList<>(result);
    }

    private String searchPattern(String keyword) {
        return StringUtils.hasText(keyword) ? "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%" : null;
    }

    private long getToday() {
        Calendar calendar = Calendar.getInstance();
        clearTime(calendar);
        return calendar.getTimeInMillis();
    }

    private long normalizeDay(long day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(day);
        clearTime(calendar);
        return calendar.getTimeInMillis();
    }

    private void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void copyReportFields(PojoReport report, Report entity) {
        entity.title = report.title;
        entity.content = report.content;
        entity.tomorrowPlan = report.tomorrowPlan;
        entity.requirement = report.requirement;
        entity.reportTo = Joiner.on(",").join(report.reportTo);
        entity.ccTo = Joiner.on(",").join(report.ccTo);
        entity.reportToGroup = Joiner.on(",").join(report.reportToGroup);
        entity.ccToGroup = Joiner.on(",").join(report.ccToGroup);
    }

    private PojoReport convertReport(Report report) {
        PojoReport result = new PojoReport();
        result.userId = report.userId;
        result.day = report.day;
        result.title = report.title;
        result.content = report.content;
        result.tomorrowPlan = report.tomorrowPlan;
        result.requirement = report.requirement;
        result.displayName = StringUtils.hasText(report.displayName) ? report.displayName : report.userId;
        result.createdAt = report.createdAt;
        result.updatedAt = report.updatedAt;
        result.reportTo = splitRecipients(report.reportTo);
        result.ccTo = splitRecipients(report.ccTo);
        result.reportToGroup = splitRecipients(report.reportToGroup);
        result.ccToGroup = splitRecipients(report.ccToGroup);
        return result;
    }

    private List<String> splitRecipients(String recipients) {
        return StringUtils.hasText(recipients) ? Arrays.asList(recipients.split(",")) : Collections.emptyList();
    }

    private void notifyReport(Report report, boolean isUpdate) {
        if (!mNotificationEnabled) {
            return;
        }
        String sendName = StringUtils.hasText(report.displayName) ? report.displayName : report.userId;
        String messageContent = sendName + (isUpdate ? " 更新了工作日志，快来看看吧" : " 提交了工作日志，快来看看吧");
        String baseUrl = mReportBaseUrl.endsWith("/") ? mReportBaseUrl.substring(0, mReportBaseUrl.length() - 1) : mReportBaseUrl;
        String reportUrl = baseUrl + "/report.html?userId=" + encode(report.userId) + "&day=" + report.day;
        RichNotificationContentBuilder builder = RichNotificationContentBuilder.newBuilder("日报提交通知", messageContent, reportUrl)
            .exName("日报小助手")
            .addItem("员工", sendName, "#173177")
            .addItem("标题", report.title, "#173177")
            .addItem("内容摘要", summary(report.content), "#173177")
            .addItem("明日计划", summary(report.tomorrowPlan), "#173177");
        if (StringUtils.hasText(report.requirement)) {
            builder.addItem("需要协助", summary(report.requirement), "#173177");
        }
        Set<String> reportTo = new LinkedHashSet<>(splitRecipients(report.reportTo));
        reportTo.add(report.userId);
        List<String> ccTo = splitRecipients(report.ccTo);
        MessagePayload payload = builder.build();
        try {
            taskExecutor.submit(() -> {
                sendTextMessage(new ArrayList<>(reportTo), payload);
                sendTextMessage(ccTo, payload);
            });
        } catch (TaskRejectedException e) {
            LOG.warn("Report notification queue is full, notification dropped for report {}/{}",
                report.userId, report.day);
        }
    }

    private String summary(String content) {
        return content.length() > 40 ? content.substring(0, 40) + "..." : content;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private void sendTextMessage(List<String> toUsers, MessagePayload payload) {
        if (toUsers == null || toUsers.isEmpty()) {
            return;
        }
        try {
            IMResult<SendMessageResult> result = mChannelServiceApi.sendMessage(0, toUsers, payload);
            if (result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                LOG.info("Send report notification success");
            } else {
                LOG.error("Send report notification error {}", result != null ? result.getErrorCode().code : "unknown");
            }
        } catch (Exception e) {
            LOG.error("Send report notification error", e);
        }
    }
}
