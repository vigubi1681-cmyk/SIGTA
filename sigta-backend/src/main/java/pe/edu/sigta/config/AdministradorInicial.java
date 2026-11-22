package pe.edu.sigta.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.edu.sigta.repository.UsuarioRepository;

@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String correo;
    private final String password;

    public AdministradorInicial(UsuarioRepository usuarios, PasswordEncoder encoder,
                                @Value("${sigta.admin.correo}") String correo,
                                @Value("${sigta.admin.password}") String password) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.correo = correo;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.buscarPorCorreo(correo) == null) {
            usuarios.registrar(null, "ADMINISTRADOR", correo, encoder.encode(password));
            log.info("Administrador inicial creado: {}", correo);
        }
    }
}