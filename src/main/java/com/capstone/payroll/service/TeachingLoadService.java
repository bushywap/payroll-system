package com.capstone.payroll.service;

import com.capstone.payroll.model.TeachingLoad;
import com.capstone.payroll.repository.TeachingLoadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class TeachingLoadService {

    @Autowired
    private TeachingLoadRepository repository;

    public void saveFromExcel(MultipartFile file) {
        try {
            // Parses the new format
            List<TeachingLoad> teachingLoads = ExcelHelper.excelToTeachingLoad(file.getInputStream());
            
            // Computes the total hours automatically for every parsed row
            for (TeachingLoad load : teachingLoads) {
                calculateHours(load);
            }
            
            // Saves all rows securely
            repository.saveAll(teachingLoads);
        } catch (IOException e) {
            throw new RuntimeException("Fail to store excel data: " + e.getMessage());
        }
    }

    public List<TeachingLoad> getAllTeachingLoads() {
        return repository.findAll();
    }

    // --- FIX: Pass the String School ID directly without trying to convert it to a Number ---
    public List<TeachingLoad> findByEmployeeId(String employeeIdStr) {
        return repository.findByEmployeeEmployeeId(employeeIdStr);
    }
    
    // Automatically calculates the total hours using Doubles to match the new model!
    private void calculateHours(TeachingLoad load) {
        int lecUnits = load.getLectureUnits();
        int labUnits = load.getLabUnits();
        int sections = load.getNoOfSections() > 0 ? load.getNoOfSections() : 1;
        
        // Changed from int to double
        double calculatedLecHours = (lecUnits * 1.0) * sections;
        
        // LABORATORY HOURS SHOULD BE 1 UNIT IS EQUAL TO 3 HOURS
        double calculatedLabHours = (labUnits * 3.0) * sections; 
        
        load.setLecHours(calculatedLecHours);
        load.setLabHours(calculatedLabHours);
        
        double totalRle = load.getRleHours() != null ? load.getRleHours() : 0.0;
        
        // Notice: We removed substitute hours here because it's now beautifully handled 
        // dynamically by your SubstituteRecordRepository during payroll generation!
        load.setTotalHours(calculatedLecHours + calculatedLabHours + totalRle);
    }
}