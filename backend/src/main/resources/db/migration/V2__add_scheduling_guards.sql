ALTER TABLE availabilities
    ADD CONSTRAINT uk_availabilities_doctor_slot UNIQUE (doctor_id, time_slot_start, time_slot_end);

CREATE INDEX idx_appointments_booked_patient_overlap
    ON appointments (patient_id, status, time_slot_start, time_slot_end);

CREATE INDEX idx_appointments_booked_doctor_overlap
    ON appointments (doctor_id, status, time_slot_start, time_slot_end);
