package pe.edu.sigta.repository;

import java.sql.Types;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import pe.edu.sigta.dto.PersonaRequest;

@Repository
public class PersonaRepository {

    private final SimpleJdbcCall registrarCall;

    public PersonaRepository(JdbcTemplate jdbc) {
        this.registrarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_registrar_persona")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_tipo_persona", Types.VARCHAR),
                        new SqlParameter("p_tipo_documento", Types.VARCHAR),
                        new SqlParameter("p_numero_doc", Types.VARCHAR),
                        new SqlParameter("p_ruc", Types.CHAR),
                        new SqlParameter("p_nombres", Types.VARCHAR),
                        new SqlParameter("p_apellidos", Types.VARCHAR),
                        new SqlParameter("p_razon_social", Types.VARCHAR),
                        new SqlParameter("p_correo", Types.VARCHAR),
                        new SqlParameter("p_telefono", Types.VARCHAR),
                        new SqlParameter("p_direccion", Types.VARCHAR),
                        new SqlOutParameter("p_persona_id", Types.BIGINT));
    }

    public Long registrar(PersonaRequest r) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_tipo_persona", r.tipoPersona())
                .addValue("p_tipo_documento", r.tipoDocumento())
                .addValue("p_numero_doc", r.numeroDocumento())
                .addValue("p_ruc", r.ruc())
                .addValue("p_nombres", r.nombres())
                .addValue("p_apellidos", r.apellidos())
                .addValue("p_razon_social", r.razonSocial())
                .addValue("p_correo", r.correo())
                .addValue("p_telefono", r.telefono())
                .addValue("p_direccion", r.direccion());

        Map<String, Object> salida = registrarCall.execute(params);
        return ((Number) salida.get("p_persona_id")).longValue();
    }
}
