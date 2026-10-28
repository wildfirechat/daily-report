package cn.wildfirechat.app.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, UserDayId> {
    String FILTER_CONDITION = "(:startDay is null or r.day >= :startDay) " +
        "and (:endDay is null or r.day <= :endDay) " +
        "and (:keyword is null or lower(r.title) like :keyword " +
        "or lower(r.content) like :keyword or lower(r.tomorrowPlan) like :keyword " +
        "or lower(r.requirement) like :keyword)";

    @Query("select r from Report r where r.userId = :userId and " + FILTER_CONDITION +
        " order by r.day desc, r.updatedAt desc")
    Page<Report> getUserReports(@Param("userId") String userId,
                                @Param("keyword") String keyword,
                                @Param("startDay") Long startDay,
                                @Param("endDay") Long endDay,
                                Pageable pageable);

    @Query("select r from Report r where r.userId = :userId and " + FILTER_CONDITION +
        " and (:cursorDay is null or r.day < :cursorDay) order by r.day desc")
    Slice<Report> getUserFeed(@Param("userId") String userId,
                              @Param("keyword") String keyword,
                              @Param("startDay") Long startDay,
                              @Param("endDay") Long endDay,
                              @Param("cursorDay") Long cursorDay,
                              Pageable pageable);

    @Query("select r from Report r where (:cursorDay is null or r.day > :cursorDay " +
        "or (r.day = :cursorDay and r.userId > :cursorUserId)) order by r.day asc, r.userId asc")
    List<Report> getMigrationBatch(@Param("cursorDay") Long cursorDay,
                                   @Param("cursorUserId") String cursorUserId,
                                   Pageable pageable);

    long countByUserId(String userId);

    long countByUserIdAndDayGreaterThanEqualAndDayLessThan(String userId, long startDay, long endDay);
}
