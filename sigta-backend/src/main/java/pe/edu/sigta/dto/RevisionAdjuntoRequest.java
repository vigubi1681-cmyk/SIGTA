package pe.edu.sigta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RevisionAdjuntoRequest(
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(regexp = "VALIDADO|OBSERVADO|RECHAZADO",
                 message = "Estado: VALIDADO, OBSERVADO o RECHAZADO") String estado,
        String observacion) {
}