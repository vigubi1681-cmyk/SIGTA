package pe.edu.sigta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record SolicitudAnuncioRequest(
        @NotNull(message = "El solicitante es obligatorio") Long solicitanteId,
        Long representanteId,
        Long establecimientoId,
        //Long usuarioId,
        @NotBlank(message = "El tipo de elemento es obligatorio") String tipoElemento,
        @NotBlank(message = "El tipo de propiedad es obligatorio")
        @Pattern(regexp = "DOMINIO_PRIVADO|DOMINIO_PUBLICO",
                 message = "Tipo de propiedad: DOMINIO_PRIVADO o DOMINIO_PUBLICO")
        String tipoPropiedad,
        String material,
        @NotNull(message = "El alto es obligatorio")
        @Positive(message = "El alto debe ser mayor a 0") BigDecimal altoM,
        @NotNull(message = "El ancho es obligatorio")
        @Positive(message = "El ancho debe ser mayor a 0") BigDecimal anchoM,
        @Positive(message = "El número de caras debe ser mayor a 0") Integer numeroCaras,
        @Pattern(regexp = "SIN_ILUMINACION|ILUMINADO|LUMINOSO",
                 message = "Iluminación: SIN_ILUMINACION, ILUMINADO o LUMINOSO")
        String tipoIluminacion,
        @NotBlank(message = "La descripción del anuncio es obligatoria") String contenidoAnuncio,
        String descripcionUbicacion,
        @Pattern(regexp = "PERMANENTE|TEMPORAL", message = "Modalidad: PERMANENTE o TEMPORAL")
        String modalidad,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String observaciones) {
}