package pe.edu.sigta.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.edu.sigta.security.UsuarioActual;

import jakarta.validation.Valid;
import pe.edu.sigta.dto.EstablecimientoRequest;
import pe.edu.sigta.dto.IdResponse;
import pe.edu.sigta.service.EstablecimientoService;

@RestController
@RequestMapping("/api/establecimientos")
public class EstablecimientoController {

    private final EstablecimientoService service;

    public EstablecimientoController(EstablecimientoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse registrar(@Valid @RequestBody EstablecimientoRequest request,
                                @AuthenticationPrincipal Jwt jwt) {
        return new IdResponse(service.registrar(request, UsuarioActual.desde(jwt)));
    }
}
