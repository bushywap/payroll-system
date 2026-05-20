# Payroll ↔ HR integration — AI context (paste this in payroll Cursor chat)

Use this file as the **single source of truth** when editing **payroll-system** (`bushywap/payroll-system`). Do **not** re-implement HR features here.

---

## What we are building

**Option A — one MySQL database, two Spring Boot apps:**

| App | Port | Context path | Role |
|-----|------|--------------|------|
| **EAC HR** (`eac-hr` repo, sibling folder) | **8080** | `/` | Source of truth: hire employees, TCMS attendance import, leave/OT/shifts |
| **Payroll** (this repo) | **8081** | `/payroll-system` | Read HR data from DB → compute pay → write `payroll` table, register, payslips |

Both use **`jdbc:mysql://127.0.0.1:3306/eac_hr_db`** (see `application.properties`).  
`spring.jpa.hibernate.ddl-auto=none` — **HR owns schema**; payroll must not auto-alter tables.

**Payroll login** uses table **`users`** (email + BCrypt password), not HR **`app_users`**. After switching DB, run `sql/seed_payroll_login_user.sql` if login returns “Bad credentials” (empty `users` table).

---

## Employee ID rules (critical)

- Official PK: **`employee.employee_id`** (varchar), format **`1-00001`** … **`1-00010`** (EAC demo roster).
- **`attendance.employee_id`** = same string (imported via HR `/hr/biometrics`).
- **`payroll.employee_id`** = FK to `employee.employee_id`.
- Legacy ids like **`EMP-4`** may exist in old payroll rows; demo roster is **`1-%`** only (10 employees in current demo DB).
- JPA `Employee` entity: PK field is **`id`** mapped to column `employee_id`. Add/use **`getEmployeeId()`** returning `id` for JSON/Thymeleaf (`employeeId` in UI).

---

## What HR already does (do NOT duplicate in payroll)

- Employee master / 201 fields / `seed_demo_school_employees.sql`
- TCMS CSV → `attendance` (~1510 rows, Jan–May 2026): `sample-data/tcms_attendance_4_payroll_cycles.csv`
- Leave, OB, OT approvals in HR tables (`leave_requests`, `eac_official_business_request`, `eac_overtime_request`)
- SQL scripts live in **sibling repo** `../eac-hr/sql/`:
  - `align_demo_employees_for_payroll.sql` — sets `employee.designation` for payroll tabs
  - `verify_hr_payroll_connection.sql` — proves HR → payroll data path
  - `clear_demo_attendance_before_reimport.sql`

---

## What we are editing in payroll (this repo)

### Already changed (verify + extend, don’t revert)

1. **`src/main/java/com/capstone/payroll/model/Employee.java`**
   - PK `employee_id`; `getAttendanceKey()`; **`getEmployeeId()`** alias for UI.

2. **`src/main/java/com/capstone/payroll/model/EmpDesignation.java`**
   - `@Column(name = "id")` on PK (DB column is lowercase `id`, not `Id`).

3. **`src/main/java/com/capstone/payroll/repository/AttendanceRepository.java`**
   - JPQL joins: **`a.employeeId = e.id`** (was wrongly `e.employeeId`).

4. **`src/main/java/com/capstone/payroll/repository/EmployeeRepository.java`**
   - `findActiveEacRoster()`, `findByIdStartingWith`.

5. **`src/main/java/com/capstone/payroll/service/AttendanceService.java`**
   - `mergeEacRosterMissingFromDesignationFilter()` — include `1-%` employees when designation filter excludes them.

6. **`src/main/java/com/capstone/payroll/controller/EmployeeController.java`**
   - `/compensation`: sorted `findAll()`, `employeeCount`, `eacDemoCount`.

7. **`src/main/resources/templates/compensation.html`**
   - Tab type: **`designation.teaching`** / `employee_status`, NOT department name contains `"School"` (that hid everyone on Non-Faculty tab).
   - Search hint; filter hint for `1-00001` on wrong tab; default **All Employees** on load.
   - Display `emp.id` for EAC id column.

8. **`src/main/java/com/capstone/payroll/config/SecurityConfig.java`**
   - Allow `/compensation`, `/employees`, `/api/employees/**` for authenticated admin.

### Known UX gotchas (support, don’t “fix” in HR)

- **Compensation Masterlist** = `/compensation` (nav label “Employee Masterlist”).
- **`1-00001`, `1-00007`, `1-00010`** = **Non-Faculty** (staff designation). They **do not show** on **Faculty** tab — use **All Employees** or **Non-Faculty**.
- **Payroll Register** only lists employees who have rows in **`payroll`** after **Calculate all → Send**; not the full employee list.
- Restart required after Java/template changes: port **8081** often already in use (`taskkill` old `java.exe` first).

---

## Verified working (MySQL `eac_hr_db`)

```sql
-- End-to-end proof for 1-00001, Apr 1-15 2026:
SELECT e.employee_id, e.basic_salary AS hr_basic_salary,
       (SELECT COUNT(*) FROM attendance a
        WHERE a.employee_id = e.employee_id
          AND a.`date` BETWEEN '2026-04-01' AND '2026-04-15') AS hr_attendance_days,
       (SELECT p.net_pay FROM payroll p
        WHERE p.employee_id = e.employee_id
          AND p.pay_period_start = '2026-04-01'
          AND p.pay_period_end = '2026-04-15' LIMIT 1) AS payroll_net
FROM employee e WHERE e.employee_id = '1-00001';
-- Example: 28000 | 15 | 12629.01
```

Demo counts: **10** employees `1-%`, **1510** attendance rows, payroll rows after processing.

---

## Payroll pages vs HR data

| Payroll URL | Reads from DB |
|-------------|----------------|
| `/compensation` | `employee` (all via `findAll()`) |
| `/attendance` | `attendance` + `employee` (+ leave/holidays) |
| `/payroll` | `employee`, `attendance`, computes → preview |
| `/payroll_register` | `payroll` join `employee` |
| `/payslips` | `payroll` |

Semi-monthly demo periods: `2026-04-01`–`15`, `16`–`30`, `2026-05-01`–`15`, `16`–`31`.

---

## Tasks still OK in payroll (if user asks)

- Fix any remaining UI/API using `employee.employeeId` instead of `employee.id`.
- Ensure **Calculate all / Send** includes all `1-%` active employees.
- Optional: filter register default to EAC demo; link “Open HR” to `http://localhost:8080/hr/employees`.
- **Do not** add employee hiring, TCMS import, or leave approval workflows here.
- **Do not** set `ddl-auto=update` against production/shared DB.

---

## Run commands

```powershell
cd C:\Users\atash\Downloads\eac-hr\payroll-system
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Open: http://localhost:8081/payroll-system/compensation

---

## Sibling HR repo

`C:\Users\atash\Downloads\eac-hr\eac-hr` — GitHub `bushywap/HR`.  
Integration doc there: `sql/PAYROLL_OPTION_A.txt`, `sample-data/IMPORT_TCMS_ATTENDANCE.txt`.

---

**When user says “HR employees not in payroll”:** check (1) tab Faculty vs Non-Faculty, (2) payroll restarted with latest `compensation.html`, (3) `SELECT COUNT(*) FROM employee WHERE employee_id LIKE '1-%'`, (4) register vs compensation confusion.
