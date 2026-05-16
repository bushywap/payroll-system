/* ==========================================================================
   PAYROLL REGISTER LOGIC
   Handles Table Filtering, Auto-Computing Totals, and Emailing
   ========================================================================== */

let contextPath = document.querySelector('meta[name="context-path"]')?.getAttribute('content') || '';
if (contextPath === '/') {
    contextPath = '';
} else if (contextPath.endsWith('/')) {
    contextPath = contextPath.slice(0, -1);
}

let currentPayrollTab = 'ALL';

document.addEventListener('DOMContentLoaded', () => {
    applyTrueCalculations(); 
    computeTableTotals();
    
    const searchInput = document.getElementById('searchEmployee');
    const deptSelect = document.getElementById('registerDepartment');
    const startDate = document.getElementById('startDateFilter');
    const endDate = document.getElementById('endDateFilter');

    if (searchInput) searchInput.addEventListener('input', filterTable);
    if (deptSelect) deptSelect.addEventListener('change', filterTable);
    if (startDate) startDate.addEventListener('change', filterTable);
    if (endDate) endDate.addEventListener('change', filterTable);
	
	const reportBtn = document.getElementById("generateReportBtn");
	    if(reportBtn) {
	        reportBtn.addEventListener("click", function() {
	            window.location.href = contextPath + '/api/payroll/export-report'; 
	        });
	    }
});

