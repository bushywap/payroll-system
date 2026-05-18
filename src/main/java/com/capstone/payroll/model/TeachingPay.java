package com.capstone.payroll.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "teaching_pay")
public class TeachingPay {

    public static final double BASE_REQUIRED_UNITS = 15.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnore
    private Employee employee;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    private Double totalLecUnits = 0.0;
    private Double totalLabUnits = 0.0;
    
    private Double totalLecHours = 0.0;
    private Double totalLabHours = 0.0;
    
    private Double excessLecUnits = 0.0;
    private Double excessLabUnits = 0.0;

    private Double excessLecHours = 0.0;
    private Double excessLabHours = 0.0;
    
    private Double absentDeductionLecHours = 0.0;
    private Double absentDeductionLabHours = 0.0;
    private BigDecimal absentDeductionPay = BigDecimal.ZERO;
    
    @Column(name = "applied_suspension_dates", length = 500)
    private String appliedSuspensionDates; 
    
    @Column(name = "excess_rle_hours")
    private Double excessRleHours = 0.0; 
    
    private Double totalExcessHours = 0.0;

    // --- NEW MAKE UP CLASS FIELDS ---
    @Column(name = "make_up_lec_hours")
    private Double makeUpLecHours = 0.0;
    
    @Column(name = "make_up_lab_hours")
    private Double makeUpLabHours = 0.0;
    
    @Column(name = "make_up_pay")
    private BigDecimal makeUpPay = BigDecimal.ZERO;
    // --------------------------------

    private BigDecimal hourlyRate = BigDecimal.ZERO;
    private BigDecimal labRate = BigDecimal.ZERO; 

    private BigDecimal lecPay = BigDecimal.ZERO;
    private BigDecimal labPay = BigDecimal.ZERO;
    
    private BigDecimal holidayPay = BigDecimal.ZERO;
    private BigDecimal suspensionDeduction = BigDecimal.ZERO;
    
    private Double totalRleHours = 0.0;
    private BigDecimal rleRate = BigDecimal.ZERO;
    private BigDecimal rlePay = BigDecimal.ZERO;
    
    private Double substituteHours = 0.0;
    private BigDecimal substitutePay = BigDecimal.ZERO;

    private Double adjustmentHours = 0.0;
    private BigDecimal adjustmentPay = BigDecimal.ZERO;
    
    @Column(length = 500)
    private String adjustmentRemarks;
    
    private Double deductionHours = 0.0;
    private Double absentDeductionHours = 0.0; 
    
    private Double sgdHours = 0.0;
    private Double tutorialLecHours = 0.0;
    private Double tutorialLabHours = 0.0;
    
    private BigDecimal sgdPay = BigDecimal.ZERO;
    private BigDecimal tutorialLecPay = BigDecimal.ZERO;
    private BigDecimal tutorialLabPay = BigDecimal.ZERO;
    
    private BigDecimal honorarium = BigDecimal.ZERO;
    private BigDecimal adminPay = BigDecimal.ZERO;
    private BigDecimal supplementalPay = BigDecimal.ZERO;

    private BigDecimal totalTeachingPay = BigDecimal.ZERO; 
    private String workloadClassification;
   

    public TeachingPay() {}

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public Double getAbsentDeductionLecHours() { return absentDeductionLecHours; }
    public void setAbsentDeductionLecHours(Double absentDeductionLecHours) { this.absentDeductionLecHours = absentDeductionLecHours; }
    public Double getAbsentDeductionLabHours() { return absentDeductionLabHours; }
    public void setAbsentDeductionLabHours(Double absentDeductionLabHours) { this.absentDeductionLabHours = absentDeductionLabHours; }
    public BigDecimal getAbsentDeductionPay() { return absentDeductionPay; }
    public void setAbsentDeductionPay(BigDecimal absentDeductionPay) { this.absentDeductionPay = absentDeductionPay; }
    
