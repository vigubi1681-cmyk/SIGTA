package pe.edu.sigta.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitudLicenciaRequest(
        @NotNull(message = "El solicitante es obligatorio") Long solicitanteId,
        Long representanteId,
        @NotNull(message = "El establecimiento es obligatorio") Long establecimientoId,
        //Long usuarioId,
        Boolean declaracionSeguridad,
        String observaciones) {
}