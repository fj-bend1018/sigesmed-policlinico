package com.webapplication.PoliclinicoSagradoCorazon.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.webapplication.PoliclinicoSagradoCorazon.dto.PacienteDTO;
import com.webapplication.PoliclinicoSagradoCorazon.model.Paciente1;

@Repository
public class PacienteDAO {

    @Autowired
    private JdbcTemplate jdbc;

    public void insertar(PacienteDTO dto, int usuarioId) {
        String sql = "INSERT INTO paciente (usuario_id, nombre, apellido_paterno, apellido_materno, dni, fecha_nacimiento, sexo, correo, celular) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbc.update(sql,
                usuarioId,
                dto.getNombre(),
                dto.getApellidoPaterno(),
                dto.getApellidoMaterno(),
                dto.getDni(),
                dto.getFechaNacimiento(),
                dto.getSexo(),
                dto.getCorreo(),
                dto.getCelular());
    }

    public PacienteDTO buscarPorDni(String dni) {
        String sql = "SELECT * FROM paciente WHERE dni = ?";
        List<PacienteDTO> lista = jdbc.query(sql, new BeanPropertyRowMapper<>(PacienteDTO.class), dni);
        return lista.isEmpty() ? null : lista.get(0);
    }

    public void actualizarDatosContacto(PacienteDTO paciente) {
        String sql = "UPDATE paciente SET correo = ?, celular = ? WHERE id = ?";
        jdbc.update(sql, paciente.getCorreo(), paciente.getCelular(), paciente.getId());
    }

    public Paciente1 obtenerPorId(int idppaciente) {
        String sql = "SELECT * FROM paciente WHERE id = ?";
        return jdbc.queryForObject(sql, new BeanPropertyRowMapper<>(Paciente1.class), idppaciente);
    }
}
