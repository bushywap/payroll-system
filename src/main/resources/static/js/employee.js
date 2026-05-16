document.addEventListener('DOMContentLoaded', () => {
    
    const employeeTableBody = document.getElementById('employeeTableBody');
    const searchInput = document.getElementById('search');

    if (!employeeTableBody) return; 

    // Helper function to format currency securely
    const formatCurrency = (amount) => {
        return (amount !== null && amount !== undefined) 
            ? parseFloat(amount).toFixed(2) 
            : '0.00';
    };

	//
	const renderEmployeeRow = (employee) => {
	    const row = document.createElement('tr');
	    
	    row.innerHTML = `
	        <td><strong>${employee.employeeId || 'N/A'}</strong></td> <td>${employee.firstName || ''}</td>
	        <td>${employee.lastName || ''}</td>
	        <td>${employee.designation || ''}</td>
	        <td>${employee.department || ''}</td>
	        <td><span class="status-badge ${employee.accountStatus === 'Active' ? 'present' : 'absent'}">${employee.accountStatus || 'Unknown'}</span></td>
	        <td>${employee.employeeStatus || 'N/A'}</td> 
	        <td>${employee.email || ''}</td>
	        <td class="currency">${formatCurrency(employee.hourlyRate)}</td>
	        <td class="currency">${formatCurrency(employee.longevity)}</td>
	        <td class="currency">${formatCurrency(employee.honorarium)}</td>
	        <td class="currency">${formatCurrency(employee.adminPay)}</td>
	        <td class="currency">${formatCurrency(employee.deMinimis)}</td>
	        <td class="action-btns">
	            <button class="action-edit" data-id="${employee.employeeId}" title="Edit"><i class="fas fa-pen"></i></button>
	            <button class="action-delete" data-id="${employee.employeeId}" title="Delete"><i class="fas fa-trash"></i></button>
	        </td>
	    `;
	    
	    employeeTableBody.appendChild(row);
	};

    // Fetches the employees from the unified search endpoint
    const fetchEmployees = async (searchTerm = '') => {
        // Set colspan to 14 to match the number of columns
        employeeTableBody.innerHTML = '<tr><td colspan="14" style="text-align:center;">Loading Employees...</td></tr>';
        
        const url = searchTerm ? `/api/employees/search?query=${encodeURIComponent(searchTerm)}` : '/api/employees';

        try {
            const response = await fetch(url, { credentials: 'include' }); 
            if (!response.ok) {
                if (response.status === 403 || response.status === 401) {
                    window.location.href = '/login';
                    return;
                }
                throw new Error(`HTTP Error: ${response.status}`);
            }
            
            const employees = await response.json();
            employeeTableBody.innerHTML = ''; 

            if (employees.length === 0) {
                employeeTableBody.innerHTML = '<tr><td colspan="14" style="text-align:center;">No employees found matching your search.</td></tr>';
                return;
            }

            // Render each employee row
            employees.forEach(renderEmployeeRow);

        } catch (error) {
            console.error('Failed to fetch employees:', error);
            employeeTableBody.innerHTML = '<tr><td colspan="14" style="text-align:center; color:red;">Error fetching data. Check server connection.</td></tr>';
        }
    };
    
    // Initial Load when the page opens
    fetchEmployees();

    // Live search functionality (debounced by 300ms so it doesn't spam the server)
    if(searchInput) {
        let searchTimeout;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                fetchEmployees(e.target.value.trim());
            }, 300);
        });
    }

    // Handle Edit and Delete button clicks
    employeeTableBody.addEventListener('click', (e) => {
        const btn = e.target.closest('button');
        if (!btn) return;
        
        const id = btn.dataset.id;

        if (btn.classList.contains('action-edit')) {
            alert(`Editing employee ID: ${id}`);
            // Add your edit logic/modal opening here
        } else if (btn.classList.contains('action-delete')) {
            if (confirm(`Are you sure you want to delete employee ID: ${id}?`)) {
                alert(`Deleting employee ID: ${id} (API Call simulation)`);
                // Add your delete API call here
            }
        }
    });
});