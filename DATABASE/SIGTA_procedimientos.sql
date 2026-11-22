-- SIGTA Chorrillos - Procedimientos almacenados (MySQL 8)
-- Requiere haber ejecutado SIGTA_MySQL_8.sql
-- En Workbench / CLI se necesita DELIMITER; desde Spring (JDBC) se invocan con CALL sp_xxx(...).
--
-- Convenciones:
--  * Errores de negocio: SIGNAL SQLSTATE '45000' con MESSAGE_TEXT claro (Spring los recibe como excepcion SQL).
--  * Cada SP que escribe usa transaccion y hace ROLLBACK ante cualquier error.
--  * La seguridad por usuario (un ciudadano solo ve lo suyo) se valida en Java; aqui se valida el negocio.

USE sigta_chorrillos;
DELIMITER $$

-- =====================================================================
-- 1. PERSONAS / USUARIOS / ESTABLECIMIENTOS
-- =====================================================================

DROP PROCEDURE IF EXISTS sp_registrar_persona$$
CREATE PROCEDURE sp_registrar_persona(
  IN  p_tipo_persona   VARCHAR(10),
  IN  p_tipo_documento VARCHAR(10),
  IN  p_numero_doc     VARCHAR(20),
  IN  p_ruc            CHAR(11),
  IN  p_nombres        VARCHAR(120),
  IN  p_apellidos      VARCHAR(120),
  IN  p_razon_social   VARCHAR(180),
  IN  p_correo         VARCHAR(150),
  IN  p_telefono       VARCHAR(30),
  IN  p_direccion      VARCHAR(250),
  OUT p_persona_id     BIGINT UNSIGNED
)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  IF p_tipo_persona NOT IN ('NATURAL','JURIDICA') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tipo de persona invalido';
  END IF;
  IF p_correo IS NULL OR p_correo NOT REGEXP '^[^@ ]+@[^@ ]+\\.[^@ ]+$' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Correo electronico invalido';
  END IF;
  IF p_telefono IS NULL OR TRIM(p_telefono) = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El telefono es obligatorio';
  END IF;

  IF p_tipo_persona = 'NATURAL' THEN
    IF p_tipo_documento IS NULL OR p_numero_doc IS NULL
       OR TRIM(IFNULL(p_nombres,'')) = '' OR TRIM(IFNULL(p_apellidos,'')) = '' THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Persona natural: documento, nombres y apellidos son obligatorios';
    END IF;
    IF p_tipo_documento = 'DNI' AND p_numero_doc NOT REGEXP '^[0-9]{8}$' THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El DNI debe tener 8 digitos';
    END IF;
  ELSE
    IF p_ruc IS NULL OR p_ruc NOT REGEXP '^[0-9]{11}$' OR TRIM(IFNULL(p_razon_social,'')) = '' THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Persona juridica: RUC (11 digitos) y razon social son obligatorios';
    END IF;
  END IF;

  START TRANSACTION;

  -- Busca si ya existe; si existe solo actualiza datos de contacto
  IF p_tipo_persona = 'NATURAL' THEN
    SELECT id INTO p_persona_id FROM personas
     WHERE tipo_documento = p_tipo_documento AND numero_documento = p_numero_doc LIMIT 1;
  ELSE
    SELECT id INTO p_persona_id FROM personas WHERE ruc = p_ruc LIMIT 1;
  END IF;

  IF p_persona_id IS NULL THEN
    INSERT INTO personas(tipo_persona,tipo_documento,numero_documento,ruc,nombres,apellidos,
                         razon_social,correo,telefono,direccion_notificacion)
    VALUES (p_tipo_persona,p_tipo_documento,p_numero_doc,p_ruc,TRIM(p_nombres),TRIM(p_apellidos),
            TRIM(p_razon_social),p_correo,p_telefono,p_direccion);
    SET p_persona_id = LAST_INSERT_ID();
  ELSE
    UPDATE personas
       SET correo = p_correo, telefono = p_telefono,
           direccion_notificacion = COALESCE(p_direccion, direccion_notificacion)
     WHERE id = p_persona_id;
  END IF;

  COMMIT;
END$$


DROP PROCEDURE IF EXISTS sp_registrar_usuario$$
CREATE PROCEDURE sp_registrar_usuario(
  IN  p_persona_id    BIGINT UNSIGNED,
  IN  p_rol_codigo    VARCHAR(40),
  IN  p_correo_acceso VARCHAR(150),
  IN  p_password_hash VARCHAR(255),   -- BCrypt generado en Spring, NUNCA texto plano
  OUT p_usuario_id    BIGINT UNSIGNED
)
BEGIN
  DECLARE v_rol SMALLINT UNSIGNED;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  SELECT id INTO v_rol FROM roles WHERE codigo = p_rol_codigo;
  IF v_rol IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rol no existe';
  END IF;
  IF EXISTS (SELECT 1 FROM usuarios WHERE correo_acceso = p_correo_acceso) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe un usuario con ese correo';
  END IF;
  IF p_password_hash IS NULL OR CHAR_LENGTH(p_password_hash) < 30 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'password_hash invalido (debe ser un hash, no texto plano)';
  END IF;

  START TRANSACTION;
  INSERT INTO usuarios(persona_id,rol_id,correo_acceso,password_hash)
  VALUES (p_persona_id,v_rol,p_correo_acceso,p_password_hash);
  SET p_usuario_id = LAST_INSERT_ID();
  COMMIT;
