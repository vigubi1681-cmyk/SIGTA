package pe.edu.sigta.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import pe.edu.sigta.dto.TipoTramiteDto;

@Repository
public class TramiteRepository {

    private final SimpleJdbcCall listarTiposCall;

    public TramiteRepository(JdbcTemplate jdbc) {
        this.listarTiposCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_listar_tipos_tramite")
                .withoutProcedureColumnMetaDataAccess()
                .returningResultSet("tipos", (rs, i) -> new TipoTramiteDto(
                        rs.getInt("id"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getBoolean("requiere_establecimiento")));
    }

    @SuppressWarnings("unchecked")
    public List<TipoTramiteDto> listarTipos() {
        Map<String, Object> resultado = listarTiposCall.execute();
        return (List<TipoTramiteDto>) resultado.get("tipos");
    }
}