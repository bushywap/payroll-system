async function addSuspension() {
    const dateVal = document.getElementById('suspDate').value;
    const reasonVal = document.getElementById('suspReason').value;
    const timeVal = document.getElementById('suspTime').value;
    
    if (!dateVal || !reasonVal) { 
        alert("Please provide both a Date and a Reason."); 
        return; 
    }
    
    const btn = document.getElementById('btnAddSusp');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
    btn.disabled = true;

    const params = new URLSearchParams({ date: dateVal, reason: reasonVal });
    if (timeVal) params.append('startTime', timeVal);

    try {
        const response = await fetch('/payroll-system/api/suspensions/add', { 
            method: 'POST', 
            body: params,
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        });
        
        if (!response.ok) throw new Error("Failed to save suspension.");
        alert("Suspension Successfully Recorded!");
        location.reload();
    } catch (e) {
        alert(e.message);
        btn.innerHTML = '<i class="fas fa-save"></i> Save Suspension';
        btn.disabled = false;
    }
}

async function deleteSuspension(btn) {
    if (!confirm("Are you sure you want to delete this suspension?")) return;
    
    const id = btn.getAttribute('data-id');
    btn.innerHTML = '...';
    btn.disabled = true;

    const params = new URLSearchParams({ id: id });

    try {
        const response = await fetch('/payroll-system/api/suspensions/delete', { 
            method: 'POST', 
            body: params 
        });
        if (!response.ok) throw new Error("Failed to delete.");
        location.reload();
    } catch (e) {
        alert(e.message);
        btn.innerHTML = '<i class="fas fa-trash-alt"></i> Remove';
        btn.disabled = false;
    }
}

// Visual Calendar Logic
function openVisualCalendar() {
    document.getElementById('visualCalendarModal').style.display = 'flex';
    
    const calendarEl = document.getElementById('calendarUI');
    const calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        events: window.suspensionEvents || [],
        height: 600,
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,listMonth'
        }
    });
    calendar.render();
}

function closeVisualCalendar() {
    document.getElementById('visualCalendarModal').style.display = 'none';
}

window.onclick = function(event) {
    const modal = document.getElementById('visualCalendarModal');
    if (event.target === modal) closeVisualCalendar();
}