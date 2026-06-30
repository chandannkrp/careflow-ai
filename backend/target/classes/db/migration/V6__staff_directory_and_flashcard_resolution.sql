alter table staff_user add column specialty varchar(255);

update staff_user set specialty = 'Emergency Medicine' where staff_code = 'DOCTOR-01';
update staff_user set specialty = 'Pediatrics' where staff_code = 'DOCTOR-PEDS';

alter table patient_flashcard add column resolved boolean not null default false;
alter table patient_flashcard add column resolved_by varchar(255);
alter table patient_flashcard add column resolved_at timestamp with time zone;

create index idx_staff_user_role_department_specialty on staff_user (role, department, specialty, active);
create index idx_flashcard_resolved on patient_flashcard (resolved, updated_at);
