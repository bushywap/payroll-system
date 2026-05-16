let contextPath = document.querySelector('meta[name="context-path"]')?.getAttribute('content') || '';
if (contextPath === '/') { contextPath = ''; } 
else if (contextPath.endsWith('/')) { contextPath = contextPath.slice(0, -1); }

document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.getElementById('searchProcess');
    const startDateInput = document.getElementById('filterStartDate');
    const endDateInput = document.getElementById('filterEndDate');
    
    // 1. Generate ALL generic cutoff dates for the dropdown
    populateCutoffDropdown();

    // 2. Check for preserved dates in URL (Prevents jumping to default cut-off)
    const urlParams = new URLSearchParams(window.location.search);
    const preservedStart = urlParams.get('start');
    const preservedEnd = urlParams.get('end');

    if (preservedStart && preservedEnd && startDateInput && endDateInput) {
        startDateInput.value = preservedStart;
        endDateInput.value = preservedEnd;
        syncDropdownWithManualDates();
        filterTable();
    } else {
        // Auto-Set to the CURRENT 15-day cutoff date ONLY if no URL params exist
        setDefaultDates();
    }

    // 3. Listeners
    if(searchInput) searchInput.addEventListener('input', filterTable);
    
    if(startDateInput) {
        startDateInput.addEventListener('change', () => { 
            syncDropdownWithManualDates();
            filterTable(); 
        });
    }
    if(endDateInput) {
        endDateInput.addEventListener('change', () => { 
            syncDropdownWithManualDates();
            filterTable(); 
        });
    }
	
	const reportBtn = document.getElementById("generateReportBtn");
	    if(reportBtn) {
	        reportBtn.addEventListener("click", function() {
	            window.location.href = contextPath + '/api/teaching-pay/export-report';
	        });
	    }
});

// Helper for formatting JS Date to YYYY-MM-DD input string
function formatDateForInput(dateObj) {
    const yyyy = dateObj.getFullYear();
    const mm = String(dateObj.getMonth() + 1).padStart(2, '0');
    const dd = String(dateObj.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
}

// Generates ALL cutoff dates for the current year (and previous year) and adds to Dropdown
function populateCutoffDropdown() {
    const dropdown = document.getElementById('cutoffDropdown');
    if (!dropdown) return;

    dropdown.innerHTML = '<option value="">-- Custom Date / Auto --</option>';

    const today = new Date();
    const currentYear = today.getFullYear();
    const yearsToGenerate = [currentYear, currentYear - 1]; 

    yearsToGenerate.forEach(year => {
        for (let month = 11; month >= 0; month--) {
            const secondStart = new Date(year, month, 16);
            const secondEnd = new Date(year, month + 1, 0); 
            
            const firstStart = new Date(year, month, 1);
            const firstEnd = new Date(year, month, 15);
            
            addOptionToDropdown(dropdown, secondStart, secondEnd);
            addOptionToDropdown(dropdown, firstStart, firstEnd);
        }
    });

    // -------------------------------------------------------------
    // UPDATED DROPDOWN LISTENER: Now triggers a server generation
    // -------------------------------------------------------------
    dropdown.addEventListener('change', function() {
        if (this.value) {
            const [s, e] = this.value.split('|');
            
            // This forces the browser to send the dates to your Controller,
            // which will rebuild the database for the missing periods!
            window.location.href = contextPath + `/teaching_payroll?start=${s}&end=${e}`;
        }
    });
}

function addOptionToDropdown(dropdown, startDt, endDt) {
    const formatOptions = { year: 'numeric', month: 'short', day: 'numeric' };
    const startStrDisp = startDt.toLocaleDateString('en-US', formatOptions);
    const endStrDisp = endDt.toLocaleDateString('en-US', formatOptions);
    
    const startVal = formatDateForInput(startDt);
    const endVal = formatDateForInput(endDt);

    const option = document.createElement('option');
    option.value = startVal + '|' + endVal;
    option.textContent = `${startStrDisp} - ${endStrDisp}`;
    dropdown.appendChild(option);
}

// Resets date to the exact 1st or 2nd cutoff of the CURRENT month
function setDefaultDates() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();
    const day = today.getDate();

    let startDate, endDate;

    if (day <= 15) {
        startDate = new Date(year, month, 1);
        endDate = new Date(year, month, 15);
    } else {
        startDate = new Date(year, month, 16);
        endDate = new Date(year, month + 1, 0);
    }

    const startVal = formatDateForInput(startDate);
    const endVal = formatDateForInput(endDate);
    
    document.getElementById('filterStartDate').value = startVal;
    document.getElementById('filterEndDate').value = endVal;
    
    syncDropdownWithManualDates();
    filterTable(); 
}

function resetToDefaultDates() {
    setDefaultDates();
}

function syncDropdownWithManualDates() {
    const dropdown = document.getElementById('cutoffDropdown');
    const s = document.getElementById('filterStartDate').value;
    const e = document.getElementById('filterEndDate').value;
    const targetValue = s + '|' + e;
    
    let optionExists = Array.from(dropdown.options).some(opt => opt.value === targetValue);
    if (optionExists) {
        dropdown.value = targetValue;
    } else {
        dropdown.value = ""; 
    }
}

