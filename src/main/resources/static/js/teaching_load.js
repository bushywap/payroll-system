let allTeachingLoadsData = []; 
let currentViewingEmpId = null; 

// SMART URL DETECTOR: Works on both Tomcat and Local Java!
const getContextPath = () => window.location.pathname.includes('/payroll-system') ? '/payroll-system' : '';

document.addEventListener('DOMContentLoaded', () => {
    fetchAllTeachingLoads();

    const deptFilter = document.getElementById('departmentFilter');
    const searchEmp = document.getElementById('searchTeaching');
    const btnSearch = document.getElementById('btnSearchSubmit');
    const btnRefresh = document.getElementById('btnRefreshTeachingLoad');

    if(deptFilter) deptFilter.addEventListener('change', runAllFilters);
    if(searchEmp) searchEmp.addEventListener('keyup', (e) => { if(e.key === 'Enter') runAllFilters(); });
    if(btnSearch) btnSearch.addEventListener('click', runAllFilters);
    if(btnRefresh) btnRefresh.addEventListener('click', fetchAllTeachingLoads);
});

function switchInnerTab(tabName) {
    const btnSchedule = document.getElementById('btnInnerSchedule');
    const btnManage = document.getElementById('btnInnerManage');
    const viewSchedule = document.getElementById('scheduleModalContent');
    const viewManage = document.getElementById('manageModalContent');

    if (tabName === 'SCHEDULE') {
        btnSchedule.classList.replace('inactive-tab', 'active-tab');
        btnManage.classList.replace('active-tab', 'inactive-tab');
        viewSchedule.style.display = 'block';
        viewManage.style.display = 'none';
    } else {
        btnManage.classList.replace('inactive-tab', 'active-tab');
        btnSchedule.classList.replace('active-tab', 'inactive-tab');
        viewManage.style.display = 'block';
        viewSchedule.style.display = 'none';
    }
}

function runAllFilters() {
    renderTeachingLoadTable();  
}

function fetchAllTeachingLoads() {
    const container = document.getElementById('teachingLoadViewContainer');
    if (!container) return;
    
    container.innerHTML = `<div class="table-responsive" style="margin-top: 20px;"><table class="custom-table"><tr><td colspan="4" class="empty-row" style="text-align:center;">Loading faculty directory...</td></tr></table></div>`;
    
    // USING SMART URL
    fetch(`${getContextPath()}/api/teaching-load/all`)
        .then(response => {
            if (!response.ok) throw new Error("Server Error: " + response.status);
            return response.json();
        })
        .then(data => { 
            allTeachingLoadsData = data || []; 
            renderTeachingLoadTable(); 
        })
        .catch(err => { 
            console.error("DEBUG FETCH ERROR:", err); // Prints the exact error to F12 Console
            container.innerHTML = `<div class="table-responsive" style="margin-top: 20px;"><table class="custom-table"><tr><td colspan="4" class="empty-row" style="text-align:center; color:red;">Error loading data. Press F12 to check the console.</td></tr></table></div>`; 
        });
}