    public Double getTotalLecUnits() { return totalLecUnits; }
    public void setTotalLecUnits(Double totalLecUnits) { this.totalLecUnits = totalLecUnits; }
    public Double getTotalLabUnits() { return totalLabUnits; }
    public void setTotalLabUnits(Double totalLabUnits) { this.totalLabUnits = totalLabUnits; }
    
    public Double getTotalLecHours() { return this.totalLecHours != null ? this.totalLecHours : 0.0; }
    public void setTotalLecHours(Double totalLecHours) { this.totalLecHours = totalLecHours; }
    public Double getTotalLabHours() { return this.totalLabHours != null ? this.totalLabHours : 0.0; }
    public void setTotalLabHours(Double totalLabHours) { this.totalLabHours = totalLabHours; }

    public Double getExcessLecUnits() { return excessLecUnits; }
    public void setExcessLecUnits(Double excessLecUnits) { this.excessLecUnits = excessLecUnits; }
    public Double getExcessLabUnits() { return excessLabUnits; }
    public void setExcessLabUnits(Double excessLabUnits) { this.excessLabUnits = excessLabUnits; }
    public Double getExcessLecHours() { return excessLecHours; }
    public void setExcessLecHours(Double excessLecHours) { this.excessLecHours = excessLecHours; }
    public Double getExcessLabHours() { return excessLabHours; }
    public void setExcessLabHours(Double excessLabHours) { this.excessLabHours = excessLabHours; }
    
    public Double getExcessRleHours() { return this.excessRleHours != null ? this.excessRleHours : 0.0; }
    public void setExcessRleHours(Double excessRleHours) { this.excessRleHours = excessRleHours; }
    
    public Double getTotalExcessHours() { return totalExcessHours; }
    public void setTotalExcessHours(Double totalExcessHours) { this.totalExcessHours = totalExcessHours; }
    
    // --- MAKE UP GETTERS / SETTERS ---
    public Double getMakeUpLecHours() { return makeUpLecHours != null ? makeUpLecHours : 0.0; }
    public void setMakeUpLecHours(Double makeUpLecHours) { this.makeUpLecHours = makeUpLecHours; }
    public Double getMakeUpLabHours() { return makeUpLabHours != null ? makeUpLabHours : 0.0; }
    public void setMakeUpLabHours(Double makeUpLabHours) { this.makeUpLabHours = makeUpLabHours; }
    public BigDecimal getMakeUpPay() { return makeUpPay != null ? makeUpPay : BigDecimal.ZERO; }
    public void setMakeUpPay(BigDecimal makeUpPay) { this.makeUpPay = makeUpPay; }
    // -------------------------------------

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public BigDecimal getLabRate() { return labRate; }
    public void setLabRate(BigDecimal labRate) { this.labRate = labRate; }
    public BigDecimal getLecPay() { return lecPay; }
    public void setLecPay(BigDecimal lecPay) { this.lecPay = lecPay; }
    public BigDecimal getLabPay() { return labPay; }
    public void setLabPay(BigDecimal labPay) { this.labPay = labPay; }
    public BigDecimal getHolidayPay() { return holidayPay; }
    public void setHolidayPay(BigDecimal holidayPay) { this.holidayPay = holidayPay; }
    public BigDecimal getSuspensionDeduction() { return suspensionDeduction; }
    public void setSuspensionDeduction(BigDecimal suspensionDeduction) { this.suspensionDeduction = suspensionDeduction; }
    public Double getTotalRleHours() { return totalRleHours; }
    public void setTotalRleHours(Double totalRleHours) { this.totalRleHours = totalRleHours; }
    public BigDecimal getRleRate() { return rleRate; }
    public void setRleRate(BigDecimal rleRate) { this.rleRate = rleRate; }
    public BigDecimal getRlePay() { return rlePay; }
    public void setRlePay(BigDecimal rlePay) { this.rlePay = rlePay; }
    public Double getSubstituteHours() { return substituteHours; }
    public void setSubstituteHours(Double substituteHours) { this.substituteHours = substituteHours; }
    public BigDecimal getSubstitutePay() { return substitutePay; }
    public void setSubstitutePay(BigDecimal substitutePay) { this.substitutePay = substitutePay; }
    public Double getAdjustmentHours() { return adjustmentHours; }
    public void setAdjustmentHours(Double adjustmentHours) { this.adjustmentHours = adjustmentHours; }
    public BigDecimal getAdjustmentPay() { return adjustmentPay; }
    public void setAdjustmentPay(BigDecimal adjustmentPay) { this.adjustmentPay = adjustmentPay; }
    
