package pe.edu.sigta.security;

import org.springframework.security.oauth2.jwt.Jwt;

public record UsuarioActual(Long usuarioId, Long personaId, String rol) {

    public static UsuarioActual desde(Jwt jwt) {
        Number uid = jwt.getClaim("uid");
        Number pid = jwt.getClaim("pid");
        return new UsuarioActual(
                uid.longValue(),
                pid == null ? null : pid.longValue(),
                jwt.getClaimAsString("rol"));
    }

    public boolean esCiudadano() {
        return "CIUDADANO".equals(rol);
    }
}