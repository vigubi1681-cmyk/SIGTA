package pe.edu.sigta.dto;

import jakarta.validation.constraints.NotBlank;

public record DerivarRequest(
        @NotBlank(message = "El área de destino es obligatoria") String areaCodigo,
        String instruccion) {
}