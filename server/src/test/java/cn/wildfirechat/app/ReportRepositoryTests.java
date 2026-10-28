package cn.wildfirechat.app;

import cn.wildfirechat.app.jpa.OffsetPageRequest;
import cn.wildfirechat.app.jpa.Report;
import cn.wildfirechat.app.jpa.ReportRecipient;
import cn.wildfirechat.app.jpa.ReportRecipientRepository;
import cn.wildfirechat.app.jpa.ReportRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@DataJpaTest
@TestPropertySource(properties = "spring.jpa.database=h2")
public class ReportRepositoryTests {
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ReportRecipientRepository recipientRepository;

    @Test
    public void filtersAndPaginatesUserReports() {
        saveReport(report("alice", 1000L, "Project kickoff", "manager", ""));
        saveReport(report("alice", 2000L, "Project delivery", "manager", ""));
        saveReport(report("alice", 3000L, "Team meeting", "manager", ""));
        saveReport(report("bob", 2000L, "Project review", "alice", ""));

        Page<Report> page = reportRepository.getUserReports("alice", "%project%", null, null,
            new OffsetPageRequest(1, 1));
        assertEquals(2, page.getTotalElements());
        assertEquals(1000L, page.getContent().get(0).day);

        Slice<Report> first = reportRepository.getUserFeed("alice", null, null, null, null,
            PageRequest.of(0, 2));
        assertTrue(first.hasNext());
        assertEquals(3000L, first.getContent().get(0).day);
        Slice<Report> second = reportRepository.getUserFeed("alice", null, null, null, 2000L,
            PageRequest.of(0, 2));
        assertFalse(second.hasNext());
        assertEquals(1, second.getContent().size());
        assertEquals(1000L, second.getContent().get(0).day);
    }

    @Test
    public void findsExactRecipientsAndUsesStableCursor() {
        saveReport(report("alice", 1000L, "First", "manager,lead", "audit"));
        saveReport(report("bob", 2000L, "Second", "manager2", "other"));
        saveReport(report("carol", 3000L, "Third", "other", "manager"));
        saveReport(report("dave", 3000L, "Fourth", "manager", ""));

        Page<Report> page = recipientRepository.getReceivedReports("manager", null, null, null,
            new OffsetPageRequest(0, 20));
        assertEquals(3, page.getTotalElements());
        assertEquals(3, recipientRepository.countByRecipientId("manager"));

        Slice<Report> first = recipientRepository.getReceivedFeed("manager", null, null, null, null,
            null, PageRequest.of(0, 2));
        assertTrue(first.hasNext());
        assertEquals("carol", first.getContent().get(0).userId);
        assertEquals("dave", first.getContent().get(1).userId);
        Slice<Report> second = recipientRepository.getReceivedFeed("manager", null, null, null, 3000L,
            "dave", PageRequest.of(0, 2));
        assertFalse(second.hasNext());
        assertEquals(1, second.getContent().size());
        assertEquals("alice", second.getContent().get(0).userId);
        assertTrue(recipientRepository.getReceivedReports("manager", "%third%", null, null,
            new OffsetPageRequest(0, 20)).getContent().size() == 1);
    }

    private void saveReport(Report report) {
        reportRepository.save(report);
        Set<String> recipients = new LinkedHashSet<>();
        for (String value : (report.reportTo + "," + report.ccTo).split(",")) {
            if (!value.trim().isEmpty()) {
                recipients.add(value.trim());
            }
        }
        for (String recipient : recipients) {
            recipientRepository.save(new ReportRecipient(report.userId, report.day, recipient));
        }
    }

    private Report report(String userId, long day, String content, String reportTo, String ccTo) {
        Report report = new Report();
        report.userId = userId;
        report.day = day;
        report.title = "日报";
        report.content = content;
        report.tomorrowPlan = "plan";
        report.requirement = "";
        report.reportTo = reportTo;
        report.ccTo = ccTo;
        report.reportToGroup = "";
        report.ccToGroup = "";
        report.displayName = userId;
        report.createdAt = day;
        report.updatedAt = day;
        return report;
    }
}
