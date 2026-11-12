-- La especialidad elegida se conserva en cada cita para validar reprogramaciones.
-- Es nullable para no inventar la especialidad de citas históricas asociadas a
-- médicos con más de una especialidad.
ALTER TABLE cita
    ADD COLUMN especialidad_id BIGINT NULL;

ALTER TABLE cita
    ADD CONSTRAINT fk_cita_especialidad
    FOREIGN KEY (especialidad_id) REFERENCES especialidad(id);

-- Solo se completa automáticamente cuando el médico tiene una única especialidad.
UPDATE cita AS c
SET especialidad_id = de.especialidad_id
FROM horario AS h
JOIN doctor_especialidad AS de ON de.doctor_id = h.doctor_id
WHERE c.horario_id = h.id
  AND 1 = (
      SELECT COUNT(*)
      FROM doctor_especialidad AS de_count
      WHERE de_count.doctor_id = h.doctor_id
  );

CREATE INDEX ix_cita_especialidad ON cita(especialidad_id);
