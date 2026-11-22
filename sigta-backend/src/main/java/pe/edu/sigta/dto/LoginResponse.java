package pe.edu.sigta.dto;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        Long usuarioId,
        Long personaId,
        String correo,
        String rol) {
}