    public String getAdjustmentRemarks() { return adjustmentRemarks; }
    public void setAdjustmentRemarks(String adjustmentRemarks) { this.adjustmentRemarks = adjustmentRemarks; }
    
    public Double getDeductionHours() { return deductionHours; }
    public void setDeductionHours(Double deductionHours) { this.deductionHours = deductionHours; }
    public Double getAbsentDeductionHours() { return absentDeductionHours; }
    public void setAbsentDeductionHours(Double absentDeductionHours) { this.absentDeductionHours = absentDeductionHours; }
    public BigDecimal getHonorarium() { return honorarium; }
    public void setHonorarium(BigDecimal honorarium) { this.honorarium = honorarium; }
    public BigDecimal getAdminPay() { return adminPay; }
    public void setAdminPay(BigDecimal adminPay) { this.adminPay = adminPay; }
    public BigDecimal getSupplementalPay() { return supplementalPay; }
    public void setSupplementalPay(BigDecimal supplementalPay) { this.supplementalPay = supplementalPay; }
    public BigDecimal getTotalTeachingPay() { return totalTeachingPay; }
    public void setTotalTeachingPay(BigDecimal totalTeachingPay) { this.totalTeachingPay = totalTeachingPay; }
    public String getWorkloadClassification() { return workloadClassification; }
    public void setWorkloadClassification(String workloadClassification) { this.workloadClassification = workloadClassification; }
    public Double getSgdHours() { return sgdHours; }
    public void setSgdHours(Double sgdHours) { this.sgdHours = sgdHours; }
    public Double getTutorialLecHours() { return tutorialLecHours; }
    public void setTutorialLecHours(Double tutorialLecHours) { this.tutorialLecHours = tutorialLecHours; }
    public Double getTutorialLabHours() { return tutorialLabHours; }
    public void setTutorialLabHours(Double tutorialLabHours) { this.tutorialLabHours = tutorialLabHours; }
    public BigDecimal getSgdPay() { return sgdPay; }
    public void setSgdPay(BigDecimal sgdPay) { this.sgdPay = sgdPay; }
    public BigDecimal getTutorialLecPay() { return tutorialLecPay; }
    public void setTutorialLecPay(BigDecimal tutorialLecPay) { this.tutorialLecPay = tutorialLecPay; }
    public BigDecimal getTutorialLabPay() { return tutorialLabPay; }
    public void setTutorialLabPay(BigDecimal tutorialLabPay) { this.tutorialLabPay = tutorialLabPay; }
    public String getAppliedSuspensionDates() { return appliedSuspensionDates; }
    public void setAppliedSuspensionDates(String appliedSuspensionDates) { this.appliedSuspensionDates = appliedSuspensionDates; }

