package cn.wildfirechat.app.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "t_schema_migration")
public class SchemaMigration {
    @Id
    @Column(length = 64)
    public String id;

    @Column(nullable = false)
    public long completedAt;

    public SchemaMigration() {
    }

    public SchemaMigration(String id) {
        this.id = id;
        this.completedAt = System.currentTimeMillis();
    }
}
