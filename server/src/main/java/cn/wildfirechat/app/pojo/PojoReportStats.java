package cn.wildfirechat.app.pojo;

public class PojoReportStats {
    public boolean todaySubmitted;
    public long totalCount;
    public long monthCount;
    public long receivedCount;

    public PojoReportStats(boolean todaySubmitted, long totalCount, long monthCount, long receivedCount) {
        this.todaySubmitted = todaySubmitted;
        this.totalCount = totalCount;
        this.monthCount = monthCount;
        this.receivedCount = receivedCount;
    }
}
