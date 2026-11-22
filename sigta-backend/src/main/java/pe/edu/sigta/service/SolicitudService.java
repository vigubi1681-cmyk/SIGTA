package pe.edu.sigta.service;

import java.util.List;
import java.util.Map;
import pe.edu.sigta.dto.DerivarRequest;
import org.springframework.stereotype.Service;
import pe.edu.sigta.dto.SolicitudAnuncioRequest;
import pe.edu.sigta.dto.SolicitudCreadaResponse;
import pe.edu.sigta.dto.SolicitudDuplicadoRequest;
import pe.edu.sigta.dto.SolicitudLicenciaRequest;
import pe.edu.sigta.exception.AccesoDenegadoException;
import pe.edu.sigta.exception.RecursoNoEncontradoException;
import pe.edu.sigta.repository.SolicitudRepository;
import pe.edu.sigta.security.UsuarioActual;

@Service
public class SolicitudService {

    private final SolicitudRepository repo;

    public SolicitudService(SolicitudRepository repo) {
        this.repo = repo;
    }

    public SolicitudCreadaResponse crearLicencia(SolicitudLicenciaRequest r, UsuarioActual u) {
        verificarSolicitante(r.solicitanteId(), u);
        return repo.crearLicencia(r, u.usuarioId());
    }

    public SolicitudCreadaResponse crearAnuncio(SolicitudAnuncioRequest r, UsuarioActual u) {
        verificarSolicitante(r.solicitanteId(), u);
        return repo.crearAnuncio(r, u.usuarioId());
    }

    public SolicitudCreadaResponse crearDuplicado(SolicitudDuplicadoRequest r, UsuarioActual u) {
        verificarSolicitante(r.solicitanteId(), u);
        return repo.crearDuplicado(r, u.usuarioId());
    }

    public void presentar(Long solicitudId, UsuarioActual u) {
        obtener(solicitudId, u); // valida que exista y que sea del usuario
        repo.presentar(solicitudId, u.usuarioId());
    }

    public Map<String, Object> obtener(Long solicitudId, UsuarioActual u) {
        Map<String, Object> solicitud = repo.obtener(solicitudId);
        if (solicitud == null) {
            throw new RecursoNoEncontradoException("La solicitud no existe");
        }
        if (u.esCiudadano()) {
            Number dueno = (Number) solicitud.get("solicitantePersonaId");
            if (dueno == null || !Long.valueOf(dueno.longValue()).equals(u.personaId())) {
                throw new RecursoNoEncontradoException("La solicitud no existe");
            }
        }
        return solicitud;
    }

    public List<Map<String, Object>> listar(Long solicitanteId, String estado, String tipo,
                                            String texto, Integer limite, Integer desplazamiento,
                                            UsuarioActual u) {
        // El ciudadano  solo puede ver lo suyo, aunque mande otro solicitanteId
        Long filtro = u.esCiudadano() ? u.personaId() : solicitanteId;
        return repo.listar(filtro, estado, tipo, texto, limite, desplazamiento);
    }


    // ---------- Revisión (personal municipal) ----------

    public void revisar(Long solicitudId, UsuarioActual u) {
        repo.cambiarEstado(solicitudId, "EN_REVISION", u.usuarioId(), null);
    }

    public void observar(Long solicitudId, String comentario, UsuarioActual u) {
        repo.cambiarEstado(solicitudId, "OBSERVADA", u.usuarioId(), comentario);
    }

    public void denegar(Long solicitudId, String comentario, UsuarioActual u) {
        repo.cambiarEstado(solicitudId, "DENEGADA", u.usuarioId(), comentario);
    }

    public void derivar(Long solicitudId, DerivarRequest r, UsuarioActual u) {
        repo.derivar(solicitudId, r.areaCodigo(), u.usuarioId(), r.instruccion());
    }


    // ---------- Acciones del solicitante ----------

    public void subsanar(Long solicitudId, String comentario, UsuarioActual u) {
        obtener(solicitudId, u); // existe y es del usuario
        repo.cambiarEstado(solicitudId, "SUBSANADA", u.usuarioId(), comentario);
    }

    public void cancelar(Long solicitudId, String comentario, UsuarioActual u) {
        obtener(solicitudId, u);
        repo.cambiarEstado(solicitudId, "CANCELADA", u.usuarioId(), comentario);
    }

    public List<Map<String, Object>> historial(Long solicitudId, UsuarioActual u) {
        obtener(solicitudId, u);
        List<Map<String, Object>> filas = repo.historial(solicitudId);
        // El ciudadano ve qué pasó y por qué, pero no el correo del funcionario
        if (u.esCiudadano()) {
            filas.forEach(f -> f.put("usuario", null));
        }
        return filas;
    }

    private void verificarSolicitante(Long solicitanteId, UsuarioActual u) {
        if (u.esCiudadano() && !solicitanteId.equals(u.personaId())) {
            throw new AccesoDenegadoException("No puede registrar solicitudes a nombre de otra persona");
        }
    }
}