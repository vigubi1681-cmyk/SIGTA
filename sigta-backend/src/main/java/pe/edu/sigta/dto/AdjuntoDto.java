package pe.edu.sigta.dto;

import java.time.LocalDateTime;

public record AdjuntoDto(
        Long id,
        String tipoDocumento,
        String nombreArchivo,
        String rutaAlmacenamiento,
        String mimeType,
        Long tamanioBytes,
        String estadoRevision,
        String observacionRevision,
        LocalDateTime cargadoEn) {

    // Versión que se envía al cliente: sin la ruta del servidor
    public AdjuntoResponse publico() {
        return new AdjuntoResponse(id, tipoDocumento, nombreArchivo, mimeType,
                tamanioBytes, estadoRevision, observacionRevision, cargadoEn);
    }
}
