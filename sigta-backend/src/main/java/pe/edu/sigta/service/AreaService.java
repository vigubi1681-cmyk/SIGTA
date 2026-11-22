package pe.edu.sigta.service;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.edu.sigta.dto.AreaDto;
import pe.edu.sigta.repository.AreaRepository;

@Service
public class AreaService {

    private final AreaRepository repo;

    public AreaService(AreaRepository repo) {
        this.repo = repo;
    }

    public List<AreaDto> listar() {
        return repo.listar();
    }
}