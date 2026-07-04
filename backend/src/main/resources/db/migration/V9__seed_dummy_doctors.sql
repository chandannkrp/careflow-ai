insert into staff_user (id, display_name, role, department, active, created_at, staff_code, specialty) values
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Dr. Anika Shah', 'DOCTOR', 'Emergency', true, now(), 'DOCTOR-EM', 'Emergency Medicine'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Dr. Mateo Rivera', 'DOCTOR', 'Emergency', true, now(), 'DOCTOR-CARD', 'Cardiology'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Dr. Priya Nair', 'DOCTOR', 'Emergency', true, now(), 'DOCTOR-PEDS', 'Pediatrics'),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Dr. Jordan Kim', 'DOCTOR', 'Emergency', true, now(), 'DOCTOR-ORTHO', 'Orthopedics'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Dr. Sofia Martinez', 'DOCTOR', 'Emergency', true, now(), 'DOCTOR-PULM', 'Pulmonology')
on conflict (id) do update set
    display_name = excluded.display_name,
    role = excluded.role,
    department = excluded.department,
    active = excluded.active,
    staff_code = excluded.staff_code,
    specialty = excluded.specialty;
