package cn.wildfirechat.app.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetPageRequest implements Pageable {
    private final long offset;
    private final int limit;

    public OffsetPageRequest(long offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + limit, limit);
    }

    @Override
    public Pageable previousOrFirst() {
        return offset == 0 ? this : new OffsetPageRequest(Math.max(0, offset - limit), limit);
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, limit);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest((long) pageNumber * limit, limit);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
