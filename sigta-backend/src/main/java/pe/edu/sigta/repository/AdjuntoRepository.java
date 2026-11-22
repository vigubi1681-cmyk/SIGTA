package pe.edu.sigta.repository;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import pe.edu.sigta.dto.AdjuntoDto;

@Repository
public class AdjuntoRepository {

    private final SimpleJdbcCall registrarCall;
    private final SimpleJdbcCall listarCall;
    private final SimpleJdbcCall revisarCall;

    public AdjuntoRepository(JdbcTemplate jdbc) {
        registrarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_registrar_adjunto")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_solicitud_id", Types.BIGINT),
                        new SqlParameter("p_tipo_documento", Types.VARCHAR),
                        new SqlParameter("p_nombre_archivo", Types.VARCHAR),
                        new SqlParameter("p_ruta", Types.VARCHAR),
                        new SqlParameter("p_mime", Types.VARCHAR),
                        new SqlParameter("p_tamanio", Types.BIGINT),
                        new SqlOutParameter("p_adjunto_id", Types.BIGINT));

        listarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_listar_adjuntos")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_solicitud_id", Types.BIGINT))
                .returningResultSet("filas", (rs, i) -> new AdjuntoDto(
                        rs.getLong("id"),
                        rs.getString("tipo_documento"),
                        rs.getString("nombre_archivo"),
                        rs.getString("ruta_almacenamiento"),
                        rs.getString("mime_type"),
                        rs.getLong("tamanio_bytes"),
                        rs.getString("estado_revision"),
                        rs.getString("observacion_revision"),
                        rs.getObject("cargado_en", LocalDateTime.class)));

        revisarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_revisar_adjunto")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_adjunto_id", Types.BIGINT),
                        new SqlParameter("p_estado", Types.VARCHAR),
                        new SqlParameter("p_observacion", Types.VARCHAR));
    }

    public Long registrar(Long solicitudId, String tipoDocumento, String nombreArchivo,
                          String ruta, String mime, long tamanio) {
        Map<String, Object> salida = registrarCall.execute(new MapSqlParameterSource()
                .addValue("p_solicitud_id", solicitudId)
                .addValue("p_tipo_documento", tipoDocumento)
                .addValue("p_nombre_archivo", nombreArchivo)
                .addValue("p_ruta", ruta)
                .addValue("p_mime", mime)
                .addValue("p_tamanio", tamanio));
        return ((Number) salida.get("p_adjunto_id")).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<AdjuntoDto> listar(Long solicitudId) {
        Map<String, Object> salida = listarCall.execute(
                new MapSqlParameterSource("p_solicitud_id", solicitudId));
        return (List<AdjuntoDto>) salida.get("filas");
    }

    public void revisar(Long adjuntoId, String estado, String observacion) {
        revisarCall.execute(new MapSqlParameterSource()
                .addValue("p_adjunto_id", adjuntoId)
                .addValue("p_estado", estado)
                .addValue("p_observacion", observacion));
    }
}