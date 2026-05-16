/* ==========================================================================
   PAYROLL MANAGEMENT COMPUTATIONS
   Handles Batch computing, individual computing, and publishing
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
    const sssBtn = document.getElementById('btn-gov-sss');
    const philBtn = document.getElementById('btn-gov-philhealth');
    const pagibigBtn = document.getElementById('btn-gov-pagibig');
    if(sssBtn) sssBtn.addEventListener('click', () => switchGovTab('sss'));
    if(philBtn) philBtn.addEventListener('click', () => switchGovTab('philhealth'));
    if(pagibigBtn) pagibigBtn.addEventListener('click', () => switchGovTab('pagibig'));

    const teachEmployeeIdInput = document.getElementById('teachEmployeeId');
    if (teachEmployeeIdInput) {
        teachEmployeeIdInput.addEventListener('blur', async function() {
            const query = this.value.trim();
            if (!query) return; 
            try {
                const response = await fetch(`/payroll-system/teaching-pay/api/get-load?query=${encodeURIComponent(query)}`);
                if (response.ok) {
                    const data = await response.json();
                    if(document.getElementById('teachLec')) document.getElementById('teachLec').value = data.totalLec || 0;
                    if(document.getElementById('teachLab')) document.getElementById('teachLab').value = data.totalLab || 0;
                    if(document.getElementById('teachRate')) document.getElementById('teachRate').value = data.rate || 0; 
                } else {
                    if(document.getElementById('teachLec')) document.getElementById('teachLec').value = '';
                    if(document.getElementById('teachLab')) document.getElementById('teachLab').value = '';
                    if(document.getElementById('teachRate')) document.getElementById('teachRate').value = '';
                }
            } catch (error) { 
                console.error("Failed to auto-fetch teaching load.", error); 
            }
        });
    }

    const container = document.getElementById('batchContainer');
    const label = document.getElementById('scrollLabel');
    if (container && label) {
        container.addEventListener('scroll', () => {
            const scrollLeft = container.scrollLeft;
            if (scrollLeft < 150) {
                label.innerText = "Viewing: Employee Info";
                label.style.color = "#444";
            } else if (scrollLeft >= 150 && scrollLeft < 800) {
                label.innerText = "Viewing: Earnings Section";
                label.style.color = "#0284c7";
            } else {
                label.innerText = "Viewing: Deductions Section";
                label.style.color = "#e11d48";
            }
        });
    }
});

// NEW CUSTOM ALERT FUNCTION TO REPLACE NATIVE ALERTS
function customAlert(title, message, type = 'success') {
    return new Promise((resolve) => {
        const modal = document.getElementById('alertModal');
        const titleEl = document.getElementById('alertModalTitle');
        const msgEl = document.getElementById('alertModalMessage');
        const btnOk = document.getElementById('btnAlertOk');
        const headerEl = document.getElementById('alertModalHeader');

        if (!modal || !titleEl || !msgEl || !btnOk) {
            alert(title + "\n" + message);
            resolve();
            return;
        }

        // Apply dynamic styling based on success, error, or info
        if (type === 'success') {
            headerEl.style.backgroundColor = '#f0fdf4';
            headerEl.style.borderBottom = '1px solid #bbf7d0';
            titleEl.innerHTML = '<i class="fas fa-check-circle" style="color: #16a34a;"></i> <span style="color: #15803d; font-weight: 800;">' + title + '</span>';
            btnOk.style.backgroundColor = '#16a34a';
        } else if (type === 'error') {
            headerEl.style.backgroundColor = '#fef2f2';
            headerEl.style.borderBottom = '1px solid #fecaca';
            titleEl.innerHTML = '<i class="fas fa-exclamation-triangle" style="color: #dc2626;"></i> <span style="color: #b91c1c; font-weight: 800;">' + title + '</span>';
            btnOk.style.backgroundColor = '#dc2626';
        } else {
            headerEl.style.backgroundColor = '#f0f9ff';
            headerEl.style.borderBottom = '1px solid #bae6fd';
            titleEl.innerHTML = '<i class="fas fa-info-circle" style="color: #0284c7;"></i> <span style="color: #0369a1; font-weight: 800;">' + title + '</span>';
            btnOk.style.backgroundColor = '#0284c7';
        }

        msgEl.innerHTML = message;
        modal.style.display = 'flex';

        const cleanup = () => {
            btnOk.onclick = null;
            modal.style.display = 'none';
            resolve();
        };

        btnOk.onclick = () => cleanup();
    });
}

function customConfirm(message) {
    return new Promise((resolve) => {
        const modal = document.getElementById('confirmModal');
        const msgEl = document.getElementById('confirmModalMessage');
        const btnOk = document.getElementById('btnConfirmOk');
        const btnCancel = document.getElementById('btnConfirmCancel');

        if (!modal || !msgEl || !btnOk || !btnCancel) {
            resolve(confirm(message));
            return;
        }

        msgEl.innerHTML = message;
        modal.style.display = 'flex';

        const cleanup = () => {
            btnOk.onclick = null;
            btnCancel.onclick = null;
            modal.onclick = null;
            modal.style.display = 'none';
        };

        btnOk.onclick = () => { cleanup(); resolve(true); };
        btnCancel.onclick = () => { cleanup(); resolve(false); };
        
        modal.onclick = function(event) {
            if (event.target === modal) {
                cleanup();
                resolve(false);
            }
        };
    });
}

function formatCurrency(amount) {
    if (amount === null || amount === undefined || isNaN(amount)) amount = 0;
    return '₱' + parseFloat(amount).toLocaleString('en-PH', {
        minimumFractionDigits: 2, maximumFractionDigits: 2
    });
}

function switchPayrollMode(mode) {
    const modeInput = document.getElementById('currentPayrollMode');
    if(modeInput) modeInput.value = mode;

    if(document.getElementById('batchPayslipPreview')) document.getElementById('batchPayslipPreview').style.display = 'none';
    if(document.getElementById('batchTeachPayslipPreview')) document.getElementById('batchTeachPayslipPreview').style.display = 'none';
    if(document.getElementById('employeePayslip')) document.getElementById('employeePayslip').style.display = 'none';
    if(document.getElementById('teachingPayslip')) document.getElementById('teachingPayslip').style.display = 'none';
    
    if(document.getElementById('emptyPreviewState')) document.getElementById('emptyPreviewState').style.display = 'block';

    if (mode === 'EMPLOYEE') {
        if(document.getElementById('btnEmpPayroll')) document.getElementById('btnEmpPayroll').className = 'btn-tab active-tab';
        if(document.getElementById('btnTeachPayroll')) document.getElementById('btnTeachPayroll').className = 'btn-tab inactive-tab';
        
        document.querySelectorAll('.emp-only').forEach(el => el.style.display = '');
        document.querySelectorAll('.teach-only').forEach(el => el.style.display = 'none');
        
        if(document.getElementById('payslipTitle')) document.getElementById('payslipTitle').innerText = "Employee Payslip Preview";
    } else {
        if(document.getElementById('btnEmpPayroll')) document.getElementById('btnEmpPayroll').className = 'btn-tab inactive-tab';
        if(document.getElementById('btnTeachPayroll')) document.getElementById('btnTeachPayroll').className = 'btn-tab active-tab';
        
        document.querySelectorAll('.emp-only').forEach(el => el.style.display = 'none');
        document.querySelectorAll('.teach-only').forEach(el => el.style.display = 'block'); 
        
        if(document.getElementById('payslipTitle')) document.getElementById('payslipTitle').innerText = "Manual Teaching Preview";
    }
}

function switchGovTab(tabName) {
    document.querySelectorAll('.gov-tab-btn').forEach(btn => { btn.classList.remove('active'); });
    document.querySelectorAll('.gov-tab-content').forEach(content => { content.style.display = 'none'; });
    const activeBtn = document.getElementById('btn-gov-' + tabName.toLowerCase());
    if(activeBtn) activeBtn.classList.add('active');
    const activeContent = document.getElementById('content-gov-' + tabName.toLowerCase());
    if(activeContent) activeContent.style.display = 'block';
}

function slideBatchTable(direction) {
    const container = document.getElementById('batchContainer');
    const scrollAmount = 400; 
    if(!container) return;
    if (direction === 'left') {
        container.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
    } else {
        container.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    }
}

async function handleFetchResponse(response) {
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `HTTP Error: ${response.status}`);
    }
    return response.json();
}

async function calculatePayroll() {
    const modeInput = document.getElementById('currentPayrollMode');
    const mode = modeInput ? modeInput.value : 'EMPLOYEE';

    if(document.getElementById('batchPayslipPreview')) document.getElementById('batchPayslipPreview').style.display = 'none';
    if(document.getElementById('batchTeachPayslipPreview')) document.getElementById('batchTeachPayslipPreview').style.display = 'none';
    
    if (mode === 'EMPLOYEE') {
        const employeeId = document.getElementById('employeeId')?.value;
        const startDate = document.getElementById('startDate')?.value;
        const endDate = document.getElementById('endDate')?.value;
        const generalLoan = document.getElementById('loan')?.value || 0;

        if (!employeeId || !startDate || !endDate) {
            await customAlert("Validation Missing", "Please provide Employee ID and Period Dates.", "error");
            return;
        }

        try {
            const params = new URLSearchParams({ 
                employeeQuery: employeeId, start: startDate, end: endDate, 
                sssLoan: generalLoan, hdmfLoan: 0, adjustment: 0, 
                honorarium: 0, longevity: 0 
            });
            const response = await fetch(`/payroll-system/api/payroll/calculate?${params.toString()}`, { method: 'POST' });
            const data = await handleFetchResponse(response);
            updateEmployeePayslipUI(data);
        } catch (error) {
            console.error(error);
            await customAlert("Calculation Error", error.message, "error");
        } 
    } else {
        const employeeId = document.getElementById('teachEmployeeId')?.value;
        const startDate = document.getElementById('startDate')?.value;
        const endDate = document.getElementById('endDate')?.value;

        if (!employeeId || !startDate || !endDate) {
            await customAlert("Validation Missing", "Please provide Employee ID and Period Dates.", "error");
            return;
        }

        let lec = parseFloat(document.getElementById("teachLec")?.value) || 0;
        let lab = parseFloat(document.getElementById("teachLab")?.value) || 0;
        let rate = parseFloat(document.getElementById("teachRate")?.value) || 0;
        let holiday = parseFloat(document.getElementById("teachHoliday")?.value) || 0;

        try {
            const params = new URLSearchParams({
                employeeQuery: employeeId, start: startDate, end: endDate,
                lec: lec, lab: lab, rate: rate, holiday: holiday, suspension: 0, loan: 0
            });
            
            const response = await fetch(`/payroll-system/api/payroll/teaching/calculate?${params.toString()}`, { method: 'POST' });
            const data = await handleFetchResponse(response);
            updateTeachingPayslipUI(data);
        } catch (error) {
            console.error(error);
            await customAlert("Teaching Calculation Error", error.message, "error");
        }
    }
}

async function sendPayroll() {
    const modeInput = document.getElementById('currentPayrollMode');
    const mode = modeInput ? modeInput.value : 'EMPLOYEE';

    if (mode === 'EMPLOYEE') {
        const employeeId = document.getElementById('employeeId')?.value;
        const startDate = document.getElementById('startDate')?.value;
        const endDate = document.getElementById('endDate')?.value;
        const generalLoan = document.getElementById('loan')?.value || 0;

        if (!employeeId || !startDate || !endDate) {
            await customAlert("Validation Missing", "Please provide Employee ID and Period Dates.", "error");
            return;
        }

        const isConfirmed = await customConfirm("Are you sure you want to save this individual employee payroll to the register?");
        if (!isConfirmed) return;

        const btn = document.getElementById('sendBtn');
        const originalText = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
        btn.disabled = true;

        try {
            const params = new URLSearchParams({ 
                employeeQuery: employeeId, start: startDate, end: endDate, 
                sssLoan: generalLoan, hdmfLoan: 0, adjustment: 0, 
                honorarium: 0, longevity: 0 
            });
            const response = await fetch(`/payroll-system/api/payroll/send?${params.toString()}`, { method: 'POST' });
            const data = await handleFetchResponse(response);
            
            updateEmployeePayslipUI(data);
            await customAlert("Success!", "Employee Payroll successfully saved to the Master Register.", "success");
        } catch (error) {
            console.error(error);
            await customAlert("Send Error", error.message, "error");
        } finally {
            if(btn) { btn.innerHTML = originalText; btn.disabled = false; }
        }
    } else {
        const employeeId = document.getElementById('teachEmployeeId')?.value;
        const startDate = document.getElementById('startDate')?.value;
        const endDate = document.getElementById('endDate')?.value;

        if (!employeeId || !startDate || !endDate) {
             await customAlert("Validation Missing", "Please provide Employee ID and Period Dates.", "error");
             return;
        }

        const isConfirmed = await customConfirm("Are you sure you want to save this individual teaching payroll to the register?");
        if (!isConfirmed) return;

        const btn = document.getElementById('sendTeachBtn');
        const originalText = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
        btn.disabled = true;

        let lec = parseFloat(document.getElementById("teachLec")?.value) || 0;
        let lab = parseFloat(document.getElementById("teachLab")?.value) || 0;
        let rate = parseFloat(document.getElementById("teachRate")?.value) || 0;
        let holiday = parseFloat(document.getElementById("teachHoliday")?.value) || 0;

        try {
            const params = new URLSearchParams({
                employeeQuery: employeeId, start: startDate, end: endDate,
                lec: lec, lab: lab, rate: rate, holiday: holiday, suspension: 0, loan: 0
            });
            
            const response = await fetch(`/payroll-system/api/payroll/teaching/send?${params.toString()}`, { method: 'POST' });
            const data = await handleFetchResponse(response);
            
            updateTeachingPayslipUI(data);
            await customAlert("Success!", "Teaching Payroll successfully saved to the Master Register.", "success");
        } catch (error) {
            console.error(error);
            await customAlert("Send Teaching Error", error.message, "error");
        } finally {
            if(btn) { btn.innerHTML = originalText; btn.disabled = false; }
        }
    }
}

function updateEmployeePayslipUI(data) {
    const setSafeText = (id, val) => { const el = document.getElementById(id); if(el) el.innerText = val; };
    setSafeText('basicSalary', formatCurrency(data.basicSalary));
    setSafeText('holidayPayDisplay', "+ " + formatCurrency(data.holidayPay));
    setSafeText('otDisplay', "+ " + formatCurrency(data.overtimePay));
    
    let teachingPayTotal = 0;
    if (data.teachingPayRecord && data.teachingPayRecord.totalTeachingPay) {
        teachingPayTotal = data.teachingPayRecord.totalTeachingPay;
    }
    setSafeText('teachingPayDisplay', "+ " + formatCurrency(teachingPayTotal));
    
    setSafeText('honorariumDisplay', "+ " + formatCurrency(data.honorarium));
    setSafeText('adjustmentDisplay', "+ " + formatCurrency(data.adjustment));
    setSafeText('lateDisplay', "- " + formatCurrency(data.lateDeduction));
    setSafeText('utDisplay', "- " + formatCurrency(data.undertimeDeduction));
    setSafeText('absentDed', "- " + formatCurrency(data.absentDeduction));
    setSafeText('leaveWithoutPayDisplay', "- " + formatCurrency(data.leaveWithoutPay));
    setSafeText('deMinimis', "+ " + formatCurrency(data.deMinimis));
    setSafeText('longevityDisplay', "+ " + formatCurrency(data.longevity));

    const lpEl = document.getElementById('leavePayDisplay');
    if(lpEl) {
        if (data.leavePay && data.leavePay > 0) {
            lpEl.innerText = `${formatCurrency(data.leavePay)} (Included in Basic Pay)`;
        } else {
            lpEl.innerText = `0 Days (No Deduction)`;
        }
    }

    setSafeText('grossPay', formatCurrency(data.grossIncome));
    setSafeText('sssDed', "- " + formatCurrency(data.sssDeduction));
    setSafeText('philhealthDed', "- " + formatCurrency(data.philhealthDeduction));
    setSafeText('pagibigDed', "- " + formatCurrency(data.pagibigDeduction));
    setSafeText('taxableIncomeBase', formatCurrency(data.taxableIncome));
    setSafeText('taxDed', "- " + formatCurrency(data.withholdingTax));
    setSafeText('loanDed', "- " + formatCurrency(data.sssLoan)); 
    setSafeText('netPay', formatCurrency(data.netPay));

    // FIX: HIDE ALL OTHER PREVIEWS AND RESET TITLE BEFORE SHOWING THIS ONE
    if(document.getElementById('batchPayslipPreview')) document.getElementById('batchPayslipPreview').style.display = 'none';
    if(document.getElementById('batchTeachPayslipPreview')) document.getElementById('batchTeachPayslipPreview').style.display = 'none';
    if(document.getElementById('teachingPayslip')) document.getElementById('teachingPayslip').style.display = 'none';
    if(document.getElementById('emptyPreviewState')) document.getElementById('emptyPreviewState').style.display = 'none';
    if(document.getElementById('payslipTitle')) document.getElementById('payslipTitle').innerText = "Employee Payslip Preview";

    if(document.getElementById('employeePayslip')) {
        document.getElementById('employeePayslip').style.display = 'block';
        document.getElementById('employeePayslip').style.opacity = '1';
    }
}

function updateTeachingPayslipUI(data) {
    const tp = data.teachingPayRecord || {};
    const setSafeText = (id, val) => { const el = document.getElementById(id); if(el) el.innerText = val; };
    const setSafeHtml = (id, html) => { const el = document.getElementById(id); if(el) el.innerHTML = html; };
    
    const isPartTime = !data.basicSalary || data.basicSalary === 0;

    const totLec = tp.totalLecUnits || 0;
    const totLab = tp.totalLabUnits || 0;
    
    let excLec = 0;
    let excLab = 0;

    if (isPartTime) {
        setSafeText("thUnitLabel", "(All Units Payable)");
        // Part-time: All units are payable, no 15-unit deduction
        excLec = totLec;
        excLab = totLab;
    } else {
        setSafeText("thUnitLabel", "(Above 15 Basic)");
        // Full-time: Accumulate Lec units FIRST up to 15, then Lab units
        let coveredLec = Math.min(totLec, 15);
        let coveredLab = Math.min(totLab, 15 - coveredLec);
        
        excLec = totLec - coveredLec;
        excLab = totLab - coveredLab;
    }

    const grandTot = totLec + totLab;
    const grandExc = excLec + excLab;

    setSafeText("previewTotLec", totLec.toFixed(1));
    setSafeText("previewTotLab", totLab.toFixed(1));
    setSafeText("previewGrandTotUnits", grandTot.toFixed(1));

    // Displays total units for part-time, or strictly excess for full-time
    setSafeText("previewExcessLec", excLec.toFixed(1));
    setSafeText("previewExcessLab", excLab.toFixed(1));

    const lecHrs = tp.excessLecHours || 0; 
    const labHrs = tp.excessLabHours || 0; 
    
    setSafeText("previewLecHrs", lecHrs.toFixed(2) + " hrs");
    setSafeText("previewLabHrs", labHrs.toFixed(2) + " hrs");
    setSafeText("previewGrandExcUnits", grandExc.toFixed(1));
    setSafeText("previewTotalHrs", (lecHrs + labHrs).toFixed(2) + " hrs");

    const hrRate = tp.hourlyRate || 0;
    const subHrs = tp.substituteHours || 0;

	
        const gridSubAdd = document.getElementById('gridPreviewSubAdd');
    if (gridSubAdd) {
        document.getElementById('previewSubDutyHrs').innerText = subHrs + " hrs";
        gridSubAdd.style.display = subHrs > 0 ? 'table-row' : 'none';
    }
    
    const subDedHrs = tp.absentDeductionHours || 0;
    const absDedPay = tp.absentDeductionPay || (subDedHrs * hrRate); 
    const gridSubDed = document.getElementById('gridPreviewSubDed');
    if (gridSubDed) {
        document.getElementById('previewSubDedDutyHrs').innerText = subDedHrs + " hrs missed";
        gridSubDed.style.display = subDedHrs > 0 ? 'table-row' : 'none';
    }
    
    if (grandTot >= 15 && !isPartTime) {
        setSafeHtml("basicPayNoticeBox", "<strong><i class='fas fa-check-circle'></i> Target Reached:</strong> Faculty reached " + grandTot.toFixed(1) + " units. The first 15 units are fully covered by the Basic/Admin Pay.");
        setSafeText("previewBasicDetail", "(Fixed Base Salary covering 15 units)");
    } else {
        setSafeHtml("basicPayNoticeBox", "<strong><i class='fas fa-info-circle'></i> Load Notice:</strong> Faculty has " + grandTot.toFixed(1) + " units. All payable hours are computed below.");
        setSafeText("previewBasicDetail", `(Computed for actual load)`);
    }

    setSafeText("previewLecRateDisplay", `(${excLec.toFixed(2)} units = ${lecHrs.toFixed(2)} hrs x ₱${hrRate.toFixed(2)}/hr)`);
    let finalLabRateDisplay = (tp.labPay && labHrs) ? (tp.labPay / labHrs) : 0;
    setSafeText("previewLabRateDisplay", `(${excLab.toFixed(2)} units = ${labHrs.toFixed(2)} hrs x ₱${finalLabRateDisplay.toFixed(2)}/hr)`);
    
    setSafeText("previewAdminPay", formatCurrency(data.basicSalary)); 
    setSafeText("previewLecPay", formatCurrency(tp.lecPay));
    setSafeText("previewLabPay", formatCurrency(tp.labPay));
    
    const holPay = data.holidayPay || tp.holidayPay || 0;
    setSafeText("previewHoliday", formatCurrency(holPay));

    const toggleRow = (id, amount) => {
        const row = document.getElementById(id);
        if (row) row.style.display = amount > 0 ? 'flex' : 'none';
    };

    const rlePay = tp.rlePay || 0;
    const subPay = tp.substitutePay || 0;
    const sgdPay = tp.sgdPay || 0;
    const tutPay = (tp.tutorialLecPay || 0) + (tp.tutorialLabPay || 0);
    const honPay = tp.honorarium || 0;
    const suppPay = tp.supplementalPay || 0;
    const adjPay = tp.totalAdjustmentPay || 0;
    
    const manDedHrs = tp.deductionHours || 0;
    const manDedAmt = manDedHrs * hrRate;

    setSafeText("previewRlePay", formatCurrency(rlePay));
    setSafeText("previewSgdPay", formatCurrency(sgdPay));
    setSafeText("previewTutPay", formatCurrency(tutPay));
    setSafeText("previewHonPay", formatCurrency(honPay));
    setSafeText("previewSuppPay", formatCurrency(suppPay));
    setSafeText("previewAdjPay", formatCurrency(adjPay));
    
    setSafeText("previewSubRateDisplay", `(${subHrs} hrs x ₱${hrRate.toFixed(2)}/hr)`);
    setSafeText("previewSubPay", "+ " + formatCurrency(subPay));
    
    setSafeText("previewSubDedRateDisplay", `(Missed: ${subDedHrs} hrs)`);
    setSafeText("previewSubDed", "- " + formatCurrency(absDedPay));
    
    setSafeText("previewManDedRateDisplay", `(${manDedHrs} hrs x ₱${hrRate.toFixed(2)}/hr)`);
    setSafeText("previewManDed", "- " + formatCurrency(manDedAmt));

    let suspDed = 0;
    let suspDates = '';

    if (data.teachingPayRecord) {
        suspDed = parseFloat(data.teachingPayRecord.suspensionDeduction) || 0;
        suspDates = data.teachingPayRecord.appliedSuspensionDates || '';
    }

    if (suspDed > 0 || suspDates !== '') {
        const rowSusp = document.getElementById('rowPreviewSusp');
        if (rowSusp) {
            rowSusp.style.display = 'flex';
            rowSusp.innerHTML = `
                <span>Suspension Deduction (-) <br>
                    <span style="color:#d32f2f; font-size: 11px;">(Rate: ₱${hrRate.toFixed(2)}/hr) Dates: ${suspDates}</span>
                </span>
                <span id="previewSuspDed" style="font-weight: 600;">- ${formatCurrency(suspDed)}</span>
            `;
        }
    } else {
        const rowSusp = document.getElementById('rowPreviewSusp');
        if(rowSusp) rowSusp.style.display = 'none';
    }

    const rowLec = document.getElementById('rowPreviewLec'); if(rowLec) rowLec.style.display = 'flex';
    const rowLab = document.getElementById('rowPreviewLab'); if(rowLab) rowLab.style.display = 'flex';

    toggleRow('rowPreviewAdmin', data.basicSalary);
    toggleRow('rowPreviewRle', rlePay);
    toggleRow('rowPreviewSgd', sgdPay);
    toggleRow('rowPreviewTut', tutPay);
    toggleRow('rowPreviewHon', honPay);
    toggleRow('rowPreviewSupp', suppPay);
    toggleRow('rowPreviewHoliday', holPay);
    toggleRow('rowPreviewAdj', adjPay);
    toggleRow('rowPreviewSub', subPay);
    toggleRow('rowPreviewSubDed', absDedPay); 
    toggleRow('rowPreviewManDed', manDedAmt);

    const hasDeductions = manDedAmt > 0 || suspDed > 0 || absDedPay > 0;
    const divider = document.getElementById('teachingDeductionsDivider');
    if(divider) divider.style.display = hasDeductions ? 'block' : 'none';

    const trueGrandTotal = (data.basicSalary || 0) + (tp.totalTeachingPay || 0);
    setSafeText("previewTotalEarnings", formatCurrency(trueGrandTotal));

    // FIX: HIDE ALL OTHER PREVIEWS AND RESET TITLE BEFORE SHOWING THIS ONE
    if(document.getElementById('batchPayslipPreview')) document.getElementById('batchPayslipPreview').style.display = 'none';
    if(document.getElementById('batchTeachPayslipPreview')) document.getElementById('batchTeachPayslipPreview').style.display = 'none';
    if(document.getElementById('employeePayslip')) document.getElementById('employeePayslip').style.display = 'none';
    if(document.getElementById('emptyPreviewState')) document.getElementById('emptyPreviewState').style.display = 'none';
    if(document.getElementById('payslipTitle')) document.getElementById('payslipTitle').innerText = "Manual Teaching Preview";

    if(document.getElementById('teachingPayslip')) {
        document.getElementById('teachingPayslip').style.display = 'block';
        document.getElementById('teachingPayslip').style.opacity = '1';
    }
}

async function calculateAllPayroll() {
    const startDate = document.getElementById('startDate')?.value;
    const endDate = document.getElementById('endDate')?.value;
    
    let department = 'ALL';
    const deptEl = document.getElementById('batchDepartment') || document.getElementById('departmentFilter') || document.getElementById('department');
    if (deptEl && deptEl.value) { department = deptEl.value; }

    if (!startDate || !endDate) {
        await customAlert("Validation Missing", "Please set Period Start and Period End dates.", "error");
        return;
    }

    const btn = document.getElementById('calcAllBtn');
    if (btn) { btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Calculating...'; btn.disabled = true; }

    try {
        const params = new URLSearchParams({ start: startDate, end: endDate, department: department });
        const response = await fetch(`/payroll-system/api/payroll/calculate-all?${params.toString()}`, { method: 'POST' });
        const data = await handleFetchResponse(response);
        
        if (document.getElementById('employeePayslip')) document.getElementById('employeePayslip').style.display = 'none';
        if (document.getElementById('teachingPayslip')) document.getElementById('teachingPayslip').style.display = 'none';
        if (document.getElementById('batchTeachPayslipPreview')) document.getElementById('batchTeachPayslipPreview').style.display = 'none';
        if (document.getElementById('emptyPreviewState')) document.getElementById('emptyPreviewState').style.display = 'none'; 

        const batchPreview = document.getElementById('batchPayslipPreview');
        if (batchPreview) {
            batchPreview.style.display = 'block';
            if (document.getElementById('payslipTitle')) document.getElementById('payslipTitle').innerText = "Batch Pre-Publish Summary";

            const tbody = document.getElementById('batchPreviewBody');
            if (tbody) {
                tbody.innerHTML = '';
                let grandTotalNet = 0;

                data.forEach(p => {
                    const empName = p.employee ? `${p.employee.firstName} ${p.employee.lastName}` : 'Unknown';
                    const empId = p.employee?.employeeId || '---';
                    const empStatus = p.employee?.employeeStatus || 'Unknown'; 
                    const penalties = (p.lateDeduction||0) + (p.undertimeDeduction||0) + (p.absentDeduction||0);
                    const totalDeduct = (p.sssDeduction||0) + (p.philhealthDeduction||0) + (p.pagibigDeduction||0) + 
                                        (p.withholdingTax||0) + (p.loanDeductions||0) + (p.sssLoan||0) + 
                                        (p.hdmfLoan||0) + (p.leaveWithoutPay||0);
                    
                    grandTotalNet += p.netPay || 0;
                    let teachPayTotal = p.teachingPayRecord ? p.teachingPayRecord.totalTeachingPay : 0;

                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${empId}</td>
                        <td style="text-align: left;">
                            <div style="font-weight: bold;">${empName}</div>
                            <span style="font-size: 0.75em; color: #fff; background-color: #64748b; padding: 2px 6px; border-radius: 4px;">${empStatus}</span>
                        </td>
                        <td>${formatCurrency(p.basicSalary)}</td>
                        <td>${formatCurrency(p.longevity)}</td>
                        <td>${formatCurrency(p.leavePay)}</td>
                        <td>${formatCurrency(p.holidayPay)}</td>
                        <td>${formatCurrency(p.honorarium)}</td>
                        <td style="color: #15803d; font-weight: bold;">${formatCurrency(teachPayTotal)}</td>
                        <td>${formatCurrency(p.overtimePay)}</td>
                        <td style="font-weight: bold;">${formatCurrency(p.grossIncome)}</td>
                        <td style="color: #0284c7; font-weight: bold;">${formatCurrency(p.totalEarnings)}</td>
                        <td>${formatCurrency(p.sssDeduction)}</td>
                        <td>${formatCurrency(p.philhealthDeduction)}</td>
                        <td>${formatCurrency(p.pagibigDeduction)}</td>
                        <td>${formatCurrency(p.sssLoan)}</td>
                        <td>${formatCurrency(p.hdmfLoan)}</td>
                        <td>${formatCurrency(p.loanDeductions)}</td>
                        <td>${formatCurrency(p.withholdingTax)}</td>
                        <td style="color: #ea580c; font-weight: bold;">${formatCurrency(penalties)}</td>
                        <td>${formatCurrency(p.leaveWithoutPay)}</td>
                        <td style="color: #b71c1c; font-weight: bold;">${formatCurrency(totalDeduct)}</td>
                        <td style="color: #059669; font-weight: bold;">${formatCurrency(p.netPay)}</td>
                    `;
                    tbody.appendChild(tr);
                });
                if (document.getElementById('batchTotalNet')) document.getElementById('batchTotalNet').innerText = formatCurrency(grandTotalNet);
            }
        }
        await customAlert("Success!", `Calculated preview successfully generated for ${data.length} employees.`, "success");
    } catch (error) {
        console.error(error);
        await customAlert("Batch Calculation Error", error.message, "error");
    } finally {
        if (btn) { btn.innerHTML = '<i class="fas fa-calculator"></i> Calculate'; btn.disabled = false; }
    }
}

async function sendAllPayroll() {
    const startDate = document.getElementById('startDate')?.value;
    const endDate = document.getElementById('endDate')?.value;
    
    let department = 'ALL';
    const deptEl = document.getElementById('batchDepartment') || document.getElementById('departmentFilter') || document.getElementById('department');
    if (deptEl && deptEl.value) { department = deptEl.value; }

    if (!startDate || !endDate) {
        await customAlert("Validation Missing", "Please set Period Start and Period End dates before saving to register.", "error");
        return;
    }
    
    const isConfirmed = await customConfirm("Are you sure you want to save the payroll for these employees to the Master Register?");
    if (!isConfirmed) return;

    const btn = document.getElementById('sendAllBtn');
    if (btn) { btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...'; btn.disabled = true; }

    try {
        const params = new URLSearchParams({ start: startDate, end: endDate, department: department });
        const response = await fetch(`/payroll-system/api/payroll/send-all?${params.toString()}`, { method: 'POST' });
        const data = await handleFetchResponse(response);
        
        await customAlert("Success!", `Saved payroll for ${data.length} employees to the Register. Redirecting...`, "success");
        window.location.href = '/payroll-system/payroll_register'; 
    } catch (error) {
        console.error(error);
        await customAlert("Batch Save Error", error.message, "error");
    } finally {
        if (btn) { btn.innerHTML = '<i class="fas fa-save"></i> Save to Register'; btn.disabled = false; }
    }
}

async function calculateAllTeachingPayroll() {
    const start = document.getElementById('startDate')?.value;
    const end = document.getElementById('endDate')?.value;
    const deptEl = document.getElementById('teachBatchDepartment');
    let department = deptEl ? deptEl.value : 'ALL';

    if (!start || !end) {
        await customAlert("Validation Missing", "Please set Period Start and Period End dates.", "error");
        return;
    }

    const btn = document.getElementById('calcAllTeachBtn');
    if (btn) { btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Calculating...'; btn.disabled = true; }

    try {
        const params = new URLSearchParams({ start: start, end: end, department: department });
        const response = await fetch(`/payroll-system/api/payroll/teaching/calculate-all?${params.toString()}`, { method: 'POST' });
        const data = await handleFetchResponse(response);
        
        if(document.getElementById('employeePayslip')) document.getElementById('employeePayslip').style.display = 'none';
        if(document.getElementById('teachingPayslip')) document.getElementById('teachingPayslip').style.display = 'none';
        if(document.getElementById('batchPayslipPreview')) document.getElementById('batchPayslipPreview').style.display = 'none';
        if(document.getElementById('emptyPreviewState')) document.getElementById('emptyPreviewState').style.display = 'none'; 

        if (data.length === 0) {
            await customAlert("Info", `No valid teaching records found to compute for the selected criteria.`, "info");
        } else {
            const batchTeachPreview = document.getElementById('batchTeachPayslipPreview');
            if (batchTeachPreview) {
                batchTeachPreview.style.display = 'block';
                if(document.getElementById('payslipTitle')) document.getElementById('payslipTitle').innerText = "Batch Teaching Pay Summary";

                const tbody = document.getElementById('batchTeachPreviewBody');
                if(tbody) {
                    tbody.innerHTML = '';
                    let grandTotalTeach = 0;

                    data.forEach(p => {
                        const tpData = p.teachingPayRecord || {}; 
                        const empName = p.employee ? `${p.employee.firstName} ${p.employee.lastName}` : 'Unknown';
                        const empId = p.employee?.employeeId || '---';
                        const empStatus = p.employee?.employeeStatus || 'Unknown';

						let rawLec = Number(tpData.totalLecUnits || 0); 
						                        let rawLab = Number(tpData.totalLabUnits || 0); 
						                        let totalUnits = (rawLec + rawLab).toFixed(1);
						                        
						                        // 🚨 THE FIX: Use excess hours from backend payload, not a fake property
						                        let totHrs = (Number(tpData.excessLecHours || 0) + Number(tpData.excessLabHours || 0)).toFixed(2);
						                        
						                        let lecPay = tpData.lecPay || 0;
						                        let labPay = tpData.labPay || 0;
                        
                        let subPay = tpData.substitutePay || 0;
                        let subDedHrs = tpData.absentDeductionHours || 0;
                        let subDedAmt = subDedHrs * (tpData.hourlyRate || 0);

                        let holPay = p.holidayPay || tpData.holidayPay || 0;
                        let totTeach = tpData.totalTeachingPay || 0;
                        
                        let basic = p.basicSalary || 0;
                        let combinedTotal = basic + totTeach;

                        grandTotalTeach += combinedTotal;

                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td style="text-align:center;">${empId}</td>
                            <td style="text-align: left; white-space: normal; line-height: 1.2;">
                                <div style="font-weight: bold; color: #1e293b;">${empName}</div>
                                <span style="font-size: 10px; color: #fff; background: #64748b; padding: 2px 6px; border-radius: 4px; display: inline-block; margin-top: 4px;">${empStatus}</span>
                            </td>
                            <td style="text-align: center; font-weight: bold; color: #0277bd;">${totalUnits}</td> 
                            <td style="font-weight: bold; color: #b71c1c;">${totHrs} hrs</td>
                            <td style="color:#166534; font-weight:bold; background-color:#f0fdf4;">${formatCurrency(basic)}</td>
                            <td>${formatCurrency(lecPay)}</td>
                            <td>${formatCurrency(labPay)}</td>
                            <td style="color: #15803d; font-weight: 600;">+ ${formatCurrency(subPay)}</td>
                            <td style="color: #b91c1c; font-weight: 600;">- ${formatCurrency(subDedAmt)}</td>
                            <td>${formatCurrency(holPay)}</td>
                            <td style="color: #059669; font-weight: bold; background-color: #f8fafc;">${formatCurrency(combinedTotal)}</td>
                        `;
                        tbody.appendChild(tr);
                    });
                    if(document.getElementById('batchTeachTotalNet')) document.getElementById('batchTeachTotalNet').innerText = formatCurrency(grandTotalTeach);
                }
            }
            await customAlert("Success!", `Calculated Teaching Pay generated for ${data.length} faculty members.`, "success");
        }
    } catch (error) { 
        console.error(error);
        await customAlert("Batch Teaching Error", error.message, "error"); 
    } finally { 
        if (btn) { btn.innerHTML = '<i class="fas fa-calculator"></i> Calculate'; btn.disabled = false; } 
    }
}

async function sendAllTeachingPayroll() {
    const start = document.getElementById('startDate')?.value;
    const end = document.getElementById('endDate')?.value;
    const deptEl = document.getElementById('teachBatchDepartment');
    let department = deptEl ? deptEl.value : 'ALL';

    if (!start || !end) {
        await customAlert("Validation Missing", "Please set Period Start and Period End dates before saving to register.", "error");
        return;
    }

    const isConfirmed = await customConfirm("Are you sure you want to save the teaching pay for this department to the Register?");
    if (!isConfirmed) return;

    const btn = document.getElementById('sendAllTeachBtn');
    if (btn) { btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...'; btn.disabled = true; }

    try {
        const params = new URLSearchParams({ start: start, end: end, department: department });
        const response = await fetch(`/payroll-system/api/payroll/teaching/send-all?${params.toString()}`, { method: 'POST' });
        const data = await handleFetchResponse(response);
        
        if (data.length === 0) {
            await customAlert("Warning", `No teaching payroll was generated. Check your dates or database.`, "error");
        } else {
            await customAlert("Success!", `Saved Teaching Pay for ${data.length} faculty members to the Register. Redirecting...`, "success");
            window.location.href = '/payroll-system/payroll_register';
        }
    } catch (error) { 
        console.error(error);
        await customAlert("Batch Save Error", error.message, "error"); 
    } finally { 
        if (btn) { btn.innerHTML = '<i class="fas fa-save"></i> Save to Register'; btn.disabled = false; } 
    }
}