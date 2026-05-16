package com.capstone.payroll.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.capstone.payroll.model.Employee;
import com.capstone.payroll.repository.EmployeeRepository;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> findAllEmployees() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> findEmployeeById(Long id) {
        return employeeRepository.findById(id); 
    }

    public Employee saveEmployee(Employee employee) {
        return employeeRepository.save(employee); 
    }

    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id); 
    }

    public List<Employee> searchEmployees(String query) {
        return employeeRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query);
    }

    public List<Employee> findActiveEmployees() {
        return employeeRepository.findByEmployeeStatus("Active");
    }
}