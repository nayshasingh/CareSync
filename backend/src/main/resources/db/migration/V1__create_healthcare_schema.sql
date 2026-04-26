CREATE TABLE users (
    user_id INT AUTO_INCREMENT,
    name VARCHAR(255),
    role VARCHAR(255) CHECK (role IN ('DOCTOR', 'PATIENT')),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    phone CHAR(10),
    CONSTRAINT user_pk PRIMARY KEY (user_id)
);

CREATE TABLE appointments (
    appointment_id INT AUTO_INCREMENT,
    patient_id INT,
    doctor_id INT,
    time_slot_start DATETIME(6),
    time_slot_end DATETIME(6),
    status VARCHAR(255) CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT appointment_pk PRIMARY KEY (appointment_id),
    CONSTRAINT appointment_patient_fk FOREIGN KEY (patient_id) REFERENCES users (user_id) ON DELETE SET NULL,
    CONSTRAINT appointment_doctor_fk FOREIGN KEY (doctor_id) REFERENCES users (user_id) ON DELETE SET NULL
);

CREATE TABLE consultations (
    consultation_id INT AUTO_INCREMENT,
    appointment_id INT UNIQUE,
    notes VARCHAR(500),
    prescription VARCHAR(1000),
    CONSTRAINT consultation_pk PRIMARY KEY (consultation_id),
    CONSTRAINT consultation_appointment_fk FOREIGN KEY (appointment_id) REFERENCES appointments (appointment_id) ON DELETE CASCADE
);

CREATE TABLE availabilities (
    availability_id INT AUTO_INCREMENT,
    doctor_id INT,
    time_slot_start DATETIME(6),
    time_slot_end DATETIME(6),
    is_available BOOLEAN DEFAULT TRUE,
    CONSTRAINT availability_pk PRIMARY KEY (availability_id),
    CONSTRAINT availability_doctor_fk FOREIGN KEY (doctor_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_appointments_patient_status_start ON appointments (patient_id, status, time_slot_start);
CREATE INDEX idx_appointments_doctor_status_start ON appointments (doctor_id, status, time_slot_start);
CREATE INDEX idx_availabilities_doctor_start_end ON availabilities (doctor_id, time_slot_start, time_slot_end);
CREATE INDEX idx_availabilities_available_start ON availabilities (is_available, time_slot_start);
