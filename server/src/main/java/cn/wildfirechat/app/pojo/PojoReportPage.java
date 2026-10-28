package cn.wildfirechat.app.pojo;

import java.util.List;

public class PojoReportPage {
    public List<PojoReport> items;
    public long total;
    public int count;
    public int offset;
    public boolean hasMore;
    public Long nextCursorDay;
    public String nextCursorUserId;

    public PojoReportPage(List<PojoReport> items, long total, int count, int offset) {
        this.items = items;
        this.total = total;
        this.count = count;
        this.offset = offset;
        this.hasMore = (long) offset + items.size() < total;
    }

    public PojoReportPage(List<PojoReport> items, int count, boolean hasMore) {
        this.items = items;
        this.total = -1;
        this.count = count;
        this.offset = 0;
        this.hasMore = hasMore;
    }
}
