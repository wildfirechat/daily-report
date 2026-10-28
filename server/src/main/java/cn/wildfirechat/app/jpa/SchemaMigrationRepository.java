package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemaMigrationRepository extends JpaRepository<SchemaMigration, String> {
}
