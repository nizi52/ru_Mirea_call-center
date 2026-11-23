package ru.mirea.callcenter.domain;
import jakarta.persistence.*;
@Entity @Table(name="operators") public class CallOperator {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @Column(name="full_name") private String fullName;
 @Column(name="is_active") private boolean active;
 @Column(name="call_center_id") private Integer centerId;
 @Column(nullable=false,length=50) private String login;
 @Column(name="center_id",nullable=false) private java.util.UUID routingCenterId;
 @Version private long version;
 public CallOperator(String name,String login,java.util.UUID center,boolean active){update(name,login,center,active);}
 public void update(String name,String login,java.util.UUID center,boolean active){this.fullName=name;this.login=login;this.routingCenterId=center;this.active=active;}
 public String getLogin(){return login;}
 public java.util.UUID getCenterId(){return routingCenterId;}
 public long getVersion(){return version;}
 protected CallOperator() {}
 public Integer getId(){return id;} public String getFullName(){return fullName;}
 public boolean isActive(){return active;} public Integer getCallCenterId(){return centerId;}
}
