let currentViewedEmployeeId = null;

function formatHrMin(totalMins) {
    if (!totalMins || isNaN(totalMins) || totalMins <= 0) return "-";
    let h = Math.floor(totalMins / 60);
    let m = Math.floor(totalMins % 60);
    let parts = [];
    if (h > 0) parts.push(`${h} hr${h > 1 ? 's' : ''}`);
    if (m > 0) parts.push(`${m} min${m > 1 ? 's' : ''}`);
    return parts.join(' ');
}

function formatTimeShort(timeStr) {
    if (!timeStr) return '--:--';
    let hr, min;
    
    if (Array.isArray(timeStr) && timeStr.length >= 2) {
        hr = parseInt(timeStr[0]); 
        min = parseInt(timeStr[1]);
    } else if (typeof timeStr === 'string') {
        let parts = timeStr.split(':');
        if(parts.length >= 2) {
            hr = parseInt(parts[0]); 
            min = parseInt(parts[1]);
        } else {
            return timeStr;
        }
    } else {
        return '--:--';
    }

    let ampm = hr >= 12 ? 'PM' : 'AM';
    let hr12 = hr % 12 || 12;
    return `${String(hr12).padStart(2, '0')}:${String(min).padStart(2, '0')} ${ampm}`;
}

document.addEventListener('DOMContentLoaded', function() {
    const startFilter = document.getElementById('startDateFilter');
    const endFilter = document.getElementById('endDateFilter');

    if(startFilter && !startFilter.value) {
        const now = new Date();
        startFilter.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
    }
    if(endFilter && !endFilter.value) {
        const now = new Date();
        endFilter.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-15`;
    }

    if (startFilter) { 
        updatePeriodNumber(); 
        startFilter.addEventListener('change', function() {
            updatePeriodNumber();
            switchAttendanceType(document.getElementById("currentAttendanceType").value);
        });
    }
    if (endFilter) { 
        endFilter.addEventListener('change', function() {
            switchAttendanceType(document.getElementById("currentAttendanceType").value);
        });
    }

    const deptFilter = document.getElementById('departmentFilter');
    if (deptFilter) { deptFilter.addEventListener('change', filterTable); }

    const searchEmp = document.getElementById('searchEmployee');
    if (searchEmp) searchEmp.addEventListener('keyup', function(e) { if(e.key === 'Enter') filterTable(); });

    const btnSearch = document.getElementById('btnSearchSubmit');
    if (btnSearch) btnSearch.addEventListener('click', filterTable);

    const btnCompute = document.getElementById('btnComputeAttendance');
    if (btnCompute) btnCompute.addEventListener('click', computeAttendance);

    const btnPayroll = document.getElementById('btnProceedToPayroll');
    if (btnPayroll) btnPayroll.addEventListener('click', proceedToPayroll);

    const btnEmpAtt = document.getElementById('btnEmployeeAttendance');
    if (btnEmpAtt) btnEmpAtt.addEventListener('click', function(e) { handleTabClick(e, 'EMPLOYEE'); });

    const btnTeachAtt = document.getElementById('btnTeachingAttendance');
    if (btnTeachAtt) btnTeachAtt.addEventListener('click', function(e) { handleTabClick(e, 'TEACHING'); });

    const btnCloseModal = document.getElementById('closeModalBtn');
    if (btnCloseModal) btnCloseModal.addEventListener('click', function() { closeModal('dailyAttendanceModal'); });

    window.onclick = function(event) {
        const modal = document.getElementById('dailyAttendanceModal');
        if (event.target == modal) {
            closeModal('dailyAttendanceModal');
        }
    };

    document.body.addEventListener('click', function(e) {
        const btn = e.target.closest('.view-daily-btn');
        if (btn) {
            openDailyAttendanceModal(
                btn.getAttribute('data-empid'), 
                btn.getAttribute('data-empname'),
                btn.getAttribute('data-dept'),
                btn.getAttribute('data-desig'),
                btn.getAttribute('data-status'),
                btn.getAttribute('data-late'), 
                btn.getAttribute('data-under'),
                btn.getAttribute('data-absent'), 
                btn.getAttribute('data-ot')
            );
        }
    });

    if(document.getElementById('attendanceSummaryBody')) {
        const initialType = document.getElementById("currentAttendanceType")?.value || 'EMPLOYEE';
        let targetBtn = document.getElementById('btnEmployeeAttendance');
        if (initialType === 'TEACHING') targetBtn = document.getElementById('btnTeachingAttendance');
        handleTabClick({ target: targetBtn }, initialType);
    }
});

function handleTabClick(event, type) {
    if (event && event.target && event.target.classList) {
        document.querySelectorAll('.btn-tab').forEach(btn => {
            btn.classList.remove('active-tab');
            btn.classList.add('inactive-tab');
        });
        event.target.classList.remove('inactive-tab');
        event.target.classList.add('active-tab');
    }
    const typeInput = document.getElementById("currentAttendanceType");
    if (typeInput) typeInput.value = type;
    switchAttendanceType(type);
}

function updatePeriodNumber() {
    const startDateVal = document.getElementById('startDateFilter').value;
    const periodInput = document.getElementById('periodNoFilter'); 
    if (startDateVal && periodInput) {
        const parts = startDateVal.split('-');
        if(parts.length === 3) {
            const day = parseInt(parts[2], 10);
            periodInput.value = (day <= 15) ? "1" : "2";
        }
    }
}

function switchAttendanceType(type) {
    const startDate = document.getElementById("startDateFilter")?.value || '';
    const endDate = document.getElementById("endDateFilter")?.value || '';
    const mainTable = document.getElementById('attendanceSummaryTable');
    const responsiveContainer = mainTable.closest('.table-responsive'); 
    const tbody = document.getElementById('attendanceSummaryBody');
    const attendanceView = document.getElementById('attendanceViewContainer');

    if (attendanceView) { attendanceView.querySelectorAll('.dept-table-wrapper').forEach(el => el.remove()); }

    responsiveContainer.classList.remove('d-none');
    tbody.innerHTML = '<tr><td colspan="11" class="text-center empty-row-td">Loading attendance data...</td></tr>';

    // ADDED /payroll-system prefix
    fetch(`/payroll-system/api/attendance/summaries?attendanceType=${type}&startDate=${startDate}&endDate=${endDate}`)
        .then(response => response.json())
        .then(data => {
            if(!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="11" class="text-center empty-row-td">No attendance records found.</td></tr>';
                return;
            }
            responsiveContainer.classList.add('d-none');
            const otClass = type === 'TEACHING' ? 'd-none' : '';
            const groupedData = data.reduce((groups, summary) => {
                const dept = summary.department || 'Unassigned';
                if (!groups[dept]) groups[dept] = [];
                groups[dept].push(summary);
                return groups;
            }, {});

            for (const [department, deptSummaries] of Object.entries(groupedData)) {
                const wrapper = document.createElement('div');
                wrapper.className = 'glass-card dept-table-wrapper';
                wrapper.style.marginTop = '20px';
                const deptHeader = document.createElement('h3');
                deptHeader.className = 'dept-header-title'; 
                deptHeader.innerHTML = `<span><i class="fas fa-building"></i> ${department}</span><span class="dept-employee-count">(${deptSummaries.length} Employees)</span>`;
                wrapper.appendChild(deptHeader);

                const newResponsiveDiv = document.createElement('div');
                newResponsiveDiv.className = 'table-responsive';
                const newTable = mainTable.cloneNode(true);
                newTable.id = ''; 
                const thead = newTable.querySelector('thead');
                if (thead) {
                    const otHeader = thead.querySelector('#header-ot');
                    if (otHeader) {
                        if (type === 'TEACHING') otHeader.classList.add('d-none');
                        else otHeader.classList.remove('d-none');
                    }
                }
                const newTBody = newTable.querySelector('tbody');
                newTBody.id = ''; newTBody.innerHTML = ''; 

                deptSummaries.forEach(summary => {
                    const tr = document.createElement('tr');
                    tr.className = 'employee-row';
                    tr.setAttribute('data-department', department);
                    tr.setAttribute('data-search', `${summary.employeeId} ${(summary.name || '').toLowerCase()}`);
                    tr.innerHTML = `
                        <td><button class="btn-gold btn-view-dtr view-daily-btn" data-empid="${summary.employeeId}" data-empname="${summary.name}" data-dept="${summary.department || 'N/A'}" data-desig="${summary.designationName || 'N/A'}" data-status="${summary.employeeStatus || 'N/A'}" data-late="${summary.totalLate}" data-under="${summary.totalUndertime}" data-absent="${summary.totalAbsent}" data-ot="${summary.totalOT}" data-leavepay="${summary.totalLeaveWithPay}" data-leaveunpay="${summary.totalLeaveWithoutPay}" data-hol="${summary.totalHoliday}" data-susp="${summary.totalSuspension}"><i class="fas fa-calendar-alt"></i> View DTR</button></td>
                        <td>${summary.employeeId}</td><td class="text-left">${summary.name}</td><td>${formatHrMin(parseFloat(summary.totalLate))}</td><td>${formatHrMin(parseFloat(summary.totalUndertime))}</td><td>${summary.totalAbsent}</td><td class="${otClass}">${formatHrMin(Math.round(parseFloat(summary.totalOT) * 60))}</td><td>${summary.totalLeaveWithPay}</td><td>${summary.totalLeaveWithoutPay}</td><td>${summary.totalHoliday}</td><td>${summary.totalSuspension}</td>`;
                    newTBody.appendChild(tr);
                });

                const newTfoot = newTable.querySelector('tfoot');
                newTfoot.id = '';
                newTfoot.innerHTML = `<tr class="dept-total-row"><td colspan="3" class="dept-total-label text-right">DEPARTMENT TOTAL:</td><td class="td-dLate">0</td><td class="td-dUnder">0</td><td class="td-dAbsent">0</td><td class="td-dOT ${otClass}">0</td><td class="td-dLeaveP">0</td><td class="td-dLeaveU">0</td><td class="td-dHol">0</td><td class="td-dSusp">0</td></tr>`;
                newResponsiveDiv.appendChild(newTable);
                wrapper.appendChild(newResponsiveDiv);
                responsiveContainer.parentNode.insertBefore(wrapper, responsiveContainer.nextSibling);
            }

            const gtWrapper = document.createElement('div');
            gtWrapper.className = 'glass-card dept-table-wrapper overall-grand-total-container';
            gtWrapper.style.marginTop = '20px';
            gtWrapper.innerHTML = `<div class="overall-grand-total-header"><span><i class="fas fa-calculator"></i> OVERALL GRAND TOTAL</span></div>`;
            const gtDiv = document.createElement('div');
            gtDiv.className = 'table-responsive gt-no-margin';
            const gtTable = mainTable.cloneNode(true);
            gtTable.id = ''; gtTable.querySelector('tbody').remove(); 
            const gtFoot = gtTable.querySelector('tfoot');
            gtFoot.id = 'dynamicGrandTotal';
            gtFoot.innerHTML = `<tr class="grand-total-row"><td colspan="3" class="grand-total-label text-right">OVERALL GRAND TOTAL:</td><td id="gtLate">0</td><td id="gtUnder">0</td><td id="gtAbsent">0</td><td id="gtOT" class="${otClass}">0</td><td id="gtLeaveP">0</td><td id="gtLeaveU">0</td><td id="gtHol">0</td><td id="gtSusp">0</td></tr>`;
            gtDiv.appendChild(gtTable); gtWrapper.appendChild(gtDiv);
            responsiveContainer.parentNode.appendChild(gtWrapper);
            filterTable(); 
        })
        .catch(err => {
            console.error(err);
            tbody.innerHTML = '<tr><td colspan="11" class="text-center text-error">Failed to load attendance data.</td></tr>';
        });
}

function filterTable() {
    const searchInput = document.getElementById('searchEmployee') ? document.getElementById('searchEmployee').value.toLowerCase() : '';
    const deptFilter = document.getElementById('departmentFilter') ? document.getElementById('departmentFilter').value : 'ALL';
    
    document.querySelectorAll('.employee-row').forEach(row => {
        const matchesSearch = (row.getAttribute('data-search') || '').includes(searchInput);
        const matchesDept = (deptFilter === 'ALL' || row.getAttribute('data-department') === deptFilter);
        row.classList.toggle('d-none', !(matchesSearch && matchesDept));
    });
    
    document.querySelectorAll('.dept-table-wrapper:not(.overall-grand-total-container)').forEach(wrapper => {
        const visibleRows = wrapper.querySelectorAll('.employee-row:not(.d-none)');
        wrapper.classList.toggle('d-none', visibleRows.length === 0);
        
        if (visibleRows.length > 0) {
            let dL = 0, dU = 0, dA = 0, dO = 0, dLP = 0, dLU = 0, dH = 0, dS = 0;
            visibleRows.forEach(emp => {
                const b = emp.querySelector('.view-daily-btn');
                if (b) {
                    dL += parseFloat(b.getAttribute('data-late')) || 0;
                    dU += parseFloat(b.getAttribute('data-under')) || 0;
                    dA += parseFloat(b.getAttribute('data-absent')) || 0;
                    dO += Math.round((parseFloat(b.getAttribute('data-ot')) || 0) * 60);
                    dLP += parseFloat(b.getAttribute('data-leavepay')) || 0;
                    dLU += parseFloat(b.getAttribute('data-leaveunpay')) || 0;
                    dH += parseFloat(b.getAttribute('data-hol')) || 0;
                    dS += parseFloat(b.getAttribute('data-susp')) || 0;
                }
            });
            const tf = wrapper.querySelector('tfoot');
            if (tf) {
                if (tf.querySelector('.td-dLate')) tf.querySelector('.td-dLate').innerText = formatHrMin(dL);
                if (tf.querySelector('.td-dUnder')) tf.querySelector('.td-dUnder').innerText = formatHrMin(dU);
                if (tf.querySelector('.td-dAbsent')) tf.querySelector('.td-dAbsent').innerText = dA;
                if (tf.querySelector('.td-dOT')) tf.querySelector('.td-dOT').innerText = formatHrMin(dO);
                if (tf.querySelector('.td-dLeaveP')) tf.querySelector('.td-dLeaveP').innerText = dLP;
                if (tf.querySelector('.td-dLeaveU')) tf.querySelector('.td-dLeaveU').innerText = dLU;
                if (tf.querySelector('.td-dHol')) tf.querySelector('.td-dHol').innerText = dH;
                if (tf.querySelector('.td-dSusp')) tf.querySelector('.td-dSusp').innerText = dS;
            }
        }
    });
    updateGrandTotal();
}

function updateGrandTotal() {
    const visibleWrappers = document.querySelectorAll('.dept-table-wrapper:not(.d-none):not(.overall-grand-total-container)');
    let gL = 0, gU = 0, gA = 0, gO = 0, gLP = 0, gLU = 0, gH = 0, gS = 0;
    
    visibleWrappers.forEach(wrapper => {
        const rows = wrapper.querySelectorAll('.employee-row:not(.d-none)');
        rows.forEach(row => {
            const b = row.querySelector('.view-daily-btn');
            if (b) {
                gL += parseFloat(b.getAttribute('data-late')) || 0;
                gU += parseFloat(b.getAttribute('data-under')) || 0;
                gA += parseFloat(b.getAttribute('data-absent')) || 0;
                gO += Math.round((parseFloat(b.getAttribute('data-ot')) || 0) * 60);
                gLP += parseFloat(b.getAttribute('data-leavepay')) || 0;
                gLU += parseFloat(b.getAttribute('data-leaveunpay')) || 0;
                gH += parseFloat(b.getAttribute('data-hol')) || 0;
                gS += parseFloat(b.getAttribute('data-susp')) || 0;
            }
        });
    });
    
    const df = document.getElementById('dynamicGrandTotal');
    if (df) {
        if(df.querySelector('#gtLate')) df.querySelector('#gtLate').innerText = formatHrMin(gL);
        if(df.querySelector('#gtUnder')) df.querySelector('#gtUnder').innerText = formatHrMin(gU);
        if(df.querySelector('#gtAbsent')) df.querySelector('#gtAbsent').innerText = gA;
        if(df.querySelector('#gtOT')) df.querySelector('#gtOT').innerText = formatHrMin(gO);
        if(df.querySelector('#gtLeaveP')) df.querySelector('#gtLeaveP').innerText = gLP;
        if(df.querySelector('#gtLeaveU')) df.querySelector('#gtLeaveU').innerText = gLU;
        if(df.querySelector('#gtHol')) df.querySelector('#gtHol').innerText = gH;
        if(df.querySelector('#gtSusp')) df.querySelector('#gtSusp').innerText = gS;
    }
}

function computeAttendance() {
    const start = document.getElementById("startDateFilter").value;
    const end = document.getElementById("endDateFilter").value;
    const type = document.getElementById("currentAttendanceType").value || 'EMPLOYEE'; 
    if(!start || !end) return alert("Please select dates.");
    // ADDED /payroll-system prefix
    window.location.href = `/payroll-system/attendance?attendanceType=${type}&startDate=${start}&endDate=${end}`;
}

// ADDED /payroll-system prefix
function proceedToPayroll() { window.location.href = '/payroll-system/payroll'; }

function closeModal(modalId) { 
    document.getElementById(modalId).style.display = 'none'; 
}

function openDailyAttendanceModal(employeeId, name, dept, desig, status, totalLate, totalUndertime, totalAbsent, totalOT) {
    currentViewedEmployeeId = employeeId;
    const startDateVal = document.getElementById("startDateFilter")?.value;
    const endDateVal = document.getElementById("endDateFilter")?.value;
    
    document.getElementById('dailyAttendanceModal').style.display = 'block';
    
    document.getElementById('dtrEmpId').innerText = employeeId;
    document.getElementById('dtrEmpName').innerText = name;
    document.getElementById('dtrEmpDept').innerText = dept;
    document.getElementById('dtrEmpDesig').innerText = desig;
    document.getElementById('dtrEmpStatus').innerText = status;
    
    const tbody = document.getElementById('dailyAttendanceBody');
    const tfoot = document.getElementById('dailyAttendanceTotal');
    tbody.innerHTML = '<tr><td colspan="9" class="text-center">Loading...</td></tr>';

    // ADDED /payroll-system prefix
    fetch(`/payroll-system/api/attendance/daily/${employeeId}?startDate=${startDateVal}&endDate=${endDateVal}`)
        .then(response => response.json())
        .then(data => {
            const attendance = data.attendance || (Array.isArray(data) ? data : []); 
            const scheduledDays = data.scheduledDays || []; 
            const holidays = data.holidays || []; 
            
            // FIX: Removed the stray 'c' that was crashing the script!
            // Smarter check: If the backend successfully sent scheduled days, OR they are labeled Faculty/School
            const isTeaching = (scheduledDays && scheduledDays.length > 0) || 
                               (dept && dept.toUpperCase().includes("SCHOOL")) || 
                               (desig && desig.toUpperCase().includes("FACULTY"));
            
            tbody.innerHTML = ''; 
            
            const normalizeDate = (dString) => {
                if (!dString) return null;
                if (Array.isArray(dString)) {
                    return `${dString[0]}-${String(dString[1]).padStart(2, '0')}-${String(dString[2]).padStart(2, '0')}`;
                }
                return dString.split('T')[0];
            };

            let currentDate = new Date(startDateVal);
            let endingDate = new Date(endDateVal);

            while(currentDate <= endingDate) {
                let dayName = currentDate.toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase();
                
                let y = currentDate.getFullYear();
                let m = String(currentDate.getMonth() + 1).padStart(2, '0');
                let d = String(currentDate.getDate()).padStart(2, '0');
                
                let targetDateStr = `${y}-${m}-${d}`;
                let displayDateStr = `${m}/${d}/${y}`;

                let record = attendance.find(r => normalizeDate(r.date) === targetDateStr);
                let holidayObj = holidays.find(h => normalizeDate(h.date) === targetDateStr);

                let isRestDay = (currentDate.getDay() === 0); 
                if (isTeaching && !isRestDay) {
                    isRestDay = !scheduledDays.includes(dayName);
                }

                const tr = document.createElement('tr');
                if (record) {
                    let badge = record.remark || "-";
                    let badgeClass = "";
                    let holStyle = "";

                    if (badge === "Holiday" || holidayObj) {
                        badge = "HOLIDAY";
                        badgeClass = "badge-weekend";
                        holStyle = 'style="background-color:#0284c7; color:white; font-weight:bold; padding:3px 8px; border-radius:12px;"';
                    } else if (badge.includes("Suspension")) {
                        badgeClass = "badge-weekend"; 
                        holStyle = 'style="background-color:#e65100; color:white; font-weight:bold; padding:3px 8px; border-radius:12px;"';
                    } else if (badge === "Absent") {
                        badge = "ABSENT";
                        badgeClass = "badge-absent";
                    } else if ((!record.totalHours || record.totalHours <= 0) && !record.timeOut) {
                        badge = "ABSENT";
                        badgeClass = "badge-absent";
                    } else if (record.minutesLate > 0 || record.undertimeHours > 0) {
                        badge = "Penalty";
                        badgeClass = "badge-penalty";
                    } else {
                        badge = "Present";
                        badgeClass = "badge-success";
                    }

                    tr.innerHTML = `<td class="text-center">${displayDateStr}</td><td class="text-center">${formatTimeShort(record.timeIn)}</td><td class="text-center">--:--</td><td class="text-center">--:--</td><td class="text-center">${formatTimeShort(record.timeOut)}</td><td class="text-center">${formatHrMin(record.minutesLate)}</td><td class="text-center">${formatHrMin(record.undertimeHours)}</td><td class="text-center">${formatHrMin(record.overtimeHours)}</td><td class="text-center"><span class="${badgeClass}" ${holStyle}>${badge}</span></td>`;
                } else {
                    let displayBadge = "ABSENT";
                    let badgeClass = "badge-absent";
                    let holStyle = "";

                    if (holidayObj) {
                        displayBadge = "HOLIDAY";
                        badgeClass = "badge-weekend"; 
                        holStyle = 'style="background-color:#0284c7; color:white; font-weight:bold; padding:3px 8px; border-radius:12px;"';
                    } else if (isRestDay) {
                        displayBadge = "Rest Day";
                        badgeClass = "badge-weekend";
                    }

                    tr.innerHTML = `
                        <td class="text-center">${displayDateStr}</td>
                        <td colspan="4" class="text-center" style="color:#aaa;">-- No Logs --</td>
                        <td class="text-center">-</td><td class="text-center">-</td><td class="text-center">-</td>
                        <td class="text-center"><span class="${badgeClass}" ${holStyle}>${displayBadge}</span></td>`;
                }
                tbody.appendChild(tr);
                currentDate.setDate(currentDate.getDate() + 1);
            }
            tfoot.innerHTML = `<tr><td colspan="5" class="text-right">TOTALS:</td><td class="text-center">${formatHrMin(totalLate)}</td><td class="text-center">${formatHrMin(totalUndertime)}</td><td class="text-center">${totalOT} hrs</td><td class="text-center" style="font-weight:bold; color:#b71c1c;">Absences: ${totalAbsent}</td></tr>`;
        })
        .catch(err => {
            console.error("DTR Fetch Error:", err);
            tbody.innerHTML = '<tr><td colspan="9" class="text-center text-error">Failed to load Daily Time Record.</td></tr>';
        });
}