-- SIGTA Chorrillos - Esquema simplificado (14 tablas) para MySQL 8
CREATE DATABASE IF NOT EXISTS sigta_chorrillos
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE sigta_chorrillos;
SET NAMES utf8mb4;

-- ========== CREAR USUARIO SIGTA ==========
CREATE USER 'sigta'@'localhost' IDENTIFIED BY 'Sigta2026!';
GRANT ALL PRIVILEGES ON sigta_chorrillos.* TO 'sigta'@'localhost';
FLUSH PRIVILEGES;
 
-- ========== CATALOGOS ==========
CREATE TABLE roles (
  id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(40) NOT NULL UNIQUE,
  nombre VARCHAR(100) NOT NULL
) ENGINE=InnoDB;
 
CREATE TABLE tipos_tramite (
  id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(40) NOT NULL UNIQUE,
  nombre VARCHAR(180) NOT NULL,
  requiere_establecimiento BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;
 
CREATE TABLE estados_solicitud (
  id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(40) NOT NULL UNIQUE,
  nombre VARCHAR(100) NOT NULL,
  es_final BOOLEAN NOT NULL DEFAULT FALSE,
  orden SMALLINT UNSIGNED NOT NULL
) ENGINE=InnoDB;
 
CREATE TABLE areas_municipales (
  id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(40) NOT NULL UNIQUE,
  nombre VARCHAR(160) NOT NULL
) ENGINE=InnoDB;
 
-- ========== PERSONAS Y USUARIOS ==========
CREATE TABLE personas (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tipo_persona ENUM('NATURAL','JURIDICA') NOT NULL,
  tipo_documento ENUM('DNI','CE','PASAPORTE') NULL,
  numero_documento VARCHAR(20) NULL,
  ruc CHAR(11) NULL,
  nombres VARCHAR(120) NULL,
  apellidos VARCHAR(120) NULL,
  razon_social VARCHAR(180) NULL,
  correo VARCHAR(150) NOT NULL,
  telefono VARCHAR(30) NULL,
  direccion_notificacion VARCHAR(250) NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_persona_documento (tipo_documento, numero_documento),
  UNIQUE KEY uq_persona_ruc (ruc),
  CONSTRAINT chk_persona_identificacion CHECK (
    (tipo_persona='NATURAL' AND nombres IS NOT NULL AND apellidos IS NOT NULL
       AND tipo_documento IS NOT NULL AND numero_documento IS NOT NULL)
    OR
    (tipo_persona='JURIDICA' AND razon_social IS NOT NULL AND ruc IS NOT NULL)
  )
) ENGINE=InnoDB;
 
CREATE TABLE usuarios (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  persona_id BIGINT UNSIGNED NULL,          -- NULL para personal municipal si no se desea registrar persona
  rol_id SMALLINT UNSIGNED NOT NULL,        -- 1 rol por usuario (simplificado)
  correo_acceso VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,      -- BCrypt generado en Spring
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  ultimo_acceso DATETIME NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE SET NULL,
  FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE RESTRICT
) ENGINE=InnoDB;
 
CREATE TABLE establecimientos (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  titular_persona_id BIGINT UNSIGNED NOT NULL,
  nombre_comercial VARCHAR(180) NOT NULL,
  actividad_economica VARCHAR(180) NULL,
  direccion VARCHAR(250) NOT NULL,
  referencia VARCHAR(250) NULL,
  area_m2 DECIMAL(10,2) NULL,
  condicion_titular ENUM('PROPIETARIO','CONDUCTOR','CESIONARIO','OTRO') NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_establecimiento_titular (titular_persona_id),
  FOREIGN KEY (titular_persona_id) REFERENCES personas(id) ON DELETE RESTRICT
) ENGINE=InnoDB;
 
-- ========== SOLICITUDES ==========
CREATE TABLE solicitudes (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  codigo_solicitud VARCHAR(30) NOT NULL UNIQUE,   -- lo genera el SP: LF-2026-000001
  tipo_tramite_id SMALLINT UNSIGNED NOT NULL,
  estado_id SMALLINT UNSIGNED NOT NULL,
  solicitante_persona_id BIGINT UNSIGNED NOT NULL,
  representante_persona_id BIGINT UNSIGNED NULL,
  establecimiento_id BIGINT UNSIGNED NULL,
  area_actual_id SMALLINT UNSIGNED NULL,          -- en que area esta (derivacion)
  usuario_registro_id BIGINT UNSIGNED NULL,
  observaciones_solicitante TEXT NULL,
  fecha_presentacion DATETIME NULL,
  fecha_cierre DATETIME NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_solicitud_estado (estado_id, creado_en),
  INDEX idx_solicitud_solicitante (solicitante_persona_id),
  FOREIGN KEY (tipo_tramite_id) REFERENCES tipos_tramite(id),
  FOREIGN KEY (estado_id) REFERENCES estados_solicitud(id),
  FOREIGN KEY (solicitante_persona_id) REFERENCES personas(id),
  FOREIGN KEY (representante_persona_id) REFERENCES personas(id) ON DELETE SET NULL,
  FOREIGN KEY (establecimiento_id) REFERENCES establecimientos(id) ON DELETE SET NULL,
  FOREIGN KEY (area_actual_id) REFERENCES areas_municipales(id) ON DELETE SET NULL,
  FOREIGN KEY (usuario_registro_id) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB;
 
CREATE TABLE solicitud_licencia_funcionamiento (
  solicitud_id BIGINT UNSIGNED PRIMARY KEY,
  nivel_riesgo ENUM('BAJO','MEDIO','ALTO','MUY_ALTO','PENDIENTE_EVALUACION')
    NOT NULL DEFAULT 'PENDIENTE_EVALUACION',
  declaracion_condiciones_seguridad BOOLEAN NULL,
  numero_certificado_itse VARCHAR(60) NULL,
  FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id) ON DELETE CASCADE
) ENGINE=InnoDB;
 
CREATE TABLE solicitud_autorizacion_anuncio (
  solicitud_id BIGINT UNSIGNED PRIMARY KEY,
  tipo_elemento VARCHAR(100) NOT NULL,
  tipo_propiedad ENUM('DOMINIO_PRIVADO','DOMINIO_PUBLICO') NOT NULL,
  material VARCHAR(100) NULL,
  alto_m DECIMAL(10,2) NOT NULL,
  ancho_m DECIMAL(10,2) NOT NULL,
  numero_caras SMALLINT UNSIGNED NULL,
  tipo_iluminacion ENUM('SIN_ILUMINACION','ILUMINADO','LUMINOSO') NOT NULL DEFAULT 'SIN_ILUMINACION',
  contenido_anuncio TEXT NOT NULL,
  descripcion_ubicacion VARCHAR(500) NULL,
  modalidad ENUM('PERMANENTE','TEMPORAL') NOT NULL DEFAULT 'PERMANENTE',
  fecha_inicio DATE NULL,
  fecha_fin DATE NULL,
  FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id) ON DELETE CASCADE,
  CONSTRAINT chk_anuncio_dim CHECK (alto_m>0 AND ancho_m>0),
  CONSTRAINT chk_anuncio_fechas CHECK (fecha_fin IS NULL OR fecha_inicio IS NULL OR fecha_fin>=fecha_inicio)
) ENGINE=InnoDB;
 
