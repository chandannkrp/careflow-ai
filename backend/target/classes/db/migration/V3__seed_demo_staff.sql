insert into staff_user (id, display_name, role, department, active, created_at) values
    ('77777777-7777-7777-7777-777777777777', 'Nina Patel', 'INTAKE_STAFF', 'Emergency', true, now()),
    ('88888888-8888-8888-8888-888888888888', 'Omar Reed', 'TRIAGE_NURSE', 'Emergency', true, now()),
    ('99999999-9999-9999-9999-999999999999', 'Maya Chen', 'CHARGE_NURSE', 'Emergency', true, now())
on conflict (id) do nothing;
