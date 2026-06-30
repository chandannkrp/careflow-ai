create table care_team_assignment (
    id uuid primary key,
    patient_id uuid not null references patient(id),
    intake_id uuid not null references intake(id),
    assigned_doctor_id uuid not null references staff_user(id),
    department varchar(255) not null,
    assignment_reason varchar(500) not null,
    active boolean not null,
    assigned_at timestamp with time zone not null
);

create table patient_flashcard (
    id uuid primary key,
    patient_id uuid not null references patient(id),
    intake_id uuid not null references intake(id),
    assigned_staff_id uuid references staff_user(id),
    audience_role varchar(255) not null,
    department varchar(255) not null,
    title varchar(255) not null,
    summary varchar(1000) not null,
    action_label varchar(255) not null,
    urgency_category varchar(255) not null,
    urgency_score integer not null,
    status varchar(255) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table patient_timeline_event (
    id uuid primary key,
    patient_id uuid not null references patient(id),
    intake_id uuid not null references intake(id),
    actor_staff_user_id uuid references staff_user(id),
    department varchar(255) not null,
    event_type varchar(255) not null,
    title varchar(255) not null,
    description varchar(1000) not null,
    source varchar(255) not null,
    created_at timestamp with time zone not null
);

insert into staff_user (id, display_name, role, department, active, created_at, staff_code) values
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Dr. Avery Singh', 'DOCTOR', 'Emergency', true, now(), 'DOCTOR-01'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Dr. Elena Brooks', 'DOCTOR', 'Pediatrics', true, now(), 'DOCTOR-PEDS')
on conflict (id) do nothing;

create index idx_care_team_assignment_doctor on care_team_assignment (assigned_doctor_id, active, assigned_at);
create index idx_flashcard_staff on patient_flashcard (assigned_staff_id, updated_at);
create index idx_flashcard_role_department on patient_flashcard (audience_role, department, updated_at);
create index idx_timeline_patient on patient_timeline_event (patient_id, created_at);
create index idx_timeline_department on patient_timeline_event (department, created_at);
