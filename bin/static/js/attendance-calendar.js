document.addEventListener('DOMContentLoaded', () => {
    // Calendar Grid and Display Elements
    const calendarGrid = document.getElementById('calendarGrid');
    const attendanceMessage = document.getElementById('attendanceMessage');
    
    // Auto-load personal attendance on page load
    async function loadMyAttendance() {
        try {
            // Uses session-based endpoint to identify the logged-in user
            const response = await fetch('/api/attendance/my-calendar');
            const data = await response.json();
            
            if (data.success) {
                renderCalendarGrid(data.calendar);
            } else {
                showToast(data.message, 'error');
            }
        } catch (error) {
            console.error('Error loading attendance data:', error);
        }
    }

    // Process Time-In / Time-Out for the authenticated user
    window.handleAutoPunch = async (actionType) => {
        try {
            const response = await fetch(`/api/attendance/${actionType}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });
            const data = await response.json();
            
            if (data.success) {
                showToast(data.message, 'success');
                loadMyAttendance(); // Refresh grid after punch
            } else {
                showToast(data.message, 'error');
            }
        } catch (error) {
            showToast('System connection error', 'error');
        }
    };

    function renderCalendarGrid(attendanceData) {
        calendarGrid.innerHTML = ''; // Clear previous days
        
        // Populate days based on the red/black theme requirements
        attendanceData.forEach(day => {
            const dayDiv = document.createElement('div');
            dayDiv.className = 'calendar-day';
            
            const dayNum = document.createElement('div');
            dayNum.className = 'day-number';
            dayNum.textContent = day.dayOfMonth;
            dayDiv.appendChild(dayNum);

            // Add status-specific styling (In/Out/Absent)
            if (day.status) {
                const statusSpan = document.createElement('div');
                statusSpan.className = `status-pill ${day.status.toLowerCase()}`;
                statusSpan.textContent = day.status;
                dayDiv.appendChild(statusSpan);
            }
            sql
            calendarGrid.appendChild(dayDiv);
        });
    }

    function showToast(message, type) {
        if (attendanceMessage) {
            attendanceMessage.textContent = message;
            attendanceMessage.className = `message-toast ${type}`;
            attendanceMessage.style.display = 'block';
            setTimeout(() => { attendanceMessage.style.display = 'none'; }, 4000);
        }
    }

    // Initial load
    loadMyAttendance();
});