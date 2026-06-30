create table patient (
    id uuid primary key,
    display_id varchar(255) not null unique,
    age_band varchar(255) not null,
    contact_metadata oid,
    created_at timestamp with time zone not null
);

create table staff_user (
    id uuid primary key,
    display_name varchar(255) not null,
    role varchar(255) not null,
    department varchar(255),
    active boolean not null,
    created_at timestamp with time zone not null
);

create table intake (
    id uuid primary key,
    patient_id uuid not null references patient(id),
    arrival_timestamp timestamp with time zone not null,
    arrival_mode varchar(255) not null,
    chief_complaint varchar(255) not null,
    symptom_notes oid,
    pain_level integer not null,
    temperature_c numeric(38, 2),
    heart_rate integer,
    systolic_pressure integer,
    diastolic_pressure integer,
    respiratory_rate integer,
    oxygen_saturation integer,
    chest_pain boolean not null,
    breathing_difficulty boolean not null,
    altered_mental_state boolean not null,
    severe_bleeding boolean not null,
    pregnancy boolean not null,
    pediatric_risk boolean not null,
    fall_or_trauma boolean not null,
    immunocompromised boolean not null,
    department varchar(255) not null,
    current_status varchar(255) not null,
    staff_notes oid,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table intake_structured_symptoms (
    intake_id uuid not null references intake(id),
    symptom varchar(255) not null
);

create table urgency_assessment (
    id uuid primary key,
    patient_id uuid not null references patient(id),
    intake_id uuid not null references intake(id),
    final_category varchar(255) not null,
    final_score integer not null,
    suggested_category varchar(255),
    suggested_score integer,
    structured_symptom_summary oid,
    staff_facing_explanation oid,
    confidence_level varchar(255),
    assessed_at timestamp with time zone not null
);

create table urgency_score_factors (
    assessment_id uuid not null references urgency_assessment(id),
    factor varchar(255) not null
);

create table urgency_red_flags (
    assessment_id uuid not null references urgency_assessment(id),
    red_flag varchar(255) not null
);

create table urgency_missing_details (
    assessment_id uuid not null references urgency_assessment(id),
    missing_detail varchar(255) not null
);

create table queue_entry (
    id uuid primary key,
    patient_id uuid not null unique references patient(id),
    intake_id uuid not null references intake(id),
    urgency_category varchar(255) not null,
    urgency_score integer not null,
    waiting_since timestamp with time zone not null,
    department varchar(255) not null,
    status varchar(255) not null,
    staff_escalated boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table priority_override (
    id uuid primary key,
    patient_id uuid not null references patient(id),
    staff_user_id uuid not null references staff_user(id),
    previous_category varchar(255) not null,
    previous_score integer not null,
    new_category varchar(255) not null,
    new_score integer not null,
    reason varchar(255) not null,
    note oid,
    created_at timestamp with time zone not null
);

create index idx_queue_entry_priority on queue_entry (urgency_category, staff_escalated, urgency_score, waiting_since);
create index idx_intake_patient on intake (patient_id);
create index idx_assessment_patient on urgency_assessment (patient_id);
