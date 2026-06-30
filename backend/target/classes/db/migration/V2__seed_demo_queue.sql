insert into patient (id, display_id, age_band, created_at) values
    ('11111111-1111-1111-1111-111111111111', 'ER-1001', 'OLDER_ADULT', now() - interval '52 minutes'),
    ('22222222-2222-2222-2222-222222222222', 'ER-1002', 'ADULT', now() - interval '38 minutes'),
    ('33333333-3333-3333-3333-333333333333', 'ER-1003', 'CHILD', now() - interval '46 minutes'),
    ('44444444-4444-4444-4444-444444444444', 'ER-1004', 'ADULT', now() - interval '24 minutes'),
    ('55555555-5555-5555-5555-555555555555', 'ER-1005', 'ADULT', now() - interval '19 minutes'),
    ('66666666-6666-6666-6666-666666666666', 'ER-1006', 'OLDER_ADULT', now() - interval '64 minutes');

insert into intake (
    id,
    patient_id,
    arrival_timestamp,
    arrival_mode,
    chief_complaint,
    pain_level,
    temperature_c,
    heart_rate,
    systolic_pressure,
    diastolic_pressure,
    respiratory_rate,
    oxygen_saturation,
    chest_pain,
    breathing_difficulty,
    altered_mental_state,
    severe_bleeding,
    pregnancy,
    pediatric_risk,
    fall_or_trauma,
    immunocompromised,
    department,
    current_status,
    created_at,
    updated_at
) values
    ('aaaaaaaa-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', now() - interval '52 minutes', 'AMBULANCE', 'Severe breathing distress', 9, 38.40, 132, 88, 56, 32, 84, false, true, false, false, false, false, false, false, 'Emergency', 'WAITING', now() - interval '52 minutes', now() - interval '52 minutes'),
    ('aaaaaaaa-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', now() - interval '38 minutes', 'WALK_IN', 'Chest pain radiating to left arm', 8, 37.10, 112, 146, 92, 22, 94, true, false, false, false, false, false, false, false, 'Emergency', 'WAITING', now() - interval '38 minutes', now() - interval '38 minutes'),
    ('aaaaaaaa-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', now() - interval '46 minutes', 'WALK_IN', 'High fever with lethargy', 6, 39.30, 126, 98, 64, 24, 97, false, false, false, false, false, true, false, false, 'Pediatrics', 'IN_TRIAGE', now() - interval '46 minutes', now() - interval '46 minutes'),
    ('aaaaaaaa-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', now() - interval '24 minutes', 'TRANSFER', 'Possible wrist fracture after fall', 7, 36.80, 88, 122, 80, 18, 99, false, false, false, false, false, false, true, false, 'Orthopedics', 'WAITING', now() - interval '24 minutes', now() - interval '24 minutes'),
    ('aaaaaaaa-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', now() - interval '19 minutes', 'WALK_IN', 'Migraine and nausea', 5, 36.70, 82, 118, 76, 16, 99, false, false, false, false, false, false, false, false, 'Emergency', 'WAITING', now() - interval '19 minutes', now() - interval '19 minutes'),
    ('aaaaaaaa-6666-6666-6666-666666666666', '66666666-6666-6666-6666-666666666666', now() - interval '64 minutes', 'REFERRAL', 'Medication refill request', 1, 36.60, 74, 126, 78, 15, 98, false, false, false, false, false, false, false, false, 'General', 'WAITING', now() - interval '64 minutes', now() - interval '64 minutes');

insert into intake_structured_symptoms (intake_id, symptom) values
    ('aaaaaaaa-1111-1111-1111-111111111111', 'shortness of breath'),
    ('aaaaaaaa-1111-1111-1111-111111111111', 'low oxygen saturation'),
    ('aaaaaaaa-2222-2222-2222-222222222222', 'chest pain'),
    ('aaaaaaaa-3333-3333-3333-333333333333', 'fever'),
    ('aaaaaaaa-3333-3333-3333-333333333333', 'lethargy'),
    ('aaaaaaaa-4444-4444-4444-444444444444', 'fall'),
    ('aaaaaaaa-5555-5555-5555-555555555555', 'headache'),
    ('aaaaaaaa-6666-6666-6666-666666666666', 'medication refill');

