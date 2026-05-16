async function addHoliday() {
    const dateVal = document.getElementById('holidayDate').value;
    const nameVal = document.getElementById('holidayName').value;
    const typeVal = document.getElementById('holidayType').value;
    
    if (!dateVal || !nameVal || !typeVal) { 
        alert("Please fill out all fields: Date, Name, and Type."); 
        return; 
    }
    
    const btn = document.getElementById('btnAddHoliday');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
    btn.disabled = true;

    const params = new URLSearchParams({ 
        date: dateVal, 
        name: nameVal, 
        type: typeVal 
    });

    try {
        const response = await fetch('/payroll-system/api/holidays/add', { 
            method: 'POST', 
            body: params,
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        });
        
        if (!response.ok) throw new Error("Failed to save holiday.");
        alert("Holiday Successfully Added!");
        location.reload();
    } catch (e) {
        alert(e.message);
        btn.innerHTML = '<i class="fas fa-save"></i> Save Holiday';
        btn.disabled = false;
    }
}

async function deleteHoliday(btn) {
    if (!confirm("Are you sure you want to delete this holiday?")) return;
    
    const id = btn.getAttribute('data-id');
    btn.innerHTML = '...';
    btn.disabled = true;

    const params = new URLSearchParams({ id: id });

    try {
        const response = await fetch('/payroll-system/api/holidays/delete', { 
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

// ========================================================
// AUTO-IMPORT PHILIPPINE HOLIDAYS FROM PUBLIC API
// ========================================================
async function autoImportPHHolidays() {
    const year = new Date().getFullYear();
    if (!confirm(`Are you sure you want to automatically import all Philippine National Holidays for ${year} into your database?`)) return;

    const btn = document.getElementById('btnAutoImport');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Fetching API...';
    btn.disabled = true;

    try {
        // Fetch official holidays from a free public API
        const response = await fetch(`https://date.nager.at/api/v3/PublicHolidays/${year}/PH`);
        if (!response.ok) throw new Error("Could not fetch holidays from public API.");
        
        const publicHolidays = await response.json();
        let addedCount = 0;

        for (const hol of publicHolidays) {
            // Check if the date already exists in the calendar to prevent duplicates
            const exists = window.holidayEvents.some(existing => existing.start === hol.date);
            
            if (!exists) {
                // FIX 1: Force the English translation instead of Tagalog
                const englishName = hol.name; 
                
                // FIX 2: Smart Filter to detect Special Non-Working Holidays
                let holidayType = 'REGULAR';
                const nameLower = englishName.toLowerCase();
                
                if (nameLower.includes('ninoy aquino') || 
                    nameLower.includes('all saints') || 
                    nameLower.includes('all souls') || 
                    nameLower.includes('black saturday') || 
                    nameLower.includes('immaculate conception') || 
                    nameLower.includes('chinese new year') || 
                    nameLower.includes('lunar new year') || 
                    nameLower.includes('edsa') ||
                    nameLower.includes('special')) {
                    
                    holidayType = 'SPECIAL_NON_WORKING';
                }

                const params = new URLSearchParams({
                    date: hol.date,
                    name: englishName, 
                    type: holidayType 
                });
                
                await fetch('/payroll-system/api/holidays/add', {
                    method: 'POST',
                    body: params,
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
                });
                addedCount++;
            }
        }
        
        if (addedCount > 0) {
            alert(`Success! Imported ${addedCount} new Philippine holidays into your database.`);
        } else {
            alert("Your database is already up to date with this year's PH holidays!");
        }
        
        location.reload();
    } catch (error) {
        alert("Error importing holidays: " + error.message);
        btn.innerHTML = '<i class="fas fa-download"></i> Auto-Import PH Holidays';
        btn.disabled = false;
    }
}

// ========================================================
// VISUAL CALENDAR LOGIC
// ========================================================
function openVisualCalendar() {
    document.getElementById('visualCalendarModal').style.display = 'flex';
    
    const calendarEl = document.getElementById('calendarUI');
    const calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        events: window.holidayEvents || [],
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

// ========================================================
// BULK DELETE CHECKBOX LOGIC
// ========================================================
document.addEventListener("DOMContentLoaded", () => {
    const selectAllCheckbox = document.getElementById("selectAllHolidays");
    const holidayCheckboxes = document.querySelectorAll(".holiday-checkbox");
    const deleteSelectedBtn = document.getElementById("deleteSelectedBtn");

    // Show/hide Delete Button if at least one checkbox is checked
    function toggleDeleteButton() {
        if (!deleteSelectedBtn) return;
        const anyChecked = Array.from(holidayCheckboxes).some(cb => cb.checked);
        deleteSelectedBtn.style.display = anyChecked ? "inline-block" : "none";
    }

    // Select All Checkbox Logic
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener("change", function() {
            holidayCheckboxes.forEach(cb => cb.checked = this.checked);
            toggleDeleteButton();
        });
    }

    // Individual Checkbox Logic
    holidayCheckboxes.forEach(cb => {
        cb.addEventListener("change", function() {
            const allChecked = Array.from(holidayCheckboxes).every(c => c.checked);
            if (selectAllCheckbox) selectAllCheckbox.checked = allChecked;
            toggleDeleteButton();
        });
    });

    // Send the Bulk Delete Request to the Backend
    if (deleteSelectedBtn) {
        deleteSelectedBtn.addEventListener("click", async () => {
            // Gather all the IDs from the checked boxes
            const selectedIds = Array.from(holidayCheckboxes)
                .filter(cb => cb.checked)
                .map(cb => parseInt(cb.value));

            if (selectedIds.length === 0) return;

            if (confirm(`Are you sure you want to permanently delete ${selectedIds.length} selected holiday(s)?`)) {
                
                const originalText = deleteSelectedBtn.innerHTML;
                deleteSelectedBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Deleting...';
                deleteSelectedBtn.disabled = true;

                try {
                    // Update this path to match your Tomcat context path
                    const response = await fetch('/payroll-system/api/holidays/delete-multiple', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(selectedIds)
                    });

                    const data = await response.json();
                    
                    if (data.success || response.ok) {
                        alert("Holidays successfully deleted!");
                        window.location.reload(); 
                    } else {
                        alert("Error: " + (data.message || "Failed to delete holidays."));
                    }
                } catch (error) {
                    console.error("Error deleting holidays:", error);
                    alert("System error while deleting. Check console.");
                } finally {
                    deleteSelectedBtn.innerHTML = originalText;
                    deleteSelectedBtn.disabled = false;
                }
            }
        });
    }
});