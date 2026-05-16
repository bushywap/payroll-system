// HR Leave Approval Management JavaScript

let leaveTypes = [];
let allLeaves = []; // Cache for faster filtering

// Initialize Page
document.addEventListener('DOMContentLoaded', () => {
    loadLeaveTypes();
    loadLeaves();
});

// Load available leave types for the filter dropdown
async function loadLeaveTypes() {
    try {
        const response = await fetch('/api/leave/types', { credentials: 'include' });
        leaveTypes = await response.json();
        
        const filterLeaveTypeSelect = document.getElementById('filterLeaveType');
        leaveTypes.forEach(type => {
            const option = document.createElement('option');
            option.value = type;
            option.textContent = type;
            filterLeaveTypeSelect.appendChild(option);
        });
        
    } catch (error) {
        console.error('Error loading leave types:', error);
    }
}

// Fetch all leaves from the server
async function loadLeaves() {
    try {
        const response = await fetch('/api/leave', { credentials: 'include' });
        allLeaves = await response.json();
        
        updateOverviewStats();
        applyFiltersAndDisplay();
        
    } catch (error) {
        console.error('Error loading leaves:', error);
        showMessage('Error loading leave requests from server', 'error');
    }
}

// Update the Top Overview Cards
function updateOverviewStats() {
    const container = document.getElementById('leaveTypesContainer');
    container.innerHTML = '';
    
    const pendingCount = allLeaves.filter(l => l.status === 'Pending').length;
    const approvedCount = allLeaves.filter(l => l.status === 'Approved').length;
    const rejectedCount = allLeaves.filter(l => l.status === 'Rejected').length;

    const stats = [
        { label: 'Pending Action', count: pendingCount, icon: 'fas fa-clock', color: '#ffc107' },
        { label: 'Approved', count: approvedCount, icon: 'fas fa-check-circle', color: '#28a745' },
        { label: 'Declined', count: rejectedCount, icon: 'fas fa-times-circle', color: '#dc3545' }
    ];

    stats.forEach(stat => {
        const card = document.createElement('div');
        card.className = 'leave-type-card';
        card.innerHTML = `
            <i class="${stat.icon}" style="color: ${stat.color}; font-size: 2rem; margin-bottom: 10px;"></i>
            <div class="type-name" style="font-weight:bold;">${stat.label}</div>
            <div class="type-count" style="font-size: 1.2rem;">${stat.count}</div>
        `;
        container.appendChild(card);
    });
}

// Apply UI Filters and Render Table
function applyFiltersAndDisplay() {
    const employeeId = document.getElementById('filterEmployeeId').value.trim();
    const status = document.getElementById('filterStatus').value;
    const leaveType = document.getElementById('filterLeaveType').value;
    
    let filteredLeaves = allLeaves;

    if (employeeId) {
        filteredLeaves = filteredLeaves.filter(leave => leave.employeeId.toString() === employeeId);
    }
    if (status) {
        filteredLeaves = filteredLeaves.filter(leave => leave.status === status);
    }
    if (leaveType) {
        filteredLeaves = filteredLeaves.filter(leave => leave.leaveType === leaveType);
    }
    
    displayLeaves(filteredLeaves);
}

// Render the HTML Table
function displayLeaves(leaves) {
    const tbody = document.getElementById('leaveTableBody');
    
    if (leaves.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="no-data" style="text-align:center; padding:20px;">No leave requests match your filters.</td></tr>';
        return;
    }
    
    // Sort so Pending is always at the top
    leaves.sort((a, b) => {
        if (a.status === 'Pending' && b.status !== 'Pending') return -1;
        if (a.status !== 'Pending' && b.status === 'Pending') return 1;
        return 0;
    });

    tbody.innerHTML = leaves.map(leave => {
        const startDate = new Date(leave.startDate).toLocaleDateString();
        const endDate = new Date(leave.endDate).toLocaleDateString();
        const statusClass = leave.status.toLowerCase();
        
        let actionButtons = '-';
        if (leave.status === 'Pending') {
            actionButtons = `
                <button class="action-btn approve" onclick="processLeave(${leave.leaveId}, 'approve')" title="Approve">
                    <i class="fas fa-check"></i>
                </button>
                <button class="action-btn reject" onclick="processLeave(${leave.leaveId}, 'reject')" title="Decline">
                    <i class="fas fa-times"></i>
                </button>
            `;
        }
        
        return `
            <tr style="border-bottom: 1px solid #eee;">
                <td>#${leave.leaveId}</td>
                <td><b>${leave.employeeId}</b></td>
                <td>${leave.leaveType}</td>
                <td>${startDate} to ${endDate}</td>
                <td>${leave.totalDays} day(s)</td>
                <td><small>${leave.reason || 'No reason provided'}</small></td>
                <td><span class="status-badge ${statusClass}">${leave.status}</span></td>
                <td><small>${leave.approvedBy || '-'}</small></td>
                <td class="action-btns">${actionButtons}</td>
            </tr>
        `;
    }).join('');
}

// Handle Approve or Reject Action
async function processLeave(leaveId, action) {
    const isApproving = action === 'approve';
    const actionText = isApproving ? 'Approve' : 'Decline';
    
    if (!confirm(`Are you sure you want to ${actionText.toUpperCase()} this leave request?`)) {
        return;
    }

    const approvedBy = prompt(`Enter your name/ID (${actionText}d By):`, "HR Admin");
    if (!approvedBy) return; // Cancelled
    
    let remarks = "";
    if (!isApproving) {
        remarks = prompt('Enter reason for declining:');
        if (!remarks) {
            showMessage('A reason is required when declining a leave request.', 'error');
            return;
        }
    } else {
        remarks = prompt('Enter approval remarks (Optional):') || "Approved";
    }
    
    try {
        const endpoint = `/api/leave/${leaveId}/${action}`; // Hits /approve or /reject
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                approvedBy: approvedBy,
                remarks: remarks
            }),
            credentials: 'include'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showMessage(`Leave request successfully ${actionText.toLowerCase()}d.`, 'success');
            loadLeaves(); // Reload table from server
        } else {
            showMessage(data.message, 'error');
        }
    } catch (error) {
        console.error(`Error processing leave (${action}):`, error);
        showMessage('Error communicating with the server.', 'error');
    }
}

// Show System Message
function showMessage(message, type) {
    const messageDiv = document.getElementById('formMessage');
    messageDiv.textContent = message;
    
    if (type === 'success') {
        messageDiv.style.backgroundColor = '#d4edda';
        messageDiv.style.color = '#155724';
        messageDiv.style.border = '1px solid #c3e6cb';
    } else {
        messageDiv.style.backgroundColor = '#f8d7da';
        messageDiv.style.color = '#721c24';
        messageDiv.style.border = '1px solid #f5c6cb';
    }
    
    messageDiv.style.display = 'block';
    
    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 4000);
}