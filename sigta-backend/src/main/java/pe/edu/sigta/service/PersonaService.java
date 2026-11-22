package pe.edu.sigta.service;

import org.springframework.stereotype.Service;
import pe.edu.sigta.dto.PersonaRequest;
import pe.edu.sigta.repository.PersonaRepository;

@Service
public class PersonaService {

    private final PersonaRepository repo;

    public PersonaService(PersonaRepository repo) {
        this.repo = repo;
    }

    public Long registrar(PersonaRequest request) {
        return repo.registrar(request);
    }
}