// EXACT BOUNDARY FILTER
function filterTable() {
    const searchVal = document.getElementById('searchProcess')?.value.toLowerCase() || '';
    const startVal = document.getElementById('filterStartDate')?.value || '';
    const endVal = document.getElementById('filterEndDate')?.value || '';
    
    // --- AUTOMATIC PERIOD DETECTION BADGE ---
    const periodBadge = document.getElementById('periodBadge');
    if (startVal && endVal && periodBadge) {
        const endDateObj = new Date(endVal);
        const dayOfMonth = endDateObj.getDate();
        
        if (dayOfMonth <= 15) {
            periodBadge.innerHTML = '<i class="fas fa-calendar-check"></i> Period: 1st Cutoff';
            periodBadge.style.background = '#0284c7'; 
            periodBadge.style.boxShadow = '0 2px 8px rgba(2, 132, 199, 0.3)';
        } else {
            periodBadge.innerHTML = '<i class="fas fa-calendar-check"></i> Period: 2nd Cutoff';
            periodBadge.style.background = '#b71c1c'; 
            periodBadge.style.boxShadow = '0 2px 8px rgba(183, 28, 28, 0.3)';
        }
    } else if (periodBadge) {
        periodBadge.innerHTML = '<i class="fas fa-clock"></i> Period: --- Select Dates ---';
        periodBadge.style.background = '#64748b'; 
        periodBadge.style.boxShadow = 'none';
    }

    const filterStart = startVal ? new Date(startVal).setHours(0,0,0,0) : null;
    const filterEnd = endVal ? new Date(endVal).setHours(23,59,59,999) : null;
    
    const rows = document.querySelectorAll('.process-row');

    rows.forEach(row => {
        const nameData = row.getAttribute('data-search') || '';
        const rowEndStr = row.getAttribute('data-end') || ''; 
        
        const matchesSearch = nameData.includes(searchVal);
        let inDateRange = true;

        if (rowEndStr) {
            const recordDate = new Date(rowEndStr).setHours(0,0,0,0);
            
            if (filterStart !== null && recordDate < filterStart) {
                inDateRange = false;
            }
            if (filterEnd !== null && recordDate > filterEnd) {
                inDateRange = false;
            }
        } else {
            inDateRange = false; 
        }

        if (matchesSearch && inDateRange) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

// -------------------------------------------------------------
// NEW: MAKE-UP CLASSES MODAL LOGIC
// -------------------------------------------------------------
function openMakeUpModal(payId, currentLec, currentLab) {
    document.getElementById('makeUpPayId').value = payId;
    document.getElementById('lecHoursInput').value = currentLec;
    document.getElementById('labHoursInput').value = currentLab;
    document.getElementById('makeUpModal').style.display = 'flex';
}

function saveMakeUpHours() {
    const payId = document.getElementById('makeUpPayId').value;
    const lecHours = document.getElementById('lecHoursInput').value || 0.0;
    const labHours = document.getElementById('labHoursInput').value || 0.0;

    const btn = document.querySelector('#makeUpModal button');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
    btn.disabled = true;

    fetch(contextPath + `/api/teaching-pay/${payId}/makeup`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            'lecHours': lecHours,
            'labHours': labHours
        })
    })
    .then(response => {
        if (response.ok) {
            closeModal('makeUpModal');
            // Refresh with current filters intact
            const currentStart = document.getElementById('filterStartDate')?.value || '';
            const currentEnd = document.getElementById('filterEndDate')?.value || '';
            
            if (currentStart && currentEnd) {
                window.location.href = window.location.pathname + `?start=${currentStart}&end=${currentEnd}`;
            } else {
                window.location.reload(); 
            }
        } else {
            alert('Failed to save make-up classes. Please try again.');
            btn.innerHTML = '<i class="fas fa-save"></i> Save Make-Up Hours';
            btn.disabled = false;
        }
    })
    .catch(error => {
        console.error('Error saving make-up hours:', error);
        alert('A network error occurred.');
        btn.innerHTML = '<i class="fas fa-save"></i> Save Make-Up Hours';
        btn.disabled = false;
    });
}
// -------------------------------------------------------------

function openDedHrsBreakdown(element) {
    const empName = element.getAttribute('data-empname') || 'Unknown Employee';
    const manHrs = parseFloat(element.getAttribute('data-mandh')) || 0;
    const absHrs = parseFloat(element.getAttribute('data-absdh')) || 0;
    const totalHrs = manHrs + absHrs;

    document.getElementById('dhEmpName').innerText = empName;
    document.getElementById('dhManHrs').innerText = manHrs.toFixed(2) + " Hrs";
    document.getElementById('dhAbsHrs').innerText = absHrs.toFixed(2) + " Hrs";
    document.getElementById('dhTotalHrs').innerText = totalHrs.toFixed(2) + " Hrs";

    document.getElementById('dedHrsModal').style.display = 'flex';
}

