package com.capstone.payroll.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "designation")
public class EmpDesignation {
	
		@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private int Id;
	    
	    private String designation;
	    private int teaching;
	    private int employee;
	    
	    public int getId() {
			return Id;
		}
		public void setId(int id) {
			Id = id;
		}
		
		public String getDesignation() {
			return designation;
		}
		public void setDesignation(String designation) {
			this.designation = designation;
		}
		public int getTeaching() {
			return teaching;
		}
		public void setTeaching(int teaching) {
			this.teaching = teaching;
		}
		public int getEmployee() {
			return employee;
		}
		public void setEmployee(int employee) {
			this.employee = employee;
		}
		

}