    public BigDecimal getAdjustmentHoursPay() {
        if (this.hourlyRate == null) return BigDecimal.ZERO;
        double safeHrs = this.adjustmentHours != null ? this.adjustmentHours : 0.0;
        return this.hourlyRate.multiply(BigDecimal.valueOf(safeHrs)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalAdjustmentPay() {
        BigDecimal flat = this.adjustmentPay != null ? this.adjustmentPay : BigDecimal.ZERO;
        return flat.add(getAdjustmentHoursPay());
    }

    public Double getTotalDeductionHours() { 
        return (this.deductionHours != null ? this.deductionHours : 0.0); 
    }

    public void calculatePay() {
        if (this.hourlyRate == null) this.hourlyRate = BigDecimal.ZERO;
        if (this.holidayPay == null) this.holidayPay = BigDecimal.ZERO;
        if (this.suspensionDeduction == null) this.suspensionDeduction = BigDecimal.ZERO;
        if (this.rleRate == null) this.rleRate = BigDecimal.ZERO; 
        if (this.honorarium == null) this.honorarium = BigDecimal.ZERO;
        if (this.adminPay == null) this.adminPay = BigDecimal.ZERO;
        if (this.supplementalPay == null) this.supplementalPay = BigDecimal.ZERO;
        if (this.adjustmentPay == null) this.adjustmentPay = BigDecimal.ZERO;

        this.labRate = this.hourlyRate.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP);

        double totalLecHrs = getTotalLecHours();
        double totalLabHrs = getTotalLabHours();
        double totalRleHrs = this.totalRleHours != null ? this.totalRleHours : 0.0;

        String empStatus = this.employee != null && this.employee.getEmployeeStatus() != null ? this.employee.getEmployeeStatus().toLowerCase() : "";
        
        // ==========================================================
        // WEEKLY BASE LOGIC
        // ==========================================================
        double weeksInCutoff = 1.0; 
        if (this.periodStart != null && this.periodEnd != null) {
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(this.periodStart, this.periodEnd) + 1;
            weeksInCutoff = Math.round(totalDays / 7.0); 
        }

        double dynamicRequiredBase = BASE_REQUIRED_UNITS * weeksInCutoff;
        double deficitUnits = 0.0;

        // DETECT UNDERLOAD: Incorporates the 1:2 Lab Substitution Rule!
        double assignedLec = this.totalLecUnits != null ? this.totalLecUnits : 0.0;
        double assignedLab = this.totalLabUnits != null ? this.totalLabUnits : 0.0;
        double effectiveAssignedUnits = assignedLec + ((assignedLab * 3.0) / 2.0);

        if (empStatus.contains("part")) {
            this.workloadClassification = "Part-Time";
            deficitUnits = 0.0;
        } else if (empStatus.contains("medicine")) {
            this.workloadClassification = "Medicine";
            deficitUnits = 0.0;
        } else if (empStatus.contains("flexi")) {
            this.workloadClassification = "Full-Time Flexi";
            deficitUnits = 0.0; 
        } else if (effectiveAssignedUnits > 0 && effectiveAssignedUnits < 15.0) {
            this.workloadClassification = "Full-Time (Underload)";
            deficitUnits = 0.0;
        } else {
            this.workloadClassification = "Regular Full-Time";
            deficitUnits = dynamicRequiredBase; 
        }

        double deductedLecHours = Math.min(deficitUnits * 1.0, totalLecHrs);
        deficitUnits -= (deductedLecHours / 1.0);
        
        // ✅ FIX: Removed Math.round to preserve decimals like .5!
        this.excessLecHours = totalLecHrs - deductedLecHours;
        this.excessLecUnits = this.excessLecHours; 

        double labHoursNeeded = deficitUnits * 2.0;
        double deductedLabHours = Math.min(labHoursNeeded, totalLabHrs);
        deficitUnits -= (deductedLabHours / 2.0);
        
        // ✅ FIX: Removed Math.round to preserve exact half hours
        this.excessLabHours = totalLabHrs - deductedLabHours;
        this.excessLabUnits = this.excessLabHours / 3.0;

        double rleHoursNeeded = deficitUnits * 1.5;
        double deductedRleHours = Math.min(rleHoursNeeded, totalRleHrs);
        deficitUnits -= (deductedRleHours / 1.5);
        
        // ✅ FIX: Removed Math.round here as well
        this.excessRleHours = totalRleHrs - deductedRleHours;

        this.totalExcessHours = this.excessLecHours + this.excessLabHours + this.excessRleHours;
        
        // DELETED THIS LINE HERE: this.absentDeductionHours = deficitUnits;

        // --- COMPUTING PAY ---
        this.lecPay = this.hourlyRate.multiply(BigDecimal.valueOf(this.excessLecHours)).setScale(2, RoundingMode.HALF_UP);
        this.labPay = this.labRate.multiply(BigDecimal.valueOf(this.excessLabHours)).setScale(2, RoundingMode.HALF_UP);
        this.rlePay = this.rleRate.multiply(BigDecimal.valueOf(this.excessRleHours)).setScale(2, RoundingMode.HALF_UP);

        double safeSub = this.substituteHours != null ? this.substituteHours : 0.0;
        this.substitutePay = this.hourlyRate.multiply(BigDecimal.valueOf(safeSub)).setScale(2, RoundingMode.HALF_UP);

        double safeSgd = this.sgdHours != null ? this.sgdHours : 0.0;
        this.sgdPay = this.hourlyRate.multiply(BigDecimal.valueOf(safeSgd)).setScale(2, RoundingMode.HALF_UP);

        double safeTutLec = this.tutorialLecHours != null ? this.tutorialLecHours : 0.0;
        this.tutorialLecPay = this.hourlyRate.multiply(BigDecimal.valueOf(safeTutLec)).setScale(2, RoundingMode.HALF_UP);

        double safeTutLab = this.tutorialLabHours != null ? this.tutorialLabHours : 0.0;
        this.tutorialLabPay = this.labRate.multiply(BigDecimal.valueOf(safeTutLab)).setScale(2, RoundingMode.HALF_UP);
        
        // --- ABSENT DEDUCTIONS ---
        double safeAbsLec = this.absentDeductionLecHours != null ? this.absentDeductionLecHours : 0.0;
        double safeAbsLab = this.absentDeductionLabHours != null ? this.absentDeductionLabHours : 0.0;
        double safeAbsGen = this.absentDeductionHours != null ? this.absentDeductionHours : 0.0;

        this.absentDeductionPay = this.hourlyRate.multiply(BigDecimal.valueOf(safeAbsLec))
            .add(this.labRate.multiply(BigDecimal.valueOf(safeAbsLab)))
            .add(this.hourlyRate.multiply(BigDecimal.valueOf(safeAbsGen)))
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal manualDeductionAmount = this.hourlyRate.multiply(BigDecimal.valueOf(this.getTotalDeductionHours())).setScale(2, RoundingMode.HALF_UP);

        // --- MAKE UP PAY CALCULATION ---
        double safeMakeUpLec = this.makeUpLecHours != null ? this.makeUpLecHours : 0.0;
        double safeMakeUpLab = this.makeUpLabHours != null ? this.makeUpLabHours : 0.0;

        this.makeUpPay = this.hourlyRate.multiply(BigDecimal.valueOf(safeMakeUpLec))
            .add(this.labRate.multiply(BigDecimal.valueOf(safeMakeUpLab)))
            .setScale(2, RoundingMode.HALF_UP);

        // --- TOTAL TEACHING PAY CALCULATION ---
        this.totalTeachingPay = this.lecPay.add(this.labPay).add(this.rlePay).add(this.substitutePay)
                .add(this.sgdPay).add(this.tutorialLecPay).add(this.tutorialLabPay).add(this.honorarium)
                .add(this.adminPay).add(this.supplementalPay).add(this.getTotalAdjustmentPay())
                .add(this.makeUpPay) 
                .add(this.holidayPay)
                .subtract(this.suspensionDeduction)
                .subtract(manualDeductionAmount)
                .subtract(this.absentDeductionPay); // ✅ FIX: ADDED THIS SUBTRACTION
                
        this.totalTeachingPay = this.totalTeachingPay.max(BigDecimal.ZERO);
    }
}