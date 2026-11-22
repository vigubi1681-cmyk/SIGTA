package pe.edu.sigta.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SolicitudDuplicadoRequest(
        @NotNull(message = "El solicitante es obligatorio") Long solicitanteId,
        Long representanteId,
        @NotNull(message = "El establecimiento es obligatorio") Long establecimientoId,
        //Long usuarioId,
        @NotBlank(message = "El número de licencia es obligatorio") String numeroLicencia,
        @NotBlank(message = "El motivo es obligatorio")
        @Pattern(regexp = "PERDIDA|DETERIORO|ROBO|OTRO",
                 message = "Motivo: PERDIDA, DETERIORO, ROBO u OTRO")
        String motivo,
        String detalleMotivo,
        String numeroComprobantePago,
        LocalDate fechaPago,
        String observaciones) {
}
