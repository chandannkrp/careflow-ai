alter table patient alter column contact_metadata type text using contact_metadata::text;
alter table intake alter column symptom_notes type text using symptom_notes::text;
alter table intake alter column staff_notes type text using staff_notes::text;
alter table urgency_assessment alter column structured_symptom_summary type text using structured_symptom_summary::text;
alter table urgency_assessment alter column staff_facing_explanation type text using staff_facing_explanation::text;
alter table priority_override alter column note type text using note::text;
