create table intake_vector_document (
    id uuid primary key,
    intake_id uuid not null unique references intake(id),
    patient_id uuid not null references patient(id),
    patient_display_id varchar(255) not null,
    content text not null,
    embedding text not null,
    updated_at timestamp with time zone not null
);

create index idx_intake_vector_patient_display_id on intake_vector_document (patient_display_id);
