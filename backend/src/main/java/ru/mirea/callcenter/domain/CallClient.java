package ru.mirea.callcenter.domain;
import jakarta.persistence.*;
/** Общая таблица клиентов, сохранённая из КР2. Обращение хранит снимок контактов. */
@Entity @Table(name="clients") public class CallClient {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @Column(name="full_name",nullable=false,length=150) private String fullName;
 @Column(nullable=false,unique=true,length=30) private String phone;
 @Column(length=150) private String email;
 protected CallClient() {}
 public CallClient(String fullName,String phone,String email){this.fullName=fullName;this.phone=phone;this.email=email;}
 @Version private long version;
 public long getVersion(){return version;}
 public String getFullName(){return fullName;}
 public String getPhone(){return phone;}
 public String getEmail(){return email;}
 public void update(String name,String phone,String email){this.fullName=name;this.phone=phone;this.email=email;}
 public Integer getId(){return id;}
}
