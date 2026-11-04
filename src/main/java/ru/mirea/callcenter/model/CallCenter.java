package ru.mirea.callcenter.model;

public class CallCenter {
    private Integer id;
    private String name;
    private String address;
    private boolean active;

    public CallCenter(Integer id, String name, String address, boolean active) {
        this.id = id; this.name = name; this.address = address; this.active = active;
    }
    public CallCenter(String name, String address, boolean active) { this(null, name, address, active); }
    public Integer getId() { return id; } public void setId(Integer id) { this.id = id; }
    public String getName() { return name; } public void setName(String value) { name = value; }
    public String getAddress() { return address; } public void setAddress(String value) { address = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    @Override public String toString() { return "%d | %s | %s | %s".formatted(id, name, address, active ? "активен" : "неактивен"); }
}
