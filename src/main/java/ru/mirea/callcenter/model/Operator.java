package ru.mirea.callcenter.model;

public class Operator {
    private Integer id;
    private String fullName;
    private String login;
    private Integer departmentId;
    private String departmentName;
    private boolean active;

    public Operator(Integer id, String fullName, String login, Integer departmentId, String departmentName, boolean active) {
        this.id = id; this.fullName = fullName; this.login = login; this.departmentId = departmentId;
        this.departmentName = departmentName; this.active = active;
    }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    @Override public String toString() {
        return "%d | %s | %s | отдел: %s (ID %d) | %s".formatted(id, fullName, login, departmentName, departmentId, active ? "активен" : "неактивен");
    }
}