END$$


DROP PROCEDURE IF EXISTS sp_obtener_usuario_por_correo$$
CREATE PROCEDURE sp_obtener_usuario_por_correo(IN p_correo VARCHAR(150))
BEGIN
  -- Para el login en Spring Security (el hash se compara en Java con BCrypt)
  SELECT u.id, u.persona_id, u.correo_acceso, u.password_hash, u.activo,
         r.codigo AS rol
    FROM usuarios u JOIN roles r ON r.id = u.rol_id
   WHERE u.correo_acceso = p_correo;
END$$


DROP PROCEDURE IF EXISTS sp_registrar_ultimo_acceso$$
CREATE PROCEDURE sp_registrar_ultimo_acceso(IN p_usuario_id BIGINT UNSIGNED)
BEGIN
  UPDATE usuarios SET ultimo_acceso = NOW() WHERE id = p_usuario_id;
END$$


DROP PROCEDURE IF EXISTS sp_registrar_establecimiento$$
CREATE PROCEDURE sp_registrar_establecimiento(
  IN  p_titular_id   BIGINT UNSIGNED,
  IN  p_nombre       VARCHAR(180),
  IN  p_actividad    VARCHAR(180),
  IN  p_direccion    VARCHAR(250),
  IN  p_referencia   VARCHAR(250),
  IN  p_area_m2      DECIMAL(10,2),
  IN  p_condicion    VARCHAR(20),
  OUT p_establecimiento_id BIGINT UNSIGNED
)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  IF NOT EXISTS (SELECT 1 FROM personas WHERE id = p_titular_id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El titular no existe';
  END IF;
  IF TRIM(IFNULL(p_nombre,'')) = '' OR TRIM(IFNULL(p_direccion,'')) = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Nombre comercial y direccion son obligatorios';
  END IF;
  IF p_area_m2 IS NOT NULL AND p_area_m2 <= 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El area debe ser mayor a 0';
  END IF;

  START TRANSACTION;
  INSERT INTO establecimientos(titular_persona_id,nombre_comercial,actividad_economica,
                               direccion,referencia,area_m2,condicion_titular)
  VALUES (p_titular_id,TRIM(p_nombre),p_actividad,TRIM(p_direccion),p_referencia,p_area_m2,p_condicion);
  SET p_establecimiento_id = LAST_INSERT_ID();
  COMMIT;
END$$


-- =====================================================================
-- 2. CREACION DE SOLICITUDES (estado BORRADOR)
-- =====================================================================

-- Helper interno: NO abre transaccion (la abre el SP que lo llama).
DROP PROCEDURE IF EXISTS sp_interno_crear_solicitud$$
CREATE PROCEDURE sp_interno_crear_solicitud(
  IN  p_tipo_codigo      VARCHAR(40),
  IN  p_solicitante_id   BIGINT UNSIGNED,
  IN  p_representante_id BIGINT UNSIGNED,
  IN  p_establecimiento_id BIGINT UNSIGNED,
  IN  p_usuario_id       BIGINT UNSIGNED,
  IN  p_observaciones    TEXT,
  OUT p_solicitud_id     BIGINT UNSIGNED,
  OUT p_codigo           VARCHAR(30)
)
BEGIN
  DECLARE v_tipo SMALLINT UNSIGNED;
  DECLARE v_req_est BOOLEAN;
  DECLARE v_estado SMALLINT UNSIGNED;
  DECLARE v_prefijo VARCHAR(4);

  SELECT id, requiere_establecimiento INTO v_tipo, v_req_est
    FROM tipos_tramite WHERE codigo = p_tipo_codigo;
  IF v_tipo IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tipo de tramite no existe';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM personas WHERE id = p_solicitante_id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El solicitante no existe';
  END IF;

  IF v_req_est AND p_establecimiento_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Este tramite requiere un establecimiento';
  END IF;
  IF p_establecimiento_id IS NOT NULL AND NOT EXISTS (
       SELECT 1 FROM establecimientos
        WHERE id = p_establecimiento_id AND titular_persona_id = p_solicitante_id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El establecimiento no pertenece al solicitante';
  END IF;

  SELECT id INTO v_estado FROM estados_solicitud WHERE codigo = 'BORRADOR';
  SET v_prefijo = CASE p_tipo_codigo
                    WHEN 'LIC_FUNC' THEN 'LF'
                    WHEN 'AUT_ANUNCIO' THEN 'AA'
                    ELSE 'DL' END;

  -- Codigo temporal unico de 30 caracteres; se reemplaza enseguida con el id real
  INSERT INTO solicitudes(codigo_solicitud,tipo_tramite_id,estado_id,solicitante_persona_id,
                          representante_persona_id,establecimiento_id,usuario_registro_id,
                          observaciones_solicitante)
  VALUES (LEFT(REPLACE(UUID(),'-',''),30),v_tipo,v_estado,p_solicitante_id,p_representante_id,
          p_establecimiento_id,p_usuario_id,p_observaciones);
  SET p_solicitud_id = LAST_INSERT_ID();
  SET p_codigo = CONCAT(v_prefijo,'-',YEAR(NOW()),'-',LPAD(p_solicitud_id,6,'0'));
  UPDATE solicitudes SET codigo_solicitud = p_codigo WHERE id = p_solicitud_id;

  INSERT INTO historial_solicitud(solicitud_id,estado_anterior_id,estado_nuevo_id,usuario_id,comentario)
  VALUES (p_solicitud_id,NULL,v_estado,p_usuario_id,'Solicitud creada en borrador');
END$$


DROP PROCEDURE IF EXISTS sp_crear_solicitud_licencia$$
CREATE PROCEDURE sp_crear_solicitud_licencia(
  IN  p_solicitante_id     BIGINT UNSIGNED,
  IN  p_representante_id   BIGINT UNSIGNED,
  IN  p_establecimiento_id BIGINT UNSIGNED,
  IN  p_usuario_id         BIGINT UNSIGNED,
  IN  p_declaracion_seguridad BOOLEAN,
  IN  p_observaciones      TEXT,
  OUT p_solicitud_id       BIGINT UNSIGNED,
  OUT p_codigo             VARCHAR(30)
)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  CALL sp_interno_crear_solicitud('LIC_FUNC',p_solicitante_id,p_representante_id,
       p_establecimiento_id,p_usuario_id,p_observaciones,p_solicitud_id,p_codigo);
  INSERT INTO solicitud_licencia_funcionamiento(solicitud_id,declaracion_condiciones_seguridad)
  VALUES (p_solicitud_id,p_declaracion_seguridad);
  COMMIT;
END$$


DROP PROCEDURE IF EXISTS sp_crear_solicitud_anuncio$$
CREATE PROCEDURE sp_crear_solicitud_anuncio(
  IN  p_solicitante_id     BIGINT UNSIGNED,
  IN  p_representante_id   BIGINT UNSIGNED,
  IN  p_establecimiento_id BIGINT UNSIGNED,   -- opcional
  IN  p_usuario_id         BIGINT UNSIGNED,
  IN  p_tipo_elemento      VARCHAR(100),
  IN  p_tipo_propiedad     VARCHAR(20),       -- DOMINIO_PRIVADO | DOMINIO_PUBLICO
  IN  p_material           VARCHAR(100),
  IN  p_alto_m             DECIMAL(10,2),
  IN  p_ancho_m            DECIMAL(10,2),
  IN  p_numero_caras       SMALLINT UNSIGNED,
  IN  p_tipo_iluminacion   VARCHAR(20),
  IN  p_contenido          TEXT,
  IN  p_ubicacion          VARCHAR(500),
  IN  p_modalidad          VARCHAR(20),
  IN  p_fecha_inicio       DATE,
  IN  p_fecha_fin          DATE,
  IN  p_observaciones      TEXT,
  OUT p_solicitud_id       BIGINT UNSIGNED,
  OUT p_codigo             VARCHAR(30)
)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  IF TRIM(IFNULL(p_tipo_elemento,'')) = '' OR TRIM(IFNULL(p_contenido,'')) = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tipo de elemento y descripcion del anuncio son obligatorios';
  END IF;
  IF p_alto_m IS NULL OR p_ancho_m IS NULL OR p_alto_m <= 0 OR p_ancho_m <= 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Alto y ancho deben ser mayores a 0';
  END IF;
  IF p_modalidad = 'TEMPORAL' AND (p_fecha_inicio IS NULL OR p_fecha_fin IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una modalidad temporal requiere fecha de inicio y fin';
  END IF;
  IF p_fecha_inicio IS NOT NULL AND p_fecha_fin IS NOT NULL AND p_fecha_fin < p_fecha_inicio THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La fecha fin no puede ser menor a la fecha inicio';
  END IF;

  START TRANSACTION;
  CALL sp_interno_crear_solicitud('AUT_ANUNCIO',p_solicitante_id,p_representante_id,
       p_establecimiento_id,p_usuario_id,p_observaciones,p_solicitud_id,p_codigo);
  INSERT INTO solicitud_autorizacion_anuncio(solicitud_id,tipo_elemento,tipo_propiedad,material,
         alto_m,ancho_m,numero_caras,tipo_iluminacion,contenido_anuncio,descripcion_ubicacion,
         modalidad,fecha_inicio,fecha_fin)
  VALUES (p_solicitud_id,TRIM(p_tipo_elemento),p_tipo_propiedad,p_material,p_alto_m,p_ancho_m,
          p_numero_caras,IFNULL(p_tipo_iluminacion,'SIN_ILUMINACION'),p_contenido,p_ubicacion,
          IFNULL(p_modalidad,'PERMANENTE'),p_fecha_inicio,p_fecha_fin);
  COMMIT;
END$$


DROP PROCEDURE IF EXISTS sp_crear_solicitud_duplicado$$
CREATE PROCEDURE sp_crear_solicitud_duplicado(
  IN  p_solicitante_id     BIGINT UNSIGNED,
  IN  p_representante_id   BIGINT UNSIGNED,
  IN  p_establecimiento_id BIGINT UNSIGNED,
  IN  p_usuario_id         BIGINT UNSIGNED,
  IN  p_numero_licencia    VARCHAR(60),
  IN  p_motivo             VARCHAR(20),       -- PERDIDA | DETERIORO | ROBO | OTRO
  IN  p_detalle_motivo     VARCHAR(500),
  IN  p_comprobante_pago   VARCHAR(80),
  IN  p_fecha_pago         DATE,
  IN  p_observaciones      TEXT,
  OUT p_solicitud_id       BIGINT UNSIGNED,
  OUT p_codigo             VARCHAR(30)
)
BEGIN
  DECLARE v_original BIGINT UNSIGNED;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  IF TRIM(IFNULL(p_numero_licencia,'')) = '' OR p_motivo IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Numero de licencia y motivo son obligatorios';
  END IF;
  IF p_motivo = 'OTRO' AND TRIM(IFNULL(p_detalle_motivo,'')) = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Detalle el motivo cuando selecciona OTRO';
  END IF;

  -- Si la licencia fue emitida por el sistema, se vincula; si no, queda solo la declaracion
  SELECT id INTO v_original FROM documentos_emitidos
   WHERE numero_documento = p_numero_licencia AND tipo_documento = 'LICENCIA_FUNCIONAMIENTO'
   LIMIT 1;

  START TRANSACTION;
  CALL sp_interno_crear_solicitud('DUP_LIC',p_solicitante_id,p_representante_id,
       p_establecimiento_id,p_usuario_id,p_observaciones,p_solicitud_id,p_codigo);
  INSERT INTO solicitud_duplicado_licencia(solicitud_id,documento_original_id,numero_licencia_declarado,
         motivo,detalle_motivo,numero_comprobante_pago,fecha_pago)
  VALUES (p_solicitud_id,v_original,TRIM(p_numero_licencia),p_motivo,p_detalle_motivo,
          p_comprobante_pago,p_fecha_pago);
  COMMIT;
END$$


-- =====================================================================
-- 3. ADJUNTOS
-- =====================================================================

DROP PROCEDURE IF EXISTS sp_registrar_adjunto$$
CREATE PROCEDURE sp_registrar_adjunto(
  IN  p_solicitud_id BIGINT UNSIGNED,
  IN  p_tipo_documento VARCHAR(120),
  IN  p_nombre_archivo VARCHAR(255),
  IN  p_ruta           VARCHAR(500),
  IN  p_mime           VARCHAR(120),
  IN  p_tamanio        BIGINT UNSIGNED,
  OUT p_adjunto_id     BIGINT UNSIGNED
)
BEGIN
  DECLARE v_final BOOLEAN;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  SELECT es.es_final INTO v_final
    FROM solicitudes s JOIN estados_solicitud es ON es.id = s.estado_id
   WHERE s.id = p_solicitud_id;
  IF v_final IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud no existe';
  END IF;
  IF v_final THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se pueden adjuntar archivos a una solicitud cerrada';
  END IF;
  IF p_mime NOT IN ('application/pdf','image/png','image/jpeg') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tipo de archivo no permitido (solo PDF, PNG o JPG)';
  END IF;
  IF p_tamanio IS NULL OR p_tamanio = 0 OR p_tamanio > 10485760 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El archivo debe pesar entre 1 byte y 10 MB';
  END IF;

  START TRANSACTION;
  INSERT INTO documentos_adjuntos(solicitud_id,tipo_documento,nombre_archivo,ruta_almacenamiento,
                                  mime_type,tamanio_bytes)
  VALUES (p_solicitud_id,p_tipo_documento,p_nombre_archivo,p_ruta,p_mime,p_tamanio);
  SET p_adjunto_id = LAST_INSERT_ID();
  COMMIT;
END$$


DROP PROCEDURE IF EXISTS sp_revisar_adjunto$$
CREATE PROCEDURE sp_revisar_adjunto(
  IN p_adjunto_id BIGINT UNSIGNED,
  IN p_estado     VARCHAR(20),     -- VALIDADO | OBSERVADO | RECHAZADO
  IN p_observacion VARCHAR(500)
)
BEGIN
  IF p_estado NOT IN ('VALIDADO','OBSERVADO','RECHAZADO') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Estado de revision invalido';
  END IF;
  IF p_estado <> 'VALIDADO' AND TRIM(IFNULL(p_observacion,'')) = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Indique la observacion de la revision';
  END IF;
  UPDATE documentos_adjuntos
     SET estado_revision = p_estado, observacion_revision = p_observacion
   WHERE id = p_adjunto_id;
  IF ROW_COUNT() = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Adjunto no existe';
  END IF;
END$$


-- =====================================================================
-- 4. FLUJO DE ESTADOS
-- =====================================================================

-- Presentar: BORRADOR -> PRESENTADA (lo ejecuta el ciudadano o mesa de partes)
DROP PROCEDURE IF EXISTS sp_presentar_solicitud$$
CREATE PROCEDURE sp_presentar_solicitud(
  IN p_solicitud_id BIGINT UNSIGNED,
  IN p_usuario_id   BIGINT UNSIGNED
)
BEGIN
  DECLARE v_estado_cod VARCHAR(40);
  DECLARE v_tipo_cod   VARCHAR(40);
  DECLARE v_est_id     BIGINT UNSIGNED;
  DECLARE v_nuevo      SMALLINT UNSIGNED;
  DECLARE v_anterior   SMALLINT UNSIGNED;
  DECLARE v_area       SMALLINT UNSIGNED;
  DECLARE v_detalle    INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  SELECT es.codigo, tt.codigo, s.establecimiento_id, s.estado_id
    INTO v_estado_cod, v_tipo_cod, v_est_id, v_anterior
    FROM solicitudes s
    JOIN estados_solicitud es ON es.id = s.estado_id
    JOIN tipos_tramite tt ON tt.id = s.tipo_tramite_id
   WHERE s.id = p_solicitud_id;

  IF v_estado_cod IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud no existe';
  END IF;
  IF v_estado_cod <> 'BORRADOR' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se puede presentar una solicitud en borrador';
  END IF;

  -- Debe existir el detalle del tramite correspondiente
  IF v_tipo_cod = 'LIC_FUNC' THEN
    SELECT COUNT(*) INTO v_detalle FROM solicitud_licencia_funcionamiento WHERE solicitud_id = p_solicitud_id;
  ELSEIF v_tipo_cod = 'AUT_ANUNCIO' THEN
    SELECT COUNT(*) INTO v_detalle FROM solicitud_autorizacion_anuncio WHERE solicitud_id = p_solicitud_id;
  ELSE
    SELECT COUNT(*) INTO v_detalle FROM solicitud_duplicado_licencia WHERE solicitud_id = p_solicitud_id;
  END IF;
  IF v_detalle = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud no tiene los datos especificos del tramite';
  END IF;

  SELECT id INTO v_nuevo FROM estados_solicitud WHERE codigo = 'PRESENTADA';
  SELECT id INTO v_area  FROM areas_municipales WHERE codigo = 'MESA_PARTES';

  START TRANSACTION;
  UPDATE solicitudes
     SET estado_id = v_nuevo, area_actual_id = v_area, fecha_presentacion = NOW()
   WHERE id = p_solicitud_id;
  INSERT INTO historial_solicitud(solicitud_id,estado_anterior_id,estado_nuevo_id,usuario_id,comentario)
  VALUES (p_solicitud_id,v_anterior,v_nuevo,p_usuario_id,'Solicitud presentada');
  COMMIT;
END$$


-- Cambio de estado generico con validacion de transiciones
-- (APROBADA no se permite aqui: se hace con sp_aprobar_y_emitir_documento)
DROP PROCEDURE IF EXISTS sp_cambiar_estado$$
CREATE PROCEDURE sp_cambiar_estado(
  IN p_solicitud_id  BIGINT UNSIGNED,
  IN p_estado_nuevo  VARCHAR(40),
  IN p_usuario_id    BIGINT UNSIGNED,
  IN p_comentario    VARCHAR(1000)
)
BEGIN
  DECLARE v_actual_cod VARCHAR(40);
  DECLARE v_actual_id  SMALLINT UNSIGNED;
  DECLARE v_nuevo_id   SMALLINT UNSIGNED;
  DECLARE v_nuevo_final BOOLEAN;
  DECLARE v_tipo_cod   VARCHAR(40);
  DECLARE v_permitido  BOOLEAN DEFAULT FALSE;
  DECLARE v_area_cod   VARCHAR(40) DEFAULT NULL;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  SELECT es.codigo, es.id, tt.codigo INTO v_actual_cod, v_actual_id, v_tipo_cod
    FROM solicitudes s
    JOIN estados_solicitud es ON es.id = s.estado_id
    JOIN tipos_tramite tt ON tt.id = s.tipo_tramite_id
   WHERE s.id = p_solicitud_id;
  IF v_actual_cod IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud no existe';
  END IF;

  SELECT id, es_final INTO v_nuevo_id, v_nuevo_final FROM estados_solicitud WHERE codigo = p_estado_nuevo;
  IF v_nuevo_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Estado destino no existe';
  END IF;
  IF p_estado_nuevo = 'APROBADA' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Para aprobar use sp_aprobar_y_emitir_documento';
  END IF;
  IF p_estado_nuevo = 'DERIVADA' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Para derivar use sp_derivar_solicitud';
  END IF;

  -- Matriz de transiciones permitidas
  SET v_permitido = CASE
    WHEN v_actual_cod = 'PRESENTADA' AND p_estado_nuevo IN ('EN_REVISION','CANCELADA') THEN TRUE
    WHEN v_actual_cod = 'EN_REVISION' AND p_estado_nuevo IN ('OBSERVADA','DENEGADA') THEN TRUE
    WHEN v_actual_cod = 'OBSERVADA'  AND p_estado_nuevo IN ('SUBSANADA','CANCELADA') THEN TRUE
    WHEN v_actual_cod = 'SUBSANADA'  AND p_estado_nuevo = 'EN_REVISION' THEN TRUE
    WHEN v_actual_cod = 'DERIVADA'   AND p_estado_nuevo = 'EN_REVISION' THEN TRUE
    WHEN v_actual_cod = 'BORRADOR'   AND p_estado_nuevo = 'CANCELADA' THEN TRUE
    ELSE FALSE END;
  IF NOT v_permitido THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Transicion de estado no permitida';
  END IF;

  IF p_estado_nuevo IN ('OBSERVADA','DENEGADA') AND TRIM(IFNULL(p_comentario,'')) = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Debe registrar el detalle de la observacion o motivo';
  END IF;

  -- Al pasar a revision desde mesa de partes, se asigna el area responsable segun el tramite
  IF v_actual_cod = 'PRESENTADA' AND p_estado_nuevo = 'EN_REVISION' THEN
    SET v_area_cod = IF(v_tipo_cod = 'AUT_ANUNCIO','OBRAS_PRIVADAS','COMERCIALIZACION');
  END IF;

  START TRANSACTION;
  UPDATE solicitudes
     SET estado_id = v_nuevo_id,
         area_actual_id = COALESCE((SELECT id FROM areas_municipales WHERE codigo = v_area_cod), area_actual_id),
         fecha_cierre = IF(v_nuevo_final, NOW(), fecha_cierre)
   WHERE id = p_solicitud_id;
  INSERT INTO historial_solicitud(solicitud_id,estado_anterior_id,estado_nuevo_id,usuario_id,comentario)
  VALUES (p_solicitud_id,v_actual_id,v_nuevo_id,p_usuario_id,p_comentario);
  COMMIT;
END$$


DROP PROCEDURE IF EXISTS sp_derivar_solicitud$$
CREATE PROCEDURE sp_derivar_solicitud(
  IN p_solicitud_id BIGINT UNSIGNED,
  IN p_area_codigo  VARCHAR(40),
  IN p_usuario_id   BIGINT UNSIGNED,
  IN p_instruccion  VARCHAR(1000)
)
BEGIN
  DECLARE v_actual_cod VARCHAR(40);
  DECLARE v_actual_id  SMALLINT UNSIGNED;
  DECLARE v_derivada   SMALLINT UNSIGNED;
  DECLARE v_area       SMALLINT UNSIGNED;
  DECLARE v_area_actual SMALLINT UNSIGNED;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  SELECT es.codigo, es.id, s.area_actual_id INTO v_actual_cod, v_actual_id, v_area_actual
    FROM solicitudes s JOIN estados_solicitud es ON es.id = s.estado_id
   WHERE s.id = p_solicitud_id;
  IF v_actual_cod IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud no existe';
  END IF;
  IF v_actual_cod NOT IN ('EN_REVISION','DERIVADA') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se puede derivar una solicitud en revision o ya derivada';
  END IF;

  SELECT id INTO v_area FROM areas_municipales WHERE codigo = p_area_codigo;
  IF v_area IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Area destino no existe';
  END IF;
  IF v_area = v_area_actual THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud ya se encuentra en esa area';
  END IF;

  SELECT id INTO v_derivada FROM estados_solicitud WHERE codigo = 'DERIVADA';

  START TRANSACTION;
  UPDATE solicitudes SET estado_id = v_derivada, area_actual_id = v_area WHERE id = p_solicitud_id;
  INSERT INTO historial_solicitud(solicitud_id,estado_anterior_id,estado_nuevo_id,usuario_id,
                                  area_destino_id,comentario)
  VALUES (p_solicitud_id,v_actual_id,v_derivada,p_usuario_id,v_area,p_instruccion);
  COMMIT;
END$$


-- =====================================================================
-- 5. EMISION DE DOCUMENTO (aprobacion) Y VERIFICACION QR
-- =====================================================================

-- El backend genera un token aleatorio, arma la URL del QR con el token
-- y envia aqui SOLO su SHA-256 (64 hex). El token original nunca se guarda.
DROP PROCEDURE IF EXISTS sp_aprobar_y_emitir_documento$$
CREATE PROCEDURE sp_aprobar_y_emitir_documento(
  IN  p_solicitud_id   BIGINT UNSIGNED,
  IN  p_usuario_id     BIGINT UNSIGNED,
  IN  p_token_hash     CHAR(64),
  IN  p_archivo_pdf    VARCHAR(500),        -- puede ser NULL si el PDF se genera despues
  OUT p_documento_id   BIGINT UNSIGNED,
  OUT p_numero_documento VARCHAR(80)
)
BEGIN
  DECLARE v_estado_cod VARCHAR(40);
  DECLARE v_actual_id  SMALLINT UNSIGNED;
  DECLARE v_tipo_cod   VARCHAR(40);
  DECLARE v_aprobada   SMALLINT UNSIGNED;
  DECLARE v_doc_tipo   VARCHAR(30);
  DECLARE v_prefijo    VARCHAR(4);
  DECLARE v_original   BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_vence      DATE DEFAULT NULL;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

  IF p_token_hash IS NULL OR p_token_hash NOT REGEXP '^[0-9a-fA-F]{64}$' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'token_hash debe ser un SHA-256 en hexadecimal (64 caracteres)';
  END IF;

  SELECT es.codigo, es.id, tt.codigo INTO v_estado_cod, v_actual_id, v_tipo_cod
    FROM solicitudes s
    JOIN estados_solicitud es ON es.id = s.estado_id
    JOIN tipos_tramite tt ON tt.id = s.tipo_tramite_id
   WHERE s.id = p_solicitud_id;
  IF v_estado_cod IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud no existe';
  END IF;
  IF v_estado_cod <> 'EN_REVISION' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se puede aprobar una solicitud en revision';
  END IF;
  IF EXISTS (SELECT 1 FROM documentos_emitidos WHERE solicitud_id = p_solicitud_id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud ya tiene un documento emitido';
  END IF;

  IF v_tipo_cod = 'LIC_FUNC' THEN
    SET v_doc_tipo = 'LICENCIA_FUNCIONAMIENTO', v_prefijo = 'LIC';
  ELSEIF v_tipo_cod = 'AUT_ANUNCIO' THEN
    SET v_doc_tipo = 'AUTORIZACION_PUBLICIDAD', v_prefijo = 'PUB';
    SELECT fecha_fin INTO v_vence FROM solicitud_autorizacion_anuncio WHERE solicitud_id = p_solicitud_id;
  ELSE
    SET v_doc_tipo = 'DUPLICADO_LICENCIA', v_prefijo = 'DUP';
    SELECT documento_original_id INTO v_original
      FROM solicitud_duplicado_licencia WHERE solicitud_id = p_solicitud_id;
  END IF;

  SET p_numero_documento = CONCAT(v_prefijo,'-',YEAR(NOW()),'-',LPAD(p_solicitud_id,6,'0'));
  SELECT id INTO v_aprobada FROM estados_solicitud WHERE codigo = 'APROBADA';

  START TRANSACTION;
  INSERT INTO documentos_emitidos(solicitud_id,documento_original_id,tipo_documento,numero_documento,
         fecha_emision,fecha_vencimiento,archivo_pdf,token_verificacion_hash,emitido_por_usuario_id)
  VALUES (p_solicitud_id,v_original,v_doc_tipo,p_numero_documento,CURDATE(),v_vence,
          p_archivo_pdf,LOWER(p_token_hash),p_usuario_id);
  SET p_documento_id = LAST_INSERT_ID();

  UPDATE solicitudes SET estado_id = v_aprobada, fecha_cierre = NOW() WHERE id = p_solicitud_id;
  INSERT INTO historial_solicitud(solicitud_id,estado_anterior_id,estado_nuevo_id,usuario_id,comentario)
  VALUES (p_solicitud_id,v_actual_id,v_aprobada,p_usuario_id,
          CONCAT('Aprobada. Documento emitido: ',p_numero_documento));
  COMMIT;
END$$


-- Actualiza la ruta del PDF una vez que el backend lo genero
DROP PROCEDURE IF EXISTS sp_actualizar_pdf_documento$$
CREATE PROCEDURE sp_actualizar_pdf_documento(
  IN p_documento_id BIGINT UNSIGNED,
  IN p_archivo_pdf  VARCHAR(500)
)
BEGIN
  UPDATE documentos_emitidos SET archivo_pdf = p_archivo_pdf WHERE id = p_documento_id;
  IF ROW_COUNT() = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Documento no existe';
  END IF;
END$$


-- Pagina publica del QR: devuelve solo informacion autorizada (0 filas si el token no existe)
DROP PROCEDURE IF EXISTS sp_verificar_documento$$
CREATE PROCEDURE sp_verificar_documento(IN p_token_hash CHAR(64))
BEGIN
  SELECT d.numero_documento,
         d.tipo_documento,
         d.fecha_emision,
         d.fecha_vencimiento,
         CASE WHEN d.estado = 'VIGENTE' AND d.fecha_vencimiento IS NOT NULL
                   AND d.fecha_vencimiento < CURDATE() THEN 'VENCIDO' ELSE d.estado END AS estado,
         CASE WHEN p.tipo_persona = 'NATURAL' THEN CONCAT_WS(' ',p.nombres,p.apellidos)
              ELSE p.razon_social END AS titular,
         e.nombre_comercial,
         e.direccion AS direccion_establecimiento
    FROM documentos_emitidos d
    JOIN solicitudes s ON s.id = d.solicitud_id
    JOIN personas p ON p.id = s.solicitante_persona_id
    LEFT JOIN establecimientos e ON e.id = s.establecimiento_id
   WHERE d.token_verificacion_hash = LOWER(p_token_hash);
END$$


DROP PROCEDURE IF EXISTS sp_cambiar_estado_documento$$
CREATE PROCEDURE sp_cambiar_estado_documento(
  IN p_documento_id BIGINT UNSIGNED,
  IN p_estado       VARCHAR(20)      -- VIGENTE | SUSPENDIDO | REVOCADO | VENCIDO
)
BEGIN
  IF p_estado NOT IN ('VIGENTE','SUSPENDIDO','REVOCADO','VENCIDO') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Estado de documento invalido';
  END IF;
  UPDATE documentos_emitidos SET estado = p_estado WHERE id = p_documento_id;
  IF ROW_COUNT() = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Documento no existe';
  END IF;
END$$


-- =====================================================================
-- 6. CONSULTAS (bandejas y detalle)
-- =====================================================================

-- Sirve para "Mis tramites" (p_solicitante_id) y para la bandeja del personal (sin solicitante)
DROP PROCEDURE IF EXISTS sp_listar_solicitudes$$
CREATE PROCEDURE sp_listar_solicitudes(
  IN p_solicitante_id BIGINT UNSIGNED,   -- NULL = todos
  IN p_estado_codigo  VARCHAR(40),       -- NULL = todos
  IN p_tipo_codigo    VARCHAR(40),       -- NULL = todos
  IN p_texto          VARCHAR(100),      -- busca en codigo, solicitante o establecimiento
  IN p_limite         INT,
  IN p_desplazamiento INT
)
BEGIN
  DECLARE v_lim INT DEFAULT 20;
  DECLARE v_off INT DEFAULT 0;
  SET v_lim = LEAST(GREATEST(IFNULL(p_limite,20),1),100);
  SET v_off = GREATEST(IFNULL(p_desplazamiento,0),0);

  SELECT v.*
    FROM vw_solicitudes_resumen v
    JOIN solicitudes s ON s.id = v.id
   WHERE (p_solicitante_id IS NULL OR s.solicitante_persona_id = p_solicitante_id)
     AND (p_estado_codigo IS NULL OR v.estado_codigo = p_estado_codigo)
     AND (p_tipo_codigo   IS NULL OR v.tipo_tramite  = p_tipo_codigo)
     AND (p_texto IS NULL OR v.codigo_solicitud LIKE CONCAT('%',p_texto,'%')
          OR v.solicitante LIKE CONCAT('%',p_texto,'%')
          OR v.nombre_comercial LIKE CONCAT('%',p_texto,'%'))
     AND (v.estado_codigo <> 'BORRADOR' OR p_solicitante_id IS NOT NULL)  -- el personal no ve borradores ajenos
   ORDER BY v.creado_en DESC
   LIMIT v_lim OFFSET v_off;
END$$


DROP PROCEDURE IF EXISTS sp_obtener_solicitud$$
CREATE PROCEDURE sp_obtener_solicitud(IN p_solicitud_id BIGINT UNSIGNED)
BEGIN
  SELECT s.id, s.codigo_solicitud,
         tt.codigo AS tipo_tramite, tt.nombre AS nombre_tramite,
         es.codigo AS estado_codigo, es.nombre AS estado,
         s.solicitante_persona_id,
         CASE WHEN p.tipo_persona='NATURAL' THEN CONCAT_WS(' ',p.nombres,p.apellidos)
              ELSE p.razon_social END AS solicitante,
         p.tipo_persona, p.tipo_documento, p.numero_documento, p.ruc, p.correo, p.telefono,
         s.representante_persona_id,
         s.establecimiento_id, e.nombre_comercial, e.actividad_economica,
         e.direccion AS direccion_establecimiento, e.area_m2, e.condicion_titular,
         a.codigo AS area_actual_codigo, a.nombre AS area_actual,
         s.observaciones_solicitante, s.fecha_presentacion, s.fecha_cierre, s.creado_en,
         -- licencia
         lf.nivel_riesgo, lf.declaracion_condiciones_seguridad, lf.numero_certificado_itse,
         -- anuncio
         an.tipo_elemento, an.tipo_propiedad, an.material, an.alto_m, an.ancho_m, an.numero_caras,
         an.tipo_iluminacion, an.contenido_anuncio, an.descripcion_ubicacion, an.modalidad,
         an.fecha_inicio, an.fecha_fin,
         -- duplicado
         du.numero_licencia_declarado, du.motivo, du.detalle_motivo,
         du.numero_comprobante_pago, du.fecha_pago
    FROM solicitudes s
    JOIN tipos_tramite tt ON tt.id = s.tipo_tramite_id
    JOIN estados_solicitud es ON es.id = s.estado_id
    JOIN personas p ON p.id = s.solicitante_persona_id
    LEFT JOIN establecimientos e ON e.id = s.establecimiento_id
    LEFT JOIN areas_municipales a ON a.id = s.area_actual_id
    LEFT JOIN solicitud_licencia_funcionamiento lf ON lf.solicitud_id = s.id
    LEFT JOIN solicitud_autorizacion_anuncio an ON an.solicitud_id = s.id
    LEFT JOIN solicitud_duplicado_licencia du ON du.solicitud_id = s.id
   WHERE s.id = p_solicitud_id;
END$$


DROP PROCEDURE IF EXISTS sp_historial_solicitud$$
CREATE PROCEDURE sp_historial_solicitud(IN p_solicitud_id BIGINT UNSIGNED)
BEGIN
  SELECT h.id, ea.nombre AS estado_anterior, en.nombre AS estado_nuevo,
         u.correo_acceso AS usuario, ar.nombre AS area_destino,
         h.comentario, h.registrado_en
    FROM historial_solicitud h
    LEFT JOIN estados_solicitud ea ON ea.id = h.estado_anterior_id
    JOIN estados_solicitud en ON en.id = h.estado_nuevo_id
    LEFT JOIN usuarios u ON u.id = h.usuario_id
    LEFT JOIN areas_municipales ar ON ar.id = h.area_destino_id
   WHERE h.solicitud_id = p_solicitud_id
   ORDER BY h.registrado_en, h.id;
END$$


DROP PROCEDURE IF EXISTS sp_listar_adjuntos$$
CREATE PROCEDURE sp_listar_adjuntos(IN p_solicitud_id BIGINT UNSIGNED)
BEGIN
  SELECT id, tipo_documento, nombre_archivo, ruta_almacenamiento, mime_type, tamanio_bytes,
         estado_revision, observacion_revision, cargado_en
    FROM documentos_adjuntos
   WHERE solicitud_id = p_solicitud_id
   ORDER BY cargado_en;
END$$


DROP PROCEDURE IF EXISTS sp_listar_documentos_emitidos$$
CREATE PROCEDURE sp_listar_documentos_emitidos(IN p_solicitante_id BIGINT UNSIGNED)
BEGIN
  SELECT d.id, d.numero_documento, d.tipo_documento, d.fecha_emision, d.fecha_vencimiento,
         d.estado, d.archivo_pdf, s.codigo_solicitud
    FROM documentos_emitidos d
    JOIN solicitudes s ON s.id = d.solicitud_id
   WHERE (p_solicitante_id IS NULL OR s.solicitante_persona_id = p_solicitante_id)
   ORDER BY d.fecha_emision DESC, d.id DESC;
END$$

DELIMITER ;

-- listar_tipos_tramites
USE sigta_chorrillos;

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_listar_tipos_tramite$$
CREATE PROCEDURE sp_listar_tipos_tramite()
BEGIN
  SELECT id, codigo, nombre, requiere_establecimiento
    FROM tipos_tramite
   ORDER BY id;
END$$
DELIMITER ;