function openMathBreakdown(element) {
    document.getElementById('mbEmpName').innerText = element.getAttribute('data-emp');
    
    // Earnings
    const lec = parseFloat(element.getAttribute('data-lecpay')) || 0;
    const lab = parseFloat(element.getAttribute('data-labpay')) || 0;
    const rle = parseFloat(element.getAttribute('data-rlepay')) || 0;
    const makeup = parseFloat(element.getAttribute('data-makeuppay')) || 0;
    const sub = parseFloat(element.getAttribute('data-subpay')) || 0;
    const sgd = parseFloat(element.getAttribute('data-sgdpay')) || 0;
    const tutLec = parseFloat(element.getAttribute('data-tutlecpay')) || 0;
    const tutLab = parseFloat(element.getAttribute('data-tutlabpay')) || 0;
    const admin = parseFloat(element.getAttribute('data-adminpay')) || 0;
    const hon = parseFloat(element.getAttribute('data-honorarium')) || 0;
    const supp = parseFloat(element.getAttribute('data-supp')) || 0;
    const hol = parseFloat(element.getAttribute('data-hol')) || 0;
    const adj = parseFloat(element.getAttribute('data-adj')) || 0;
    
    document.getElementById('mbLec').innerText = '₱' + lec.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbLab').innerText = '₱' + lab.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbRle').innerText = '₱' + rle.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbMakeUp').innerText = '₱' + makeup.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbSub').innerText = '₱' + sub.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbSgd').innerText = '₱' + sgd.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbTut').innerText = '₱' + (tutLec + tutLab).toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbAdmin').innerText = '₱' + admin.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbHon').innerText = '₱' + hon.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbSupp').innerText = '₱' + supp.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbHol').innerText = '₱' + hol.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbAdj').innerText = '₱' + adj.toLocaleString('en-US', {minimumFractionDigits: 2});

    // ---------------------------------------------------------
    // DYNAMIC BREAKDOWN TEXT LOGIC
    // ---------------------------------------------------------
    const rate = parseFloat(element.getAttribute('data-rate')) || 0;
    const labRate = rate * 0.75; // Apply the 75% rule for display

    const classification = element.getAttribute('data-class') || 'Regular Full-Time';
    const totalLec = parseFloat(element.getAttribute('data-totallec')) || 0;
    const excLec = parseFloat(element.getAttribute('data-exclec')) || 0;
    const totalLab = parseFloat(element.getAttribute('data-totallab')) || 0;
    const excLab = parseFloat(element.getAttribute('data-exclab')) || 0;

    let lecStr = '';
    let labStr = '';

    if (classification === 'Full-Time Flexi') {
         lecStr = `(${totalLec.toFixed(1)} total units = ${excLec.toFixed(2)} Flexi hrs @ ₱${rate.toFixed(2)}/hr)`;
         labStr = `(${totalLab.toFixed(1)} total units = ${excLab.toFixed(2)} Flexi hrs @ ₱${labRate.toFixed(2)}/hr)`;
    } else {
         lecStr = `(${totalLec.toFixed(1)} total units = ${excLec.toFixed(2)} payable hrs @ ₱${rate.toFixed(2)}/hr)`;
         labStr = `(${totalLab.toFixed(1)} total units = ${excLab.toFixed(2)} payable hrs @ ₱${labRate.toFixed(2)}/hr)`;
    }

    document.getElementById('mbLecText').innerText = lecStr;
    document.getElementById('mbLabText').innerText = labStr;
    // ---------------------------------------------------------

    const absentHrs = parseFloat(element.getAttribute('data-absenthrs')) || 0;
    
    // ✅ NO GHOST PENALTY OVERRIDE: We only trust the exact Absent Pay amount generated by the Backend!
    let absentPay = parseFloat(element.getAttribute('data-absentpay')) || 0;
    
    let effectiveSubRate = rate;
    if(absentHrs > 0 && absentPay > 0) {
        effectiveSubRate = absentPay / absentHrs;
    }

    const dedHrs = parseFloat(element.getAttribute('data-mandedhrs')) || 0;
    const dedAmt = rate * dedHrs;
    const suspAmt = parseFloat(element.getAttribute('data-susp')) || 0;
    const suspRate = rate; 
    
    let suspDates = element.getAttribute('data-suspdates');
    if (!suspDates || suspDates.trim() === '') {
        suspDates = 'None';
    }
    
    const totalDeductions = absentPay + dedAmt + suspAmt;
    
    document.getElementById('mbBaseRate').innerText = rate.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbSubRateVal').innerText = effectiveSubRate.toLocaleString('en-US', {minimumFractionDigits: 2}); 
    document.getElementById('mbSuspRateVal').innerText = suspRate.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbSuspDates').innerText = suspDates;
    document.getElementById('mbManDedRateVal').innerText = rate.toLocaleString('en-US', {minimumFractionDigits: 2});
    
    document.getElementById('mbSubHrs').innerText = absentHrs;
    document.getElementById('mbManHrs').innerText = dedHrs;
    
    document.getElementById('mbSubDed').innerText = '- ₱' + absentPay.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbDed').innerText = '- ₱' + dedAmt.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbSusp').innerText = '- ₱' + suspAmt.toLocaleString('en-US', {minimumFractionDigits: 2});
    document.getElementById('mbTotalDed').innerText = '- ₱' + totalDeductions.toLocaleString('en-US', {minimumFractionDigits: 2});

    const gross = parseFloat(element.getAttribute('data-gross')) || 0;
    document.getElementById('mbGross').innerText = '₱' + gross.toLocaleString('en-US', {minimumFractionDigits: 2});

    document.getElementById('mathBreakdownModal').style.display = 'flex';
}

