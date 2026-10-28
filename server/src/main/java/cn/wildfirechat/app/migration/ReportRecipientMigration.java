package cn.wildfirechat.app.migration;

import cn.wildfirechat.app.jpa.Report;
import cn.wildfirechat.app.jpa.ReportRecipient;
import cn.wildfirechat.app.jpa.ReportRecipientRepository;
import cn.wildfirechat.app.jpa.ReportRepository;
import cn.wildfirechat.app.jpa.SchemaMigration;
import cn.wildfirechat.app.jpa.SchemaMigrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ReportRecipientMigration implements ApplicationRunner {
    public static final String MIGRATION_ID = "report-recipient-v1";
    private static final int BATCH_SIZE = 500;
    private static final Logger LOG = LoggerFactory.getLogger(ReportRecipientMigration.class);

    private final ReportRepository reportRepository;
    private final ReportRecipientRepository recipientRepository;
    private final SchemaMigrationRepository migrationRepository;
    private final TransactionTemplate transactionTemplate;

    public ReportRecipientMigration(ReportRepository reportRepository,
                                    ReportRecipientRepository recipientRepository,
                                    SchemaMigrationRepository migrationRepository,
                                    TransactionTemplate transactionTemplate) {
        this.reportRepository = reportRepository;
        this.recipientRepository = recipientRepository;
        this.migrationRepository = migrationRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (migrationRepository.existsById(MIGRATION_ID)) {
            return;
        }
        LOG.info("Start report recipient migration");
        Long cursorDay = null;
        String cursorUserId = null;
        int migrated = 0;
        while (true) {
            List<Report> batch = reportRepository.getMigrationBatch(cursorDay, cursorUserId, PageRequest.of(0, BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            transactionTemplate.executeWithoutResult(status -> batch.forEach(this::migrateReport));
            Report last = batch.get(batch.size() - 1);
            cursorDay = last.day;
            cursorUserId = last.userId;
            migrated += batch.size();
            LOG.info("Migrated {} reports", migrated);
        }
        migrationRepository.save(new SchemaMigration(MIGRATION_ID));
        LOG.info("Report recipient migration completed, {} reports processed", migrated);
    }

    private void migrateReport(Report report) {
        Set<String> recipients = recipients(report.reportTo);
        recipients.addAll(recipients(report.ccTo));
        List<ReportRecipient> relations = new ArrayList<>();
        for (String recipientId : recipients) {
            relations.add(new ReportRecipient(report.userId, report.day, recipientId));
        }
        recipientRepository.saveAll(relations);
    }

    private Set<String> recipients(String value) {
        Set<String> recipients = new LinkedHashSet<>();
        if (!StringUtils.hasText(value)) {
            return recipients;
        }
        for (String recipient : value.split(",")) {
            if (StringUtils.hasText(recipient)) {
                recipients.add(recipient.trim());
            }
        }
        return recipients;
    }
}
