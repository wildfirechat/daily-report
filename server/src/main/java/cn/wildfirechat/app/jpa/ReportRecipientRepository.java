package cn.wildfirechat.app.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRecipientRepository extends JpaRepository<ReportRecipient, ReportRecipientId> {
    String REPORT_JOIN = "join ReportRecipient p with p.reportUserId = r.userId and p.reportDay = r.day";
    String FILTER_CONDITION = "(:startDay is null or r.day >= :startDay) " +
        "and (:endDay is null or r.day <= :endDay) " +
        "and (:keyword is null or lower(r.title) like :keyword " +
        "or lower(r.content) like :keyword or lower(r.tomorrowPlan) like :keyword " +
        "or lower(r.requirement) like :keyword or lower(r.displayName) like :keyword)";

    @Query("select r from Report r " + REPORT_JOIN + " where p.recipientId = :recipientId and " + FILTER_CONDITION +
        " order by r.day desc, r.updatedAt desc")
    Page<Report> getReceivedReports(@Param("recipientId") String recipientId,
                                    @Param("keyword") String keyword,
                                    @Param("startDay") Long startDay,
                                    @Param("endDay") Long endDay,
                                    Pageable pageable);

    @Query("select r from Report r " + REPORT_JOIN + " where p.recipientId = :recipientId and " + FILTER_CONDITION +
        " and (:cursorDay is null or r.day < :cursorDay " +
        "or (r.day = :cursorDay and r.userId > :cursorUserId)) order by r.day desc, r.userId asc")
    Slice<Report> getReceivedFeed(@Param("recipientId") String recipientId,
                                  @Param("keyword") String keyword,
                                  @Param("startDay") Long startDay,
                                  @Param("endDay") Long endDay,
                                  @Param("cursorDay") Long cursorDay,
                                  @Param("cursorUserId") String cursorUserId,
                                  Pageable pageable);

    long countByRecipientId(String recipientId);

    boolean existsByReportUserIdAndReportDayAndRecipientId(String reportUserId, long reportDay, String recipientId);

    @Modifying
    @Query("delete from ReportRecipient p where p.reportUserId = :reportUserId and p.reportDay = :reportDay")
    void deleteReportRecipients(@Param("reportUserId") String reportUserId, @Param("reportDay") long reportDay);
}
