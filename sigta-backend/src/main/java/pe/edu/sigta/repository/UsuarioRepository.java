package pe.edu.sigta.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import pe.edu.sigta.security.UsuarioAuth;

@Repository
public class UsuarioRepository {

    private final SimpleJdbcCall buscarCall;
    private final SimpleJdbcCall registrarCall;
    private final SimpleJdbcCall accesoCall;

    public UsuarioRepository(JdbcTemplate jdbc) {
        buscarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_obtener_usuario_por_correo")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_correo", Types.VARCHAR))
                .returningResultSet("filas", (rs, i) -> new UsuarioAuth(
                        rs.getLong("id"),
                        aLong(rs, "persona_id"),
                        rs.getString("correo_acceso"),
                        rs.getString("password_hash"),
                        rs.getBoolean("activo"),
                        rs.getString("rol")));

        registrarCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_registrar_usuario")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_persona_id", Types.BIGINT),
                        new SqlParameter("p_rol_codigo", Types.VARCHAR),
                        new SqlParameter("p_correo_acceso", Types.VARCHAR),
                        new SqlParameter("p_password_hash", Types.VARCHAR),
                        new SqlOutParameter("p_usuario_id", Types.BIGINT));

        accesoCall = new SimpleJdbcCall(jdbc)
                .withProcedureName("sp_registrar_ultimo_acceso")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_usuario_id", Types.BIGINT));
    }

    @SuppressWarnings("unchecked")
    public UsuarioAuth buscarPorCorreo(String correo) {
        Map<String, Object> salida = buscarCall.execute(new MapSqlParameterSource("p_correo", correo));
        List<UsuarioAuth> filas = (List<UsuarioAuth>) salida.get("filas");
        return filas.isEmpty() ? null : filas.get(0);
    }

    public Long registrar(Long personaId, String rol, String correo, String passwordHash) {
        Map<String, Object> salida = registrarCall.execute(new MapSqlParameterSource()
                .addValue("p_persona_id", personaId)
                .addValue("p_rol_codigo", rol)
                .addValue("p_correo_acceso", correo)
                .addValue("p_password_hash", passwordHash));
        return ((Number) salida.get("p_usuario_id")).longValue();
    }

    public void registrarUltimoAcceso(Long usuarioId) {
        accesoCall.execute(new MapSqlParameterSource("p_usuario_id", usuarioId));
    }

    private static Long aLong(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }
}