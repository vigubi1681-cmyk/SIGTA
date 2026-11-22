package pe.edu.sigta.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.edu.sigta.dto.LoginRequest;
import pe.edu.sigta.dto.LoginResponse;
import pe.edu.sigta.dto.RegistroRequest;
import pe.edu.sigta.dto.RegistroResponse;
import pe.edu.sigta.dto.UsuarioNuevoRequest;
import pe.edu.sigta.exception.CredencialesInvalidasException;
import pe.edu.sigta.repository.PersonaRepository;
import pe.edu.sigta.repository.UsuarioRepository;
import pe.edu.sigta.security.JwtService;
import pe.edu.sigta.security.UsuarioAuth;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PersonaRepository personas;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, PersonaRepository personas,
                       PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.personas = personas;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public LoginResponse login(LoginRequest r) {
        UsuarioAuth u = usuarios.buscarPorCorreo(r.correo());
        // Mismo error para "no existe", "inactivo" y "clave incorrecta": no revela cuál fue
        if (u == null || !u.activo() || !encoder.matches(r.password(), u.passwordHash())) {
            throw new CredencialesInvalidasException();
        }
        usuarios.registrarUltimoAcceso(u.id());
        return new LoginResponse(jwt.generar(u), "Bearer", jwt.segundosExpiracion(),
                u.id(), u.personaId(), u.correo(), u.rol());
    }

    // Autorregistro de ciudadanos: persona + usuario con rol CIUDADANO
    public RegistroResponse registrarCiudadano(RegistroRequest r) {
        Long personaId = personas.registrar(r.persona());
        Long usuarioId = usuarios.registrar(personaId, "CIUDADANO",
                r.persona().correo(), encoder.encode(r.password()));
        return new RegistroResponse(usuarioId, personaId);
    }

    // Lo usa el administrador para crear personal municipal
    public Long crearUsuario(UsuarioNuevoRequest r) {
        return usuarios.registrar(r.personaId(), r.rol(), r.correo(), encoder.encode(r.password()));
    }
}
