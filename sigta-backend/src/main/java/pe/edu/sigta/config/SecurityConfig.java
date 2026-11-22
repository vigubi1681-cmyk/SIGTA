package pe.edu.sigta.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Convierte el claim "rol" del token en autoridad ROLE_xxx
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt ->
                List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("rol"))));
        return conv;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationConverter conv) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/login", "/api/auth/registro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tramites").permitAll()
                .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/personas/**").hasAnyRole("MESA_PARTES", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/establecimientos")
                    .hasAnyRole("CIUDADANO", "MESA_PARTES")
                .requestMatchers(HttpMethod.POST,
                        "/api/solicitudes/licencia",
                        "/api/solicitudes/anuncio",
                        "/api/solicitudes/duplicado",
                        "/api/solicitudes/*/presentar")
                    .hasAnyRole("CIUDADANO", "MESA_PARTES")
                .requestMatchers(HttpMethod.POST,
                        "/api/solicitudes/*/observar",
                        "/api/solicitudes/*/denegar")
                    .hasAnyRole("EVALUADOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST,
                        "/api/solicitudes/*/revisar",
                        "/api/solicitudes/*/derivar")
                    .hasAnyRole("MESA_PARTES", "EVALUADOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST,
                        "/api/solicitudes/*/subsanar",
                        "/api/solicitudes/*/cancelar")
                    .hasAnyRole("CIUDADANO", "MESA_PARTES")
                .requestMatchers(HttpMethod.GET, "/api/areas")
                    .hasAnyRole("MESA_PARTES", "EVALUADOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/solicitudes/*/adjuntos")
                    .hasAnyRole("CIUDADANO", "MESA_PARTES")
                .requestMatchers(HttpMethod.POST, "/api/solicitudes/*/adjuntos/*/revisar")
                    .hasAnyRole("EVALUADOR", "ADMINISTRADOR")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conv)));
        return http.build();
    }
}