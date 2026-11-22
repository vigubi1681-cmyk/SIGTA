package pe.edu.sigta.repository;

import java.sql.Types;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import pe.edu.sigta.dto.EstablecimientoRequest;

@Repository
public class EstablecimientoRepository {

    private final SimpleJdbcCall registrarCall;

    public EstablecimientoRepository(JdbcTemplate jdbc) {
        this.registrarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_registrar_establecimiento")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_titular_id", Types.BIGINT),
                        new SqlParameter("p_nombre", Types.VARCHAR),
                        new SqlParameter("p_actividad", Types.VARCHAR),
                        new SqlParameter("p_direccion", Types.VARCHAR),
                        new SqlParameter("p_referencia", Types.VARCHAR),
                        new SqlParameter("p_area_m2", Types.DECIMAL),
                        new SqlParameter("p_condicion", Types.VARCHAR),
                        new SqlOutParameter("p_establecimiento_id", Types.BIGINT));
    }

    public Long registrar(EstablecimientoRequest r) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_titular_id", r.titularPersonaId())
                .addValue("p_nombre", r.nombreComercial())
                .addValue("p_actividad", r.actividadEconomica())
                .addValue("p_direccion", r.direccion())
                .addValue("p_referencia", r.referencia())
                .addValue("p_area_m2", r.areaM2())
                .addValue("p_condicion", r.condicionTitular());

        Map<String, Object> salida = registrarCall.execute(params);
        return ((Number) salida.get("p_establecimiento_id")).longValue();
    }
}