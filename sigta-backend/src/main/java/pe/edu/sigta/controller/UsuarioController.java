package pe.edu.sigta.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import pe.edu.sigta.dto.IdResponse;
import pe.edu.sigta.dto.UsuarioNuevoRequest;
import pe.edu.sigta.service.AuthService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final AuthService service;

    public UsuarioController(AuthService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse crear(@Valid @RequestBody UsuarioNuevoRequest request) {
        return new IdResponse(service.crearUsuario(request));
    }
}