insert into urgency_assessment (id, patient_id, intake_id, final_category, final_score, confidence_level, assessed_at) values
    ('bbbbbbbb-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-1111-1111-1111-111111111111', 'CRITICAL', 96, 'HIGH', now() - interval '51 minutes'),
    ('bbbbbbbb-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'aaaaaaaa-2222-2222-2222-222222222222', 'HIGH', 84, 'HIGH', now() - interval '37 minutes'),
    ('bbbbbbbb-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'aaaaaaaa-3333-3333-3333-333333333333', 'HIGH', 78, 'MEDIUM', now() - interval '45 minutes'),
    ('bbbbbbbb-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'aaaaaaaa-4444-4444-4444-444444444444', 'MEDIUM', 58, 'MEDIUM', now() - interval '23 minutes'),
    ('bbbbbbbb-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'aaaaaaaa-5555-5555-5555-555555555555', 'MEDIUM', 45, 'MEDIUM', now() - interval '18 minutes'),
    ('bbbbbbbb-6666-6666-6666-666666666666', '66666666-6666-6666-6666-666666666666', 'aaaaaaaa-6666-6666-6666-666666666666', 'LOW', 18, 'HIGH', now() - interval '63 minutes');

insert into urgency_score_factors (assessment_id, factor) values
    ('bbbbbbbb-1111-1111-1111-111111111111', 'Low oxygen saturation'),
    ('bbbbbbbb-1111-1111-1111-111111111111', 'High respiratory rate'),
    ('bbbbbbbb-2222-2222-2222-222222222222', 'Chest pain risk flag'),
    ('bbbbbbbb-3333-3333-3333-333333333333', 'Pediatric risk flag'),
    ('bbbbbbbb-4444-4444-4444-444444444444', 'Fall or trauma'),
    ('bbbbbbbb-5555-5555-5555-555555555555', 'Moderate pain'),
    ('bbbbbbbb-6666-6666-6666-666666666666', 'Stable vitals');

insert into urgency_red_flags (assessment_id, red_flag) values
    ('bbbbbbbb-1111-1111-1111-111111111111', 'Breathing difficulty'),
    ('bbbbbbbb-2222-2222-2222-222222222222', 'Chest pain'),
    ('bbbbbbbb-3333-3333-3333-333333333333', 'Pediatric fever');

insert into queue_entry (
    id,
    patient_id,
    intake_id,
    urgency_category,
    urgency_score,
    waiting_since,
    department,
    status,
    staff_escalated,
    created_at,
    updated_at
) values
    ('cccccccc-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-1111-1111-1111-111111111111', 'CRITICAL', 96, now() - interval '52 minutes', 'Emergency', 'WAITING', false, now() - interval '52 minutes', now() - interval '52 minutes'),
    ('cccccccc-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'aaaaaaaa-2222-2222-2222-222222222222', 'HIGH', 84, now() - interval '38 minutes', 'Emergency', 'WAITING', false, now() - interval '38 minutes', now() - interval '38 minutes'),
    ('cccccccc-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'aaaaaaaa-3333-3333-3333-333333333333', 'HIGH', 78, now() - interval '46 minutes', 'Pediatrics', 'IN_TRIAGE', true, now() - interval '46 minutes', now() - interval '46 minutes'),
    ('cccccccc-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'aaaaaaaa-4444-4444-4444-444444444444', 'MEDIUM', 58, now() - interval '24 minutes', 'Orthopedics', 'WAITING', false, now() - interval '24 minutes', now() - interval '24 minutes'),
    ('cccccccc-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'aaaaaaaa-5555-5555-5555-555555555555', 'MEDIUM', 45, now() - interval '19 minutes', 'Emergency', 'WAITING', false, now() - interval '19 minutes', now() - interval '19 minutes'),
    ('cccccccc-6666-6666-6666-666666666666', '66666666-6666-6666-6666-666666666666', 'aaaaaaaa-6666-6666-6666-666666666666', 'LOW', 18, now() - interval '64 minutes', 'General', 'WAITING', false, now() - interval '64 minutes', now() - interval '64 minutes');
