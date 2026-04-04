package com.ouro.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalTime;

@Entity
@Table(name = "client")
public class Client {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "date_of_birth")
    private Timestamp dateOfBirth;
    
    @Column(name = "time_of_birth")
    private LocalTime timeOfBirth;
    
    // Constructors
    public Client() {
    }
    
    public Client(User user) {
        this.user = user;
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Timestamp getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(Timestamp dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public LocalTime getTimeOfBirth() {
        return timeOfBirth;
    }
    
    public void setTimeOfBirth(LocalTime timeOfBirth) {
        this.timeOfBirth = timeOfBirth;
    }
}
