let debounceTimer;

// Auto-calculates 500ms after the user stops typing
function debounceCalculate() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(calculatePayroll, 500);
}

function formatCurrency(amount) {
    if (!amount) amount = 0;
    return '₱' + parseFloat(amount).toLocaleString('en-PH', {
        minimumFractionDigits: 2, maximumFractionDigits: 2
    });
}

async function calculatePayroll() {
    const employeeId = document.getElementById('employeeId').value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const loan = document.getElementById('loan').value || 0;

    // Don't calculate if missing crucial details
    if (!employeeId || !startDate || !endDate) return; 

    try {
        const params = new URLSearchParams({ employeeQuery: employeeId, start: startDate, end: endDate, loan: loan });
        const response = await fetch(`/api/payroll/calculate?${params.toString()}`, { method: 'POST' });
        if (!response.ok) throw new Error("Calculation failed");
        
        const data = await response.json();
        updatePayslipUI(data);
    } catch (error) {
        console.error("Error loading payroll preview:", error);
    } 
}

async function sendPayroll() {
    const employeeId = document.getElementById('employeeId').value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const loan = document.getElementById('loan').value || 0;

    if (!employeeId || !startDate || !endDate) {
        alert("Please fill in Employee ID/Name and Dates first.");
        return;
    }

    const btn = document.getElementById('sendBtn');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending to Employee...';
    btn.disabled = true;

    try {
        const params = new URLSearchParams({ employeeQuery: employeeId, start: startDate, end: endDate, loan: loan });
        const response = await fetch(`/api/payroll/send?${params.toString()}`, { method: 'POST' });
        if (!response.ok) throw new Error("Send failed");
        
        const data = await response.json();
        updatePayslipUI(data);
        alert("Success! Payroll Generated and Sent to Employee Portal.");
    } catch (error) {
        alert("Error sending payroll.");
    } finally {
        btn.innerHTML = '<i class="fas fa-paper-plane"></i> Generate & Send to Employee';
        btn.disabled = false;
    }
}

function updatePayslipUI(data) {
    document.getElementById('basicSalary').innerText = formatCurrency(data.basicSalary);
    document.getElementById('otDisplay').innerText = "+ " + formatCurrency(data.overtimePay);
    document.getElementById('utDisplay').innerText = "- " + formatCurrency(data.undertimeDeduction);
    document.getElementById('grossPay').innerText = formatCurrency(data.grossIncome);
    
    document.getElementById('sssDed').innerText = "- " + formatCurrency(data.sssDeduction);
    document.getElementById('philhealthDed').innerText = "- " + formatCurrency(data.philhealthDeduction);
    document.getElementById('pagibigDed').innerText = "- " + formatCurrency(data.pagibigDeduction);
    document.getElementById('loanDed').innerText = "- " + formatCurrency(data.loanDeductions);
    
    const totalDeductions = (data.govtContributions || 0) + (data.loanDeductions || 0);
    document.getElementById('totalDed').innerText = formatCurrency(totalDeductions);
    
    document.getElementById('netPay').innerText = formatCurrency(data.netPay);
}