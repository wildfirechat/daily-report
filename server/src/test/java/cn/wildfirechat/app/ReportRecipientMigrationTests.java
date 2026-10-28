package cn.wildfirechat.app;

import cn.wildfirechat.app.jpa.Report;
import cn.wildfirechat.app.jpa.ReportRecipientRepository;
import cn.wildfirechat.app.jpa.ReportRepository;
import cn.wildfirechat.app.jpa.SchemaMigrationRepository;
import cn.wildfirechat.app.migration.ReportRecipientMigration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@DataJpaTest
@TestPropertySource(properties = "spring.jpa.database=h2")
@Import(ReportRecipientMigration.class)
public class ReportRecipientMigrationTests {
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ReportRecipientRepository recipientRepository;
    @Autowired
    private SchemaMigrationRepository migrationRepository;
    @Autowired
    private ReportRecipientMigration migration;

    @Test
    public void migratesLegacyRecipientsOnlyOnce() {
        migrationRepository.deleteById(ReportRecipientMigration.MIGRATION_ID);
        reportRepository.save(report("alice", 1000L, " manager,lead ", "audit,manager"));
        reportRepository.save(report("bob", 2000L, "manager2", "other"));

        migration.run(null);
        assertEquals(1, recipientRepository.countByRecipientId("manager"));
        assertEquals(1, recipientRepository.countByRecipientId("lead"));
        assertEquals(1, recipientRepository.countByRecipientId("manager2"));
        assertTrue(migrationRepository.existsById(ReportRecipientMigration.MIGRATION_ID));

        migration.run(null);
        assertEquals(5, recipientRepository.count());
    }

    private Report report(String userId, long day, String reportTo, String ccTo) {
        Report report = new Report();
        report.userId = userId;
        report.day = day;
        report.title = "日报";
        report.content = "legacy";
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
