alter table staff_user add column staff_code varchar(64);

update staff_user set staff_code = 'INTAKE-01' where id = '77777777-7777-7777-7777-777777777777';
update staff_user set staff_code = 'TRIAGE-01' where id = '88888888-8888-8888-8888-888888888888';
update staff_user set staff_code = 'CHARGE-01' where id = '99999999-9999-9999-9999-999999999999';

update staff_user
set staff_code = concat('STAFF-', substring(cast(id as varchar), 1, 8))
where staff_code is null;

alter table staff_user alter column staff_code set not null;
alter table staff_user add constraint uk_staff_user_staff_code unique (staff_code);