CREATE TABLE documentos_adjuntos (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  solicitud_id BIGINT UNSIGNED NOT NULL,
  tipo_documento VARCHAR(120) NOT NULL,
  nombre_archivo VARCHAR(255) NOT NULL,
  ruta_almacenamiento VARCHAR(500) NOT NULL,   -- el archivo vive en disco, no en MySQL
  mime_type VARCHAR(120) NOT NULL,
  tamanio_bytes BIGINT UNSIGNED NOT NULL,
  estado_revision ENUM('PENDIENTE','VALIDADO','OBSERVADO','RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
  observacion_revision VARCHAR(500) NULL,
  cargado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_adjunto_solicitud (solicitud_id),
  FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id) ON DELETE CASCADE
) ENGINE=InnoDB;
 
-- Historial = trazabilidad + observaciones + derivaciones (todo en una tabla)
CREATE TABLE historial_solicitud (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  solicitud_id BIGINT UNSIGNED NOT NULL,
  estado_anterior_id SMALLINT UNSIGNED NULL,
  estado_nuevo_id SMALLINT UNSIGNED NOT NULL,
  usuario_id BIGINT UNSIGNED NULL,
  area_destino_id SMALLINT UNSIGNED NULL,      -- solo cuando es una derivacion
  comentario VARCHAR(1000) NULL,               -- observacion / instruccion / motivo
  registrado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_historial (solicitud_id, registrado_en),
  FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id) ON DELETE CASCADE,
  FOREIGN KEY (estado_anterior_id) REFERENCES estados_solicitud(id),
  FOREIGN KEY (estado_nuevo_id) REFERENCES estados_solicitud(id),
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
  FOREIGN KEY (area_destino_id) REFERENCES areas_municipales(id) ON DELETE SET NULL
) ENGINE=InnoDB;
 
