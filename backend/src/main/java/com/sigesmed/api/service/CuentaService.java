package com.sigesmed.api.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigesmed.api.domain.entity.Administrador;
import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.Recepcionista;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.dto.cuenta.ActualizarPerfilRequest;
import com.sigesmed.api.dto.cuenta.PerfilResponse;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.AdministradorRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.PacienteRepository;
import com.sigesmed.api.repository.RecepcionistaRepository;
import com.sigesmed.api.repository.UsuarioRepository;

@Service
public class CuentaService {
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final RecepcionistaRepository recepcionistaRepository;
    private final AdministradorRepository administradorRepository;
    private final DoctorRepository doctorRepository;

    public CuentaService(UsuarioRepository usuarioRepository, PacienteRepository pacienteRepository,
            RecepcionistaRepository recepcionistaRepository, AdministradorRepository administradorRepository,
            DoctorRepository doctorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.recepcionistaRepository = recepcionistaRepository;
        this.administradorRepository = administradorRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtener(Long usuarioId) {
        Usuario usuario = usuario(usuarioId);
        if (usuario.getPaciente() != null) {
            Paciente p = usuario.getPaciente();
            return new PerfilResponse(usuario.getRol(), usuario.getDni(), p.getNombre(), p.getApellidoPaterno(),
                    p.getApellidoMaterno(), p.getCorreo(), p.getCelular(), p.getDireccion());
        }
        if (usuario.getRecepcionista() != null) {
            Recepcionista r = usuario.getRecepcionista();
            return new PerfilResponse(usuario.getRol(), usuario.getDni(), r.getNombre(), r.getApellidoPaterno(),
                    r.getApellidoMaterno(), r.getCorreo(), r.getCelular(), r.getDireccion());
        }
        Administrador a = usuario.getAdministrador();
        if (a != null) {
            return new PerfilResponse(usuario.getRol(), usuario.getDni(), null, null, null,
                    a.getCorreo(), a.getCelular(), a.getDireccion());
        }
        if (usuario.getDoctor() != null) {
            var d = usuario.getDoctor();
            return new PerfilResponse(usuario.getRol(), usuario.getDni(), d.getNombre(), null, null,
                    d.getCorreo(), null, null);
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "Perfil no encontrado");
    }

    @Transactional
    public PerfilResponse actualizar(Long usuarioId, ActualizarPerfilRequest request) {
        Usuario usuario = usuario(usuarioId);
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        List<Usuario> coincidencias = usuarioRepository.findByIdentifier(correo);
        if (coincidencias.stream().anyMatch(item -> !item.getId().equals(usuarioId))) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está asociado a otra cuenta");
        }
        String celular = blankToNull(request.celular());
        String direccion = blankToNull(request.direccion());
        if (usuario.getPaciente() != null) {
            Paciente p = usuario.getPaciente();
            p.setCorreo(correo);
            p.setCelular(celular);
            p.setDireccion(direccion);
            pacienteRepository.save(p);
        } else if (usuario.getRecepcionista() != null) {
            Recepcionista r = usuario.getRecepcionista();
            r.actualizar(r.getNombre(), r.getApellidoPaterno(), r.getApellidoMaterno(), correo, celular, direccion);
            recepcionistaRepository.save(r);
        } else if (usuario.getAdministrador() != null) {
            Administrador a = usuario.getAdministrador();
            a.actualizar(correo, celular, direccion);
            administradorRepository.save(a);
        } else if (usuario.getDoctor() != null) {
            var d = usuario.getDoctor();
            d.actualizarCorreo(correo);
            doctorRepository.save(d);
        } else {
            throw new ApiException(HttpStatus.NOT_FOUND, "Perfil no encontrado");
        }
        return obtener(usuarioId);
    }

    private Usuario usuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
