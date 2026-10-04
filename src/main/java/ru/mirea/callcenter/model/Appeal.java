package ru.mirea.callcenter.model;

import java.time.LocalDateTime;

public class Appeal {
    private Integer id;
    private Integer operatorId;
    private Integer clientId;
    private String clientName;
    private String clientPhone;
    private String topic;
    private String category;
    private AppealStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    public Appeal(Integer id, Integer operatorId, Integer clientId, String clientName, String clientPhone, String topic, String category,
                  AppealStatus status, LocalDateTime createdAt, LocalDateTime closedAt) {
        this.id=id; this.operatorId=operatorId; this.clientId=clientId; this.clientName=clientName; this.clientPhone=clientPhone;
        this.topic=topic; this.category=category; this.status=status; this.createdAt=createdAt; this.closedAt=closedAt;
    }
    public Integer getId() { return id; } public void setId(Integer id) { this.id=id; }
    public Integer getOperatorId() { return operatorId; } public void setOperatorId(Integer v) { operatorId=v; }
    public Integer getClientId() { return clientId; } public void setClientId(Integer v) { clientId=v; }
    public String getClientName() { return clientName; } public void setClientName(String v) { clientName=v; }
    public String getClientPhone() { return clientPhone; } public void setClientPhone(String v) { clientPhone=v; }
    public String getTopic() { return topic; } public void setTopic(String v) { topic=v; }
    public String getCategory() { return category; } public void setCategory(String v) { category=v; }
    public AppealStatus getStatus() { return status; } public void setStatus(AppealStatus v) { status=v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt=v; }
    public LocalDateTime getClosedAt() { return closedAt; } public void setClosedAt(LocalDateTime v) { closedAt=v; }
    @Override public String toString() {
        return "%d | клиент: %s (ID %d), %s | оператор: %s | %s | %s | %s | создано: %s | закрыто: %s".formatted(
                id, clientName, clientId, clientPhone, operatorId == null ? "не назначен" : operatorId,
                category, status, topic, createdAt, closedAt == null ? "—" : closedAt);
    }
}
