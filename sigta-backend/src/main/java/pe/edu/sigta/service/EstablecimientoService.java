package pe.edu.sigta.service;

import org.springframework.stereotype.Service;
import pe.edu.sigta.dto.EstablecimientoRequest;
import pe.edu.sigta.exception.AccesoDenegadoException;
import pe.edu.sigta.repository.EstablecimientoRepository;
import pe.edu.sigta.security.UsuarioActual;

@Service
public class EstablecimientoService {

    private final EstablecimientoRepository repo;

    public EstablecimientoService(EstablecimientoRepository repo) {
        this.repo = repo;
    }

    public Long registrar(EstablecimientoRequest request, UsuarioActual u) {
        if (u.esCiudadano() && !request.titularPersonaId().equals(u.personaId())) {
            throw new AccesoDenegadoException("No puede registrar establecimientos de otra persona");
        }
        return repo.registrar(request);
    }
}