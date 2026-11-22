package pe.edu.sigta.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EstablecimientoRequest(
        @NotNull(message = "El titular es obligatorio")
        Long titularPersonaId,
        @NotBlank(message = "El nombre comercial es obligatorio")
        String nombreComercial,
        String actividadEconomica,
        @NotBlank(message = "La dirección es obligatoria")
        String direccion,
        String referencia,
        @Positive(message = "El área debe ser mayor a 0")
        BigDecimal areaM2,
        String condicionTitular) {
}