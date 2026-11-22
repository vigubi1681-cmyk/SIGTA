package pe.edu.sigta.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import pe.edu.sigta.dto.AdjuntoResponse;
import pe.edu.sigta.dto.RevisionAdjuntoRequest;
import pe.edu.sigta.security.UsuarioActual;
import pe.edu.sigta.service.AdjuntoService;

@RestController
@RequestMapping("/api/solicitudes/{solicitudId}/adjuntos")
public class AdjuntoController {

    private final AdjuntoService service;

    public AdjuntoController(AdjuntoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdjuntoResponse subir(@PathVariable Long solicitudId,
                                 @RequestParam String tipoDocumento,
                                 @RequestParam MultipartFile archivo,
                                 @AuthenticationPrincipal Jwt jwt) {
        return service.subir(solicitudId, tipoDocumento, archivo, UsuarioActual.desde(jwt));
    }

    @GetMapping
    public List<AdjuntoResponse> listar(@PathVariable Long solicitudId,
                                        @AuthenticationPrincipal Jwt jwt) {
        return service.listar(solicitudId, UsuarioActual.desde(jwt));
    }

    @GetMapping("/{adjuntoId}/archivo")
    public ResponseEntity<Resource> descargar(@PathVariable Long solicitudId,
                                              @PathVariable Long adjuntoId,
                                              @AuthenticationPrincipal Jwt jwt) {
        AdjuntoService.ArchivoDescarga a =
                service.descargar(solicitudId, adjuntoId, UsuarioActual.desde(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.mime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(a.nombre(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(a.recurso());
    }

    @PostMapping("/{adjuntoId}/revisar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revisar(@PathVariable Long solicitudId,
                        @PathVariable Long adjuntoId,
                        @Valid @RequestBody RevisionAdjuntoRequest r,
                        @AuthenticationPrincipal Jwt jwt) {
        service.revisar(solicitudId, adjuntoId, r, UsuarioActual.desde(jwt));
    }
}