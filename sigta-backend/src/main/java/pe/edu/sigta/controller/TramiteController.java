package pe.edu.sigta.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.sigta.dto.TipoTramiteDto;
import pe.edu.sigta.service.TramiteService;

@RestController
@RequestMapping("/api/tramites")
public class TramiteController {

    private final TramiteService service;

    public TramiteController(TramiteService service) {
        this.service = service;
    }

    @GetMapping
    public List<TipoTramiteDto> listar() {
        return service.listarTipos();
    }
}