function renderTeachingLoadTable() {
    const container = document.getElementById('teachingLoadViewContainer');
    if (!container) return;

    const deptFilter = document.getElementById('departmentFilter') ? document.getElementById('departmentFilter').value : 'ALL';
    const searchInput = document.getElementById('searchTeaching') ? document.getElementById('searchTeaching').value.toLowerCase() : '';

    container.innerHTML = '';

    if (!allTeachingLoadsData || allTeachingLoadsData.length === 0) {
        container.innerHTML = `<div style="text-align: center; padding: 40px; background: white; border-radius: 8px; border: 1px dashed #ccc;"><i class="fas fa-folder-open" style="font-size: 40px; color: #ccc; margin-bottom: 15px;"></i><h3 style="color: #666;">No Teaching Load Data Found</h3></div>`;
        return;
    }

    let enrichedData = allTeachingLoadsData.map(load => {
        let loadEmpId = load.employee ? (load.employee.employeeId || load.employee.id) : null;
        let empName = "Unknown Instructor"; 
        let empDept = "Unassigned";
        
        if (load.employee) {
            if (load.employee.firstName && load.employee.lastName) empName = (load.employee.firstName + ' ' + load.employee.lastName).trim();
            if (load.employee.department && load.employee.department.departmentName) empDept = load.employee.department.departmentName;
        }
        return { ...load, empId: loadEmpId, empName: empName, empDept: empDept };
    });

    if (deptFilter !== 'ALL') enrichedData = enrichedData.filter(d => d.empDept && d.empDept.trim().toLowerCase() === deptFilter.trim().toLowerCase());
    if (searchInput !== '') enrichedData = enrichedData.filter(d => (d.empId && d.empId.toString().toLowerCase().includes(searchInput)) || d.empName.toLowerCase().includes(searchInput));

    if (enrichedData.length === 0) {
        container.innerHTML = `<div class="table-responsive" style="margin-top: 20px;"><table class="custom-table"><tr><td colspan="4" class="empty-row" style="text-align:center;">No faculty found matching the criteria.</td></tr></table></div>`;
        return;
    }

    const groupedData = enrichedData.reduce((groups, load) => {
        const dept = load.empDept; 
        if (!groups[dept]) groups[dept] = {};
        
        const empId = load.empId;
        if (!groups[dept][empId]) {
            groups[dept][empId] = { name: load.empName, uniqueSubjects: new Set() };
        }
        
        let subjectName = load.subject ? load.subject.trim().toUpperCase() : (load.subjectCode ? load.subjectCode.trim().toUpperCase() : 'UNKNOWN');
        groups[dept][empId].uniqueSubjects.add(subjectName);
        
        return groups;
    }, {});

    for (const [department, employees] of Object.entries(groupedData)) {
        const uniqueEmployees = Object.keys(employees).length;
        const wrapper = document.createElement('div');
        wrapper.className = 'glass-card dept-table-wrapper'; 
        wrapper.style.marginTop = '20px'; 
        wrapper.style.padding = '15px';
        wrapper.innerHTML = `<h3 class="dept-header-title" style="color: #cc0000; border-bottom: 2px solid #1a1a1a; padding-bottom: 5px;"><span><i class="fas fa-building"></i> ${department}</span><span class="dept-employee-count" style="font-size:14px; color:#666; font-weight:normal; margin-left: 10px;">(${uniqueEmployees} Instructors)</span></h3>`;

        const tableResponsive = document.createElement('div'); 
        tableResponsive.className = 'table-responsive'; 
        
        const table = document.createElement('table'); 
        table.className = 'custom-table'; 
        table.innerHTML = `
            <thead style="background-color: #1a1a1a; color: white;">
                <tr>
                    <th style="width: 15%; border-bottom: 2px solid #cc0000;">Emp ID</th>
                    <th style="text-align: left; border-bottom: 2px solid #cc0000;">Instructor Name</th>
                    <th style="border-bottom: 2px solid #cc0000;">Total Assigned Subjects</th>
                    <th style="width: 15%; border-bottom: 2px solid #cc0000;">Action</th>
                </tr>
            </thead>
            <tbody></tbody>
        `;

        const tbody = table.querySelector('tbody'); 

        Object.keys(employees).forEach(empId => {
            const empData = employees[empId];
            const safeName = empData.name.replace(/'/g, "\\'"); 
            const trueSubjectCount = empData.uniqueSubjects.size;

            const tr = document.createElement('tr');
            tr.className = "clickable-instructor-row";
            tr.onclick = function() { openScheduleModal(empId, safeName); };
            
            tr.innerHTML = `
                <td style="text-align:center; font-weight:bold; color:#555;">${empId || '-'}</td>
                <td style="font-weight: bold; color: #1a1a1a; text-align: left;">${empData.name}</td>
                <td style="text-align:center;"><span style="background:#fbe9e7; color:#d84315; padding:3px 10px; border-radius:12px; font-weight:bold;">${trueSubjectCount} Subjects</span></td>
                <td style="text-align:center;">
                    <button class="btn-red" style="padding: 6px 12px; font-size: 12px;" title="View & Edit Detailed Schedule">
                        <i class="fas fa-folder-open"></i> Schedule
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });

        tableResponsive.appendChild(table); 
        wrapper.appendChild(tableResponsive); 
        container.appendChild(wrapper);
    }
}

function openScheduleModal(empId, empName) {
    currentViewingEmpId = empId;
    document.getElementById('scheduleModalEmpName').innerText = empName;
    
    switchInnerTab('SCHEDULE');
    
    const scheduleContainer = document.getElementById('scheduleModalContent');
    const manageContainer = document.getElementById('manageModalContent');
    
    const empLoads = allTeachingLoadsData.filter(load => {
        let loadEmpId = load.employee ? (load.employee.employeeId || load.employee.id) : null;
        return loadEmpId == empId;
    });

    let htmlSchedule = `<table class="custom-table" style="margin-bottom:0; width: 100%; table-layout: auto;">
        <thead style="position: sticky; top: 0; z-index: 5; background: #f8fafc; color: #333;">
            <tr>
                <th style="text-align:left; border-bottom: 2px solid #ccc;">Subject(s)</th>
                <th style="border-bottom: 2px solid #ccc;">Subject Code</th>
                <th style="border-bottom: 2px solid #ccc;">Section</th>
                <th style="border-bottom: 2px solid #ccc;">Days</th>
                <th style="border-bottom: 2px solid #ccc;">Time</th>
                <th style="border-bottom: 2px solid #ccc;">Room</th>
                <th style="border-bottom: 2px solid #ccc;">Units</th>
                <th style="border-bottom: 2px solid #ccc;">Lec</th>
                <th style="border-bottom: 2px solid #ccc;">Lab</th>
                <th style="border-bottom: 2px solid #ccc;">Total Hours</th>
            </tr>
        </thead>
        <tbody style="background-color: #ffffff; color: #333;">`;
        
    let htmlManage = `<table class="custom-table" style="margin-bottom:0; width: 100%; table-layout: auto;">
        <thead style="position: sticky; top: 0; z-index: 5; background: #fdf2f8; color: #333;">
            <tr>
                <th style="width: 25%; text-align:left; border-bottom: 2px solid #ccc;">Subject</th>
                <th style="border-bottom: 2px solid #ccc;">Schedule</th>
                <th style="border-bottom: 2px solid #ccc;">RLE Hrs</th>
                <th style="border-bottom: 2px solid #ccc;">Sub. Hrs</th>
                <th style="width: 20%; border-bottom: 2px solid #ccc;">Substitute Name (Date)</th>
                <th style="width: 15%; text-align:center; border-bottom: 2px solid #ccc;">Action</th>
            </tr>
        </thead>
        <tbody style="background-color: #ffffff; color: #333;">`;
    
    let tUnit = 0, tLab = 0, tLec = 0, tRle = 0, tSub = 0, tHrs = 0;

    empLoads.forEach(load => {
        let lecUnits = load.lectureUnits || 0; 
        let labUnits = load.labUnits || 0;
        let sections = load.noOfSections || 1; 
        let rleHrs = load.rleHours || 0;
        
        let subName = load.substituteFacultyName || '-'; 
        let subHrs = load.substituteRenderedHours || 0;
        let subDate = load.substituteDate || ''; 
        
        let subType = load.subjectType || '';

        if (lecUnits > 0) {
            let lecHours = lecUnits * 1; 
            tUnit += lecUnits;
            tLec += lecUnits;
            tHrs += lecHours;

            htmlSchedule += `<tr>
                <td style="text-align:left; font-weight: bold;">${load.subject || 'N/A'}</td>
                <td style="text-align:center; color: #555; white-space: nowrap;">
                    ${load.subjectCode || '---'}
                    <span style="font-size: 10px; background: #e3f2fd; color: #1565c0; padding: 2px 6px; border-radius: 4px; margin-left: 5px; vertical-align: middle;">LEC</span>
                </td>
                <td style="text-align:center; color: #e65100; font-weight: bold;">${sections}</td>
                <td style="text-align:center; font-weight: bold;">${load.dayOfWeek || '-'}</td>
                <td style="text-align:center; white-space: nowrap;">${load.timeSchedule || '-'}</td>
                <td style="text-align:center;">${load.room || 'TBA'}</td>
                <td style="text-align:center; font-weight: bold; color: #1a237e;">${lecUnits}</td>
                <td style="text-align:center; color: #0277bd;">${lecUnits}</td>
                <td style="text-align:center; color: #0277bd;">0</td>
                <td style="text-align:center; font-weight: bold; color: #b71c1c; font-size:15px;">${lecHours}</td>
            </tr>`;
        }

        if (labUnits > 0) {
            let labHours = load.labHours || (labUnits * 3); 
            tUnit += labUnits;
            tLab += labHours; 
            tHrs += labHours;

            htmlSchedule += `<tr>
                <td style="text-align:left; font-weight: bold;">${load.subject || 'N/A'}</td>
                <td style="text-align:center; color: #555; white-space: nowrap;">
                    ${load.subjectCode || '---'}
                    <span style="font-size: 10px; background: #fbe9e7; color: #d84315; padding: 2px 6px; border-radius: 4px; margin-left: 5px; vertical-align: middle;">LAB</span>
                </td>
                <td style="text-align:center; color: #e65100; font-weight: bold;">${sections}</td>
                <td style="text-align:center; font-weight: bold;">${load.dayOfWeek || '-'}</td>
                <td style="text-align:center; white-space: nowrap;">${load.timeSchedule || '-'}</td>
                <td style="text-align:center;">${load.room || 'TBA'}</td>
                <td style="text-align:center; font-weight: bold; color: #1a237e;">${labUnits}</td>
                <td style="text-align:center; color: #0277bd;">0</td>
                <td style="text-align:center; color: #0277bd;">${labHours}</td> 
                <td style="text-align:center; font-weight: bold; color: #b71c1c; font-size:15px;">${labHours}</td>
            </tr>`;
        }
                
        let subjectHTML = `<td style="text-align:left; word-wrap: break-word;"><small style="color: #555;">${load.subjectCode || '---'}</small><br><strong>${load.subject || 'N/A'}</strong></td>`;
        let scheduleHTML = `<td style="text-align:center; font-weight: bold; white-space: nowrap;">${load.dayOfWeek || '-'}<br><small>${load.timeSchedule || '-'}</small></td>`;
        let subBackground = subHrs > 0 ? 'background-color: #fff3e0;' : '';

        tRle += rleHrs; 
        tSub += subHrs; 

        htmlManage += `<tr style="${subBackground}">
                    ${subjectHTML}
                    ${scheduleHTML}
                    <td style="text-align:center; color: #2e7d32; font-weight: bold;">${rleHrs}</td>
                    <td style="text-align:center; color: #e65100; font-weight: bold; font-size:15px;">${subHrs}</td>
                    <td style="text-align:center; font-style: italic; color: #555; word-wrap: break-word;">${subName}<br><small style="color: #888;">${subDate}</small></td>
                    <td style="text-align:center; white-space: nowrap;">
                        <button class="btn-red" style="padding: 6px 10px; font-size: 12px; border-radius: 4px;" onclick="openEditLoadModal(${load.id}, ${sections}, ${rleHrs}, '${subName === '-' ? '' : subName}', ${subHrs}, '${subDate}', '${subType}')" title="Assign Substitute or Edit RLE">
                            <i class="fas fa-edit"></i> Edit Load
                        </button>
                    </td>
                </tr>`;
    });

    htmlSchedule += `</tbody>
        <tfoot style="position: sticky; bottom: 0; background: #f8fafc; color: #333; box-shadow: 0 -2px 5px rgba(0,0,0,0.05);">
            <tr style="font-weight:bold;">
                <td colspan="6" style="text-align:right;">TOTALS:</td>
                <td style="text-align:center; color:#1a237e;">${tUnit}</td>
                <td style="text-align:center; color:#0277bd;">${tLec}</td>
                <td style="text-align:center; color:#0277bd;">${tLab}</td>
                <td style="text-align:center; color:#b71c1c; font-size:18px;">${tHrs}</td>
            </tr>
        </tfoot>
    </table>`;
    
    htmlManage += `</tbody>
        <tfoot style="position: sticky; bottom: 0; background: #fdf2f8; color: #333; box-shadow: 0 -2px 5px rgba(0,0,0,0.05);">
            <tr style="font-weight:bold;">
                <td colspan="2" style="text-align:right;">TOTALS:</td>
                <td style="text-align:center; color:#2e7d32; font-size: 16px;">${tRle}</td>
                <td style="text-align:center; color:#e65100; font-size: 16px;">${tSub}</td>
                <td colspan="2"></td>
            </tr>
        </tfoot>
    </table>`;
            
    scheduleContainer.innerHTML = htmlSchedule;
    manageContainer.innerHTML = htmlManage;
    document.getElementById('instructorScheduleModal').style.display = 'block';
}

function openEditLoadModal(id, sections, rle, subName, subHours, subDate, subType) {
    document.getElementById('editLoadId').value = id;
    document.getElementById('editSections').value = sections || 1;
    document.getElementById('editRleHours').value = rle || 0;
    document.getElementById('editSubstitute').value = subName === 'null' || subName === '-' ? '' : subName;
    document.getElementById('editSubHours').value = subHours || 0;
    document.getElementById('editSubDate').value = subDate || '';
    
    const typeDropdown = document.getElementById('editSubjectType');
    if(typeDropdown) {
        typeDropdown.value = subType || '';
    }
    
    document.getElementById('editLoadModal').style.display = 'block';
}

function saveTeachingLoad() {
    const id = document.getElementById('editLoadId').value;
    
    const typeDropdown = document.getElementById('editSubjectType');
    const selectedType = typeDropdown ? typeDropdown.value : null;

    const data = {
        noOfSections: parseInt(document.getElementById('editSections').value) || 1,
        rleHours: parseInt(document.getElementById('editRleHours').value) || 0,
        substituteFacultyName: document.getElementById('editSubstitute').value.trim(),
        substituteRenderedHours: parseInt(document.getElementById('editSubHours').value) || 0,
        substituteDate: document.getElementById('editSubDate').value || null,
        subjectType: selectedType
    };

    // USING SMART URL
    fetch(`${getContextPath()}/api/teaching-load/update/${id}`, {
        method: 'PUT', 
        headers: { 'Content-Type': 'application/json' }, 
        body: JSON.stringify(data)
    })
    .then(res => {
        closeModal('editLoadModal'); 
        
        // USING SMART URL
        fetch(`${getContextPath()}/api/teaching-load/all`)
            .then(response => response.json())
            .then(data => { 
                allTeachingLoadsData = data || []; 
                renderTeachingLoadTable(); 
                
                if (document.getElementById('instructorScheduleModal').style.display === 'block' && currentViewingEmpId) {
                    const empName = document.getElementById('scheduleModalEmpName').innerText;
                    openScheduleModal(currentViewingEmpId, empName);
                    switchInnerTab('MANAGE'); 
                }
            });

        alert("Teaching load updated successfully!");
    })
    .catch(error => { 
        alert("Error saving teaching load."); 
    });
}

function closeModal(modalId) { 
    document.getElementById(modalId).style.display = 'none'; 
    if(modalId === 'instructorScheduleModal') {
        currentViewingEmpId = null;
    }
}

window.onclick = function(event) { 
    let scheduleModal = document.getElementById('instructorScheduleModal');
    let editModal = document.getElementById('editLoadModal');
    
    if (event.target === scheduleModal) { closeModal('instructorScheduleModal'); }
    if (event.target === editModal) { closeModal('editLoadModal'); } 
}