-- ========== DOCUMENTOS EMITIDOS (PDF + QR) ==========
CREATE TABLE documentos_emitidos (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  solicitud_id BIGINT UNSIGNED NOT NULL,
  documento_original_id BIGINT UNSIGNED NULL,  -- para duplicados
  tipo_documento ENUM('LICENCIA_FUNCIONAMIENTO','AUTORIZACION_PUBLICIDAD','DUPLICADO_LICENCIA') NOT NULL,
  numero_documento VARCHAR(80) NOT NULL UNIQUE,
  fecha_emision DATE NOT NULL,
  fecha_vencimiento DATE NULL,
  estado ENUM('VIGENTE','SUSPENDIDO','REVOCADO','VENCIDO') NOT NULL DEFAULT 'VIGENTE',
  archivo_pdf VARCHAR(500) NULL,
  token_verificacion_hash CHAR(64) NOT NULL UNIQUE,  -- SHA-256 del token del QR
  emitido_por_usuario_id BIGINT UNSIGNED NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_emitido_solicitud (solicitud_id),
  FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id),
  FOREIGN KEY (documento_original_id) REFERENCES documentos_emitidos(id) ON DELETE SET NULL,
  FOREIGN KEY (emitido_por_usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
  CONSTRAINT chk_doc_fechas CHECK (fecha_vencimiento IS NULL OR fecha_vencimiento>=fecha_emision)
) ENGINE=InnoDB;
 
CREATE TABLE solicitud_duplicado_licencia (
  solicitud_id BIGINT UNSIGNED PRIMARY KEY,
  documento_original_id BIGINT UNSIGNED NULL,   -- se llena si la licencia existe en el sistema
  numero_licencia_declarado VARCHAR(60) NOT NULL,
  motivo ENUM('PERDIDA','DETERIORO','ROBO','OTRO') NOT NULL,
  detalle_motivo VARCHAR(500) NULL,
  numero_comprobante_pago VARCHAR(80) NULL,
  fecha_pago DATE NULL,
  FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id) ON DELETE CASCADE,
  FOREIGN KEY (documento_original_id) REFERENCES documentos_emitidos(id) ON DELETE SET NULL
) ENGINE=InnoDB;
 
-- ========== DATOS INICIALES ==========
INSERT INTO roles(codigo,nombre) VALUES
('CIUDADANO','Ciudadano'),('MESA_PARTES','Mesa de partes'),
('EVALUADOR','Evaluador'),('ADMINISTRADOR','Administrador');
 
INSERT INTO tipos_tramite(codigo,nombre,requiere_establecimiento) VALUES
('LIC_FUNC','Licencia de funcionamiento',TRUE),
('AUT_ANUNCIO','Autorización de elementos de publicidad exterior',FALSE),
('DUP_LIC','Duplicado de licencia de funcionamiento',TRUE);
 
INSERT INTO estados_solicitud(codigo,nombre,es_final,orden) VALUES
('BORRADOR','Borrador',FALSE,1),('PRESENTADA','Presentada',FALSE,2),
('EN_REVISION','En revisión',FALSE,3),('OBSERVADA','Observada',FALSE,4),
('SUBSANADA','Subsanada',FALSE,5),('DERIVADA','Derivada',FALSE,6),
('APROBADA','Aprobada',TRUE,7),('DENEGADA','Denegada',TRUE,8),('CANCELADA','Cancelada',TRUE,9);
 
INSERT INTO areas_municipales(codigo,nombre) VALUES
('MESA_PARTES','Mesa de Partes'),
('COMERCIALIZACION','Subgerencia de Comercialización y Licencias'),
('OBRAS_PRIVADAS','Subgerencia de Obras Privadas y Control Urbano'),
('ARCHIVO','Unidad de Administración Documentaria y Archivo');
 
-- ========== VISTA ==========
CREATE OR REPLACE VIEW vw_solicitudes_resumen AS
SELECT s.id, s.codigo_solicitud, tt.codigo AS tipo_tramite, tt.nombre AS nombre_tramite,
       es.codigo AS estado_codigo, es.nombre AS estado,
       CASE WHEN p.tipo_persona='NATURAL' THEN CONCAT_WS(' ',p.nombres,p.apellidos)
            ELSE p.razon_social END AS solicitante,
       p.numero_documento, p.ruc, e.nombre_comercial, e.direccion AS direccion_establecimiento,
       a.nombre AS area_actual, s.fecha_presentacion, s.creado_en
FROM solicitudes s
JOIN tipos_tramite tt ON tt.id=s.tipo_tramite_id
JOIN estados_solicitud es ON es.id=s.estado_id
JOIN personas p ON p.id=s.solicitante_persona_id
LEFT JOIN establecimientos e ON e.id=s.establecimiento_id
LEFT JOIN areas_municipales a ON a.id=s.area_actual_id;