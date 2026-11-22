package pe.edu.sigta.service;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.edu.sigta.dto.TipoTramiteDto;
import pe.edu.sigta.repository.TramiteRepository;

@Service
public class TramiteService {

    private final TramiteRepository repo;

    public TramiteService(TramiteRepository repo) {
        this.repo = repo;
    }

    public List<TipoTramiteDto> listarTipos() {
        return repo.listarTipos();
    }
}