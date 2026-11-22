package pe.edu.sigta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PersonaRequest(
        @NotBlank(message = "El tipo de persona es obligatorio")
        @Pattern(regexp = "NATURAL|JURIDICA", message = "Tipo de persona debe ser NATURAL o JURIDICA")
        String tipoPersona,
        String tipoDocumento,
        String numeroDocumento,
        String ruc,
        String nombres,
        String apellidos,
        String razonSocial,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Correo con formato inválido")
        String correo,
        @NotBlank(message = "El teléfono es obligatorio")
        String telefono,
        String direccion) {
}