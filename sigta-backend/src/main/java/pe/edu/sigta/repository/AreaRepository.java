package pe.edu.sigta.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import pe.edu.sigta.dto.AreaDto;

@Repository
public class AreaRepository {

    private final SimpleJdbcCall listarCall;

    public AreaRepository(JdbcTemplate jdbc) {
        this.listarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_listar_areas")
                .withoutProcedureColumnMetaDataAccess()
                .returningResultSet("filas", (rs, i) -> new AreaDto(
                        rs.getInt("id"),
                        rs.getString("codigo"),
                        rs.getString("nombre")));
    }

    @SuppressWarnings("unchecked")
    public List<AreaDto> listar() {
        return (List<AreaDto>) listarCall.execute().get("filas");
    }
}