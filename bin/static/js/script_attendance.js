// Attendance Management JavaScript

function formatTime(timeString) {
    if (!timeString) return '-';
    const time = new Date('2000-01-01T' + timeString);
    return time.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });
}

function showStatus(message, type = 'info') {
    const statusDiv = document.getElementById('attendanceStatus');
    if (statusDiv) {
        statusDiv.textContent = message;
        statusDiv.className = `status-message ${type}`;
        statusDiv.style.display = 'block';
        setTimeout(() => {
            statusDiv.style.display = 'none';
        }, 5000);
    }
}

function formatTimeShort(timeStr) {
    if (!timeStr) return '-';
    const time = new Date('2000-01-01T' + timeStr);
    return time.toLocaleTimeString('en-US', { 
        hour: 'numeric', 
        minute: '2-digit',
        hour12: true 
    });
}

function formatDateForAPI(date) {
    return date.getFullYear() + '-' + 
           String(date.getMonth() + 1).padStart(2, '0') + '-' + 
           String(date.getDate()).padStart(2, '0');
}

// ==========================================
// QUICK FILTERS
// ==========================================

function setFilterToday() {
    const todayStr = formatDateForAPI(new Date());
    document.getElementById('startDate').value = todayStr;
    document.getElementById('endDate').value = todayStr;
    document.getElementById('filterEmployeeQuery').value = '';
    loadAttendanceHistory();
}

function setFilterThisMonth() {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    
    document.getElementById('startDate').value = formatDateForAPI(firstDay);
    document.getElementById('endDate').value = formatDateForAPI(lastDay);
    document.getElementById('filterEmployeeQuery').value = '';
    loadAttendanceHistory();
}

function setFilterAllTime() {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), 0, 1); // Jan 1st of current year
    
    document.getElementById('startDate').value = formatDateForAPI(firstDay);
    document.getElementById('endDate').value = formatDateForAPI(today);
    document.getElementById('filterEmployeeQuery').value = '';
    loadAttendanceHistory();
}


// ==========================================
// MASTERLIST FUNCTIONS
// ==========================================

