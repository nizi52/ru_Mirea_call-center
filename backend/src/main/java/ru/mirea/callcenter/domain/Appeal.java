package ru.mirea.callcenter.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Основная доменная сущность обращения.
 *
 * <p>Обычное удаление реализовано через архивирование, поэтому история и связанные
 * записи остаются доступными для восстановления.</p>
 */
@Entity
@Table(name = "appeal")
public class Appeal extends AuditedVersionedEntity {
    @Id
    private UUID id;

    @Column(name = "external_id", unique = true, length = 100)
    private String externalId;

    @Column(name = "topic", nullable = false, length = 300)
    private String topic;

    @Column(name = "client_name", nullable = false, length = 150)
    private String clientName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(length = 254)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    private CallCenter center;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppealStatus status = AppealStatus.NEW;

    @Column(name = "impact_score", precision = 5, scale = 2)
    private BigDecimal impactScore;

    @Column(name = "urgent", nullable = false)
    private boolean urgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 32)
    private PriorityLevel priorityLevel = PriorityLevel.LOW;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by")
    private UserAccount archivedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserAccount updatedBy;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "appeal_tag", joinColumns = @JoinColumn(name = "appeal_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "operator_id")
    private Integer operatorId;
    public Integer getOperatorId() { return operatorId; }
    public void setOperatorId(Integer value) { operatorId = value; }

    @Column(name = "legacy_client_id")
    private Integer clientId;
    public Integer getClientId() { return clientId; }
    public void setClientId(Integer id) { clientId = id; }

    @Column(name = "closed_at")
    private Instant closedAt;
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant value) { closedAt = value; }

    protected Appeal() {
    }

    public Appeal(UUID id, String topic, String clientName, UserAccount creator) {
        this.id = id;
        this.topic = topic;
        this.clientName = clientName;
        this.createdBy = creator;
        this.updatedBy = creator;
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public CallCenter getCallCenter() { return center; }
    public void setCallCenter(CallCenter center) { this.center = center; }
    public AppealStatus getStatus() { return status; }
    public void setStatus(AppealStatus status) { this.status = status; }
    public BigDecimal getImpactScore() { return impactScore; }
    public void setImpactScore(BigDecimal impactScore) { this.impactScore = impactScore; }
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public UserAccount getArchivedBy() { return archivedBy; }
    public void setArchivedBy(UserAccount archivedBy) { this.archivedBy = archivedBy; }
    public UserAccount getCreatedBy() { return createdBy; }
    public UserAccount getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UserAccount updatedBy) { this.updatedBy = updatedBy; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = new LinkedHashSet<>(tags); }
}
