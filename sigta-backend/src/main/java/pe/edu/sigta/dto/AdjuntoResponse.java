package pe.edu.sigta.dto;

import java.time.LocalDateTime;

public record AdjuntoResponse(
        Long id,
        String tipoDocumento,
        String nombreArchivo,
        String mimeType,
        Long tamanioBytes,
        String estadoRevision,
        String observacionRevision,
        LocalDateTime cargadoEn) {
}