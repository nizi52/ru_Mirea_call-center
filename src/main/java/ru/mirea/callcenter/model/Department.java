package ru.mirea.callcenter.model;

public class Department {
    private Integer id;
    private String name;
    private String description;
    private boolean active;

    public Department(Integer id, String name, String description, boolean active) {
        this.id = id; this.name = name; this.description = description; this.active = active;
    }
    public Department(String name, String description, boolean active) { this(null, name, description, active); }
    public Integer getId() { return id; } public void setId(Integer id) { this.id = id; }
    public String getName() { return name; } public void setName(String value) { name = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    @Override public String toString() { return "%d | %s | %s | %s".formatted(id, name, description, active ? "активен" : "неактивен"); }
}
