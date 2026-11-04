package ru.mirea.callcenter.model;

public class Client {
    private Integer id;
    private String fullName;
    private String phone;
    private String email;

    public Client(Integer id, String fullName, String phone, String email) {
        this.id = id; this.fullName = fullName; this.phone = phone; this.email = email;
    }
    public Client(String fullName, String phone, String email) { this(null, fullName, phone, email); }
    public Integer getId() { return id; } public void setId(Integer id) { this.id = id; }
    public String getFullName() { return fullName; } public void setFullName(String value) { fullName = value; }
    public String getPhone() { return phone; } public void setPhone(String value) { phone = value; }
    public String getEmail() { return email; } public void setEmail(String value) { email = value; }
    @Override public String toString() { return "%d | %s | %s | %s".formatted(id, fullName, phone, email == null ? "" : email); }
}
