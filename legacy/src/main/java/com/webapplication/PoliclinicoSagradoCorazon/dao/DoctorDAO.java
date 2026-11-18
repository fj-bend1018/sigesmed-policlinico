package com.webapplication.PoliclinicoSagradoCorazon.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.webapplication.PoliclinicoSagradoCorazon.dto.DoctorDTO;
import com.webapplication.PoliclinicoSagradoCorazon.model.Doctor;

@Repository
public class DoctorDAO {

    @Autowired
    private JdbcTemplate jdbc;

    public List<Doctor> listarTodos() {
        String sql = "SELECT * FROM doctor WHERE estado = 'ACTIVO'";
        return jdbc.query(sql, new BeanPropertyRowMapper<>(Doctor.class));
    }

    public Doctor buscarPorId(int id) {
        try {
            String sql = "SELECT * FROM doctor WHERE id = ?";
            return jdbc.queryForObject(sql, new BeanPropertyRowMapper<>(Doctor.class), id);
        } catch (Exception e) {
            return null;
        }
    }

    public int obetenerEspecialidadID(int doctorID) {
        String sql = "SELECT especialidad_id FROM doctor WHERE id = ?";
        Integer espId = jdbc.queryForObject(sql, Integer.class, doctorID);
        return espId != null ? espId : 0;
    }

    public List<DoctorDTO> obtenerTodosLosDoctores() {
        String sql = """
                    SELECT d.id,
                        d.nombre,
                        e.nombre AS especialidad,
                        d.cmp,
                        d.estado
                FROM doctor d
                JOIN especialidad e ON d.especialidad_id = e.id
                """;
        return jdbc.query(sql, new BeanPropertyRowMapper<>(DoctorDTO.class));
    }

    public List<DoctorDTO> filtrarDoctores(String nombreFiltro, String especialidadFiltro) {
        // Consulta SQL base
        StringBuilder sql = new StringBuilder("""
                SELECT d.id,
                        d.nombre,
                        e.nombre AS especialidad,
                        d.cmp,
                        d.estado
                FROM doctor d
                JOIN especialidad e ON d.especialidad_id = e.id
                """);

        // Lista para almacenar los parámetros
        List<Object> params = new ArrayList<>();

        // Condiciones para los filtros
        List<String> conditions = new ArrayList<>();

        if (nombreFiltro != null && !nombreFiltro.isBlank()) {
            conditions.add("LOWER(d.nombre) LIKE ?");
            params.add("%" + nombreFiltro.toLowerCase() + "%");
        }

        if (especialidadFiltro != null && !especialidadFiltro.isBlank()) {
            conditions.add("LOWER(e.nombre) = ?");
            params.add(especialidadFiltro.toLowerCase());
        }

        // Agregar WHERE si hay condiciones
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        // Ejecutar la consulta
        return jdbc.query(
                sql.toString(),
                params.toArray(),
                new BeanPropertyRowMapper<>(DoctorDTO.class));
    }

    public DoctorDTO obtenerPorId(int id) {
        String sql = """
                    SELECT d.id,
                        d.nombre,
                        e.nombre AS especialidad,
                        d.cmp,
                        e.id AS especialidadId
                FROM doctor d
                JOIN especialidad e ON d.especialidad_id = e.id
                WHERE d.id = ?
                """;
        return jdbc.queryForObject(sql, new BeanPropertyRowMapper<>(DoctorDTO.class), id);
    }

    public void insertar(DoctorDTO doctor, int especialidadID) {
        String sql = "INSERT INTO doctor (nombre, especialidad_id, cmp, imagen) VALUES (?, ?, ?, ?)";

        jdbc.update(
                sql,
                doctor.getNombre(),
                especialidadID,
                doctor.getCmp(),
                null // o "" si prefieres que no sea NULL
        );
    }

    public void actualizar(DoctorDTO doctor, int especialidadID) {
        String sql = "UPDATE doctor SET nombre = ?, cmp = ?, especialidad_id = ? WHERE id = ?";

        jdbc.update(sql,
                doctor.getNombre(),
                doctor.getCmp(),
                especialidadID,
                doctor.getId());
    }

    public int obtenerIDporNombre(String nombreDoctor) {
        String sql = "SELECT id FROM doctor WHERE nombre = ?";
        Integer id = jdbc.queryForObject(sql, Integer.class, nombreDoctor);
        return id != null ? id : 0;
    }

    public void cambiarActivado(int iddoctor){
        String sql = "UPDATE doctor SET estado = 'ACTIVO' WHERE id = ?";
        jdbc.update(sql,iddoctor);
    }

    public void cambiarDesactivado(int iddoctor){
        String sql = "UPDATE doctor SET estado = 'INACTIVO' WHERE id = ?";
        jdbc.update(sql,iddoctor);
    }

    /*
     * Metodo en revision: recordar que si se eliminara se eliminarian todos los
     * horarios y citas vinculados; mejor opcion es cambiar estado a INACTIVO.
     * public void eliminar(int id){
     * String sql = "DELETE FROM doctor WHERE id = ?";
     * jdbc.update(sql, id);
     * }
     */
}
