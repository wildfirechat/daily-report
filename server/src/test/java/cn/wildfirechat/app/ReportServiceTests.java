package cn.wildfirechat.app;

import cn.wildfirechat.app.jpa.Report;
import cn.wildfirechat.app.jpa.ReportRecipientRepository;
import cn.wildfirechat.app.jpa.ReportRepository;
import cn.wildfirechat.app.jpa.UserDayId;
import cn.wildfirechat.app.pojo.PojoReport;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReportServiceTests {
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportRecipientRepository recipientRepository;
    @Mock
    private ThreadPoolTaskExecutor taskExecutor;

    private ServiceImpl service;
    private Subject subject;

    @Before
    public void setUp() {
        service = new ServiceImpl();
        ReflectionTestUtils.setField(service, "reportRepository", reportRepository);
        ReflectionTestUtils.setField(service, "reportRecipientRepository", recipientRepository);
        ReflectionTestUtils.setField(service, "taskExecutor", taskExecutor);
        ReflectionTestUtils.setField(service, "mReportBaseUrl", "https://reports.example.com");
        subject = mock(Subject.class);
        Session session = mock(Session.class);
        when(subject.isAuthenticated()).thenReturn(true);
        when(subject.getPrincipal()).thenReturn("alice");
        when(subject.getSession()).thenReturn(session);
        when(session.getAttribute("displayName")).thenReturn("Alice");
        ThreadContext.bind(subject);
    }

    @After
    public void tearDown() {
        ThreadContext.unbindSubject();
        SecurityManager securityManager = ThreadContext.getSecurityManager();
        if (securityManager != null) {
            ThreadContext.unbindSecurityManager();
        }
    }

    @Test
    public void createsOwnReportAndSynchronizesRecipients() {
        PojoReport request = validReport();
        request.userId = "someone-else";
        request.reportTo = Arrays.asList("manager", "manager", " lead ");
        request.ccTo = Arrays.asList("manager", "audit");
        when(reportRepository.findById(any(UserDayId.class))).thenReturn(Optional.empty());

        RestResult result = service.createReport(request);

        assertEquals(RestResult.RestCode.SUCCESS.code, result.getCode());
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report saved = captor.getValue();
        assertEquals("alice", saved.userId);
        assertEquals("Alice", saved.displayName);
        assertEquals("manager,lead", saved.reportTo);
        assertEquals("audit", saved.ccTo);
        assertNotNull(saved.createdAt);
        assertEquals(saved.createdAt, saved.updatedAt);
        verify(recipientRepository).deleteReportRecipients(anyString(), anyLong());
        verify(recipientRepository).saveAll(any());
    }

    @Test
    public void updatePreservesCreationTime() {
        Report existing = storedReport("alice", "manager", "");
        existing.createdAt = 123L;
        existing.updatedAt = 456L;
        when(reportRepository.findById(any(UserDayId.class))).thenReturn(Optional.of(existing));

        RestResult result = service.createReport(validReport());

        assertEquals(RestResult.RestCode.SUCCESS.code, result.getCode());
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertEquals(Long.valueOf(123L), captor.getValue().createdAt);
        assertTrue(captor.getValue().updatedAt > 456L);
    }

    @Test
    public void recipientCanReadButStrangerCannot() {
        Report received = storedReport("bob", "alice", "");
        when(reportRepository.findById(any(UserDayId.class))).thenReturn(Optional.of(received));
        assertEquals(RestResult.RestCode.SUCCESS.code, service.getReport("bob", received.day).getCode());

        Report forbidden = storedReport("bob", "manager", "alice2");
        when(reportRepository.findById(any(UserDayId.class))).thenReturn(Optional.of(forbidden));
        assertEquals(RestResult.RestCode.ERROR_NOT_EXIST.code, service.getReport("bob", forbidden.day).getCode());
    }

    @Test
    public void rejectsFutureReportsAndInvalidPagination() {
        PojoReport report = validReport();
        report.day = System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000;
        assertEquals(RestResult.RestCode.ERROR_INVALID_PARAMETER.code, service.createReport(report).getCode());
        verify(reportRepository, never()).save(any(Report.class));

        assertEquals(RestResult.RestCode.ERROR_INVALID_PARAMETER.code,
            service.getReportPage(0, 0, null, null, null).getCode());
        assertEquals(RestResult.RestCode.ERROR_INVALID_PARAMETER.code,
            service.getReportPage(20, -1, null, null, null).getCode());
    }

    @Test
    public void deletingUnknownReportReturnsNotFound() {
        when(reportRepository.findById(any(UserDayId.class))).thenReturn(Optional.empty());
        assertEquals(RestResult.RestCode.ERROR_NOT_EXIST.code, service.deleteReport(0).getCode());
    }

    private PojoReport validReport() {
        PojoReport report = new PojoReport();
        report.title = "日报";
        report.content = "Completed work";
        report.tomorrowPlan = "Next work";
        report.requirement = "";
        report.reportTo = Arrays.asList("manager");
        report.ccTo = Arrays.asList();
        report.reportToGroup = Arrays.asList();
        report.ccToGroup = Arrays.asList();
        return report;
    }

    private Report storedReport(String userId, String reportTo, String ccTo) {
        Report report = new Report();
        report.userId = userId;
        report.day = 1704067200000L;
        report.title = "日报";
        report.content = "Completed work";
        report.tomorrowPlan = "Next work";
        report.requirement = "";
        report.reportTo = reportTo;
        report.ccTo = ccTo;
        report.reportToGroup = "";
        report.ccToGroup = "";
        report.displayName = userId;
        report.createdAt = 1L;
        report.updatedAt = 1L;
        return report;
    }
}
