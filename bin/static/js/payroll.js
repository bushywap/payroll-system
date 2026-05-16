function formatCurrency(amount) {
    if (!amount) amount = 0;
    return '₱' + parseFloat(amount).toLocaleString('en-PH', {
        minimumFractionDigits: 2, maximumFractionDigits: 2
    });
}

function getPayrollParams() {
    return new URLSearchParams({ 
        employeeQuery: document.getElementById('employeeId').value, 
        start: document.getElementById('startDate').value, 
        end: document.getElementById('endDate').value, 
        loan: document.getElementById('loan').value || 0
    });
}

async function calculatePayroll() {
    const params = getPayrollParams();
    if (!params.get('employeeQuery') || !params.get('start') || !params.get('end')) {
        alert("Please fill in Employee ID and Dates");
        return; 
    }

    const btn = document.getElementById('calculateBtn');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Calculating...';

    try {
        const response = await fetch(`/api/payroll/calculate?${params.toString()}`, { method: 'POST' });
        if (!response.ok) throw new Error("Calculation failed");
        
        const data = await response.json();
        updatePayslipUI(data);
    } catch (error) {
        console.error(error);
        alert("Error calculating payroll. Ensure DTR exists for the dates.");
    } finally {
        btn.innerHTML = '<i class="fas fa-calculator"></i> Preview Calculation';
    }
}

async function sendPayroll() {
    const params = getPayrollParams();
    if (!params.get('employeeQuery') || !params.get('start') || !params.get('end')) {
        alert("Please fill in Employee ID and Dates");
        return;
    }

    const btn = document.getElementById('sendBtn');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Publishing...';
    btn.disabled = true;

    try {
        params.append('status', 'PUBLISHED');
        const response = await fetch(`/api/payroll/send?${params.toString()}`, { method: 'POST' });
        if (!response.ok) throw new Error("Publishing failed");
        
        const data = await response.json();
        updatePayslipUI(data);
        alert("Payroll officially Published and Sent to Employee Portal!");
    } catch (error) {
        alert("Error publishing payroll.");
    } finally {
        btn.innerHTML = '<i class="fas fa-paper-plane"></i> Publish & Send to Employee';
        btn.disabled = false;
    }
}

function updatePayslipUI(data) {
    // Earnings
    document.getElementById('basicSalary').innerText = formatCurrency(data.basicSalary);
    
    // Extract the total from the linked TeachingPay record, fallback to 0 if it doesn't exist
    const teachingTotal = data.teachingPayRecord ? data.teachingPayRecord.totalTeachingPay : 0;
    document.getElementById('teachingPay').innerText = "+ " + formatCurrency(teachingTotal);
    
    document.getElementById('deMinimis').innerText = "+ " + formatCurrency(data.deMinimis);
    document.getElementById('otDisplay').innerText = "+ " + formatCurrency(data.overtimePay);
    document.getElementById('grossPay').innerText = formatCurrency(data.grossIncome);

    // Deductions
    document.getElementById('utDisplay').innerText = "- " + formatCurrency(data.undertimeDeduction);
    document.getElementById('absentDed').innerText = "- " + formatCurrency(data.absentDeduction);
    document.getElementById('sssDed').innerText = "- " + formatCurrency(data.sssDeduction);
    document.getElementById('philhealthDed').innerText = "- " + formatCurrency(data.philhealthDeduction);
    document.getElementById('pagibigDed').innerText = "- " + formatCurrency(data.pagibigDeduction);
    document.getElementById('taxDed').innerText = "- " + formatCurrency(data.withholdingTax);
    document.getElementById('loanDed').innerText = "- " + formatCurrency(data.loanDeductions);

    // Net and Accruals
    document.getElementById('netPay').innerText = formatCurrency(data.netPay);
    document.getElementById('thirteenthAccrual').innerText = formatCurrency(data.thirteenthMonthPay);
}