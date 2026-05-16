function formatCurrency(amount) {
    if (!amount) amount = 0;
    return '₱' + parseFloat(amount).toLocaleString('en-PH', {
        minimumFractionDigits: 2, maximumFractionDigits: 2
    });
}

async function previewTeachingPay() {
    const employeeId = document.getElementById('employeeId').value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const excessLec = document.getElementById('excessLec').value || 0;
    const excessLab = document.getElementById('excessLab').value || 0;

    if (!employeeId || !startDate || !endDate) {
        alert("Please fill in the Faculty ID and Period Dates.");
        return; 
    }

    const btn = document.getElementById('calcTeachingBtn');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Computing...';

    try {
        const params = new URLSearchParams({ 
            employeeQuery: employeeId, 
            start: startDate, 
            end: endDate, 
            excessLec: excessLec, 
            excessLab: excessLab 
        });

        // We use the same payroll calculation endpoint, but we will only extract the Teaching Pay portion for this screen
        const response = await fetch(`/api/payroll/calculate?${params.toString()}`, { method: 'POST' });
        
        if (!response.ok) throw new Error("Calculation failed");
        
        const data = await response.json();
        
        if (data.teachingPayRecord) {
            document.getElementById('hourlyRateDisplay').innerText = formatCurrency(data.teachingPayRecord.hourlyRate);
            document.getElementById('lecPayDisplay').innerText = formatCurrency(data.teachingPayRecord.lecPay);
            document.getElementById('labPayDisplay').innerText = formatCurrency(data.teachingPayRecord.labPay);
            document.getElementById('totalTeachingDisplay').innerText = formatCurrency(data.teachingPayRecord.totalTeachingPay);
        } else {
            alert("This employee does not have a Teaching Pay record generated (ensure they are faculty and have an hourly rate).");
            resetDisplay();
        }

    } catch (error) {
        console.error(error);
        alert("Error calculating teaching pay. Ensure the employee exists.");
        resetDisplay();
    } finally {
        btn.innerHTML = '<i class="fas fa-calculator"></i> Preview Teaching Pay';
    }
}

function resetDisplay() {
    document.getElementById('hourlyRateDisplay').innerText = "₱0.00";
    document.getElementById('lecPayDisplay').innerText = "₱0.00";
    document.getElementById('labPayDisplay').innerText = "₱0.00";
    document.getElementById('totalTeachingDisplay').innerText = "₱0.00";
}