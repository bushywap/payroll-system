package com.capstone.payroll.service;

import com.capstone.payroll.model.*;
import com.capstone.payroll.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PayrollService {

    @Autowired private PayrollRepository payrollRepository;
    @Autowired private AttendanceService attendanceService;
    @Autowired private TeachingPayRepository teachingPayRepository; 
    
    @Autowired private LeaveRepository leaveRepository;
    @Autowired private HolidayRepository holidayRepo;
    @Autowired private SuspensionRepository suspensionRepo;
    @Autowired private TeachingLoadRepository facultyLoadRepository; 
    
    @Autowired private SubstituteRecordRepository substituteRecordRepository; 

    private boolean isEmployeePartTime(Employee emp) {
        if (emp == null) return false;
        String status = emp.getEmployeeStatus() != null ? emp.getEmployeeStatus().toLowerCase() : "";
        String desig = (emp.getDesignation() != null && emp.getDesignation().getDesignation() != null) 
                        ? emp.getDesignation().getDesignation().toLowerCase() : "";
        return status.contains("part") || status.contains("flexi") || desig.contains("part") || desig.contains("flexi");
    }

    public void computeTaxableIncomeAndTax(Payroll payroll) {
        BigDecimal basic = payroll.getBasicSalary() != null ? payroll.getBasicSalary() : BigDecimal.ZERO;
        
        boolean isPartTime = isEmployeePartTime(payroll.getEmployee());
        
        // ✅ FIX 1: Detect Underload correctly by reading TeachingPay's exact classification
        boolean isUnderload = false;
        if (!isPartTime && payroll.getTeachingPayRecord() != null) {
            if ("Full-Time (Underload)".equals(payroll.getTeachingPayRecord().getWorkloadClassification())) {
                isUnderload = true;
            }
        }
        
        BigDecimal teachingPayNet = BigDecimal.ZERO;
        BigDecimal teachingDeductions = BigDecimal.ZERO;
        BigDecimal hourlyAbsenceDed = BigDecimal.ZERO;
        
        if (payroll.getTeachingPayRecord() != null) {
            TeachingPay tp = payroll.getTeachingPayRecord();
            BigDecimal hrRate = tp.getHourlyRate() != null ? tp.getHourlyRate() : BigDecimal.ZERO;
            
            double originalAbsentHrs = tp.getAbsentDeductionHours() != null ? tp.getAbsentDeductionHours() : 0.0;
            
            hourlyAbsenceDed = hrRate.multiply(BigDecimal.valueOf(originalAbsentHrs)).setScale(2, RoundingMode.HALF_UP);
            
            tp.setAbsentDeductionHours(0.0);
            tp.calculatePay(); 
            teachingPayNet = tp.getTotalTeachingPay() != null ? tp.getTotalTeachingPay() : BigDecimal.ZERO;
            
            double safeDedHrs = tp.getTotalDeductionHours() != null ? tp.getTotalDeductionHours() : 0.0;
            BigDecimal manualDed = hrRate.multiply(BigDecimal.valueOf(safeDedHrs)).setScale(2, RoundingMode.HALF_UP);
            teachingDeductions = manualDed;
            
            tp.setAbsentDeductionHours(originalAbsentHrs);
        }
        
        BigDecimal teachingPayGross = teachingPayNet.add(teachingDeductions);
        
        BigDecimal minuteRate = payroll.getMinuteRate() != null ? payroll.getMinuteRate() : BigDecimal.ZERO;
        int lateMins = payroll.getLateMinutes() != null ? payroll.getLateMinutes() : 0;
        int utMins = payroll.getUndertimeMinutes() != null ? payroll.getUndertimeMinutes() : 0;
        
        BigDecimal lates = minuteRate.multiply(BigDecimal.valueOf(lateMins)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal undertime = minuteRate.multiply(BigDecimal.valueOf(utMins)).setScale(2, RoundingMode.HALF_UP);
        
        payroll.setLateDeduction(lates);
        payroll.setUndertimeDeduction(undertime);
        
        BigDecimal absences = BigDecimal.ZERO;
        
        if (!isPartTime && !isUnderload) {
            BigDecimal dailyRate = payroll.getDailyRate() != null ? payroll.getDailyRate() : BigDecimal.ZERO;
            int totalAbsences = payroll.getTotalAbsences() != null ? payroll.getTotalAbsences() : 0;
            BigDecimal regularAbsenceDed = dailyRate.multiply(BigDecimal.valueOf(totalAbsences)).setScale(2, RoundingMode.HALF_UP);
            
            absences = regularAbsenceDed.add(teachingDeductions);
        } else {
            absences = hourlyAbsenceDed.add(teachingDeductions);
        }
        
        payroll.setAbsentDeduction(absences);

        BigDecimal ot = payroll.getOvertimePay() != null ? payroll.getOvertimePay() : BigDecimal.ZERO;
        BigDecimal holiday = payroll.getHolidayPay() != null ? payroll.getHolidayPay() : BigDecimal.ZERO;
        BigDecimal honorarium = payroll.getHonorarium() != null ? payroll.getHonorarium() : BigDecimal.ZERO;
        BigDecimal longevity = payroll.getLongevity() != null ? payroll.getLongevity() : BigDecimal.ZERO;
        BigDecimal adjustment = payroll.getAdjustment() != null ? payroll.getAdjustment() : BigDecimal.ZERO;
        BigDecimal deMinimis = payroll.getDeMinimis() != null ? payroll.getDeMinimis() : BigDecimal.ZERO;
        BigDecimal cashGift = payroll.getCashGift() != null ? payroll.getCashGift() : BigDecimal.ZERO;
        BigDecimal incentive = payroll.getIncentive() != null ? payroll.getIncentive() : BigDecimal.ZERO;
        BigDecimal allowance = payroll.getAllowance() != null ? payroll.getAllowance() : BigDecimal.ZERO;
        BigDecimal relocationPay = payroll.getRelocationPay() != null ? payroll.getRelocationPay() : BigDecimal.ZERO;
        BigDecimal manualAddition = payroll.getManualAddition() != null ? payroll.getManualAddition() : BigDecimal.ZERO; 

        BigDecimal totalEarnings = basic.add(teachingPayGross).add(ot).add(holiday)
                .add(honorarium).add(longevity).add(adjustment).add(deMinimis)
                .add(cashGift).add(incentive).add(allowance).add(relocationPay)
                .add(manualAddition); 
        payroll.setTotalEarnings(totalEarnings);

        BigDecimal penalties = absences.add(lates).add(undertime);
        
        BigDecimal grossIncome = totalEarnings.subtract(penalties);
        if (grossIncome.compareTo(BigDecimal.ZERO) < 0) grossIncome = BigDecimal.ZERO;
        payroll.setGrossIncome(grossIncome);

        BigDecimal finalGross = payroll.getGrossIncome() != null ? payroll.getGrossIncome() : BigDecimal.ZERO;
        
        BigDecimal nonTaxableIncomes = deMinimis.add(longevity).add(cashGift).add(incentive);
        BigDecimal taxableIncomeBase = finalGross.subtract(nonTaxableIncomes); 
        BigDecimal govtDeductionBase = taxableIncomeBase;

        if (govtDeductionBase.compareTo(BigDecimal.ZERO) > 0) {
            boolean isFirstCutoff = payroll.getPayPeriodEnd() != null && payroll.getPayPeriodEnd().getDayOfMonth() <= 15;
            
            BigDecimal sssMsc;
            if (govtDeductionBase.compareTo(new BigDecimal("5250")) < 0) {
                sssMsc = new BigDecimal("5000");
            } else if (govtDeductionBase.compareTo(new BigDecimal("34750")) >= 0) {
                sssMsc = new BigDecimal("35000");
            } else {
                sssMsc = govtDeductionBase.divide(new BigDecimal("500"), 0, RoundingMode.HALF_UP).multiply(new BigDecimal("500"));
            }
            
            payroll.setSssDeduction(sssMsc.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP));
            payroll.setSssEmployerShare(sssMsc.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP));
            
            if (isFirstCutoff) {
                BigDecimal phicBase = govtDeductionBase.max(new BigDecimal("10000")).min(new BigDecimal("100000"));
                payroll.setPhilhealthDeduction(phicBase.multiply(new BigDecimal("0.025")).setScale(2, RoundingMode.HALF_UP));
                payroll.setPhilhealthEmployerShare(payroll.getPhilhealthDeduction());
            } else {
                payroll.setPhilhealthDeduction(BigDecimal.ZERO);
                payroll.setPhilhealthEmployerShare(BigDecimal.ZERO);
            }
            
            if (!isFirstCutoff) {
                payroll.setPagibigDeduction(new BigDecimal("200.00"));
                payroll.setPagibigEmployerShare(new BigDecimal("200.00"));
            } else {
                payroll.setPagibigDeduction(BigDecimal.ZERO);
                payroll.setPagibigEmployerShare(BigDecimal.ZERO);
            }
        } else {
            payroll.setSssDeduction(BigDecimal.ZERO);
            payroll.setPhilhealthDeduction(BigDecimal.ZERO);
            payroll.setPagibigDeduction(BigDecimal.ZERO);
        }
        
        BigDecimal sss = payroll.getSssDeduction() != null ? payroll.getSssDeduction() : BigDecimal.ZERO;
        BigDecimal phic = payroll.getPhilhealthDeduction() != null ? payroll.getPhilhealthDeduction() : BigDecimal.ZERO;
        BigDecimal hdmf = payroll.getPagibigDeduction() != null ? payroll.getPagibigDeduction() : BigDecimal.ZERO;
        
        BigDecimal totalGovt = sss.add(phic).add(hdmf);
        payroll.setGovtContributions(totalGovt);

        BigDecimal taxableIncome = taxableIncomeBase.subtract(totalGovt); 
        if (taxableIncome.compareTo(BigDecimal.ZERO) < 0) taxableIncome = BigDecimal.ZERO;
        
        payroll.setTaxableIncome(taxableIncome);
        payroll.setWithholdingTax(calculateBIRSemiMonthlyTax(taxableIncome));
    }

    private BigDecimal calculateBIRSemiMonthlyTax(BigDecimal taxableIncome) {
        if (taxableIncome == null || taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        double income = taxableIncome.doubleValue();
        double tax = 0.0;
        
        if (income <= 10417.00) {
            tax = 0.0;
        } else if (income <= 16666.00) {
            tax = (income - 10417.00) * 0.15;
        } else if (income <= 33333.00) {
            tax = 937.50 + ((income - 16666.00) * 0.20);
        } else if (income <= 83333.00) {
            tax = 4270.70 + ((income - 33333.00) * 0.25);
        } else if (income <= 333333.00) {
            tax = 16770.70 + ((income - 83333.00) * 0.30);
        } else {
            tax = 91770.70 + ((income - 333333.00) * 0.35);
        }
        
        return BigDecimal.valueOf(tax).setScale(2, RoundingMode.HALF_UP);
    }

    private TeachingPay getOrComputeTeachingPay(Employee emp, LocalDate start, LocalDate end, BigDecimal hourlyRate, List<Attendance> attendanceRecords) {
        TeachingPay computedTp = processTeachingLogic(emp, start, end, hourlyRate, null, null, attendanceRecords);

        TeachingPay dbTp = null;
        try { dbTp = teachingPayRepository.findByEmployeeAndPeriodStartAndPeriodEnd(emp, start, end); } 
        catch (Exception e) {}

        if (computedTp != null && dbTp != null) {
            computedTp.setId(dbTp.getId()); 
            
            if (dbTp.getAdjustmentHours() != null) computedTp.setAdjustmentHours(dbTp.getAdjustmentHours());
            if (dbTp.getAdjustmentPay() != null) computedTp.setAdjustmentPay(dbTp.getAdjustmentPay());
            if (dbTp.getDeductionHours() != null) computedTp.setDeductionHours(dbTp.getDeductionHours());
            if (dbTp.getSgdHours() != null) computedTp.setSgdHours(dbTp.getSgdHours());
            if (dbTp.getTutorialLecHours() != null) computedTp.setTutorialLecHours(dbTp.getTutorialLecHours());
            if (dbTp.getTutorialLabHours() != null) computedTp.setTutorialLabHours(dbTp.getTutorialLabHours());
            if (dbTp.getHonorarium() != null) computedTp.setHonorarium(dbTp.getHonorarium());
            if (dbTp.getAdminPay() != null) computedTp.setAdminPay(dbTp.getAdminPay());
            if (dbTp.getSupplementalPay() != null) computedTp.setSupplementalPay(dbTp.getSupplementalPay());
            
            if (dbTp.getMakeUpLecHours() != null) computedTp.setMakeUpLecHours(dbTp.getMakeUpLecHours());
            if (dbTp.getMakeUpLabHours() != null) computedTp.setMakeUpLabHours(dbTp.getMakeUpLabHours());
            if (dbTp.getMakeUpPay() != null) computedTp.setMakeUpPay(dbTp.getMakeUpPay());
        }

        if (computedTp != null) {
            computedTp.calculatePay();
            return computedTp;
        } else if (dbTp != null) {
            dbTp.calculatePay();
            return dbTp;
        }
        return null;
    }

    private TeachingPay processTeachingLogic(Employee emp, LocalDate start, LocalDate end, BigDecimal hourlyRate, BigDecimal manualHoliday, BigDecimal manualSuspension, List<Attendance> attendanceRecords) {
        List<TeachingLoad> loads = null;
        try { loads = facultyLoadRepository.findByEmployeeEmployeeId(emp.getEmployeeId()); } catch(Exception e) {}
        
        if (loads == null || loads.isEmpty()) return null;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

        double semLecUnits = 0.0;
        double semLabUnits = 0.0;
        double theoreticalTotalLabHours = 0.0; 
        java.util.Set<Long> processedLoadIds = new java.util.HashSet<>();
        
        for (TeachingLoad load : loads) {
            if (!processedLoadIds.contains(load.getId())) {
                int sections = load.getNoOfSections() > 0 ? load.getNoOfSections() : 1;
                semLecUnits += (load.getLectureUnits() * sections);
                semLabUnits += (load.getLabUnits() * sections);
                
                double scheduledDuration = 0.0;
                java.time.LocalTime cStart = load.getStartTime();
                java.time.LocalTime cEnd = load.getEndTime();
                
                if ((cStart == null || cEnd == null) && load.getTimeSchedule() != null) {
                    java.time.LocalTime[] extracted = extractTimes(load.getTimeSchedule());
                    if (extracted != null) { cStart = extracted[0]; cEnd = extracted[1]; }
                }
                if (cStart != null && cEnd != null) {
                    scheduledDuration = java.time.Duration.between(cStart, cEnd).toMinutes() / 60.0;
                }

                double baseLabHrs = 0.0;
                if (load.getLabHours() != null && load.getLabHours() > 0) {
                    baseLabHrs = load.getLabHours();
                } else if (scheduledDuration > 0 && load.getLabUnits() > 0) {
                    baseLabHrs = scheduledDuration; 
                } else {
                    baseLabHrs = load.getLabUnits() * 3.0;
                }
                
                theoreticalTotalLabHours += (baseLabHrs * sections);
                processedLoadIds.add(load.getId());
            }
        }

        boolean isPartTime = isEmployeePartTime(emp);
        double reqUnits = isPartTime ? 0.0 : 15.0; 

        double effectiveSemUnits = semLecUnits + (theoreticalTotalLabHours / 2.0);
        boolean isUnderload = !isPartTime && (effectiveSemUnits > 0 && effectiveSemUnits < 15.0);

        double excessLecUnits = 0.0;
        double theoreticalExcessLabHours = 0.0;

        if (!isPartTime && !isUnderload) {
            if (semLecUnits >= reqUnits) {
                excessLecUnits = semLecUnits - reqUnits;
                theoreticalExcessLabHours = theoreticalTotalLabHours; 
            } else {
                double lecDeficit = reqUnits - semLecUnits;      
                double labHoursNeeded = lecDeficit * 2.0;        

                if (theoreticalTotalLabHours >= labHoursNeeded) {
                    theoreticalExcessLabHours = theoreticalTotalLabHours - labHoursNeeded;
                    excessLecUnits = 0.0; 
                } else {
                    theoreticalExcessLabHours = 0.0; 
                    excessLecUnits = 0.0; 
                }
            }
        } else {
            excessLecUnits = semLecUnits;
            theoreticalExcessLabHours = theoreticalTotalLabHours;
        }

        double excessLabUnits = theoreticalExcessLabHours / 3.0; 

        double renderedLecHours = 0.0, renderedLabHours = 0.0, renderedRleHours = 0.0;
        double totalSuspendedHours = 0.0, suspendedLecHours = 0.0, suspendedLabHours = 0.0;
        
        List<Holidays> holidaysList = holidayRepo.findAll();
        List<Suspension> suspensionsList = suspensionRepo.findAll();
        java.util.Set<String> appliedSuspensions = new java.util.LinkedHashSet<>();
        
        double absentDeductHours = 0.0;
        boolean hasAnyLogs = attendanceRecords != null && !attendanceRecords.isEmpty();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            String dayShort = currentDate.getDayOfWeek().name().substring(0, 3); 

            boolean isHoliday = holidaysList.stream().anyMatch(h -> h.getDate() != null && h.getDate().equals(currentDate));
            Suspension activeSuspension = suspensionsList.stream().filter(s -> s.getDate() != null && s.getDate().equals(currentDate)).findFirst().orElse(null);

            boolean hasValidAttendance = false;
            if (hasAnyLogs) {
                hasValidAttendance = attendanceRecords.stream().anyMatch(a -> a.getDate() != null && a.getDate().equals(currentDate) && 
                                     (a.getTimeIn() != null || (a.getTotalHours() != null && a.getTotalHours() > 0)));
            }

            for (TeachingLoad load : loads) {
                if (load.getDayOfWeek() != null && load.getDayOfWeek().toUpperCase().contains(dayShort)) {
                    int sections = load.getNoOfSections() > 0 ? load.getNoOfSections() : 1;
                    
                    java.time.LocalTime classStart = load.getStartTime();
                    java.time.LocalTime classEnd = load.getEndTime();
                    
                    if ((classStart == null || classEnd == null) && load.getTimeSchedule() != null) {
                        java.time.LocalTime[] extracted = extractTimes(load.getTimeSchedule());
                        if (extracted != null) {
                            classStart = extracted[0];
                            classEnd = extracted[1];
                        }
                    }
                    
                    double scheduledDuration = 0.0;
                    if (classStart != null && classEnd != null) {
                        scheduledDuration = java.time.Duration.between(classStart, classEnd).toMinutes() / 60.0;
                    }

                    double baseLecHrs = load.getLecHours() != null && load.getLecHours() > 0 ? load.getLecHours() : 
                                       (load.getLectureUnits() > 0 && scheduledDuration > 0 ? scheduledDuration : load.getLectureUnits() * 1.0);
                                       
                    double baseLabHrs = load.getLabHours() != null && load.getLabHours() > 0 ? load.getLabHours() : 
                                       (load.getLabUnits() > 0 && scheduledDuration > 0 ? scheduledDuration : load.getLabUnits() * 3.0);
                                       
                    double baseRleHrs = load.getRleHours() != null ? load.getRleHours() : 0.0;

                    boolean isSuspended = false;
                    double partialDeductHours = 0.0;

                    if (activeSuspension != null) {
                        if (activeSuspension.getStartTime() == null) {
                            isSuspended = true; 
                        } else if (classStart != null && classEnd != null) {
                            java.time.LocalTime suspStart = activeSuspension.getStartTime();
                            if (!classEnd.isAfter(suspStart)) isSuspended = false;
                            else if (!classStart.isBefore(suspStart)) isSuspended = true; 
                            else {
                                java.time.Duration duration = java.time.Duration.between(suspStart, classEnd);
                                partialDeductHours = duration.toMinutes() / 60.0;
                            }
                        }
                    }

                    if (isSuspended) {
                        totalSuspendedHours += (baseLecHrs + baseLabHrs) * sections;
                        suspendedLecHours += (baseLecHrs * sections);
                        suspendedLabHours += (baseLabHrs * sections);
                        
                        renderedLecHours += (baseLecHrs * sections);
                        renderedLabHours += (baseLabHrs * sections);
                        renderedRleHours += baseRleHrs;
                        
                        String timeLabel = activeSuspension.getStartTime() != null ? " @" + activeSuspension.getStartTime().format(timeFormatter) : " (Full Day)";
                        appliedSuspensions.add(currentDate.toString() + timeLabel);
                    } else if (!isHoliday) {
                        if (!isPartTime) {
                            if (!hasAnyLogs || hasValidAttendance) {
                                renderedLecHours += (baseLecHrs * sections);
                                renderedLabHours += (baseLabHrs * sections);
                                renderedRleHours += baseRleHrs;

                                if (partialDeductHours > 0) {
                                    totalSuspendedHours += partialDeductHours * sections;
                                    if (baseLecHrs > 0) suspendedLecHours += (partialDeductHours * sections);
                                    else suspendedLabHours += (partialDeductHours * sections);
                                    appliedSuspensions.add(currentDate.toString() + " [Partial Suspension]");
                                }
                            } else {
                                absentDeductHours += (baseLecHrs * sections) + (baseLabHrs * sections);
                            }
                        } else {
                            if (hasValidAttendance) {
                                renderedLecHours += (baseLecHrs * sections);
                                renderedLabHours += (baseLabHrs * sections);
                                renderedRleHours += baseRleHrs;
                            }
                        }
                    }
                }
            }
        }

        double finalExcessLecHours = 0.0;
        double finalExcessLabHours = 0.0;

        if (!isPartTime && !isUnderload) {
            if (semLecUnits > 0) finalExcessLecHours = excessLecUnits * (renderedLecHours / semLecUnits);
            if (theoreticalTotalLabHours > 0) {
                double percentageLabExcess = theoreticalExcessLabHours / theoreticalTotalLabHours;
                finalExcessLabHours = renderedLabHours * percentageLabExcess;
            }
        } else {
            finalExcessLecHours = renderedLecHours;
            finalExcessLabHours = renderedLabHours;
        }

        double subHoursAccrued = 0.0;
        try {
            List<SubstituteRecord> allSubs = substituteRecordRepository.findAll();
            for (SubstituteRecord sub : allSubs) {
                LocalDate subDate = sub.getDateSubstituted();
                if (subDate != null && start != null && end != null && !subDate.isBefore(start) && !subDate.isAfter(end)) {
                    if (sub.getSubstituteFaculty() != null && sub.getSubstituteFaculty().getId().equals(emp.getId())) {
                        subHoursAccrued += sub.getHoursRendered() != null ? sub.getHoursRendered() : 0.0;
                    }
                }
            }
        } catch(Exception e) {}
        
        if (renderedLecHours > 0 || renderedLabHours > 0 || renderedRleHours > 0 || subHoursAccrued > 0 || absentDeductHours > 0 || totalSuspendedHours > 0 || manualHoliday != null) {
            TeachingPay tp = new TeachingPay();
            tp.setEmployee(emp);
            tp.setPeriodStart(start);
            tp.setPeriodEnd(end);
            tp.setTotalLecUnits(semLecUnits);
            tp.setTotalLabUnits(semLabUnits);
            tp.setTotalLecHours(renderedLecHours);
            tp.setTotalLabHours(renderedLabHours);
            tp.setTotalRleHours(renderedRleHours);

            tp.setExcessLecUnits(excessLecUnits);
            tp.setExcessLabUnits(excessLabUnits);
            tp.setExcessLecHours(finalExcessLecHours);
            tp.setExcessLabHours(finalExcessLabHours); 
            
            tp.setSubstituteHours(subHoursAccrued);
            tp.setAbsentDeductionHours(absentDeductHours);
            tp.setHourlyRate(hourlyRate);
            
            if (manualHoliday != null) tp.setHolidayPay(manualHoliday);

            BigDecimal suspLecPayDed = hourlyRate.multiply(BigDecimal.valueOf(suspendedLecHours));
            BigDecimal labRateValue = hourlyRate.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal suspLabPayDed = labRateValue.multiply(BigDecimal.valueOf(suspendedLabHours));
            
            BigDecimal autoSuspensionDeduction = suspLecPayDed.add(suspLabPayDed).setScale(2, RoundingMode.HALF_UP);

            if (manualSuspension != null && manualSuspension.compareTo(BigDecimal.ZERO) > 0) {
                tp.setSuspensionDeduction(manualSuspension);
            } else {
                tp.setSuspensionDeduction(autoSuspensionDeduction);
            }

            if (!appliedSuspensions.isEmpty()) tp.setAppliedSuspensionDates(String.join(", ", appliedSuspensions));
            else tp.setAppliedSuspensionDates("");
            
            tp.calculatePay(); 
            return tp;
        }
        return null;
    }

    public Payroll calculatePayrollPreview(Employee emp, LocalDate start, LocalDate end, 
            BigDecimal sssLoanInput, BigDecimal hdmfLoanInput, 
            BigDecimal adjustmentInput, BigDecimal honorariumInput, BigDecimal longevityInput) {

        List<Attendance> records = attendanceService.getAttendanceByEmpIdAndDateRange(emp.getEmployeeId(), start, end);
        boolean hasAnyLogs = records != null && !records.isEmpty();
        
        List<Leave> employeeLeaves = leaveRepository.findByEmployeeId(emp.getId()).stream()
                .filter(l -> "APPROVED".equalsIgnoreCase(l.getStatus()) && l.getStartDate() != null && l.getEndDate() != null &&
                             !l.getEndDate().isBefore(start) && !l.getStartDate().isAfter(end)).toList();
                             
        List<Holidays> holidaysList = holidayRepo.findAll();
        List<Suspension> suspensionsList = suspensionRepo.findAll();

        boolean isFirstCutoff = end != null && end.getDayOfMonth() <= 15;
        boolean isSecondCutoff = end != null && end.getDayOfMonth() > 15;
        BigDecimal appliedSssLoan = isFirstCutoff ? (sssLoanInput != null ? sssLoanInput : BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal appliedHdmfLoan = isSecondCutoff ? (hdmfLoanInput != null ? hdmfLoanInput : BigDecimal.ZERO) : BigDecimal.ZERO;

        BigDecimal hourlyRate = BigDecimal.ZERO;
        if (emp.getBasicSalary() != null && emp.getBasicSalary().compareTo(BigDecimal.ZERO) > 0) {
            hourlyRate = emp.getBasicSalary().multiply(new BigDecimal("12")).divide(new BigDecimal("313"), 5, RoundingMode.HALF_UP).divide(new BigDecimal("8"), 5, RoundingMode.HALF_UP);
        } else if (emp.getHourlyRate() != null) {
            hourlyRate = emp.getHourlyRate();
        }

        int totalLateMinutes = 0, totalUndertimeMinutes = 0, totalAbsences = 0, holidayDays = 0;
        double totalOvertimeMinutes = 0.0;
        int leaveWithoutPayDays = 0, leaveWithPayDays = 0; 

        List<TeachingLoad> loads = null;
        try { loads = facultyLoadRepository.findByEmployeeEmployeeId(emp.getEmployeeId()); } catch(Exception e) {}
        List<String> scheduledDays = new java.util.ArrayList<>();
        if (loads != null) {
            for (TeachingLoad load : loads) if (load.getDayOfWeek() != null) scheduledDays.add(load.getDayOfWeek().toUpperCase());
        }

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            java.time.DayOfWeek day = date.getDayOfWeek();
            boolean isRestDay = (day == java.time.DayOfWeek.SUNDAY) || (!scheduledDays.isEmpty() && !scheduledDays.contains(day.toString()));
            
            final LocalDate currentDate = date;
            
            Leave activeLeave = employeeLeaves.stream().filter(l -> !currentDate.isBefore(l.getStartDate()) && !currentDate.isAfter(l.getEndDate())).findFirst().orElse(null);
            
            Holidays activeHoliday = holidaysList.stream().filter(h -> h.getDate() != null && h.getDate().equals(currentDate)).findFirst().orElse(null);
            Suspension activeSuspension = suspensionsList.stream().filter(s -> s.getDate() != null && s.getDate().equals(currentDate)).findFirst().orElse(null);

            if (activeLeave != null) {
                if (!isRestDay) { 
                    String type = activeLeave.getLeaveType();
                    if (type != null && (type.equalsIgnoreCase("Leave Without Pay") || type.equalsIgnoreCase("Unpaid Leave"))) leaveWithoutPayDays++; 
                    else leaveWithPayDays++; 
                }
            } else {
                var recordOpt = records.stream().filter(r -> r.getDate() != null && r.getDate().equals(currentDate)).findFirst();
                boolean isPresent = recordOpt.isPresent() && ((recordOpt.get().getTimeIn() != null) || (recordOpt.get().getTotalHours() != null && recordOpt.get().getTotalHours() > 0));
                
                if (recordOpt.isEmpty() || !isPresent) {
                    if (!isRestDay) {
                        if (activeHoliday != null || activeSuspension != null) holidayDays++;
                        else if (hasAnyLogs) totalAbsences++; 
                    }
                } else {
                    Attendance a = recordOpt.get();
                    totalLateMinutes += a.getMinutesLate() != null ? a.getMinutesLate() : 0;
                    totalUndertimeMinutes += a.getUndertimeHours() != null ? a.getUndertimeHours() : 0; 
                    if (a.getOvertimeHours() != null && a.getOvertimeHours() > 0) totalOvertimeMinutes += a.getOvertimeHours();
                }
            }
        }

        BigDecimal dailyRate = hourlyRate.multiply(new BigDecimal("8"));
        BigDecimal rawMinuteRate = hourlyRate.divide(new BigDecimal("60"), 6, RoundingMode.HALF_UP);
        BigDecimal exactMinuteRate = rawMinuteRate.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalHolidayPay = dailyRate.multiply(new BigDecimal(holidayDays)).setScale(2, RoundingMode.HALF_UP);

        TeachingPay tp = getOrComputeTeachingPay(emp, start, end, hourlyRate, records);
        
        BigDecimal finalAdjustment = (adjustmentInput != null && adjustmentInput.compareTo(BigDecimal.ZERO) != 0) ? adjustmentInput : BigDecimal.ZERO;
        BigDecimal finalHonorarium = (honorariumInput != null && honorariumInput.compareTo(BigDecimal.ZERO) != 0) ? honorariumInput : (emp.getHonorarium() != null ? emp.getHonorarium() : BigDecimal.ZERO);
        BigDecimal finalLongevity = (longevityInput != null && longevityInput.compareTo(BigDecimal.ZERO) != 0) ? longevityInput : (emp.getLongevity() != null ? emp.getLongevity() : BigDecimal.ZERO);

        Payroll generatedPayroll = new Payroll();
        generatedPayroll.setEmployee(emp);
        generatedPayroll.setPayPeriodStart(start);
        generatedPayroll.setPayPeriodEnd(end);
        
        if (tp != null) generatedPayroll.setTeachingPayRecord(tp);
        
        generatedPayroll.setDailyRate(dailyRate);
        generatedPayroll.setMinuteRate(exactMinuteRate);
        generatedPayroll.setLateMinutes(totalLateMinutes);
        generatedPayroll.setUndertimeMinutes(totalUndertimeMinutes);
        generatedPayroll.setTotalAbsences(totalAbsences);
        
        BigDecimal leaveWithPayAmount = dailyRate.multiply(BigDecimal.valueOf(leaveWithPayDays)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal leaveWithoutPayAmount = dailyRate.multiply(BigDecimal.valueOf(leaveWithoutPayDays)).setScale(2, RoundingMode.HALF_UP);
        generatedPayroll.setLeavePay(leaveWithPayAmount);
        generatedPayroll.setLeaveWithoutPay(leaveWithoutPayAmount);
        
        BigDecimal exactOtRate = exactMinuteRate.multiply(new BigDecimal("1.25")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal otPay = exactOtRate.multiply(BigDecimal.valueOf(totalOvertimeMinutes)).setScale(2, RoundingMode.HALF_UP);
        generatedPayroll.setOvertimePay(otPay);
        generatedPayroll.setHolidayPay(totalHolidayPay);
        
        boolean isPartTime = isEmployeePartTime(emp);
        BigDecimal basicSalary = BigDecimal.ZERO;

        // ✅ FIX 2: Let TeachingPay decide if it's an Underload to protect the 1:2 substitution and Non-Faculty Admin
        if (!isPartTime) { 
            if (tp != null && "Full-Time (Underload)".equals(tp.getWorkloadClassification())) {
                basicSalary = BigDecimal.ZERO; // Underload gets 0
            } else {
                basicSalary = emp.getBasicSalary() != null ? emp.getBasicSalary().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            }
        } 

        generatedPayroll.setBasicSalary(basicSalary);
        
        generatedPayroll.setLongevity(finalLongevity);
        generatedPayroll.setHonorarium(finalHonorarium);
        generatedPayroll.setAdjustment(finalAdjustment);
        generatedPayroll.setCashGift(emp.getCashGift() != null ? emp.getCashGift() : BigDecimal.ZERO);
        generatedPayroll.setIncentive(emp.getIncentive() != null ? emp.getIncentive() : BigDecimal.ZERO);
        
        BigDecimal standardAllowance = emp.getAllowance() != null ? emp.getAllowance() : BigDecimal.ZERO;
        BigDecimal adminAllowance = emp.getAdminPay() != null ? emp.getAdminPay() : BigDecimal.ZERO;

        generatedPayroll.setAdminPay(adminAllowance); 
        generatedPayroll.setAllowance(standardAllowance.add(adminAllowance)); 
        generatedPayroll.setRelocationPay(emp.getRelocationPay() != null ? emp.getRelocationPay() : BigDecimal.ZERO);
        generatedPayroll.setDeMinimis(emp.getDeMinimis() != null ? emp.getDeMinimis() : BigDecimal.ZERO);
        
        generatedPayroll.setLoanDeductions(appliedSssLoan.add(appliedHdmfLoan));
        generatedPayroll.setSssLoan(appliedSssLoan);
        generatedPayroll.setHdmfLoan(appliedHdmfLoan); 
        
        computeTaxableIncomeAndTax(generatedPayroll); 
        
        BigDecimal mDed = generatedPayroll.getManualDeduction() != null ? generatedPayroll.getManualDeduction() : BigDecimal.ZERO; 
        BigDecimal net = generatedPayroll.getGrossIncome() != null ? generatedPayroll.getGrossIncome() : BigDecimal.ZERO;
        net = net.subtract(generatedPayroll.getGovtContributions() != null ? generatedPayroll.getGovtContributions() : BigDecimal.ZERO)
                 .subtract(generatedPayroll.getWithholdingTax() != null ? generatedPayroll.getWithholdingTax() : BigDecimal.ZERO)
                 .subtract(appliedSssLoan).subtract(appliedHdmfLoan)
                 .subtract(mDed); 
                 
        generatedPayroll.setNetPay(net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net);
        return generatedPayroll;
    }

    public Payroll calculateTeachingPayrollPreview(Employee emp, LocalDate start, LocalDate end, Double lecOverride, Double labOverride, BigDecimal rate, BigDecimal holidayOverride, BigDecimal suspensionOverride, BigDecimal loans) {
        
        BigDecimal actualRate = rate;
        if (actualRate == null || actualRate.compareTo(BigDecimal.ZERO) == 0) {
            if (emp.getBasicSalary() != null && emp.getBasicSalary().compareTo(BigDecimal.ZERO) > 0) {
                actualRate = emp.getBasicSalary().multiply(new BigDecimal("12"))
                    .divide(new BigDecimal("313"), 5, RoundingMode.HALF_UP)
                    .divide(new BigDecimal("8"), 5, RoundingMode.HALF_UP);
            } else if (emp.getHourlyRate() != null) {
                actualRate = emp.getHourlyRate();
            } else {
                actualRate = BigDecimal.ZERO;
            }
        }

        BigDecimal safeLoans = loans != null ? loans : BigDecimal.ZERO;
        List<Attendance> records = attendanceService.getAttendanceByEmpIdAndDateRange(emp.getEmployeeId(), start, end);
        
        TeachingPay tp = processTeachingLogic(emp, start, end, actualRate, holidayOverride, suspensionOverride, records);
        
        TeachingPay dbTp = null;
        try { dbTp = teachingPayRepository.findByEmployeeAndPeriodStartAndPeriodEnd(emp, start, end); } 
        catch (Exception e) {}

        if (tp == null) {
            tp = new TeachingPay();
            tp.setEmployee(emp);
            tp.setPeriodStart(start);
            tp.setPeriodEnd(end);
            tp.setHourlyRate(actualRate);
            tp.setHolidayPay(holidayOverride != null ? holidayOverride : BigDecimal.ZERO);
            tp.setSuspensionDeduction(suspensionOverride != null ? suspensionOverride : BigDecimal.ZERO);
        }

        if (dbTp != null) {
            tp.setId(dbTp.getId());
            if (dbTp.getAdjustmentHours() != null) tp.setAdjustmentHours(dbTp.getAdjustmentHours());
            if (dbTp.getAdjustmentPay() != null) tp.setAdjustmentPay(dbTp.getAdjustmentPay());
            if (dbTp.getDeductionHours() != null) tp.setDeductionHours(dbTp.getDeductionHours());
            if (dbTp.getSgdHours() != null) tp.setSgdHours(dbTp.getSgdHours());
            if (dbTp.getTutorialLecHours() != null) tp.setTutorialLecHours(dbTp.getTutorialLecHours());
            if (dbTp.getTutorialLabHours() != null) tp.setTutorialLabHours(dbTp.getTutorialLabHours());
            if (dbTp.getHonorarium() != null) tp.setHonorarium(dbTp.getHonorarium());
            if (dbTp.getAdminPay() != null) tp.setAdminPay(dbTp.getAdminPay());
            if (dbTp.getSupplementalPay() != null) tp.setSupplementalPay(dbTp.getSupplementalPay());
            if (dbTp.getMakeUpLecHours() != null) tp.setMakeUpLecHours(dbTp.getMakeUpLecHours());
            if (dbTp.getMakeUpLabHours() != null) tp.setMakeUpLabHours(dbTp.getMakeUpLabHours());
            if (dbTp.getMakeUpPay() != null) tp.setMakeUpPay(dbTp.getMakeUpPay());
        }

        if (lecOverride != null && lecOverride > 0) {
            tp.setTotalLecUnits(lecOverride);
            tp.setExcessLecUnits(lecOverride);
            tp.setExcessLecHours(lecOverride * 1.0); 
        }
        if (labOverride != null && labOverride > 0) {
            tp.setTotalLabUnits(labOverride);
            tp.setExcessLabUnits(labOverride);
            tp.setExcessLabHours(labOverride * 3.0); 
        }
        
        tp.calculatePay();
        
        int totalLateMinutes = 0, totalUndertimeMinutes = 0, totalAbsences = 0;
        List<TeachingLoad> loads = null;
        try { loads = facultyLoadRepository.findByEmployeeEmployeeId(emp.getEmployeeId()); } catch(Exception e) {}
        List<String> scheduledDays = new java.util.ArrayList<>();
        if (loads != null) {
            for (TeachingLoad load : loads) if (load.getDayOfWeek() != null) scheduledDays.add(load.getDayOfWeek().toUpperCase());
        }
        
        List<Holidays> holidaysList = holidayRepo.findAll();
        List<Suspension> suspensionsList = suspensionRepo.findAll();
        
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            java.time.DayOfWeek day = date.getDayOfWeek();
            String dayShort = day.name().substring(0, 3);
            
            boolean isScheduled = scheduledDays.stream().anyMatch(d -> d.contains(dayShort));
            if (!isScheduled) continue; 

            final LocalDate currentDate = date;
            boolean isHoliday = holidaysList.stream().anyMatch(h -> h.getDate() != null && h.getDate().equals(currentDate));
            boolean isSuspendedFull = suspensionsList.stream().anyMatch(s -> s.getDate() != null && s.getDate().equals(currentDate) && s.getStartTime() == null);
            
            if (isHoliday || isSuspendedFull) continue;

            var recordOpt = records.stream().filter(r -> r.getDate() != null && r.getDate().equals(currentDate)).findFirst();
            boolean isPresent = recordOpt.isPresent() && ((recordOpt.get().getTimeIn() != null) || (recordOpt.get().getTotalHours() != null && recordOpt.get().getTotalHours() > 0));
            
            if (!isPresent && records != null && !records.isEmpty()) {
                totalAbsences++;
            } else if (isPresent) {
                Attendance a = recordOpt.get();
                totalLateMinutes += a.getMinutesLate() != null ? a.getMinutesLate() : 0;
                totalUndertimeMinutes += a.getUndertimeHours() != null ? a.getUndertimeHours() : 0;
            }
        }
        
        Payroll generatedPayroll = new Payroll();
        generatedPayroll.setEmployee(emp);
        generatedPayroll.setPayPeriodStart(start);
        generatedPayroll.setPayPeriodEnd(end);
        
        generatedPayroll.setLateMinutes(totalLateMinutes);
        generatedPayroll.setUndertimeMinutes(totalUndertimeMinutes);
        generatedPayroll.setTotalAbsences(totalAbsences);
        
        boolean isPartTime = isEmployeePartTime(emp);
        BigDecimal basicSalary = BigDecimal.ZERO;

        // ✅ FIX 3: Let TeachingPay decide if it's an Underload to protect the 1:2 substitution and Non-Faculty Admin
        if (!isPartTime) { 
            if (tp != null && "Full-Time (Underload)".equals(tp.getWorkloadClassification())) {
                basicSalary = BigDecimal.ZERO; // Underload gets 0
            } else {
                basicSalary = emp.getBasicSalary() != null ? emp.getBasicSalary().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            }
        } 

        generatedPayroll.setBasicSalary(basicSalary);
        
        BigDecimal standardAllowance = emp.getAllowance() != null ? emp.getAllowance() : BigDecimal.ZERO;
        BigDecimal adminAllowance = emp.getAdminPay() != null ? emp.getAdminPay() : BigDecimal.ZERO;
        generatedPayroll.setAdminPay(adminAllowance);
        generatedPayroll.setAllowance(standardAllowance.add(adminAllowance));
        
        generatedPayroll.setTeachingPayRecord(tp);
        generatedPayroll.setLoanDeductions(safeLoans);
        
        BigDecimal rawMinuteRate = actualRate.divide(new BigDecimal("60"), 6, RoundingMode.HALF_UP);
        BigDecimal exactMinuteRate = rawMinuteRate.setScale(2, RoundingMode.HALF_UP);
        BigDecimal dailyRate = actualRate.multiply(new BigDecimal("8"));
        
        generatedPayroll.setMinuteRate(exactMinuteRate);
        generatedPayroll.setDailyRate(dailyRate);
        
        generatedPayroll.setLongevity(emp.getLongevity() != null ? emp.getLongevity() : BigDecimal.ZERO);
        generatedPayroll.setHonorarium(emp.getHonorarium() != null ? emp.getHonorarium() : BigDecimal.ZERO);
        generatedPayroll.setCashGift(emp.getCashGift() != null ? emp.getCashGift() : BigDecimal.ZERO);
        generatedPayroll.setIncentive(emp.getIncentive() != null ? emp.getIncentive() : BigDecimal.ZERO);
        generatedPayroll.setRelocationPay(emp.getRelocationPay() != null ? emp.getRelocationPay() : BigDecimal.ZERO);
        generatedPayroll.setDeMinimis(emp.getDeMinimis() != null ? emp.getDeMinimis() : BigDecimal.ZERO);

        computeTaxableIncomeAndTax(generatedPayroll);
        
        BigDecimal gross = generatedPayroll.getGrossIncome() != null ? generatedPayroll.getGrossIncome() : BigDecimal.ZERO;
        BigDecimal govt = generatedPayroll.getGovtContributions() != null ? generatedPayroll.getGovtContributions() : BigDecimal.ZERO;
        BigDecimal withTax = generatedPayroll.getWithholdingTax() != null ? generatedPayroll.getWithholdingTax() : BigDecimal.ZERO;
        BigDecimal mDed = generatedPayroll.getManualDeduction() != null ? generatedPayroll.getManualDeduction() : BigDecimal.ZERO; 
        
        BigDecimal net = gross.subtract(govt).subtract(withTax).subtract(safeLoans).subtract(mDed); 
        generatedPayroll.setNetPay(net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net);

        return generatedPayroll;
    }

    @Transactional
    public Payroll savePayroll(Payroll payroll) {
        computeTaxableIncomeAndTax(payroll);
        BigDecimal gross = payroll.getGrossIncome() != null ? payroll.getGrossIncome() : BigDecimal.ZERO;
        BigDecimal govt = payroll.getGovtContributions() != null ? payroll.getGovtContributions() : BigDecimal.ZERO;
        BigDecimal withTax = payroll.getWithholdingTax() != null ? payroll.getWithholdingTax() : BigDecimal.ZERO;
        BigDecimal mDed = payroll.getManualDeduction() != null ? payroll.getManualDeduction() : BigDecimal.ZERO; 
        BigDecimal loans = payroll.getLoanDeductions() != null ? payroll.getLoanDeductions() : BigDecimal.ZERO;
        
        BigDecimal net = gross.subtract(govt).subtract(withTax).subtract(loans).subtract(mDed); 
        payroll.setNetPay(net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net);

        if (payroll.getTeachingPayRecord() != null && payroll.getTeachingPayRecord().getId() != null) {
            TeachingPay managedTp = teachingPayRepository.save(payroll.getTeachingPayRecord());
            payroll.setTeachingPayRecord(managedTp);
        }
        return payrollRepository.save(payroll);
    }

    private java.time.LocalTime[] extractTimes(String timeSchedule) {
        if (timeSchedule == null || timeSchedule.trim().isEmpty() || timeSchedule.equalsIgnoreCase("TBA")) return null;
        try {
            String clean = timeSchedule.toUpperCase().replaceAll("\\s+", " ").replace("–", "-").replace(" TO ", " - ");
            String[] pts = clean.split("-");
            if (pts.length != 2) return null;
            java.time.format.DateTimeFormatter fmt = new java.time.format.DateTimeFormatterBuilder()
                    .parseCaseInsensitive().appendPattern("h:mm a").toFormatter(java.util.Locale.ENGLISH);
            return new java.time.LocalTime[]{
                    java.time.LocalTime.parse(pts[0].trim(), fmt),
                    java.time.LocalTime.parse(pts[1].trim(), fmt)
            };
        } catch(Exception e) {
            return null;
        }
    }

    public Payroll calculate13thMonthPay(Employee emp, int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        List<Payroll> yearlyPayrolls = payrollRepository.findByEmployeeIdAndPayPeriodStartBetween(
                emp.getId(), startOfYear, endOfYear);

        BigDecimal totalBasicEarned = BigDecimal.ZERO;

        for (Payroll p : yearlyPayrolls) {
            BigDecimal basic = p.getBasicSalary() != null ? p.getBasicSalary() : BigDecimal.ZERO;
            
            BigDecimal pLates = p.getLateDeduction() != null ? p.getLateDeduction() : BigDecimal.ZERO;
            BigDecimal pUndertime = p.getUndertimeDeduction() != null ? p.getUndertimeDeduction() : BigDecimal.ZERO;
            BigDecimal pAbsences = BigDecimal.ZERO;
            
            if (p.getTeachingPayRecord() == null) {
                pAbsences = p.getAbsentDeduction() != null ? p.getAbsentDeduction() : BigDecimal.ZERO;
            }

            BigDecimal earnedBasic = basic.subtract(pAbsences).subtract(pLates).subtract(pUndertime);
            if (earnedBasic.compareTo(BigDecimal.ZERO) < 0) {
                earnedBasic = BigDecimal.ZERO; 
            }
            
            BigDecimal teachingPay = BigDecimal.ZERO;
            if (p.getTeachingPayRecord() != null) {
                BigDecimal lec = p.getTeachingPayRecord().getLecPay() != null ? p.getTeachingPayRecord().getLecPay() : BigDecimal.ZERO;
                BigDecimal lab = p.getTeachingPayRecord().getLabPay() != null ? p.getTeachingPayRecord().getLabPay() : BigDecimal.ZERO;
                BigDecimal makeUp = p.getTeachingPayRecord().getMakeUpPay() != null ? p.getTeachingPayRecord().getMakeUpPay() : BigDecimal.ZERO;
                
                teachingPay = lec.add(lab).add(makeUp);
            }

            totalBasicEarned = totalBasicEarned.add(earnedBasic).add(teachingPay);
        }

        BigDecimal thirteenthMonth = totalBasicEarned.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

        Payroll bonusPayroll = new Payroll();
        bonusPayroll.setEmployee(emp);
        bonusPayroll.setPayPeriodStart(startOfYear);
        bonusPayroll.setPayPeriodEnd(endOfYear);
        bonusPayroll.setTotalEarnings(thirteenthMonth);
        bonusPayroll.setGrossIncome(thirteenthMonth);
        
        if (thirteenthMonth.compareTo(new BigDecimal("90000")) > 0) {
            BigDecimal taxableExcess = thirteenthMonth.subtract(new BigDecimal("90000"));
            bonusPayroll.setTaxableIncome(taxableExcess);
            bonusPayroll.setWithholdingTax(calculateBIRSemiMonthlyTax(taxableExcess)); 
        } else {
            bonusPayroll.setTaxableIncome(BigDecimal.ZERO);
            bonusPayroll.setWithholdingTax(BigDecimal.ZERO);
        }

        bonusPayroll.setGovtContributions(BigDecimal.ZERO);
        bonusPayroll.setSssDeduction(BigDecimal.ZERO);
        bonusPayroll.setPhilhealthDeduction(BigDecimal.ZERO);
        bonusPayroll.setPagibigDeduction(BigDecimal.ZERO);

        BigDecimal net = thirteenthMonth.subtract(bonusPayroll.getWithholdingTax() != null ? bonusPayroll.getWithholdingTax() : BigDecimal.ZERO);
        bonusPayroll.setNetPay(net.max(BigDecimal.ZERO));

        return bonusPayroll;
    }
}