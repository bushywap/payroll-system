document.addEventListener('DOMContentLoaded', () => {
    
    const employeeTableBody = document.getElementById('employeeTableBody');
    const searchInput = document.getElementById('search');

    if (!employeeTableBody) return; 

    const formatCurrency = (amount) => {
        return (amount !== null && amount !== undefined) 
            ? parseFloat(amount).toFixed(2) 
            : '0.00';
    };

    const renderEmployeeRow = (employee) => {
        const row = document.createElement('tr');
        
        // Updated to match the actual fields in Employee.java
        row.innerHTML = `
            <td>${employee.employeeId || 'N/A'}</td>
            <td>${employee.employeeNo || ''}</td>
            <td>${employee.firstName || ''}</td>
            <td>N/A</td> <td>${employee.lastName || ''}</td>
            <td>${employee.designation || ''}</td>
            <td>${employee.department || ''}</td>
            <td>N/A</td> <td>N/A</td> <td><span class="status-badge ${employee.status === 'Active' ? 'present' : 'absent'}">${employee.status || 'Unknown'}</span></td>
            <td>N/A</td> <td>${employee.email || ''}</td>
            <td>N/A</td> <td class="currency">${formatCurrency(employee.longevity)}</td>
            <td class="currency">${formatCurrency(employee.honorarium)}</td>
            <td class="currency">${formatCurrency(employee.adminPay)}</td>
            <td class="action-btns">
                <button class="action-edit" data-id="${employee.employeeId}"><i class="fas fa-pen"></i></button>
                <button class="action-delete" data-id="${employee.employeeId}"><i class="fas fa-trash"></i></button>
                <button class="action-payslip" data-id="${employee.employeeId}"><i class="fas fa-file-invoice-dollar"></i> Payslip</button>
            </td>
        `;
        
        employeeTableBody.appendChild(row);
    };

    const fetchEmployees = async (searchTerm = '') => {
        employeeTableBody.innerHTML = '<tr><td colspan="17" style="text-align:center;">Loading Employees...</td></tr>';
        
        const url = searchTerm ? `/api/employees/search?query=${encodeURIComponent(searchTerm)}` : '/api/employees';

        try {
            const response = await fetch(url, { credentials: 'include' }); 
            if (!response.ok) {
                if (response.status === 403 || response.status === 401) {
                    window.location.href = '/login';
                    return;
                }
                throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
            }
            const employees = await response.json();

            employeeTableBody.innerHTML = ''; 

            if (employees.length === 0) {
                employeeTableBody.innerHTML = '<tr><td colspan="17" style="text-align:center;">No employees found.</td></tr>';
                return;
            }

            employees.forEach(renderEmployeeRow);

        } catch (error) {
            console.error('Failed to fetch employees:', error);
            employeeTableBody.innerHTML = '<tr><td colspan="17" style="text-align:center; color:red;">Error fetching data. Check server connection.</td></tr>';
        }
    };
    
    fetchEmployees();

    if(searchInput) {
        let searchTimeout;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                fetchEmployees(e.target.value.trim());
            }, 300);
        });
    }

    employeeTableBody.addEventListener('click', (e) => {
        const btn = e.target.closest('button');
        if (!btn) return;
        
        const id = btn.dataset.id;

        if (btn.classList.contains('action-edit')) {
            alert(`Editing employee ID: ${id}`);
        } else if (btn.classList.contains('action-delete')) {
            if (confirm(`Are you sure you want to delete employee ID: ${id}?`)) {
                alert(`Deleting employee ID: ${id} (API Call simulation)`);
            }
        } else if (btn.classList.contains('action-payslip')) {
            alert(`Generating payslip for employee ID: ${id}`);
        }
    });
});