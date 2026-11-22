package pe.edu.sigta.service;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.sigta.dto.AdjuntoDto;
import pe.edu.sigta.dto.AdjuntoResponse;
import pe.edu.sigta.dto.RevisionAdjuntoRequest;
import pe.edu.sigta.exception.RecursoNoEncontradoException;
import pe.edu.sigta.exception.ReglaNegocioException;
import pe.edu.sigta.repository.AdjuntoRepository;
import pe.edu.sigta.security.UsuarioActual;

@Service
public class AdjuntoService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    public record ArchivoDescarga(Resource recurso, String mime, String nombre) {
    }

    private final AdjuntoRepository repo;
    private final SolicitudService solicitudes;
    private final AlmacenamientoService almacen;

    public AdjuntoService(AdjuntoRepository repo, SolicitudService solicitudes,
                          AlmacenamientoService almacen) {
        this.repo = repo;
        this.solicitudes = solicitudes;
        this.almacen = almacen;
    }

    public AdjuntoResponse subir(Long solicitudId, String tipoDocumento,
                                 MultipartFile archivo, UsuarioActual u) {
        solicitudes.obtener(solicitudId, u); // existe y es del usuario

        if (tipoDocumento == null || tipoDocumento.isBlank() || tipoDocumento.length() > 120) {
            throw new ReglaNegocioException("El tipo de documento es obligatorio (máximo 120 caracteres)");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debe seleccionar un archivo");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ReglaNegocioException("El archivo supera el tamaño máximo de 10 MB");
        }
        String mime = almacen.detectarMime(archivo);
        if (mime == null) {
            throw new ReglaNegocioException("El contenido del archivo no es un PDF, PNG o JPG válido");
        }

        String ruta = almacen.guardar(solicitudId, archivo, mime);
        try {
            Long id = repo.registrar(solicitudId, tipoDocumento.trim(),
                    almacen.nombreSeguro(archivo), ruta, mime, archivo.getSize());
            return buscar(solicitudId, id).publico();
        } catch (RuntimeException e) {
            almacen.eliminar(ruta); // no dejar archivos huérfanos si la base rechazó el registro
            throw e;
        }
    }

    public List<AdjuntoResponse> listar(Long solicitudId, UsuarioActual u) {
        solicitudes.obtener(solicitudId, u);
        return repo.listar(solicitudId).stream().map(AdjuntoDto::publico).toList();
    }

    public ArchivoDescarga descargar(Long solicitudId, Long adjuntoId, UsuarioActual u) {
        solicitudes.obtener(solicitudId, u);
        AdjuntoDto a = buscar(solicitudId, adjuntoId);
        return new ArchivoDescarga(almacen.cargar(a.rutaAlmacenamiento()), a.mimeType(), a.nombreArchivo());
    }

    public void revisar(Long solicitudId, Long adjuntoId, RevisionAdjuntoRequest r, UsuarioActual u) {
        solicitudes.obtener(solicitudId, u);
        buscar(solicitudId, adjuntoId); // confirma que el adjunto pertenece a esa solicitud
        repo.revisar(adjuntoId, r.estado(), r.observacion());
    }

    // Busca dentro de los adjuntos de la solicitud: evita pedir el adjunto de otra solicitud
    private AdjuntoDto buscar(Long solicitudId, Long adjuntoId) {
        return repo.listar(solicitudId).stream()
                .filter(a -> a.id().equals(adjuntoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El adjunto no existe"));
    }
}
