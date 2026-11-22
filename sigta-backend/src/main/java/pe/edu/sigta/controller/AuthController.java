package pe.edu.sigta.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import pe.edu.sigta.dto.LoginRequest;
import pe.edu.sigta.dto.LoginResponse;
import pe.edu.sigta.dto.RegistroRequest;
import pe.edu.sigta.dto.RegistroResponse;
import pe.edu.sigta.security.UsuarioActual;
import pe.edu.sigta.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse registro(@Valid @RequestBody RegistroRequest request) {
        return service.registrarCiudadano(request);
    }

    @GetMapping("/me")
    public UsuarioActual me(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioActual.desde(jwt);
    }
}