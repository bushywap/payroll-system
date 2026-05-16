package com.capstone.payroll.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "substitute_records")
public class SubstituteRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_load_id", nullable = false)
    private TeachingLoad teachingLoad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_faculty_id", nullable = false)
    private Employee originalFaculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_faculty_id", nullable = false)
    private Employee substituteFaculty;

    @Column(name = "date_substituted", nullable = false)
    private LocalDate dateSubstituted;
    
    @Column(name = "hours_rendered", nullable = false)
    private Double hoursRendered;

    @Column(name = "subject_type", length = 10)
    private String subjectType; 

    public SubstituteRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TeachingLoad getTeachingLoad() { return teachingLoad; }
    public void setTeachingLoad(TeachingLoad teachingLoad) { this.teachingLoad = teachingLoad; }
    public Employee getOriginalFaculty() { return originalFaculty; }
    public void setOriginalFaculty(Employee originalFaculty) { this.originalFaculty = originalFaculty; }
    public Employee getSubstituteFaculty() { return substituteFaculty; }
    public void setSubstituteFaculty(Employee substituteFaculty) { this.substituteFaculty = substituteFaculty; }
    public LocalDate getDateSubstituted() { return dateSubstituted; }
    public void setDateSubstituted(LocalDate dateSubstituted) { this.dateSubstituted = dateSubstituted; }
    public Double getHoursRendered() { return hoursRendered; }
    public void setHoursRendered(Double hoursRendered) { this.hoursRendered = hoursRendered; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
}