function openInfoModal(rate, classification, subHrs, adjHrs, suspDates, absentHrs) {
    document.getElementById('infoRate').innerText = '₱' + parseFloat(rate).toFixed(2);
    document.getElementById('infoSched').innerText = classification || 'N/A';
    document.getElementById('infoSub').innerText = (subHrs || 0) + ' Hrs';
    document.getElementById('infoAdj').innerText = (adjHrs || 0) + ' Hrs';
    
    document.getElementById('infoSusp').innerText = suspDates && suspDates.trim() !== '' ? suspDates : 'None';
    
    const absenceRow = document.getElementById('infoAbsenceWarningRow');
    if (absentHrs && parseFloat(absentHrs) > 0) {
        absenceRow.style.display = 'block';
        document.getElementById('infoAbsence').innerText = absentHrs + ' Hrs Deducted';
    } else {
        absenceRow.style.display = 'none';
    }
    
    document.getElementById('infoModal').style.display = 'block';
}

function openProcessModal(id) {
    document.getElementById('editPayId').value = id;
    document.getElementById('processModal').style.display = 'flex';
    
    fetch(contextPath + `/teaching-pay/api/get-process/${id}`)
        .then(response => response.json())
        .then(data => {
            document.getElementById('epAdjHrs').value = data.adjustmentHours || 0;
            document.getElementById('epAdjPay').value = data.adjustmentPay || 0;
            
            document.getElementById('epAbsentHrs').value = data.absentDeductionHours || 0;
            document.getElementById('epDedHrs').value = data.deductionHours || 0; 
            
            document.getElementById('epSgdHrs').value = data.sgdHours || 0;
            document.getElementById('epTutLecHrs').value = data.tutorialLecHours || 0;
            document.getElementById('epTutLabHrs').value = data.tutorialLabHours || 0;
            document.getElementById('epHon').value = data.honorarium || 0;
            document.getElementById('epAdmin').value = data.adminPay || 0;
            document.getElementById('epSupp').value = data.supplementalPay || 0;
        })
        .catch(error => console.error("Error fetching process data:", error));
}

function openRatesModal(id) {
    const container = document.getElementById('ratesContent');
    container.innerHTML = '<div style="text-align:center; padding: 20px; color:#555;"><i class="fas fa-spinner fa-spin"></i> Loading rates data...</div>';
    document.getElementById('ratesModal').style.display = 'flex';

    fetch(contextPath + `/teaching-pay/api/get-process/${id}`)
        .then(res => res.json())
        .then(data => {
            const lecRate = data.hourlyRate || 0;
            const labRate = data.labRate || 0;
            const rleRate = data.rleRate || 0; 
            
            const empId = data.employee ? data.employee.employeeId : 'N/A';
            const empName = data.employee ? `${data.employee.firstName} ${data.employee.lastName}` : 'N/A';
            const status = data.workloadClassification || 'N/A';
            
            container.innerHTML = `
                <div class="info-row"><strong>Emp ID:</strong> <span>${empId}</span></div>
                <div class="info-row"><strong>Name:</strong> <span>${empName}</span></div>
                <div class="info-row"><strong>Status:</strong> <span>${status}</span></div>
                <hr style="border: 0; border-top: 1px solid #eee; margin: 10px 0;">
                <div class="info-row"><strong>Lecture Rates:</strong> <span style="color:#2e7d32;">₱${lecRate.toFixed(2)}</span></div>
                <div class="info-row"><strong>Lab Rates:</strong> <span style="color:#2e7d32;">₱${labRate.toFixed(2)}</span></div>
                <div class="info-row"><strong>RLE Rates:</strong> <span style="color:#2e7d32;">₱${rleRate.toFixed(2)}</span></div>
                <div class="info-row"><strong>Deduction Hrs:</strong> <span style="color:#d32f2f;">${(data.totalDeductionHours || 0).toFixed(2)}</span></div>
                <div class="info-row"><strong>Admin Pay:</strong> <span>₱${(data.adminPay || 0).toFixed(2)}</span></div>
                <div class="info-row"><strong>Honorarium:</strong> <span>₱${(data.honorarium || 0).toFixed(2)}</span></div>
                <div class="info-row"><strong>Supplemental:</strong> <span>₱${(data.supplementalPay || 0).toFixed(2)}</span></div>
            `;
        })
        .catch(err => {
            console.error("Error fetching rates:", err);
            container.innerHTML = '<div style="color:#d32f2f; text-align:center; padding: 20px;">Failed to load data. Check backend connection.</div>';
        });
}

