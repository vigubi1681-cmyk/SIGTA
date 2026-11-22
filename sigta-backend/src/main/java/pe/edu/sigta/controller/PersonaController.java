package pe.edu.sigta.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import pe.edu.sigta.dto.IdResponse;
import pe.edu.sigta.dto.PersonaRequest;
import pe.edu.sigta.service.PersonaService;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService service;

    public PersonaController(PersonaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse registrar(@Valid @RequestBody PersonaRequest request) {
        return new IdResponse(service.registrar(request));
    }
}