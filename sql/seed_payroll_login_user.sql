-- Payroll login accounts (separate from HR app_users).
-- Run against shared DB: mysql -u root -p eac_hr_db < sql/seed_payroll_login_user.sql
-- Password below is BCrypt for: 123123

USE eac_hr_db;

INSERT INTO users (email, enabled, first_name, last_name, password, role)
SELECT
    'ralphaveno@gmail.com',
    1,
    'Ralph',
    'Aveno',
    '$2a$10$dH3uK7FKGuCCrZEF8Gr1ouMD/wugONnC/mI/ENwBkbIwWAEOgZZHm',
    'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'ralphaveno@gmail.com');
