package com.capstone.payroll.model;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "teaching_load")
public class TeachingLoad {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) 
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "designation") 
    private EmpDesignation designation;
    
    private String subjectCode; 
    private String subject; 
    private String room; 
    
    private int lectureUnits;
    private int labUnits;
    
    private String timeSchedule; 
    private String dayOfWeek;

    @Column(columnDefinition = "DOUBLE default 0.0")
    private Double lecHours = 0.0;
    
    @Column(columnDefinition = "DOUBLE default 0.0")
    private Double labHours = 0.0;
    
    @Column(columnDefinition = "DOUBLE default 0.0")
    private Double totalHours = 0.0;

    @Column(columnDefinition = "integer default 1")
    private int noOfSections = 1; 

    @Column(columnDefinition = "DOUBLE default 0.0")
    private Double rleHours = 0.0; 

    public TeachingLoad() {}
    
 // ... inside TeachingLoad class ...

    @Transient // ✅ This keeps the data separate from the teaching_load table
    private String substituteFacultyName;
    
    @Transient
    private Double substituteRenderedHours = 0.0;
    
    @Transient
    private String substituteDate;
    
    @Transient
    private String subjectType;
    
    

    // ✅ Add Getters and Setters for these 4 fields at the bottom of the file
    public String getSubstituteFacultyName() { return substituteFacultyName; }
    public void setSubstituteFacultyName(String substituteFacultyName) { this.substituteFacultyName = substituteFacultyName; }
    public Double getSubstituteRenderedHours() { return substituteRenderedHours; }
    public void setSubstituteRenderedHours(Double substituteRenderedHours) { this.substituteRenderedHours = substituteRenderedHours; }
    public String getSubstituteDate() { return substituteDate; }
    public void setSubstituteDate(String substituteDate) { this.substituteDate = substituteDate; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    
    public EmpDesignation getDesignation() { return designation; }
    public void setDesignation(EmpDesignation designation) { this.designation = designation; }
    
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; } 
    
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    
    public int getLectureUnits() { return lectureUnits; }
    public void setLectureUnits(int lectureUnits) { this.lectureUnits = lectureUnits; }
    
    public int getLabUnits() { return labUnits; }
    public void setLabUnits(int labUnits) { this.labUnits = labUnits; }
    
    public String getTimeSchedule() { return timeSchedule; }
    public void setTimeSchedule(String timeSchedule) { this.timeSchedule = timeSchedule; }
    
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Double getLecHours() { 
        return (lecHours != null && lecHours > 0) ? lecHours : (this.lectureUnits * 1.0); 
    }
    public void setLecHours(Double lecHours) { this.lecHours = lecHours; }
    
    public Double getLabHours() { 
        return (labHours != null && labHours > 0) ? labHours : (this.labUnits * 3.0); 
    }
    public void setLabHours(Double labHours) { this.labHours = labHours; }
    
    public Double getTotalHours() { 
        return (totalHours != null && totalHours > 0) ? totalHours : (getLecHours() + getLabHours()); 
    }
    public void setTotalHours(Double totalHours) { this.totalHours = totalHours; }
    
    public int getNoOfSections() { return noOfSections; }
    public void setNoOfSections(int noOfSections) { this.noOfSections = noOfSections; }
    
    public Double getRleHours() { return rleHours; }
    public void setRleHours(Double rleHours) { this.rleHours = rleHours; }
    
    

    @Transient
    public LocalTime getStartTime() {
        if (this.timeSchedule == null || this.timeSchedule.trim().isEmpty()) { return null; }
        try {
            String startStr = this.timeSchedule.split("-")[0].trim().toUpperCase();
            startStr = startStr.replaceAll("(?i)(AM|PM)", " $1").replaceAll("\\s+", " ").trim();
            if (startStr.contains("AM") || startStr.contains("PM")) {
                try { return LocalTime.parse(startStr, DateTimeFormatter.ofPattern("h:mm a")); } 
                catch (Exception e1) { return LocalTime.parse(startStr, DateTimeFormatter.ofPattern("hh:mm a")); }
            } else {
                try { return LocalTime.parse(startStr, DateTimeFormatter.ofPattern("H:mm")); } 
                catch (Exception e2) { return LocalTime.parse(startStr, DateTimeFormatter.ofPattern("HH:mm")); }
            }
        } catch (Exception e) { return null; }
    }

    @Transient
    public LocalTime getEndTime() {
        if (this.timeSchedule == null || !this.timeSchedule.contains("-")) { return null; }
        try {
            String endStr = this.timeSchedule.split("-")[1].trim().toUpperCase();
            endStr = endStr.replaceAll("(?i)(AM|PM)", " $1").replaceAll("\\s+", " ").trim();
            if (endStr.contains("AM") || endStr.contains("PM")) {
                try { return LocalTime.parse(endStr, DateTimeFormatter.ofPattern("h:mm a")); } 
                catch (Exception e1) { return LocalTime.parse(endStr, DateTimeFormatter.ofPattern("hh:mm a")); }
            } else {
                try { return LocalTime.parse(endStr, DateTimeFormatter.ofPattern("H:mm")); } 
                catch (Exception e2) { return LocalTime.parse(endStr, DateTimeFormatter.ofPattern("HH:mm")); }
            }
        } catch (Exception e) { return null; }
    }
}