package ru.mirea.callcenter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Колл-центр, на которую ссылаются карточки обращений. */
@Entity
@Table(name = "center")
public class CallCenter extends AuditedVersionedEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 200)
    private String name;

    @Column(name = "sla_hours")
    private Integer slaHours;

    @Column(nullable = false)
    private boolean active = true;

    protected CallCenter() {
    }

    public CallCenter(UUID id, String code, String name, Integer slaHours) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.slaHours = slaHours;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
