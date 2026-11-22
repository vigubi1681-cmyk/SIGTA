package pe.edu.sigta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioNuevoRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") String correo,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String password,
        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "MESA_PARTES|EVALUADOR|ADMINISTRADOR",
                 message = "Rol: MESA_PARTES, EVALUADOR o ADMINISTRADOR") String rol,
        Long personaId) {
}