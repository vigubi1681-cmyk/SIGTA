package pe.edu.sigta.exception;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Errores lanzados por los procedimientos con SIGNAL SQLSTATE '45000'
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> manejarBaseDeDatos(DataAccessException ex) {
        Throwable causa = ex.getMostSpecificCause();
        if (causa instanceof SQLException sql && "45000".equals(sql.getSQLState())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", sql.getMessage()));
        }
        ex.printStackTrace();
        return ResponseEntity.status(500).body(Map.of("mensaje", "Error interno de base de datos"));
    }

    // Errores de @Valid en los DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(Map.of("mensaje", "Datos inválidos", "errores", errores));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(404).body(Map.of("mensaje", ex.getMessage()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> manejarCredenciales(CredencialesInvalidasException ex) {
        return ResponseEntity.status(401).body(Map.of("mensaje", ex.getMessage()));
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> manejarAccesoDenegado(AccesoDenegadoException ex) {
        return ResponseEntity.status(403).body(Map.of("mensaje", ex.getMessage()));
    }

        @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarRegla(ReglaNegocioException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> manejarTamano(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(413)
                .body(Map.of("mensaje", "El archivo supera el tamaño máximo de 10 MB"));
    }
}