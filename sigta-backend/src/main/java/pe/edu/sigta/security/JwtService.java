package pe.edu.sigta.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long horas;

    public JwtService(JwtEncoder encoder,
                      @Value("${sigta.jwt.expiracion-horas:8}") long horas) {
        this.encoder = encoder;
        this.horas = horas;
    }

    public String generar(UsuarioAuth u) {
        Instant ahora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("sigta-chorrillos")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(horas, ChronoUnit.HOURS))
                .subject(u.correo())
                .claim("uid", u.id())
                .claim("rol", u.rol());
        if (u.personaId() != null) {
            claims.claim("pid", u.personaId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    public long segundosExpiracion() {
        return horas * 3600;
    }
}