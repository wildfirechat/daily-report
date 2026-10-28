package cn.wildfirechat.app.jpa;

import java.io.Serializable;
import java.util.Objects;

public class ReportRecipientId implements Serializable {
    public String reportUserId;
    public long reportDay;
    public String recipientId;

    public ReportRecipientId() {
    }

    public ReportRecipientId(String reportUserId, long reportDay, String recipientId) {
        this.reportUserId = reportUserId;
        this.reportDay = reportDay;
        this.recipientId = recipientId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ReportRecipientId)) {
            return false;
        }
        ReportRecipientId that = (ReportRecipientId) object;
        return reportDay == that.reportDay && Objects.equals(reportUserId, that.reportUserId)
            && Objects.equals(recipientId, that.recipientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportUserId, reportDay, recipientId);
    }
}