function openScheduleSummaryModal(id) {
    const container = document.getElementById('scheduleSummaryContent');
    container.innerHTML = '<div style="text-align:center; padding: 40px; color:#555;"><i class="fas fa-spinner fa-spin fa-2x"></i><br><br>Loading schedule...</div>';
    
    document.getElementById('scheduleSummaryModal').style.display = 'flex';

    fetch(contextPath + `/teaching-pay/api/get-process/${id}`)
        .then(res => res.json())
        .then(processData => {
            // ✅ FIX 1: Use employeeId (String School ID) instead of id (Numeric)
            const empId = processData.employee ? processData.employee.employeeId : null;

            if (!empId) throw new Error("Could not find Employee ID attached to this record.");

            // ✅ FIX 2: Corrected the duplicated URL path
            return fetch(contextPath + `/api/teaching-load/schedule/${empId}`)
                .then(scheduleRes => {
                    if (!scheduleRes.ok) {
                        throw new Error(`Server returned ${scheduleRes.status}`);
                    }
                    return scheduleRes.json();
                })
                .then(scheduleData => {
                    // ... (rest of the code remains the same)
                    let totalLec = 0;
                    let totalLab = 0;
                    let totalRle = 0;
                    let totalHrs = 0;

                    let tableHtml = `
                        <div style="max-height: 350px; overflow-y: auto; margin-top: 15px; border: 1px solid #e0e0e0; border-radius: 8px;">
                            <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 13px;">
                                <thead style="background-color: #f8f9fa; position: sticky; top: 0; z-index: 1;">
                                    <tr>
                                        <th style="padding: 14px 15px; border-bottom: 2px solid #ddd;">Subject Code</th>
                                        <th style="padding: 14px 15px; border-bottom: 2px solid #ddd;">Description</th>
                                        <th style="padding: 14px 15px; border-bottom: 2px solid #ddd;">Day</th>
                                        <th style="padding: 14px 15px; border-bottom: 2px solid #ddd;">Time</th>
                                        <th style="padding: 14px 15px; border-bottom: 2px solid #ddd; text-align: center;">Hours</th>
                                    </tr>
                                </thead>
                                <tbody>
                    `;

                    if (!Array.isArray(scheduleData) || scheduleData.length === 0) {
                        tableHtml += `<tr><td colspan="5" style="padding: 25px; text-align: center; color: #888;">No schedule records found in database for Employee ID: ${empId}</td></tr>`;
                    } else {
						scheduleData.forEach(load => {
						    // ✅ FIX: Use actual hours from the backend instead of hardcoded units * 3
						    let actualLecHrs = load.lecHours || (load.lectureUnits * 1.0);
						    let actualLabHrs = load.labHours || (load.labUnits * 3.0);
						    let rleHrs = load.rleHours || 0;

						    totalLec += actualLecHrs;
						    totalLab += actualLabHrs;
						    totalRle += rleHrs;
						    totalHrs += (actualLecHrs + actualLabHrs + rleHrs);

						    tableHtml += `
						        <tr style="border-bottom: 1px solid #f0f0f0;">
						            <td style="padding: 12px 15px; font-weight: 700;">${load.subjectCode || 'N/A'}</td>
						            <td style="padding: 12px 15px;">${load.subject || 'N/A'}</td>
						            <td style="padding: 12px 15px;">${load.dayOfWeek || 'N/A'}</td>
						            <td style="padding: 12px 15px;">${load.timeSchedule || 'N/A'}</td>
						            <td style="padding: 12px 15px; text-align: center;">
						                <span style="color: #2e7d32; font-weight: 800; background: #e8f5e9; padding: 3px 8px; border-radius: 4px;">${actualLecHrs.toFixed(1)}</span> 
						                <span style="color: #d32f2f; font-weight: 800; background: #ffebee; padding: 3px 8px; border-radius: 4px;">${actualLabHrs.toFixed(1)}</span>
						            </td>
						        </tr>
						    `;
						});
                    }

                    tableHtml += `</tbody></table></div>`;

                    container.innerHTML = `
                        <div style="padding: 0 5px;">
                            <div style="font-size: 16px; font-weight: 700; color: #cc0000; margin-bottom: 5px;"><i class="fas fa-chalkboard-teacher"></i> TEACHING LOAD</div> 
                            ${tableHtml}
                        </div>
                        <div style="display: flex; flex-wrap: wrap; gap: 15px; margin-top: 25px; padding: 0 5px;">
                            <div style="flex: 1; background: #f8f9fa; border: 1px solid #e0e0e0; padding: 15px; border-radius: 8px; text-align: center;">
                                <div style="color: #666; font-size: 12px; font-weight: 700; margin-bottom: 5px;">TOTAL LECTURES</div>
                                <div style="color: #333; font-size: 20px; font-weight: 800;">${totalLec.toFixed(2)} Hrs</div>
                            </div>
                            <div style="flex: 1; background: #f8f9fa; border: 1px solid #e0e0e0; padding: 15px; border-radius: 8px; text-align: center;">
                                <div style="color: #666; font-size: 12px; font-weight: 700; margin-bottom: 5px;">TOTAL LAB</div>
                                <div style="color: #333; font-size: 20px; font-weight: 800;">${totalLab.toFixed(2)} Hrs</div>
                            </div>
                            <div style="flex: 1; background: #f8f9fa; border: 1px solid #e0e0e0; padding: 15px; border-radius: 8px; text-align: center;">
                                <div style="color: #666; font-size: 12px; font-weight: 700; margin-bottom: 5px;">TOTAL RLE</div>
                                <div style="color: #333; font-size: 20px; font-weight: 800;">${totalRle.toFixed(2)} Hrs</div>
                            </div>
                            <div style="flex: 1.2; background: #e3f2fd; border: 1px solid #bbdefb; padding: 15px; border-radius: 8px; text-align: center;">
                                <div style="color: #1565c0; font-size: 12px; font-weight: 800; margin-bottom: 5px;">TOTAL HRS</div>
                                <div style="color: #1565c0; font-size: 22px; font-weight: 900;">${totalHrs.toFixed(2)} Hrs</div>
                            </div>
                        </div>
                    `;
                });
        })
        .catch(err => {
            console.error("Error building schedule summary:", err);
            container.innerHTML = `<div style="color:#d32f2f; text-align:center; padding: 30px;">Failed to load data. Ensure the faculty has assigned records.</div>`;
        });
}

