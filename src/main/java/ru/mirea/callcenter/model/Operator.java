package ru.mirea.callcenter.model;

public class Operator {
    private Integer id;
    private String fullName;
    private String login;
    private Integer callCenterId;
    private String callCenterName;
    private boolean active;

    public Operator(Integer id, String fullName, String login, Integer callCenterId, String callCenterName, boolean active) {
        this.id = id; this.fullName = fullName; this.login = login; this.callCenterId = callCenterId;
        this.callCenterName = callCenterName; this.active = active;
    }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public Integer getCallCenterId() { return callCenterId; }
    public void setCallCenterId(Integer callCenterId) { this.callCenterId = callCenterId; }
    public String getCallCenterName() { return callCenterName; }
    public void setCallCenterName(String callCenterName) { this.callCenterName = callCenterName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    @Override public String toString() {
        return "%d | %s | %s | колл-центр: %s (ID %d) | %s".formatted(id, fullName, login, callCenterName, callCenterId, active ? "активен" : "неактивен");
    }
}
