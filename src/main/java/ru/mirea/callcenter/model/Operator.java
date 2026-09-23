package ru.mirea.callcenter.model;

public class Operator {
    private Integer id;
    private String fullName;
    private String login;
    private String department;
    private boolean active;

    public Operator(Integer id, String fullName, String login, String department, boolean active) {
        this.id = id; this.fullName = fullName; this.login = login; this.department = department; this.active = active;
    }
    public Operator(String fullName, String login, String department, boolean active) {
        this(null, fullName, login, department, active);
    }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    @Override public String toString() {
        return "%d | %s | %s | %s | %s".formatted(id, fullName, login, department, active ? "активен" : "неактивен");
    }
}
