package cn.wildfirechat.app.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@IdClass(ReportRecipientId.class)
@Table(name = "t_report_recipient", indexes = {
    @Index(name = "idx_recipient_day_user", columnList = "recipientId,reportDay,reportUserId")
})
public class ReportRecipient implements Serializable {
    @Id
    @Column(length = 64)
    public String reportUserId;

    @Id
    public long reportDay;

    @Id
    @Column(length = 64)
    public String recipientId;

    public ReportRecipient() {
    }

    public ReportRecipient(String reportUserId, long reportDay, String recipientId) {
        this.reportUserId = reportUserId;
        this.reportDay = reportDay;
        this.recipientId = recipientId;
    }
}
