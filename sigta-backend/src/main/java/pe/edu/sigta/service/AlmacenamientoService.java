package pe.edu.sigta.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.sigta.exception.RecursoNoEncontradoException;

@Service
public class AlmacenamientoService {

    private final Path base;

    public AlmacenamientoService(@Value("${sigta.storage.ruta:./uploads}") String ruta) {
        this.base = Paths.get(ruta).toAbsolutePath().normalize();
    }

    // Detecta el tipo REAL por los primeros bytes; devuelve null si no es PDF, PNG ni JPG
    public String detectarMime(MultipartFile archivo) {
        try (InputStream in = archivo.getInputStream()) {
            byte[] h = in.readNBytes(8);
            if (h.length >= 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F') {
                return "application/pdf";
            }
            if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
                return "image/png";
            }
            if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            return null;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo", e);
        }
    }

    // Guarda con nombre aleatorio y devuelve la ruta RELATIVA (la que se registra en la base)
    public String guardar(Long solicitudId, MultipartFile archivo, String mime) {
        try {
            Path dir = base.resolve("solicitudes").resolve(String.valueOf(solicitudId));
            Files.createDirectories(dir);
            String nombre = UUID.randomUUID() + "." + extension(mime);
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, dir.resolve(nombre));
            }
            return "solicitudes/" + solicitudId + "/" + nombre;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }
    }

    public Resource cargar(String rutaRelativa) {
        Path archivo = resolverSeguro(rutaRelativa);
        if (archivo == null || !Files.exists(archivo)) {
            throw new RecursoNoEncontradoException("El archivo no se encuentra en el servidor");
        }
        return new FileSystemResource(archivo);
    }

    public void eliminar(String rutaRelativa) {
        try {
            Path archivo = resolverSeguro(rutaRelativa);
            if (archivo != null) {
                Files.deleteIfExists(archivo);
            }
        } catch (IOException e) {
            // Si no se puede borrar no interrumpimos el flujo principal
        }
    }

    // Nombre original limpio, solo para mostrar (nunca se usa como nombre en disco)
    public String nombreSeguro(MultipartFile archivo) {
        String original = archivo.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "archivo";
        }
        String nombre = original.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
        if (nombre.isEmpty()) {
            nombre = "archivo";
        }
        return nombre.length() > 255 ? nombre.substring(0, 255) : nombre;
    }

    // Evita salirse de la carpeta de archivos (path traversal)
    private Path resolverSeguro(String rutaRelativa) {
        Path destino = base.resolve(rutaRelativa).normalize();
        return destino.startsWith(base) ? destino : null;
    }

    private static String extension(String mime) {
        return switch (mime) {
            case "application/pdf" -> "pdf";
            case "image/png" -> "png";
            default -> "jpg";
        };
    }
}