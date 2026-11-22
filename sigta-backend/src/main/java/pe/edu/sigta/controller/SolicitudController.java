package pe.edu.sigta.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import pe.edu.sigta.dto.SolicitudAnuncioRequest;
import pe.edu.sigta.dto.SolicitudCreadaResponse;
import pe.edu.sigta.dto.SolicitudDuplicadoRequest;
import pe.edu.sigta.dto.SolicitudLicenciaRequest;
import pe.edu.sigta.security.UsuarioActual;
import pe.edu.sigta.service.SolicitudService;
import pe.edu.sigta.dto.ComentarioRequest;
import pe.edu.sigta.dto.DerivarRequest;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService service;

    public SolicitudController(SolicitudService service) {
        this.service = service;
    }

    @PostMapping("/licencia")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudCreadaResponse crearLicencia(@Valid @RequestBody SolicitudLicenciaRequest r,
                                                 @AuthenticationPrincipal Jwt jwt) {
        return service.crearLicencia(r, UsuarioActual.desde(jwt));
    }

    @PostMapping("/anuncio")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudCreadaResponse crearAnuncio(@Valid @RequestBody SolicitudAnuncioRequest r,
                                                @AuthenticationPrincipal Jwt jwt) {
        return service.crearAnuncio(r, UsuarioActual.desde(jwt));
    }

    @PostMapping("/duplicado")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudCreadaResponse crearDuplicado(@Valid @RequestBody SolicitudDuplicadoRequest r,
                                                  @AuthenticationPrincipal Jwt jwt) {
        return service.crearDuplicado(r, UsuarioActual.desde(jwt));
    }

    @PostMapping("/{id}/presentar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void presentar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.presentar(id, UsuarioActual.desde(jwt));
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.obtener(id, UsuarioActual.desde(jwt));
    }

    @GetMapping
    public List<Map<String, Object>> listar(
            @RequestParam(required = false) Long solicitanteId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false) Integer desplazamiento,
            @AuthenticationPrincipal Jwt jwt) {
        return service.listar(solicitanteId, estado, tipo, texto, limite, desplazamiento,
                UsuarioActual.desde(jwt));
    }

    @PostMapping("/{id}/revisar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revisar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.revisar(id, UsuarioActual.desde(jwt));
    }

    @PostMapping("/{id}/observar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void observar(@PathVariable Long id, @RequestBody ComentarioRequest r,
                         @AuthenticationPrincipal Jwt jwt) {
        service.observar(id, r.comentario(), UsuarioActual.desde(jwt));
    }

    @PostMapping("/{id}/denegar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void denegar(@PathVariable Long id, @RequestBody ComentarioRequest r,
                        @AuthenticationPrincipal Jwt jwt) {
        service.denegar(id, r.comentario(), UsuarioActual.desde(jwt));
    }

    @PostMapping("/{id}/derivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void derivar(@PathVariable Long id, @Valid @RequestBody DerivarRequest r,
                        @AuthenticationPrincipal Jwt jwt) {
        service.derivar(id, r, UsuarioActual.desde(jwt));
    }

    @PostMapping("/{id}/subsanar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subsanar(@PathVariable Long id,
                         @RequestBody(required = false) ComentarioRequest r,
                         @AuthenticationPrincipal Jwt jwt) {
        service.subsanar(id, r == null ? null : r.comentario(), UsuarioActual.desde(jwt));
    }

    @PostMapping("/{id}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long id,
                         @RequestBody(required = false) ComentarioRequest r,
                         @AuthenticationPrincipal Jwt jwt) {
        service.cancelar(id, r == null ? null : r.comentario(), UsuarioActual.desde(jwt));
    }

    @GetMapping("/{id}/historial")
    public List<Map<String, Object>> historial(@PathVariable Long id,
                                               @AuthenticationPrincipal Jwt jwt) {
        return service.historial(id, UsuarioActual.desde(jwt));
    }
}