function openManualAdjModal(id) {
    document.getElementById('adjPayId').value = id;
    
    document.getElementById('adjEmpId').innerText = "Loading...";
    document.getElementById('adjEmpName').innerText = "Loading...";
    document.getElementById('adjAmount').value = '';
    document.getElementById('adjRemarks').value = '';
    document.getElementById('adjType').value = 'INCOME';
    
    document.getElementById('manualAdjModal').style.display = 'flex';
    
    fetch(contextPath + `/teaching-pay/api/get-process/${id}`)
        .then(res => res.json())
        .then(data => {
            document.getElementById('adjEmpId').innerText = data.employee ? data.employee.employeeId : 'N/A';
            document.getElementById('adjEmpName').innerText = data.employee ? `${data.employee.firstName} ${data.employee.lastName}` : 'N/A';
            
            if (data.adjustmentPay !== undefined && data.adjustmentPay !== null && data.adjustmentPay !== 0) {
                document.getElementById('adjAmount').value = Math.abs(data.adjustmentPay);
                document.getElementById('adjType').value = data.adjustmentPay >= 0 ? 'INCOME' : 'DEDUCTION';
            }
            if (data.adjustmentRemarks) {
                document.getElementById('adjRemarks').value = data.adjustmentRemarks;
            } else if (data.remarks) { 
                document.getElementById('adjRemarks').value = data.remarks;
            }
        })
        .catch(err => {
            console.error("Error fetching adjustment data:", err);
            document.getElementById('adjEmpName').innerText = "Database Error";
        });
}

