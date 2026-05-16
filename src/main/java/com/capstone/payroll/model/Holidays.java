package com.capstone.payroll.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "holidays")
public class Holidays {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false)
    private String name;
    
    // Expected values: "REGULAR" or "SPECIAL_NON_WORKING"
    @Column(nullable = false)
    private String type; 

    // Default constructor required by Spring/JPA
    public Holidays() {}

    // Convenience constructor for auto-loading our dates
    public Holidays(LocalDate date, String name, String type) {
        this.date = date;
        this.name = name;
        this.type = type;
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================
    public int getId() { 
        return id; 
    }
    public void setId(int id) { 
        this.id = id; 
    }
    
    public LocalDate getDate() { 
        return date; 
    }
    public void setDate(LocalDate date) { 
        this.date = date; 
    }
    
    public String getName() { 
        return name; 
    }
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getType() { 
        return type; 
    }
    public void setType(String type) { 
        this.type = type; 
    }
}