package pe.edu.sigta.security;

public record UsuarioAuth(
        Long id,
        Long personaId,
        String correo,
        String passwordHash,
        boolean activo,
        String rol) {
}