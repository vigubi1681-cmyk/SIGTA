package pe.edu.sigta.repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import pe.edu.sigta.dto.SolicitudAnuncioRequest;
import pe.edu.sigta.dto.SolicitudCreadaResponse;
import pe.edu.sigta.dto.SolicitudDuplicadoRequest;
import pe.edu.sigta.dto.SolicitudLicenciaRequest;

@Repository
public class SolicitudRepository {

    private final SimpleJdbcCall crearLicenciaCall;
    private final SimpleJdbcCall crearAnuncioCall;
    private final SimpleJdbcCall crearDuplicadoCall;
    private final SimpleJdbcCall presentarCall;
    private final SimpleJdbcCall obtenerCall;
    private final SimpleJdbcCall listarCall;
    private final SimpleJdbcCall cambiarEstadoCall;
    private final SimpleJdbcCall derivarCall;
    private final SimpleJdbcCall historialCall;

    public SolicitudRepository(JdbcTemplate jdbc) {

        crearLicenciaCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_crear_solicitud_licencia")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_solicitante_id", Types.BIGINT),
                        new SqlParameter("p_representante_id", Types.BIGINT),
                        new SqlParameter("p_establecimiento_id", Types.BIGINT),
                        new SqlParameter("p_usuario_id", Types.BIGINT),
                        new SqlParameter("p_declaracion_seguridad", Types.BOOLEAN),
                        new SqlParameter("p_observaciones", Types.VARCHAR),
                        new SqlOutParameter("p_solicitud_id", Types.BIGINT),
                        new SqlOutParameter("p_codigo", Types.VARCHAR));

        crearAnuncioCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_crear_solicitud_anuncio")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_solicitante_id", Types.BIGINT),
                        new SqlParameter("p_representante_id", Types.BIGINT),
                        new SqlParameter("p_establecimiento_id", Types.BIGINT),
                        new SqlParameter("p_usuario_id", Types.BIGINT),
                        new SqlParameter("p_tipo_elemento", Types.VARCHAR),
                        new SqlParameter("p_tipo_propiedad", Types.VARCHAR),
                        new SqlParameter("p_material", Types.VARCHAR),
                        new SqlParameter("p_alto_m", Types.DECIMAL),
                        new SqlParameter("p_ancho_m", Types.DECIMAL),
                        new SqlParameter("p_numero_caras", Types.SMALLINT),
                        new SqlParameter("p_tipo_iluminacion", Types.VARCHAR),
                        new SqlParameter("p_contenido", Types.VARCHAR),
                        new SqlParameter("p_ubicacion", Types.VARCHAR),
                        new SqlParameter("p_modalidad", Types.VARCHAR),
                        new SqlParameter("p_fecha_inicio", Types.DATE),
                        new SqlParameter("p_fecha_fin", Types.DATE),
                        new SqlParameter("p_observaciones", Types.VARCHAR),
                        new SqlOutParameter("p_solicitud_id", Types.BIGINT),
                        new SqlOutParameter("p_codigo", Types.VARCHAR));

        crearDuplicadoCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_crear_solicitud_duplicado")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_solicitante_id", Types.BIGINT),
                        new SqlParameter("p_representante_id", Types.BIGINT),
                        new SqlParameter("p_establecimiento_id", Types.BIGINT),
                        new SqlParameter("p_usuario_id", Types.BIGINT),
                        new SqlParameter("p_numero_licencia", Types.VARCHAR),
                        new SqlParameter("p_motivo", Types.VARCHAR),
                        new SqlParameter("p_detalle_motivo", Types.VARCHAR),
                        new SqlParameter("p_comprobante_pago", Types.VARCHAR),
                        new SqlParameter("p_fecha_pago", Types.DATE),
                        new SqlParameter("p_observaciones", Types.VARCHAR),
                        new SqlOutParameter("p_solicitud_id", Types.BIGINT),
                        new SqlOutParameter("p_codigo", Types.VARCHAR));

        presentarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_presentar_solicitud")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_solicitud_id", Types.BIGINT),
                        new SqlParameter("p_usuario_id", Types.BIGINT));

        obtenerCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_obtener_solicitud")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_solicitud_id", Types.BIGINT))
                .returningResultSet("filas", SolicitudRepository::filaAMapa);

        listarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_listar_solicitudes")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_solicitante_id", Types.BIGINT),
                        new SqlParameter("p_estado_codigo", Types.VARCHAR),
                        new SqlParameter("p_tipo_codigo", Types.VARCHAR),
                        new SqlParameter("p_texto", Types.VARCHAR),
                        new SqlParameter("p_limite", Types.INTEGER),
                        new SqlParameter("p_desplazamiento", Types.INTEGER))
                .returningResultSet("filas", SolicitudRepository::filaAMapa);

        cambiarEstadoCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_cambiar_estado")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_solicitud_id", Types.BIGINT),
                        new SqlParameter("p_estado_nuevo", Types.VARCHAR),
                        new SqlParameter("p_usuario_id", Types.BIGINT),
                        new SqlParameter("p_comentario", Types.VARCHAR));

        derivarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_derivar_solicitud")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_solicitud_id", Types.BIGINT),
                        new SqlParameter("p_area_codigo", Types.VARCHAR),
                        new SqlParameter("p_usuario_id", Types.BIGINT),
                        new SqlParameter("p_instruccion", Types.VARCHAR));

        historialCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_historial_solicitud")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_solicitud_id", Types.BIGINT))
                .returningResultSet("filas", SolicitudRepository::filaAMapa);
    }

    // ---------- Crear ----------

    public SolicitudCreadaResponse crearLicencia(SolicitudLicenciaRequest r, Long usuarioId) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("p_solicitante_id", r.solicitanteId())
                .addValue("p_representante_id", r.representanteId())
                .addValue("p_establecimiento_id", r.establecimientoId())
                .addValue("p_usuario_id", usuarioId)
                .addValue("p_declaracion_seguridad", r.declaracionSeguridad())
                .addValue("p_observaciones", r.observaciones());
        return leerCreada(crearLicenciaCall.execute(p));
    }

    public SolicitudCreadaResponse crearAnuncio(SolicitudAnuncioRequest r, Long usuarioId) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("p_solicitante_id", r.solicitanteId())
                .addValue("p_representante_id", r.representanteId())
                .addValue("p_establecimiento_id", r.establecimientoId())
                .addValue("p_usuario_id", usuarioId)
                .addValue("p_tipo_elemento", r.tipoElemento())
                .addValue("p_tipo_propiedad", r.tipoPropiedad())
                .addValue("p_material", r.material())
                .addValue("p_alto_m", r.altoM())
                .addValue("p_ancho_m", r.anchoM())
                .addValue("p_numero_caras", r.numeroCaras())
                .addValue("p_tipo_iluminacion", r.tipoIluminacion())
                .addValue("p_contenido", r.contenidoAnuncio())
                .addValue("p_ubicacion", r.descripcionUbicacion())
                .addValue("p_modalidad", r.modalidad())
                .addValue("p_fecha_inicio", aSqlDate(r.fechaInicio()))
                .addValue("p_fecha_fin", aSqlDate(r.fechaFin()))
                .addValue("p_observaciones", r.observaciones());
        return leerCreada(crearAnuncioCall.execute(p));
    }

    public SolicitudCreadaResponse crearDuplicado(SolicitudDuplicadoRequest r, Long usuarioId) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("p_solicitante_id", r.solicitanteId())
                .addValue("p_representante_id", r.representanteId())
                .addValue("p_establecimiento_id", r.establecimientoId())
                .addValue("p_usuario_id", usuarioId)
                .addValue("p_numero_licencia", r.numeroLicencia())
                .addValue("p_motivo", r.motivo())
                .addValue("p_detalle_motivo", r.detalleMotivo())
                .addValue("p_comprobante_pago", r.numeroComprobantePago())
                .addValue("p_fecha_pago", aSqlDate(r.fechaPago()))
                .addValue("p_observaciones", r.observaciones());
        return leerCreada(crearDuplicadoCall.execute(p));
    }

    // ---------- Flujo ----------

    public void presentar(Long solicitudId, Long usuarioId) {
        presentarCall.execute(new MapSqlParameterSource()
                .addValue("p_solicitud_id", solicitudId)
                .addValue("p_usuario_id", usuarioId));
    }

    // ---------- Consultas ----------

    @SuppressWarnings("unchecked")
    public Map<String, Object> obtener(Long solicitudId) {
        Map<String, Object> salida = obtenerCall.execute(
                new MapSqlParameterSource("p_solicitud_id", solicitudId));
        List<Map<String, Object>> filas = (List<Map<String, Object>>) salida.get("filas");
        return filas.isEmpty() ? null : filas.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listar(Long solicitanteId, String estado, String tipo,
                                            String texto, Integer limite, Integer desplazamiento) {
        Map<String, Object> salida = listarCall.execute(new MapSqlParameterSource()
                .addValue("p_solicitante_id", solicitanteId)
                .addValue("p_estado_codigo", estado)
                .addValue("p_tipo_codigo", tipo)
                .addValue("p_texto", texto)
                .addValue("p_limite", limite)
                .addValue("p_desplazamiento", desplazamiento));
        return (List<Map<String, Object>>) salida.get("filas");
    }


    // ---------- Revisión ----------

    public void cambiarEstado(Long solicitudId, String estadoNuevo, Long usuarioId, String comentario) {
        cambiarEstadoCall.execute(new MapSqlParameterSource()
                .addValue("p_solicitud_id", solicitudId)
                .addValue("p_estado_nuevo", estadoNuevo)
                .addValue("p_usuario_id", usuarioId)
                .addValue("p_comentario", comentario));
    }

    public void derivar(Long solicitudId, String areaCodigo, Long usuarioId, String instruccion) {
        derivarCall.execute(new MapSqlParameterSource()
                .addValue("p_solicitud_id", solicitudId)
                .addValue("p_area_codigo", areaCodigo)
                .addValue("p_usuario_id", usuarioId)
                .addValue("p_instruccion", instruccion));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> historial(Long solicitudId) {
        Map<String, Object> salida = historialCall.execute(
                new MapSqlParameterSource("p_solicitud_id", solicitudId));
        return (List<Map<String, Object>>) salida.get("filas");
    }


    // ---------- Auxiliares ----------

    private static SolicitudCreadaResponse leerCreada(Map<String, Object> salida) {
        return new SolicitudCreadaResponse(
                ((Number) salida.get("p_solicitud_id")).longValue(),
                (String) salida.get("p_codigo"));
    }

    private static java.sql.Date aSqlDate(LocalDate fecha) {
        return fecha == null ? null : java.sql.Date.valueOf(fecha);
    }

    // Convierte una fila del ResultSet a Map con claves en camelCase
    private static Map<String, Object> filaAMapa(ResultSet rs, int fila) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int c = 1; c <= md.getColumnCount(); c++) {
            mapa.put(aCamelCase(md.getColumnLabel(c)), rs.getObject(c));
        }
        return mapa;
    }

    private static String aCamelCase(String snake) {
        StringBuilder sb = new StringBuilder();
        boolean mayuscula = false;
        for (char ch : snake.toCharArray()) {
            if (ch == '_') {
                mayuscula = true;
            } else {
                sb.append(mayuscula ? Character.toUpperCase(ch) : ch);
                mayuscula = false;
            }
        }
        return sb.toString();
    }
}