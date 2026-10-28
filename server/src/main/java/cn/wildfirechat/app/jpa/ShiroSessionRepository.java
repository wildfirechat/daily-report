package cn.wildfirechat.app.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ShiroSessionRepository extends CrudRepository<ShiroSession, String> {
    @Transactional
    void deleteByUserId(String userId);
}