function saveManualAdjustment() {
    const id = document.getElementById('adjPayId').value;
    const type = document.getElementById('adjType').value;
    const amount = parseFloat(document.getElementById('adjAmount').value);
    const remarks = document.getElementById('adjRemarks').value;
    
    if(!amount || amount <= 0) {
        alert("Please enter a valid amount.");
        return;
    }
    
    const finalAmount = type === 'DEDUCTION' ? -Math.abs(amount) : Math.abs(amount);
    
    fetch(contextPath + `/teaching-pay/api/update-process/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
            adjustmentPay: finalAmount, 
            adjustmentRemarks: remarks,
            remarks: remarks
        }) 
    })
    .then(response => {
        if (!response.ok) throw new Error("Update failed");
        document.getElementById('manualAdjModal').style.display = 'none';
        
        const currentStart = document.getElementById('filterStartDate')?.value || '';
        const currentEnd = document.getElementById('filterEndDate')?.value || '';
        
        if (currentStart && currentEnd) {
            window.location.href = window.location.pathname + `?start=${currentStart}&end=${currentEnd}`;
        } else {
            window.location.reload(); 
        }
    })
    .catch(error => alert("Error posting adjustment to database."));
}

function openSubHistoryModal(id) {
    const subAsSubstituteList = document.getElementById('subAsSubstituteList');
    const subWhenAbsentList = document.getElementById('subWhenAbsentList');
    
    subAsSubstituteList.innerHTML = '<li style="color:#555;"><i class="fas fa-spinner fa-spin"></i> Loading...</li>';
    subWhenAbsentList.innerHTML = '<li style="color:#555;"><i class="fas fa-spinner fa-spin"></i> Loading...</li>';
    
    document.getElementById('subHistoryModal').style.display = 'flex';

    fetch(contextPath + `/teaching-pay/api/get-process/${id}`)
        .then(res => res.json())
        .then(data => {
            subAsSubstituteList.innerHTML = '';
            subWhenAbsentList.innerHTML = '';

            if (data.substitutionsAsReliever && data.substitutionsAsReliever.length > 0) {
                data.substitutionsAsReliever.forEach(sub => {
                    const hrs = sub.hoursRendered ? sub.hoursRendered : 0;
                    const sTime = sub.startTime ? sub.startTime : '';
                    const eTime = sub.endTime ? sub.endTime : '';
                    const type = sub.loadType ? sub.loadType : 'LEC';
                    const displayDate = sub.actualDate && sub.actualDate !== 'N/A' ? sub.actualDate : '00/00/0000'; 

                    subAsSubstituteList.innerHTML += `<li><i class="fas fa-check" style="color:#2e7d32;"></i> Relieved ${sub.absentFacultyName} (${hrs} Hrs) - DATE: ${displayDate} | TIME: ${sTime} - ${eTime} | TYPE: ${type}</li>`;
                });
            } else {
                subAsSubstituteList.innerHTML = `<li><i class="fas fa-info-circle" style="color:#888;"></i> No records of substituting for others.</li>`;
            }

            if (data.substitutionsWhenAbsent && data.substitutionsWhenAbsent.length > 0) {
                data.substitutionsWhenAbsent.forEach(sub => {
                    const hrs = sub.hoursRendered ? sub.hoursRendered : 0;
                    const sTime = sub.startTime ? sub.startTime : '';
                    const eTime = sub.endTime ? sub.endTime : '';
                    const type = sub.loadType ? sub.loadType : 'LEC';
                    const displayDate = sub.actualDate && sub.actualDate !== 'N/A' ? sub.actualDate : '00/00/0000'; 

                    subWhenAbsentList.innerHTML += `<li><i class="fas fa-times" style="color:#d32f2f;"></i> Relieved by ${sub.relieverFacultyName} (${hrs} Hrs) - DATE: ${displayDate} | TIME: ${sTime} - ${eTime} | TYPE: ${type}</li>`;
                });
            } else {
                subWhenAbsentList.innerHTML = `<li><i class="fas fa-info-circle" style="color:#888;"></i> No records of being substituted.</li>`;
            }
        })
        .catch(err => {
            console.error("Error fetching substitution history:", err);
            subAsSubstituteList.innerHTML = '<li style="color:#d32f2f;">Failed to load data.</li>';
            subWhenAbsentList.innerHTML = '<li style="color:#d32f2f;">Failed to load data.</li>';
        });
}

function openAdjHistoryModal(element) {
    const id = element.getAttribute('data-id');
    const empName = element.getAttribute('data-empname');

    document.getElementById('adjHistEmpName').innerText = empName;
    const listContainer = document.getElementById('adjHistoryList');
    listContainer.innerHTML = '<div style="text-align:center; padding: 20px; color:#555;"><i class="fas fa-spinner fa-spin"></i> Loading adjustment history...</div>';
    
    document.getElementById('adjHistoryModal').style.display = 'flex';

    fetch(contextPath + `/teaching-pay/api/get-process/${id}`)
        .then(res => res.json())
        .then(data => {
            listContainer.innerHTML = '';
            let hasHistory = false;
            
            if (data.adjustmentHistory && data.adjustmentHistory.length > 0) {
                hasHistory = true;
                data.adjustmentHistory.forEach(item => {
                    const date = item.date || 'N/A';
                    const amt = item.amount || 0;
                    const remarks = item.remarks || item.adjustmentRemarks || 'No description provided';
                    const color = amt >= 0 ? '#2e7d32' : '#d32f2f';
                    const sign = amt >= 0 ? '+' : '';
                    
                    listContainer.innerHTML += `
                        <div style="border-bottom: 1px solid #eee; padding: 12px 0;">
                            <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                                <span style="font-weight: bold; color: #555;"><i class="fas fa-calendar-day" style="color:#1565c0; margin-right:4px;"></i> ${date}</span>
                                <span style="font-weight: 900; color: ${color}; font-size: 15px;">${sign}₱${Math.abs(amt).toFixed(2)}</span>
                            </div>
                            <div style="font-size: 13px; color: #666; font-style: italic;">"${remarks}"</div>
                        </div>
                    `;
                });
            } 
            else if (data.adjustmentPay && data.adjustmentPay !== 0) {
                hasHistory = true;
                const amt = data.adjustmentPay;
                const remarks = data.adjustmentRemarks || data.remarks || 'No description provided';
                const color = amt >= 0 ? '#2e7d32' : '#d32f2f';
                const sign = amt >= 0 ? '+' : '';
                const date = data.periodStart || 'Current Period';
                
                listContainer.innerHTML += `
                    <div style="border-bottom: 1px solid #eee; padding: 12px 0;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                            <span style="font-weight: bold; color: #555;"><i class="fas fa-calendar-day" style="color:#1565c0; margin-right:4px;"></i> ${date}</span>
                            <span style="font-weight: 900; color: ${color}; font-size: 15px;">${sign}₱${Math.abs(amt).toFixed(2)}</span>
                        </div>
                        <div style="font-size: 13px; color: #666; font-style: italic;">"${remarks}"</div>
                    </div>
                `;
            }
            
            if (!hasHistory) {
                listContainer.innerHTML = `
                    <div style="text-align:center; padding: 30px; color:#888;">
                        <i class="fas fa-info-circle fa-2x" style="margin-bottom:10px; color:#cbd5e1;"></i><br>
                        No manual adjustments have been recorded for this period.
                    </div>`;
            }
        })
        .catch(err => {
            console.error("Error fetching adjustment history:", err);
            listContainer.innerHTML = '<div style="color:#d32f2f; text-align:center; padding: 20px;"><i class="fas fa-exclamation-triangle"></i> Failed to load adjustment data.</div>';
        });
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

window.onclick = function(event) {
    const modals = [
        document.getElementById('processModal'),
        document.getElementById('infoModal'),
        document.getElementById('ratesModal'),
        document.getElementById('scheduleSummaryModal'),
        document.getElementById('manualAdjModal'),
        document.getElementById('subHistoryModal'),
        document.getElementById('mathBreakdownModal'),
        document.getElementById('dedHrsModal'),
        document.getElementById('adjHistoryModal'),
        document.getElementById('makeUpModal')
    ];
    
    modals.forEach(modal => {
        if (event.target === modal) {
            modal.style.display = "none";
        }
    });
}

// ==========================================================
// MISSING MODAL FUNCTIONS & HELPERS FOR TEACHING PAYROLL
// ==========================================================

function formatCurrency(amount) {
    const num = parseFloat(amount);
    if (isNaN(num)) return '₱0.00';
    return '₱' + num.toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function safeFloat(val) {
    if (!val || val === "null" || val === "undefined") return 0;
    const parsed = parseFloat(val);
    return isNaN(parsed) ? 0 : parsed;
}

// Opens the Government Deductions Modal
function viewDeductionBreakdown(element) { 
    const empNameEl = document.getElementById('deductionEmpName');
    if (empNameEl) empNameEl.innerText = element.getAttribute('data-emp-name') || '----'; 
    
    const sss = safeFloat(element.getAttribute('data-sss'));
    const phil = safeFloat(element.getAttribute('data-phil'));
    const pagibig = safeFloat(element.getAttribute('data-pagibig'));
    const tax = safeFloat(element.getAttribute('data-tax'));
    const sLoan = safeFloat(element.getAttribute('data-sss-loan'));
    const hLoan = safeFloat(element.getAttribute('data-hdmf-loan'));
    const oLoan = safeFloat(element.getAttribute('data-oth-loan'));

    if (document.getElementById('modSss')) document.getElementById('modSss').innerText = formatCurrency(sss); 
    if (document.getElementById('modPhil')) document.getElementById('modPhil').innerText = formatCurrency(phil); 
    if (document.getElementById('modPagibig')) document.getElementById('modPagibig').innerText = formatCurrency(pagibig); 
    if (document.getElementById('modTax')) document.getElementById('modTax').innerText = formatCurrency(tax); 
    if (document.getElementById('modSssLoan')) document.getElementById('modSssLoan').innerText = formatCurrency(sLoan); 
    if (document.getElementById('modHdmfLoan')) document.getElementById('modHdmfLoan').innerText = formatCurrency(hLoan); 
    if (document.getElementById('modOthLoan')) document.getElementById('modOthLoan').innerText = formatCurrency(oLoan); 
    
    const total = sss + phil + pagibig + tax + sLoan + hLoan + oLoan; 
    if (document.getElementById('modTotalDed')) document.getElementById('modTotalDed').innerText = formatCurrency(total); 
    
    const modal = document.getElementById('deductionModal');
    if (modal) modal.style.display = 'block'; 
}

// Opens the Total Earnings Breakdown Modal
function viewEarningsBreakdown(element) { 
    const empNameEl = document.getElementById('earnEmpName');
    if (empNameEl) empNameEl.innerText = element.getAttribute('data-emp') || '----'; 
    
    const basic = safeFloat(element.getAttribute('data-basic'));
    const teach = safeFloat(element.getAttribute('data-teach'));
    const ot = safeFloat(element.getAttribute('data-ot'));
    const oth = safeFloat(element.getAttribute('data-oth'));

    if (document.getElementById('modBasicAmt')) document.getElementById('modBasicAmt').innerText = formatCurrency(basic); 
    if (document.getElementById('modTeachAmt')) document.getElementById('modTeachAmt').innerText = formatCurrency(teach); 
    if (document.getElementById('modOtAmt')) document.getElementById('modOtAmt').innerText = formatCurrency(ot); 
    if (document.getElementById('modEarnOthAmt')) document.getElementById('modEarnOthAmt').innerText = formatCurrency(oth); 
    
    const tbody = document.querySelector('#earningsModal .mod-table tbody');
    if (tbody) {
        let adjRow = document.getElementById('modAdjRow');
        if (!adjRow) {
            adjRow = document.createElement('tr');
            adjRow.id = 'modAdjRow';
            adjRow.innerHTML = `<td class="label-cell">Adjustments (+)</td><td class="val-cell text-blue" id="modAdjAmt">₱0.00</td>`;
            tbody.appendChild(adjRow);
        }
        
        const adjCell = element.closest('tr').querySelector('.col-adj');
        let adj = 0;
        if (adjCell) {
            adj = parseFloat(adjCell.innerText.replace(/[^0-9.-]+/g, "")) || 0;
        } else {
            adj = safeFloat(element.getAttribute('data-adj'));
        }
        document.getElementById('modAdjAmt').innerText = formatCurrency(adj);

        const grandTotal = basic + teach + ot + oth + adj;
        if (document.getElementById('modTotEarnAmt')) document.getElementById('modTotEarnAmt').innerText = formatCurrency(grandTotal); 
    }
    
    const modal = document.getElementById('earningsModal');
    if (modal) modal.style.display = 'block'; 
}

// Ensure close functions exist for these new modals
function closeDeductionModal() { 
    if(document.getElementById('deductionModal')) document.getElementById('deductionModal').style.display = 'none'; 
}
function closeEarningsModal() { 
    if(document.getElementById('earningsModal')) document.getElementById('earningsModal').style.display = 'none'; 
}