// Custom Promise-based Confirmation logic
function customConfirm(message) {
    return new Promise((resolve) => {
        const modal = document.getElementById('confirmModal');
        const msgEl = document.getElementById('confirmModalMessage');
        const btnOk = document.getElementById('btnConfirmOk');
        const btnCancel = document.getElementById('btnConfirmCancel');

        msgEl.innerHTML = message;
        modal.style.display = 'block';

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

function switchPayrollTab(tabName) {
    currentPayrollTab = tabName;
    
    document.getElementById('btnTabAll').className = tabName === 'ALL' ? 'btn-tab active-tab' : 'btn-tab inactive-tab';
    document.getElementById('btnTabFaculty').className = tabName === 'FACULTY' ? 'btn-tab active-tab' : 'btn-tab inactive-tab';
    document.getElementById('btnTabNonFaculty').className = tabName === 'NON-FACULTY' ? 'btn-tab active-tab' : 'btn-tab inactive-tab';
    
    filterTable();
}

function formatCurrency(amount) {
    const num = parseFloat(amount);
    if (isNaN(num)) return '₱0.00';
    return '₱' + num.toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function parseCurrencyToFloat(text) {
    if (!text) return 0;
    const num = parseFloat(text.replace(/[^0-9.-]+/g, ""));
    return isNaN(num) ? 0 : num;
}

function safeFloat(val) {
    if (!val || val === "null" || val === "undefined") return 0;
    const parsed = parseFloat(val);
    return isNaN(parsed) ? 0 : parsed;
}

function applyTrueCalculations() {
    document.querySelectorAll('.payroll-record-row').forEach(row => {
        const basicCell = row.querySelector('.col-basic');
        const basicPay = parseCurrencyToFloat(basicCell?.innerText);

        const otCell = row.querySelector('.col-ot');
        const otPay = safeFloat(otCell?.getAttribute('data-ot-pay'));
        
        const adjCell = row.querySelector('.col-adj');
        const adjPay = parseCurrencyToFloat(adjCell?.innerText);

        const teachCell = row.querySelector('.col-teach');
        let pureOverloadTotal = 0;
        let holPay = 0;
        
        if (teachCell) {
            holPay = safeFloat(teachCell.getAttribute('data-hol-pay'));
            pureOverloadTotal = safeFloat(teachCell.getAttribute('data-pure-overload'));
            
            if (pureOverloadTotal === 0 && teachCell.innerText.trim() !== '' && teachCell.innerText.trim() !== '₱0.00') {
                 pureOverloadTotal = parseCurrencyToFloat(teachCell.innerText);
            }

            teachCell.innerText = formatCurrency(pureOverloadTotal);
            teachCell.setAttribute('data-pure-overload', pureOverloadTotal);
        }

        const othCell = row.querySelector('.col-oth-income');
        let trueOthIncome = 0;
        if (othCell) {
            const deminimis = safeFloat(othCell.getAttribute('data-deminimis'));
            const longevity = safeFloat(othCell.getAttribute('data-longevity'));
            const honorarium = safeFloat(othCell.getAttribute('data-honorarium'));
            const leavepay = safeFloat(othCell.getAttribute('data-leavepay'));
            const cashgift = safeFloat(othCell.getAttribute('data-cashgift'));
            const incentive = safeFloat(othCell.getAttribute('data-incentive'));
            const allowance = safeFloat(othCell.getAttribute('data-allowance'));
            const relocation = safeFloat(othCell.getAttribute('data-relocation'));
            
            let holiday = safeFloat(othCell.getAttribute('data-holiday'));
            if (holPay > 0 && holiday === holPay) holiday = 0;

            trueOthIncome = deminimis + longevity + honorarium + holiday + leavepay + cashgift + incentive + allowance + relocation;
            othCell.innerHTML = `<i class="fas fa-coins" style="margin-right: 3px; font-size: 9px;"></i> ${formatCurrency(trueOthIncome)}`;
            othCell.setAttribute('data-holiday', holiday);
        }

        const earnCell = row.querySelector('.col-earn');
        const trueEarn = basicPay + pureOverloadTotal + otPay + adjPay + trueOthIncome;
        
        if (earnCell) {
            earnCell.innerHTML = `<i class="fas fa-search-plus" style="font-size: 9px;"></i> ${formatCurrency(trueEarn)}`;
            earnCell.setAttribute('data-tot', trueEarn);
            earnCell.setAttribute('data-basic', basicPay);
            earnCell.setAttribute('data-teach', pureOverloadTotal);
            earnCell.setAttribute('data-ot', otPay);
            earnCell.setAttribute('data-oth', trueOthIncome);
        }

        // ==========================================
        // FIXED PENALTIES BLOCK (No Ghost Auto-Calcs)
        // ==========================================
        const penCell = row.querySelector('.col-penalties');
        let truePenalties = 0;
        if (penCell) {
            // We ONLY trust what the backend sends us now. No JS overrides!
            let abs = safeFloat(penCell.getAttribute('data-absent-ded'));
            let late = safeFloat(penCell.getAttribute('data-late-ded'));
            let ut = safeFloat(penCell.getAttribute('data-ut-ded'));

            truePenalties = abs + late + ut;
            penCell.innerHTML = `<i class="fas fa-user-clock" style="margin-right: 3px; font-size: 9px;"></i> ${formatCurrency(truePenalties)}`;
        }

        const grossCell = row.querySelector('.col-gross');
        let trueGross = trueEarn - truePenalties;
        if (trueGross < 0) trueGross = 0; 
        if (grossCell) {
            grossCell.innerText = formatCurrency(trueGross);
        }

        const sss = parseCurrencyToFloat(row.querySelector('.col-sss')?.innerText);
        const phic = parseCurrencyToFloat(row.querySelector('.col-phic')?.innerText);
        const hdmf = parseCurrencyToFloat(row.querySelector('.col-hdmf')?.innerText);
        const sLoan = parseCurrencyToFloat(row.querySelector('.col-sss-loan')?.innerText);
        const hLoan = parseCurrencyToFloat(row.querySelector('.col-hdmf-loan')?.innerText);
        const oLoan = parseCurrencyToFloat(row.querySelector('.col-oth-loan')?.innerText);
        const tax = parseCurrencyToFloat(row.querySelector('.col-tax')?.innerText);
        const lvu = parseCurrencyToFloat(row.querySelector('.col-lvu')?.innerText);

        const dedCell = row.querySelector('.col-ded');
        const trueDed = sss + phic + hdmf + sLoan + hLoan + oLoan + tax + lvu;
        if (dedCell) {
            dedCell.innerHTML = `<i class="fas fa-search-dollar" style="margin-right: 3px; font-size: 9px;"></i> ${formatCurrency(trueDed)}`;
        }

        const netCell = row.querySelector('.col-net');
        let trueNet = trueGross - trueDed;
        if (trueNet < 0) trueNet = 0; 
        if (netCell) {
            netCell.innerText = formatCurrency(trueNet);
        }
    });
}

function filterTable() {
    const searchVal = document.getElementById('searchEmployee')?.value.toLowerCase() || '';
    const deptVal = document.getElementById('registerDepartment')?.value || 'ALL';
    const startVal = document.getElementById('startDateFilter')?.value || '';
    const endVal = document.getElementById('endDateFilter')?.value || '';

    document.querySelectorAll('.dept-group-body').forEach(group => {
        const groupDept = group.getAttribute('data-department');
        let groupHasVisibleRows = false;
        
        if (deptVal !== 'ALL' && groupDept !== deptVal) {
            group.style.display = 'none'; return;
        }

        group.querySelectorAll('.payroll-record-row').forEach(row => {
            const empName = row.querySelector('.row-name')?.innerText.toLowerCase() || '';
            const empId = row.querySelector('.row-id')?.innerText.toLowerCase() || '';
            const rowStart = row.getAttribute('data-start') || '';
            const rowEnd = row.getAttribute('data-end') || '';
            
            const empType = row.getAttribute('data-emp-type') || 'NON-FACULTY';

            const matchesSearch = (empName.includes(searchVal) || empId.includes(searchVal));
            const matchesStart = (startVal === '' || rowStart >= startVal);
            const matchesEnd = (endVal === '' || rowEnd <= endVal);
            const matchesTab = (currentPayrollTab === 'ALL' || currentPayrollTab === empType);

            if (matchesSearch && matchesStart && matchesEnd && matchesTab) {
                row.style.display = '';
                groupHasVisibleRows = true;
            } else {
                row.style.display = 'none';
            }
        });
        group.style.display = groupHasVisibleRows ? '' : 'none';
    });
    
    document.querySelectorAll('.payroll-record-row[style*="display: none"] .row-checkbox').forEach(cb => cb.checked = false);
    
    const selectAllCb = document.getElementById('selectAllCheckbox');
    if (selectAllCb) selectAllCb.checked = false;
    document.querySelectorAll('.dept-checkbox').forEach(cb => cb.checked = false);

    computeTableTotals();
}

function computeTableTotals() {
    let grandTotals = { basic: 0, teach: 0, ot: 0, adj: 0, oth: 0, earn: 0, pen: 0, gross: 0, sss: 0, phic: 0, hdmf: 0, sloan: 0, hloan: 0, oloan: 0, tax: 0, lvu: 0, ded: 0, net: 0 };

    document.querySelectorAll('.dept-group-body').forEach(tbody => {
        if (tbody.style.display === 'none') return;
        let deptTotals = { basic: 0, teach: 0, ot: 0, adj: 0, oth: 0, earn: 0, pen: 0, gross: 0, sss: 0, phic: 0, hdmf: 0, sloan: 0, hloan: 0, oloan: 0, tax: 0, lvu: 0, ded: 0, net: 0 };
        let visibleRows = 0;
        
        tbody.querySelectorAll('.payroll-record-row').forEach(row => {
            if (row.style.display !== 'none') {
                visibleRows++;
                deptTotals.basic += parseCurrencyToFloat(row.querySelector('.col-basic')?.innerText);
                deptTotals.teach += parseCurrencyToFloat(row.querySelector('.col-teach')?.innerText);
                deptTotals.ot += parseCurrencyToFloat(row.querySelector('.col-ot')?.innerText);
                deptTotals.adj += parseCurrencyToFloat(row.querySelector('.col-adj')?.innerText); 
                deptTotals.oth += parseCurrencyToFloat(row.querySelector('.col-oth-income')?.innerText);
                deptTotals.earn += parseCurrencyToFloat(row.querySelector('.col-earn')?.innerText);
                deptTotals.pen += parseCurrencyToFloat(row.querySelector('.col-penalties')?.innerText);
                deptTotals.gross += parseCurrencyToFloat(row.querySelector('.col-gross')?.innerText);
                deptTotals.sss += parseCurrencyToFloat(row.querySelector('.col-sss')?.innerText);
                deptTotals.phic += parseCurrencyToFloat(row.querySelector('.col-phic')?.innerText);
                deptTotals.hdmf += parseCurrencyToFloat(row.querySelector('.col-hdmf')?.innerText);
                deptTotals.sloan += parseCurrencyToFloat(row.querySelector('.col-sss-loan')?.innerText);
                deptTotals.hloan += parseCurrencyToFloat(row.querySelector('.col-hdmf-loan')?.innerText);
                deptTotals.oloan += parseCurrencyToFloat(row.querySelector('.col-oth-loan')?.innerText);
                deptTotals.tax += parseCurrencyToFloat(row.querySelector('.col-tax')?.innerText);
                deptTotals.lvu += parseCurrencyToFloat(row.querySelector('.col-lvu')?.innerText);
                deptTotals.ded += parseCurrencyToFloat(row.querySelector('.col-ded')?.innerText);
                deptTotals.net += parseCurrencyToFloat(row.querySelector('.col-net')?.innerText);
            }
        });

        const subRow = tbody.querySelector('.dept-subtotal-row');
        if (subRow) {
            if (visibleRows > 0) {
                subRow.style.display = '';
                subRow.querySelector('.sub-basic').innerText = formatCurrency(deptTotals.basic);
                subRow.querySelector('.sub-teach').innerText = formatCurrency(deptTotals.teach);
                subRow.querySelector('.sub-ot').innerText = formatCurrency(deptTotals.ot);
                subRow.querySelector('.sub-adj').innerText = formatCurrency(deptTotals.adj); 
                subRow.querySelector('.sub-oth-income').innerText = formatCurrency(deptTotals.oth);
                subRow.querySelector('.sub-earn').innerText = formatCurrency(deptTotals.earn);
                subRow.querySelector('.sub-penalties').innerText = formatCurrency(deptTotals.pen);
                subRow.querySelector('.sub-gross').innerText = formatCurrency(deptTotals.gross);
                subRow.querySelector('.sub-sss').innerText = formatCurrency(deptTotals.sss);
                subRow.querySelector('.sub-phic').innerText = formatCurrency(deptTotals.phic);
                subRow.querySelector('.sub-hdmf').innerText = formatCurrency(deptTotals.hdmf);
                subRow.querySelector('.sub-sss-loan').innerText = formatCurrency(deptTotals.sloan);
                subRow.querySelector('.sub-hdmf-loan').innerText = formatCurrency(deptTotals.hloan);
                subRow.querySelector('.sub-oth-loan').innerText = formatCurrency(deptTotals.oloan);
                subRow.querySelector('.sub-tax').innerText = formatCurrency(deptTotals.tax);
                subRow.querySelector('.sub-lvu').innerText = formatCurrency(deptTotals.lvu);
                subRow.querySelector('.sub-ded').innerText = formatCurrency(deptTotals.ded);
                subRow.querySelector('.sub-net').innerText = formatCurrency(deptTotals.net);
                for (let key in grandTotals) grandTotals[key] += deptTotals[key];
            } else {
                subRow.style.display = 'none';
            }
        }
    });

    const gtBasic = document.querySelector('.gt-basic'); if(gtBasic) gtBasic.innerText = formatCurrency(grandTotals.basic);
    const gtTeach = document.querySelector('.gt-teach'); if(gtTeach) gtTeach.innerText = formatCurrency(grandTotals.teach);
    const gtOt = document.querySelector('.gt-ot'); if(gtOt) gtOt.innerText = formatCurrency(grandTotals.ot);
    const gtAdj = document.querySelector('.gt-adj'); if(gtAdj) gtAdj.innerText = formatCurrency(grandTotals.adj); 
    const gtOth = document.querySelector('.gt-oth-income'); if(gtOth) gtOth.innerText = formatCurrency(grandTotals.oth);
    const gtEarn = document.querySelector('.gt-earn'); if(gtEarn) gtEarn.innerText = formatCurrency(grandTotals.earn);
    const gtPen = document.querySelector('.gt-penalties'); if(gtPen) gtPen.innerText = formatCurrency(grandTotals.pen);
    const gtGross = document.querySelector('.gt-gross'); if(gtGross) gtGross.innerText = formatCurrency(grandTotals.gross);
    const gtSss = document.querySelector('.gt-sss'); if(gtSss) gtSss.innerText = formatCurrency(grandTotals.sss);
    const gtPhic = document.querySelector('.gt-phic'); if(gtPhic) gtPhic.innerText = formatCurrency(grandTotals.phic);
    const gtHdmf = document.querySelector('.gt-hdmf'); if(gtHdmf) gtHdmf.innerText = formatCurrency(grandTotals.hdmf);
    const gtSloan = document.querySelector('.gt-sss-loan'); if(gtSloan) gtSloan.innerText = formatCurrency(grandTotals.sloan);
    const gtHloan = document.querySelector('.gt-hdmf-loan'); if(gtHloan) gtHloan.innerText = formatCurrency(grandTotals.hloan);
    const gtOloan = document.querySelector('.gt-oth-loan'); if(gtOloan) gtOloan.innerText = formatCurrency(grandTotals.oloan);
    const gtTax = document.querySelector('.gt-tax'); if(gtTax) gtTax.innerText = formatCurrency(grandTotals.tax);
    const gtLvu = document.querySelector('.gt-lvu'); if(gtLvu) gtLvu.innerText = formatCurrency(grandTotals.lvu);
    const gtDed = document.querySelector('.gt-ded'); if(gtDed) gtDed.innerText = formatCurrency(grandTotals.ded);
    const gtNet = document.querySelector('.gt-net'); if(gtNet) gtNet.innerText = formatCurrency(grandTotals.net);
}

// ================= MODAL FUNCTIONS ================= //
function viewOtherIncomeBreakdown(element) { 
    document.getElementById('othEmpName').innerText = element.getAttribute('data-emp-name') || '----'; 
    const deminimis = safeFloat(element.getAttribute('data-deminimis'));
    const longevity = safeFloat(element.getAttribute('data-longevity'));
    const cashgift = safeFloat(element.getAttribute('data-cashgift'));
    const incentive = safeFloat(element.getAttribute('data-incentive'));
    const relocation = safeFloat(element.getAttribute('data-relocation'));
    const honorarium = safeFloat(element.getAttribute('data-honorarium'));
    const holiday = safeFloat(element.getAttribute('data-holiday'));
    const leavepay = safeFloat(element.getAttribute('data-leavepay'));
    const totalBundledAllowance = safeFloat(element.getAttribute('data-allowance'));
    const adminPay = safeFloat(element.getAttribute('data-adminpay'));
    const pureAllowance = totalBundledAllowance - adminPay;

    document.getElementById('modOthDeminimis').innerText = formatCurrency(deminimis); 
    document.getElementById('modOthLongevity').innerText = formatCurrency(longevity); 
    document.getElementById('modOthCashGift').innerText = formatCurrency(cashgift); 
    document.getElementById('modOthIncentive').innerText = formatCurrency(incentive); 
    document.getElementById('modOthRelocation').innerText = formatCurrency(relocation); 
    document.getElementById('modOthHonorarium').innerText = formatCurrency(honorarium); 
    document.getElementById('modOthHoliday').innerText = formatCurrency(holiday); 
    document.getElementById('modOthLeave').innerText = formatCurrency(leavepay); 
    
    if (document.getElementById('modOthAdminPay')) document.getElementById('modOthAdminPay').innerText = formatCurrency(adminPay);
    document.getElementById('modOthAllowance').innerText = formatCurrency(pureAllowance); 

    const total = deminimis + longevity + cashgift + incentive + totalBundledAllowance + relocation + honorarium + holiday + leavepay;
    document.getElementById('modOthTotal').innerText = formatCurrency(total); 
    document.getElementById('otherIncomeModal').style.display = 'block'; 
}

function viewEarningsBreakdown(element) { 
    document.getElementById('earnEmpName').innerText = element.getAttribute('data-emp') || '----'; 
    const basic = safeFloat(element.getAttribute('data-basic'));
    const teach = safeFloat(element.getAttribute('data-teach'));
    const ot = safeFloat(element.getAttribute('data-ot'));
    const oth = safeFloat(element.getAttribute('data-oth'));

    document.getElementById('modBasicAmt').innerText = formatCurrency(basic); 
    document.getElementById('modTeachAmt').innerText = formatCurrency(teach); 
    document.getElementById('modOtAmt').innerText = formatCurrency(ot); 
    document.getElementById('modEarnOthAmt').innerText = formatCurrency(oth); 
    
    const tbody = document.querySelector('#earningsModal .mod-table tbody');
    let adjRow = document.getElementById('modAdjRow');
    if (!adjRow) {
        adjRow = document.createElement('tr');
        adjRow.id = 'modAdjRow';
        adjRow.innerHTML = `<td class="label-cell">Adjustments (+)</td><td class="val-cell text-blue" id="modAdjAmt">₱0.00</td>`;
        tbody.appendChild(adjRow);
    }
    
    const adjCell = element.closest('tr').querySelector('.col-adj');
    const adj = adjCell ? parseCurrencyToFloat(adjCell.innerText) : 0;
    document.getElementById('modAdjAmt').innerText = formatCurrency(adj);

    const grandTotal = basic + teach + ot + oth + adj;
    document.getElementById('modTotEarnAmt').innerText = formatCurrency(grandTotal); 
    document.getElementById('earningsModal').style.display = 'block'; 
}

function viewPenaltiesBreakdown(btn) {
    const empName = btn.getAttribute('data-emp-name') || 'Unknown';
    
    const absDays = parseFloat(btn.getAttribute('data-absent-days')) || 0;
    const lateMins = parseFloat(btn.getAttribute('data-late-mins')) || 0;
    const utMins = parseFloat(btn.getAttribute('data-ut-mins')) || 0;

    // NO MORE AUTO CALCULATION OVERRIDES. WE TRUST THE BACKEND ONLY!
    const absDed = parseFloat(btn.getAttribute('data-absent-ded')) || 0;
    const lateDed = parseFloat(btn.getAttribute('data-late-ded')) || 0;
    const utDed = parseFloat(btn.getAttribute('data-ut-ded')) || 0;

    const dailyRate = parseFloat(btn.getAttribute('data-daily-rate')) || 0;
    const minRate = parseFloat(btn.getAttribute('data-minute-rate')) || 0;

    const totalPenalties = absDed + lateDed + utDed;

    document.getElementById('penaltiesEmpName').innerText = empName;
    
    document.getElementById('modPenAbsDays').innerText = `(${String(absDays)} days)`;
    const rateTextElement = document.getElementById('modPenAbsRate');
    if (absDays > 0 && absDed > 0) {
        rateTextElement.innerText = `(Calculated at ${formatCurrency(dailyRate)} / day)`;
    } else if (absDed > 0) {
        rateTextElement.innerText = '(Unmet Base / Missing Attendance)';
    } else {
        rateTextElement.innerText = '';
    }

    document.getElementById('modPenLateMins').innerText = `(${String(lateMins)} mins)`;
    const lateRateTextElement = document.getElementById('modPenLateRate');
    if (lateMins > 0 && lateDed > 0) {
        lateRateTextElement.innerText = `(Calculated at ${formatCurrency(minRate)} / min)`;
    } else {
        lateRateTextElement.innerText = '';
    }

    document.getElementById('modPenUtMins').innerText = `(${String(utMins)} mins)`;
    const utRateTextElement = document.getElementById('modPenUtRate');
    if (utMins > 0 && utDed > 0) {
        utRateTextElement.innerText = `(Calculated at ${formatCurrency(minRate)} / min)`;
    } else {
        utRateTextElement.innerText = '';
    }

    document.getElementById('modPenAbsAmt').innerText = formatCurrency(absDed);
    document.getElementById('modPenLateAmt').innerText = formatCurrency(lateDed);
    document.getElementById('modPenUtAmt').innerText = formatCurrency(utDed);
    document.getElementById('modPenTotal').innerText = formatCurrency(totalPenalties);

    const modal = document.getElementById('penaltiesModal');
    if (modal) {
        modal.style.display = 'flex'; 
    }
}

function viewDeductionBreakdown(element) { 
    document.getElementById('deductionEmpName').innerText = element.getAttribute('data-emp-name') || '----'; 
    const sss = safeFloat(element.getAttribute('data-sss'));
    const phil = safeFloat(element.getAttribute('data-phil'));
    const pagibig = safeFloat(element.getAttribute('data-pagibig'));
    const tax = safeFloat(element.getAttribute('data-tax'));
    const sLoan = safeFloat(element.getAttribute('data-sss-loan'));
    const hLoan = safeFloat(element.getAttribute('data-hdmf-loan'));
    const oLoan = safeFloat(element.getAttribute('data-oth-loan'));

    document.getElementById('modSss').innerText = formatCurrency(sss); 
    document.getElementById('modPhil').innerText = formatCurrency(phil); 
    document.getElementById('modPagibig').innerText = formatCurrency(pagibig); 
    document.getElementById('modTax').innerText = formatCurrency(tax); 
    document.getElementById('modSssLoan').innerText = formatCurrency(sLoan); 
    document.getElementById('modHdmfLoan').innerText = formatCurrency(hLoan); 
    document.getElementById('modOthLoan').innerText = formatCurrency(oLoan); 
    
    const total = sss + phil + pagibig + tax + sLoan + hLoan + oLoan; 
    document.getElementById('modTotalDed').innerText = formatCurrency(total); 
    document.getElementById('deductionModal').style.display = 'block'; 
}

function viewOvertimeBreakdown(element) { 
    document.getElementById('otEmpName').innerText = element.getAttribute('data-emp-name') || '----'; 
    const otPay = safeFloat(element.getAttribute('data-ot-pay'));
    const minRate = safeFloat(element.getAttribute('data-min-rate'));
    
    let otMins = 0;
    let displayRateText = "(0 mins)";
    if (minRate > 0 && otPay > 0) {
        otMins = Math.round(otPay / (minRate * 1.25));
        const otPremiumRate = minRate * 1.25;
        displayRateText = `(${otMins} mins @ ${formatCurrency(otPremiumRate)} / min)`;
    }

    const detailSpan = document.getElementById('modOtMinsDetail');
    if (detailSpan) {
        detailSpan.innerText = displayRateText;
        detailSpan.style.display = 'block';
    }

    document.getElementById('modOtTotalAmt').innerText = formatCurrency(otPay); 
    document.getElementById('modOtGrandTotal').innerText = formatCurrency(otPay); 
    document.getElementById('overtimeModal').style.display = 'flex'; 
}

function viewTeachingBreakdown(element) {
    document.getElementById('teachEmpName').innerText = element.getAttribute('data-emp') || '----';
    
    const tr = element.closest('tr');
    const startStr = tr.getAttribute('data-start');
    const endStr = tr.getAttribute('data-end');
    let weeks = 2; 
    if (startStr && endStr) {
        const start = new Date(startStr);
        const end = new Date(endStr);
        const diffDays = Math.round(Math.abs((end - start) / (1000 * 60 * 60 * 24))) + 1;
        if (diffDays > 16) weeks = 4; 
    }

    const totLec = safeFloat(element.getAttribute('data-tot-lec-units')) * weeks;
    const totLab = safeFloat(element.getAttribute('data-tot-lab-units')) * weeks;
    
    const basicPay = safeFloat(element.getAttribute('data-basic-pay'));
    const adminPay = safeFloat(element.getAttribute('data-admin-pay'));
    const holPay = safeFloat(element.getAttribute('data-hol-pay'));
    
    const makeUpPay = safeFloat(element.getAttribute('data-makeup-pay'));
    const makeUpLec = safeFloat(element.getAttribute('data-makeup-lec'));
    const makeUpLab = safeFloat(element.getAttribute('data-makeup-lab'));
    
    const excLec = safeFloat(element.getAttribute('data-exc-lec-units'));
    const excLab = safeFloat(element.getAttribute('data-exc-lab-units'));
    const lecHrs = safeFloat(element.getAttribute('data-lec-hrs')); 
    const labHrs = safeFloat(element.getAttribute('data-lab-hrs')); 
    
    const lecRate = safeFloat(element.getAttribute('data-lec-rate'));
    let labRate = safeFloat(element.getAttribute('data-lab-rate'));
    if (labRate === 0) labRate = lecRate * 0.75;

    const suspDed = parseFloat(element.getAttribute('data-susp-ded')) || 0;
    const suspDates = element.getAttribute('data-susp-dates') || '';
    
    const displayedBase = adminPay > 0 ? adminPay : basicPay;
    const isPartTime = (displayedBase === 0);

    let calcLecPay = safeFloat(element.getAttribute('data-lec-pay'));
    let calcLabPay = safeFloat(element.getAttribute('data-lab-pay'));
    const pureOverload = safeFloat(element.getAttribute('data-pure-overload'));
    
    const grandTotalTeachingPay = displayedBase + pureOverload;
    const grandTotUnits = totLec + totLab;
    const grandExcUnits = excLec + excLab;

    document.getElementById('modTeachTotLec').innerText = totLec.toFixed(1);
    document.getElementById('modTeachTotLab').innerText = totLab.toFixed(1);
    document.getElementById('modTeachExcLec').innerText = excLec.toFixed(1);
    document.getElementById('modTeachExcLab').innerText = excLab.toFixed(1);
    document.getElementById('modTeachGrandTotUnits').innerText = grandTotUnits.toFixed(1);
    document.getElementById('modTeachGrandExcUnits').innerText = grandExcUnits.toFixed(1);

    const noticeBox = document.getElementById('modTeachNoticeBox');
    const tbody = document.querySelector('#teachingModal .mod-table:nth-of-type(2) tbody');
    let basicRow = document.getElementById('modTeachBasicRow');
    if (!basicRow) {
        basicRow = document.createElement('tr');
        basicRow.id = 'modTeachBasicRow';
        tbody.insertBefore(basicRow, tbody.firstChild);
    }

    if (isPartTime) {
        noticeBox.style.backgroundColor = '#f8fafc';
        noticeBox.style.borderLeftColor = '#64748b';
        noticeBox.style.color = '#334155';
        noticeBox.innerHTML = `<strong><i class="fas fa-info-circle"></i> Part-Time Basis:</strong> Faculty has no fixed base salary. All <b>${grandTotUnits.toFixed(1)} units</b> are treated as payable teaching hours.`;
        basicRow.style.display = 'none';
    } else {
        if (lecHrs > 0 || labHrs > 0) {
            noticeBox.style.backgroundColor = '#e0f2fe';
            noticeBox.style.borderLeftColor = '#0284c7';
            noticeBox.style.color = '#0369a1';
            noticeBox.innerHTML = `<strong><i class="fas fa-check-circle"></i> Overload Generated:</strong> Base salary covers the required 15 hours/week. Overload math is detailed below based on the actual cutoff dates.`;
        } else {
            noticeBox.style.backgroundColor = '#fff3cd';
            noticeBox.style.borderLeftColor = '#ffc107';
            noticeBox.style.color = '#856404';
            noticeBox.innerHTML = `<strong><i class="fas fa-exclamation-triangle"></i> Underload / Exact Load:</strong> Faculty only has <b>${grandTotUnits.toFixed(1)} units</b> for this cutoff. Base Salary covers all hours taught. No overload pay generated.`;
        }
        basicRow.innerHTML = `<td class="label-cell">Base Pay <span style="font-size:11px; color:#666; display:block; margin-top:2px;">(Covers standard cutoff requirement)</span></td><td class="val-cell text-dark" style="font-weight: 600;">${formatCurrency(displayedBase)}</td>`;
        basicRow.style.display = 'table-row';
    }

    document.getElementById('modLecRateDetail').innerText = lecHrs > 0 ? `(${totLec.toFixed(1)} total units = ${lecHrs.toFixed(2)} payable hrs @ ₱${lecRate.toFixed(2)}/hr)` : (isPartTime ? '' : '(Fully Covered by Base Salary)');
    document.getElementById('modLabRateDetail').innerText = labHrs > 0 ? `(${totLab.toFixed(1)} total units = ${labHrs.toFixed(2)} payable hrs @ ₱${labRate.toFixed(2)}/hr)` : (isPartTime ? '' : '(Fully Covered by Base Salary)');
    
    document.getElementById('modTeachLecPay').innerText = formatCurrency(calcLecPay);
    document.getElementById('modTeachLabPay').innerText = formatCurrency(calcLabPay);
    document.getElementById('modTeachHolPay').innerText = formatCurrency(holPay);
    
    const makeUpPayEl = document.getElementById('modTeachMakeUpPay');
    const makeUpRateDetailEl = document.getElementById('modMakeUpRateDetail');
    if (makeUpPayEl) makeUpPayEl.innerText = formatCurrency(makeUpPay);
    if (makeUpRateDetailEl) {
        let makeUpStr = '';
        if (makeUpLec > 0 || makeUpLab > 0) {
            let parts = [];
            if (makeUpLec > 0) parts.push(`${makeUpLec.toFixed(2)} Lec hrs @ ₱${lecRate.toFixed(2)}/hr`);
            if (makeUpLab > 0) parts.push(`${makeUpLab.toFixed(2)} Lab hrs @ ₱${labRate.toFixed(2)}/hr`);
            makeUpStr = `(${parts.join(' + ')})`;
        }
        makeUpRateDetailEl.innerText = makeUpStr;
    }
    
    const suspDedEl = document.getElementById('modTeachSuspDed');
    const suspDatesEl = document.getElementById('modTeachSuspDates');
    if (suspDedEl) suspDedEl.innerText = '- ' + formatCurrency(suspDed);
    if (suspDatesEl) suspDatesEl.innerText = suspDates ? `Dates: ${suspDates}` : '';

    document.getElementById('modTeachTotAmt').innerText = formatCurrency(grandTotalTeachingPay);
    document.getElementById('teachingModal').style.display = 'flex';
}

function closeOtherIncomeModal() { document.getElementById('otherIncomeModal').style.display = 'none'; }
function closeEarningsModal() { document.getElementById('earningsModal').style.display = 'none'; }
function closePenaltiesModal() { document.getElementById('penaltiesModal').style.display = 'none'; }
function closeDeductionModal() { document.getElementById('deductionModal').style.display = 'none'; }
function closeOvertimeModal() { document.getElementById('overtimeModal').style.display = 'none'; }
function closeTeachingModal() { document.getElementById('teachingModal').style.display = 'none'; }

window.onclick = function(event) { 
    if (event.target.classList.contains('modal')) { 
        if (event.target.id !== 'confirmModal') {
            event.target.style.display = 'none'; 
        }
    } 
}

// ==========================================
// BATCH EMAIL LOGIC & CHECKBOXES
// ==========================================

function toggleSelectAll(source) {
    const visibleCheckboxes = document.querySelectorAll('.payroll-record-row:not([style*="display: none"]) .row-checkbox');
    visibleCheckboxes.forEach(cb => { cb.checked = source.checked; });

    const visibleDeptCheckboxes = document.querySelectorAll('.dept-group-body:not([style*="display: none"]) .dept-checkbox');
    visibleDeptCheckboxes.forEach(cb => { cb.checked = source.checked; });
}

function toggleDeptSelect(source) {
    const tbody = source.closest('.dept-group-body');
    const visibleCheckboxes = tbody.querySelectorAll('.payroll-record-row:not([style*="display: none"]) .row-checkbox');
    visibleCheckboxes.forEach(cb => { cb.checked = source.checked; });
    
    updateMasterCheckbox();
}

document.addEventListener('change', function(e) {
    if (e.target && e.target.classList.contains('row-checkbox')) {
        updateDeptCheckboxes(e.target);
        updateMasterCheckbox();
    }
});

function updateDeptCheckboxes(rowCb) {
    const tbody = rowCb.closest('.dept-group-body');
    const visibleCheckboxes = Array.from(tbody.querySelectorAll('.payroll-record-row:not([style*="display: none"]) .row-checkbox'));
    const deptCb = tbody.querySelector('.dept-checkbox');
    
    if (deptCb && visibleCheckboxes.length > 0) {
        deptCb.checked = visibleCheckboxes.every(cb => cb.checked);
    }
}

function updateMasterCheckbox() {
    const visibleCheckboxes = Array.from(document.querySelectorAll('.payroll-record-row:not([style*="display: none"]) .row-checkbox'));
    const selectAllCb = document.getElementById('selectAllCheckbox');
    
    if (selectAllCb && visibleCheckboxes.length > 0) {
        selectAllCb.checked = visibleCheckboxes.every(cb => cb.checked);
    }
}

async function sendPayslipEmails() {
    const start = document.getElementById('startDateFilter')?.value;
    const end = document.getElementById('endDateFilter')?.value;
    const dept = document.getElementById('registerDepartment')?.value || 'ALL';

    if (!start || !end) { 
        Swal.fire('Missing Dates', 'Please select the Cutoff Start Date and End Date to specify which payslips to send.', 'warning');
        return; 
    }

    const result = await Swal.fire({
        title: 'Email All Payslips?',
        text: 'Are you sure you want to email ALL VISIBLE payslips for this period?',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#0284c7',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Yes, send them all!'
    });

    if (!result.isConfirmed) return; 

    const btn = document.getElementById('btnEmailPayslips');
    const originalHtml = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending Emails...'; 
    btn.disabled = true;

    try {
        const params = new URLSearchParams({ start: start, end: end, department: dept });
        const response = await fetch(contextPath + `/api/payroll/email-all?${params.toString()}`, { method: 'POST' });
        if (!response.ok) throw new Error("Server Error");
        
        const resultMessage = await response.text(); 
        
        if (resultMessage.trim() === "0 sent successfully.") { 
            Swal.fire('No Records', 'No saved payrolls found for these dates. Did you generate them in Payroll Management first?', 'info');
        } else { 
            Swal.fire('Success!', resultMessage, 'success');
        }
    } catch (error) { 
        Swal.fire('Error', 'Failed to send payslip emails. Check console.', 'error');
    } finally { 
        btn.innerHTML = originalHtml; 
        btn.disabled = false; 
    }
}

async function sendSelectedPayslips() {
    const start = document.getElementById('startDateFilter')?.value;
    const end = document.getElementById('endDateFilter')?.value;
    
    if (!start || !end) { 
        Swal.fire('Missing Dates', 'Please select the Cutoff Start Date and End Date.', 'warning');
        return; 
    }

    const selectedIds = Array.from(document.querySelectorAll('.payroll-record-row:not([style*="display: none"]) .row-checkbox:checked'))
                             .map(cb => cb.value);

    if (selectedIds.length === 0) {
        Swal.fire('No Employees Selected', 'Please check at least one employee or department from the table to send their payslip.', 'info');
        return;
    }

    const result = await Swal.fire({
        title: 'Email Selected Payslips?',
        text: `Are you sure you want to email payslips to the ${selectedIds.length} explicitly selected employee(s)?`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#0284c7',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Yes, send them!'
    });

    if (!result.isConfirmed) return;

    const btn = document.getElementById('btnEmailSelected');
    const originalHtml = btn.innerHTML;
    if (btn) {
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';
        btn.disabled = true;
    }

    try {
        const params = new URLSearchParams();
        params.append('start', start);
        params.append('end', end);
        selectedIds.forEach(id => params.append('employeeIds', id));

        const response = await fetch(contextPath + `/api/payroll/email-selected`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: params.toString()
        });
        
        const resultMessage = await response.text(); 
        
        if (!response.ok) throw new Error(resultMessage);

        if (resultMessage.trim() === "0 sent successfully.") { 
            Swal.fire('No Records', 'No saved payrolls found for these dates for the selected employees.', 'info');
        } else { 
            Swal.fire('Success!', resultMessage, 'success');
        }
    } catch (error) { 
        Swal.fire('Error', 'Failed to send payslip emails: ' + error.message, 'error');
    } finally { 
        if (btn) {
            btn.innerHTML = originalHtml; 
            btn.disabled = false; 
        }
    }
}

async function sendSinglePayslip(btn) {
    const row = btn.closest('tr');
    const empId = btn.getAttribute('data-emp');
    const empName = btn.getAttribute('data-name');
    const startDate = row.getAttribute('data-start');
    const endDate = row.getAttribute('data-end');

    if (!empId || !startDate || !endDate) { 
        Swal.fire('Error', 'Missing date information.', 'error');
        return; 
    }

    const result = await Swal.fire({
        title: 'Send Payslip?',
        text: `Are you sure you want to email the payslip directly to ${empName}?`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#0284c7',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Yes, send it!'
    });

    if (!result.isConfirmed) return; 

    const originalHtml = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>'; 
    btn.disabled = true; 
    btn.style.color = '#888';

    try {
        const params = new URLSearchParams({ employeeQuery: empId, start: startDate, end: endDate });
        const response = await fetch(contextPath + `/api/payroll/email-single?${params.toString()}`, { method: 'POST' });
        const responseMsg = await response.text();
        
        if (!response.ok) throw new Error(responseMsg);
        
        Swal.fire('Sent!', responseMsg, 'success');
        
    } catch (error) { 
        Swal.fire('Failed', error.message, 'error');
    } finally { 
        btn.innerHTML = originalHtml; 
        btn.disabled = false; 
        btn.style.color = '#0284c7'; 
    }
}

// =================================================================
// LIVE SEARCH AUTOCOMPLETE MODAL LOGIC
// =================================================================

function openGlobalAdjustmentModal() {
    document.getElementById('adjSearchInput').value = ""; 
    document.getElementById('adjSearchInput').style.borderColor = "#e0f2fe";
    document.getElementById('adjEmployeeSelect').value = ""; 
    document.getElementById('customDropdownList').style.display = 'none';
    
    document.getElementById('adjustmentInputsArea').style.display = 'none'; 
    document.getElementById('btnSaveAdj').style.display = 'none'; 
    document.getElementById('adjNotice').style.display = 'flex'; 
    
    buildCustomDropdown(); 
    document.getElementById('adjustmentModal').style.display = 'block'; 
}

function closeAdjustmentModal() {
    document.getElementById('adjustmentModal').style.display = 'none';
    document.getElementById('customDropdownList').style.display = 'none';
}

function buildCustomDropdown() {
    const select = document.getElementById('adjEmployeeSelect');
    const list = document.getElementById('customDropdownList');
    list.innerHTML = '';
    
    Array.from(select.options).forEach((opt, index) => {
        if (index === 0) return; 
        
        let li = document.createElement('li');
        li.innerHTML = `<i class="fas fa-user" style="color:#94a3b8; margin-right:8px;"></i> ${opt.text}`;
        li.setAttribute('data-value', opt.value);
        
        li.style.padding = "12px 15px";
        li.style.cursor = "pointer";
        li.style.borderBottom = "1px solid #f1f5f9";
        li.style.fontSize = "14px";
        li.style.color = "#334155";
        li.style.fontWeight = "600";
        li.style.transition = "0.2s";
        
        li.onmouseover = function() { this.style.backgroundColor = '#f0f9ff'; this.style.color = '#0ea5e9'; };
        li.onmouseout = function() { this.style.backgroundColor = 'transparent'; this.style.color = '#334155'; };
        
        li.onclick = function() {
            document.getElementById('adjSearchInput').value = opt.text;
            document.getElementById('adjSearchInput').style.borderColor = "#0ea5e9";
            document.getElementById('adjEmployeeSelect').value = this.getAttribute('data-value');
            document.getElementById('customDropdownList').style.display = 'none';
            
            loadEmployeeAdjustmentData();
        };
        list.appendChild(li);
    });
}

function toggleCustomDropdown() {
    const list = document.getElementById('customDropdownList');
    const input = document.getElementById('adjSearchInput');
    if (list.style.display === 'none') {
        list.style.display = 'block';
        input.value = ''; 
        filterCustomDropdown(); 
    } else {
        list.style.display = 'none';
    }
}

function filterCustomDropdown() {
    const input = document.getElementById('adjSearchInput');
    const filter = input.value.toLowerCase();
    const list = document.getElementById('customDropdownList');
    const items = list.getElementsByTagName('li');
    
    list.style.display = 'block'; 
    
    let hasVisible = false;
    for (let i = 0; i < items.length; i++) {
        let text = items[i].textContent || items[i].innerText;
        if (text.toLowerCase().indexOf(filter) > -1) {
            items[i].style.display = "";
            hasVisible = true;
        } else {
            items[i].style.display = "none";
        }
    }
}

document.addEventListener('click', function(event) {
    const searchInput = document.getElementById('adjSearchInput');
    const list = document.getElementById('customDropdownList');
    const chevron = document.querySelector('.fa-chevron-down');
    if (searchInput && list) {
        if (event.target !== searchInput && event.target !== list && event.target !== chevron) {
            list.style.display = 'none';
        }
    }
});

// The Data Loader
function loadEmployeeAdjustmentData() {
    const selectBox = document.getElementById('adjEmployeeSelect');
    const selectedOption = selectBox.options[selectBox.selectedIndex];
    
    if (selectBox.value === "") return;

    document.getElementById('adjNotice').style.display = 'none'; 
    document.getElementById('adjustmentInputsArea').style.display = 'grid';
    document.getElementById('btnSaveAdj').style.display = 'inline-block';

    // Time & Attendance
    document.getElementById('adjLateMins').value = selectedOption.getAttribute('data-late-mins') || 0;
    document.getElementById('adjUtMins').value = selectedOption.getAttribute('data-ut-mins') || 0;
    document.getElementById('adjAbsDays').value = selectedOption.getAttribute('data-abs-days') || 0;
    
    // Financial Additions/Deductions
    document.getElementById('adjManualAdd').value = selectedOption.getAttribute('data-manual-add') || 0;
    document.getElementById('adjManualDed').value = selectedOption.getAttribute('data-manual-ded') || 0;
    
    // NEW: Separated Loans
    document.getElementById('adjSssLoan').value = selectedOption.getAttribute('data-sss-loan') || 0;
    document.getElementById('adjHdmfLoan').value = selectedOption.getAttribute('data-hdmf-loan') || 0;
    document.getElementById('adjOtherLoan').value = selectedOption.getAttribute('data-oth-loan') || 0;
}

// The API Saver
async function saveAdjustment() {
    const pid = document.getElementById('adjEmployeeSelect').value;
    if (!pid) return;

    const btn = document.getElementById('btnSaveAdj');
    const originalHtml = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
    btn.disabled = true;

    try {
        const params = new URLSearchParams({ 
            payrollId: pid, 
            lateMinutes: document.getElementById('adjLateMins').value || 0,
            undertimeMinutes: document.getElementById('adjUtMins').value || 0,
            totalAbsences: document.getElementById('adjAbsDays').value || 0,
            manualAddition: document.getElementById('adjManualAdd').value || 0,
            manualDeduction: document.getElementById('adjManualDed').value || 0,
            sssLoan: document.getElementById('adjSssLoan').value || 0,     // NEW
            hdmfLoan: document.getElementById('adjHdmfLoan').value || 0,   // NEW
            otherLoan: document.getElementById('adjOtherLoan').value || 0
        });
        
        const response = await fetch(contextPath + `/api/payroll/update-manual?${params.toString()}`, { method: 'POST' });
        if (!response.ok) throw new Error("Failed to update adjustments in database.");

        window.location.reload(); 
        
    } catch (error) {
        alert("Error updating adjustment: " + error.message);
        btn.innerHTML = originalHtml;
        btn.disabled = false;
    }
}

// =================================================================
// PBCOM EXPORT LOGIC
// =================================================================
function exportPBCOM() {
    const startDate = document.getElementById('startDateFilter')?.value;
    const endDate = document.getElementById('endDateFilter')?.value;
    const department = document.getElementById('registerDepartment')?.value || 'ALL';

    if (!startDate || !endDate) {
        Swal.fire({
            icon: 'warning',
            title: 'Select Dates First',
            text: 'You must select a Cutoff Start Date and End Date to generate the PBCOM export.',
            confirmButtonColor: '#0f172a'
        });
        return;
    }

    const params = new URLSearchParams({ start: startDate, end: endDate, department: department });
    window.location.href = contextPath + `/api/payroll/export/pbcom?${params.toString()}`;
}