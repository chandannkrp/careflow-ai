create table system_agent (
    id uuid primary key,
    code varchar(100) not null unique,
    name varchar(255) not null,
    task_type varchar(100) not null,
    description varchar(1000) not null,
    instructions text,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table patient_thread_comment (
    id uuid primary key,
    patient_id uuid not null references patient(id),
    intake_id uuid not null references intake(id),
    author_name varchar(255) not null,
    body text not null,
    created_at timestamp with time zone not null
);

create table patient_thread_attachment (
    id uuid primary key,
    comment_id uuid not null references patient_thread_comment(id),
    file_name varchar(255) not null,
    file_type varchar(255),
    url text not null,
    created_at timestamp with time zone not null
);

insert into system_agent (id, code, name, task_type, description, instructions, active, created_at, updated_at) values
    ('10101010-1010-1010-1010-101010101010', 'ASSIGNMENT_AGENT', 'Assignment Agent', 'ASSIGNMENT', 'Matches patients to doctors and care sections.', 'Use symptoms, risk flags, vitals, department, and doctor specialty to assign the most relevant doctor.', true, now(), now()),
    ('20202020-2020-2020-2020-202020202020', 'PRIORITY_AGENT', 'Priority Agent', 'PRIORITY', 'Keeps queue order sensitive to urgency and wait thresholds.', 'Escalate visibility when patients approach 30 minutes or exceed 40 minutes of waiting.', true, now(), now()),
    ('30303030-3030-3030-3030-303030303030', 'NOTIFICATION_AGENT', 'Notification Agent', 'NOTIFICATION', 'Writes timeline notifications for involved care teams.', 'Notify assigned doctor, intake staff, and care-team viewers when assignment or status changes.', true, now(), now()),
    ('40404040-4040-4040-4040-404040404040', 'DISCOVERY_BRIEF_AGENT', 'Discovery Brief Agent', 'BRIEFING', 'Creates a short natural-language patient discovery brief.', 'Summarize chief complaint, symptoms, risk flags, vitals, urgency, and next action in plain clinical operations language.', true, now(), now())
on conflict (code) do nothing;

create index idx_thread_comment_patient on patient_thread_comment (patient_id, created_at);
create index idx_attachment_comment on patient_thread_attachment (comment_id, created_at);
create index idx_system_agent_task_active on system_agent (task_type, active);
