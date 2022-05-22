package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ReportRepository extends CrudRepository<Report, UserDayId> {
    @Query(value = "select * from t_report where user_id = ?1 order by day desc limit ?2 offset ?3", nativeQuery = true)
    List<Report> getUserReports(String userId, int count, int offset);
}