async function loadTodayAttendance() {
    const tbody = document.getElementById('todayAttendanceTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding: 15px;">Loading today\'s attendance...</td></tr>';

    try {
        const response = await fetch(`/api/attendance/today`, { credentials: 'include' });
        const data = await response.json();

        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding: 15px;">No attendance records for today</td></tr>';
            return;
        }

        tbody.innerHTML = data.map(att => {
            const timeIn = formatTimeShort(att.actualTimeIn);
            const timeOut = formatTimeShort(att.actualTimeOut);
            const statusClass = (att.status || 'Present').toLowerCase().replace(' ', '-');
            return `
                <tr style="border-bottom: 1px solid #eee;">
                    <td style="padding: 10px;"><b>${att.employeeId}</b></td>
                    <td style="padding: 10px;">${timeIn}</td>
                    <td style="padding: 10px;">${timeOut}</td>
                    <td style="padding: 10px;"><span class="status-badge ${statusClass}">${att.status || 'Present'}</span></td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('Error loading today attendance:', error);
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:red;">Error loading data</td></tr>';
    }
}

async function loadAttendanceHistory() {
    const filterQueryInput = document.getElementById('filterEmployeeQuery');
    const query = filterQueryInput ? filterQueryInput.value.trim() : '';
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;

    if (!startDate || !endDate) {
        showStatus('Please select both start and end dates', 'error');
        return;
    }

    const tbody = document.getElementById('attendanceTableBody');
    if (tbody) tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding: 20px;">Searching...</td></tr>';

    try {
        let url = `/api/attendance/range?startDate=${startDate}&endDate=${endDate}`;
        
        if (query) {
            if (!isNaN(query)) {
                url = `/api/attendance/employee/${query}/range?startDate=${startDate}&endDate=${endDate}`;
            } else {
                url = `/api/attendance/employee/name/${encodeURIComponent(query)}/range?startDate=${startDate}&endDate=${endDate}`;
            }
        }

        const response = await fetch(url, { credentials: 'include' });
        if (!response.ok) throw new Error('Failed to load attendance history');

        const attendances = await response.json();
        displayAttendanceTable(attendances);
    } catch (error) {
        console.error('Error loading attendance history:', error);
        if (tbody) tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; color:red;">Error loading data</td></tr>';
    }
}

function displayAttendanceTable(attendances) {
    const tbody = document.getElementById('attendanceTableBody');
    if (!tbody) return;
    
    if (attendances.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="no-data" style="text-align:center; padding:20px;">No attendance records found for the selected period</td></tr>';
        return;
    }

    tbody.innerHTML = attendances.map(attendance => {
        const date = new Date(attendance.attendanceDate).toLocaleDateString();
        const timeIn = formatTimeShort(attendance.actualTimeIn);
        const timeOut = formatTimeShort(attendance.actualTimeOut);
        
        const minutesLate = attendance.minutesLate || 0;
        const minutesEarlyOut = attendance.minutesEarlyOut || 0; // Undertime
        const overtimeHours = attendance.overtimeHours || 0;
        
        const status = attendance.status || 'Present';
        const statusClass = status.toLowerCase().replace(' ', '-');

        // Style the values to make them obvious
        const lateDisplay = minutesLate > 0 ? `<span class="text-danger">${minutesLate}</span>` : '-';
        const undertimeDisplay = minutesEarlyOut > 0 ? `<span class="text-danger">${minutesEarlyOut}</span>` : '-';
        const otDisplay = overtimeHours > 0 ? `<span class="text-success">+${overtimeHours.toFixed(2)}</span>` : '-';

        return `
            <tr style="border-bottom: 1px solid #e2e8f0;">
                <td style="padding: 12px;"><b>${attendance.employeeId}</b></td>
                <td style="padding: 12px;">${date}</td>
                <td style="padding: 12px;">${timeIn}</td>
                <td style="padding: 12px;">${timeOut}</td>
                <td style="padding: 12px;">${lateDisplay}</td>
                <td style="padding: 12px;">${undertimeDisplay}</td>
                <td style="padding: 12px;">${otDisplay}</td>
                <td style="padding: 12px;"><span class="status-badge ${statusClass}">${status}</span></td>
            </tr>
        `;
    }).join('');
}


// ==========================================
// CALENDAR FUNCTIONS
// ==========================================

let currentCalendarData = [];

async function loadAttendanceCalendar() {
    const query = document.getElementById('calendarEmployeeQuery').value.trim();
    const selectedMonth = parseInt(document.getElementById('calendarMonth').value);
    const selectedYear = parseInt(document.getElementById('calendarYear').value);
    
    if (!query) {
        showStatus('Please enter an Employee ID or Name for calendar', 'error');
        return;
    }
    
    const startDate = new Date(selectedYear, selectedMonth, 1);
    const endDate = new Date(selectedYear, selectedMonth + 1, 0);
    
    const startDateStr = formatDateForAPI(startDate);
    const endDateStr = formatDateForAPI(endDate);
    
    const isId = !isNaN(query);
    const baseUrl = isId ? `/api/attendance/employee/${query}` : `/api/attendance/employee/name/${encodeURIComponent(query)}`;
    
    try {
        const calendarResponse = await fetch(`${baseUrl}/calendar?startDate=${startDateStr}&endDate=${endDateStr}`, { credentials: 'include' });
        const calendarData = await calendarResponse.json();
        
        const statsResponse = await fetch(`${baseUrl}/stats?startDate=${startDateStr}&endDate=${endDateStr}`, { credentials: 'include' });
        const statsData = await statsResponse.json();
        
        if (calendarData.success) {
            currentCalendarData = calendarData.calendar;
            renderAttendanceCalendar(selectedYear, selectedMonth);
        } else {
             document.getElementById('attendanceCalendarGrid').innerHTML = 
                `<div style="grid-column: 1 / -1; text-align: center; padding: 20px; color: red;">${calendarData.message || 'Data not found'}</div>`;
        }
        
        if (statsData.success) {
            updateCalendarStatistics(statsData.stats);
        } else {
            updateCalendarStatistics({ totalDays: 0, presentDays: 0, lateDays: 0, absentDays: 0, totalHours: 0, attendancePercentage: 0 });
        }
    } catch (error) {
        console.error('Error loading attendance calendar:', error);
        showStatus('Error loading attendance calendar', 'error');
    }
}

function renderAttendanceCalendar(year, month) {
    const grid = document.getElementById('attendanceCalendarGrid');
    if (!grid) return;
    
    grid.innerHTML = '';
    
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const today = new Date();
    
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());
    
    const endDate = new Date(lastDay);
    const remainingDays = 6 - lastDay.getDay();
    endDate.setDate(endDate.getDate() + remainingDays);
    
    const currentDate = new Date(startDate);
    while (currentDate <= endDate) {
        const dayElement = createCalendarDayElement(currentDate, month, today);
        grid.appendChild(dayElement);
        currentDate.setDate(currentDate.getDate() + 1);
    }
}

function createCalendarDayElement(date, currentMonth, today) {
    const dayDiv = document.createElement('div');
    dayDiv.className = 'calendar-day-attendance';
    
    const isCurrentMonth = date.getMonth() === currentMonth;
    const isToday = date.toDateString() === today.toDateString();
    
    if (!isCurrentMonth) {
        dayDiv.classList.add('other-month');
    }
    
    if (isToday) {
        dayDiv.classList.add('today');
    }
    
    const dayNumber = document.createElement('div');
    dayNumber.className = 'day-number';
    dayNumber.textContent = date.getDate();
    dayDiv.appendChild(dayNumber);
    
    const dateStr = formatDateForAPI(date);
    const attendanceRecord = currentCalendarData.find(record => record.date === dateStr);
    
    if (attendanceRecord && isCurrentMonth) {
        const statusDiv = document.createElement('div');
        const status = attendanceRecord.status.toLowerCase();
        statusDiv.className = `day-status ${status.replace(' ', '-')}`;
        dayDiv.appendChild(statusDiv);
        
        const infoDiv = document.createElement('div');
        infoDiv.className = 'day-info';
        infoDiv.textContent = attendanceRecord.status;
        dayDiv.appendChild(infoDiv);
        
        let tooltipText = `${attendanceRecord.status}`;
        if (attendanceRecord.workHours > 0) tooltipText += ` - ${attendanceRecord.workHours.toFixed(1)} hours`;
        if (attendanceRecord.remarks) tooltipText += ` - ${attendanceRecord.remarks}`;
        dayDiv.title = tooltipText;
    }
    
    return dayDiv;
}

function updateCalendarStatistics(stats) {
    document.getElementById('calTotalDays').textContent = stats.totalDays;
    document.getElementById('calPresentDays').textContent = stats.presentDays;
    document.getElementById('calLateDays').textContent = stats.lateDays;
    document.getElementById('calAbsentDays').textContent = stats.absentDays;
    document.getElementById('calTotalHours').textContent = stats.totalHours.toFixed(1);
    document.getElementById('calAttendancePercent').textContent = stats.attendancePercentage.toFixed(1) + '%';
}

function initializeCalendarYearSelect() {
    const yearSelect = document.getElementById('calendarYear');
    if (!yearSelect) return;
    
    const currentYear = new Date().getFullYear();
    yearSelect.innerHTML = '';
    
    for (let year = currentYear - 2; year <= currentYear + 1; year++) {
        const option = document.createElement('option');
        option.value = year;
        option.textContent = year;
        if (year === currentYear) {
            option.selected = true;
        }
        yearSelect.appendChild(option);
    }
}

function initializeCalendarMonthSelect() {
    const monthSelect = document.getElementById('calendarMonth');
    if (monthSelect) {
        const currentMonth = new Date().getMonth();
        monthSelect.value = currentMonth;
    }
}

// ==========================================
// INITIALIZATION ON PAGE LOAD
// ==========================================

document.addEventListener('DOMContentLoaded', () => {

    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    if (startDateInput && endDateInput) {
        startDateInput.value = firstDay.toISOString().split('T')[0];
        endDateInput.value = lastDay.toISOString().split('T')[0];
    }
    
    initializeCalendarYearSelect();
    initializeCalendarMonthSelect();
    
    const calendarEmployeeQueryInput = document.getElementById('calendarEmployeeQuery');
    if (calendarEmployeeQueryInput) {
        calendarEmployeeQueryInput.addEventListener('blur', () => {
            if (calendarEmployeeQueryInput.value.trim()) {
                loadAttendanceCalendar();
            }
        });
    }

    loadTodayAttendance(); 
    loadAttendanceHistory(); 
});