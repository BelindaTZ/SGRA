CREATE OR REPLACE FUNCTION sgra.fn_docente_dash_cards(
    p_idusuario int,
    p_periodo_id int DEFAULT NULL
)
RETURNS TABLE(
    solicitudes_pendientes int,
    sesiones_programadas int,
    sesiones_completadas int,
    estudiantes_atendidos int
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_iddocente int;
    v_periodo_id int;
BEGIN
    SELECT d.iddocente
      INTO v_iddocente
      FROM sgra.tbdocentes d
     WHERE d.idusuario = p_idusuario
       AND d.estado = true
     LIMIT 1;

    v_periodo_id := COALESCE(
        p_periodo_id,
        (SELECT p.idperiodo
           FROM sgra.tbperiodos p
          WHERE p.estado = true
          ORDER BY p.fechainicio DESC
          LIMIT 1)
    );

    RETURN QUERY
    SELECT
        COALESCE((
            SELECT COUNT(*)
              FROM sgra.tbsolicitudesrefuerzos sr
              JOIN sgra.tbestadossolicitudesrefuerzos es
                ON es.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
             WHERE sr.iddocente = v_iddocente
               AND sr.idperiodo = v_periodo_id
               AND es.estado = true
               AND LOWER(es.nombreestado) LIKE 'pend%'
        ), 0)::int AS solicitudes_pendientes,
        COALESCE((
            SELECT COUNT(DISTINCT rp.idrefuerzoprogramado)
              FROM sgra.tbrefuerzosprogramados rp
              JOIN sgra.tbdetallesrefuerzosprogramadas drp
                ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado
              JOIN sgra.tbsolicitudesrefuerzos sr
                ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
             WHERE sr.iddocente = v_iddocente
               AND sr.idperiodo = v_periodo_id
        ), 0)::int AS sesiones_programadas,
        COALESCE((
            SELECT COUNT(DISTINCT rr.idrefuerzorealizado)
              FROM sgra.tbrefuerzosrealizados rr
              JOIN sgra.tbrefuerzosprogramados rp
                ON rp.idrefuerzoprogramado = rr.idrefuerzoprogramado
              JOIN sgra.tbdetallesrefuerzosprogramadas drp
                ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado
              JOIN sgra.tbsolicitudesrefuerzos sr
                ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
             WHERE sr.iddocente = v_iddocente
               AND sr.idperiodo = v_periodo_id
        ), 0)::int AS sesiones_completadas,
        COALESCE((
            SELECT COUNT(DISTINCT ar.idestudiante)
              FROM sgra.tbasistenciasrefuerzos ar
              JOIN sgra.tbrefuerzosrealizados rr
                ON rr.idrefuerzorealizado = ar.idrefuerzorealizado
              JOIN sgra.tbrefuerzosprogramados rp
                ON rp.idrefuerzoprogramado = rr.idrefuerzoprogramado
              JOIN sgra.tbdetallesrefuerzosprogramadas drp
                ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado
              JOIN sgra.tbsolicitudesrefuerzos sr
                ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
             WHERE sr.iddocente = v_iddocente
               AND sr.idperiodo = v_periodo_id
               AND ar.asistencia = true
        ), 0)::int AS estudiantes_atendidos;
END;
$$;

CREATE OR REPLACE FUNCTION sgra.fn_docente_dash_solicitudes_pendientes(
    p_idusuario int,
    p_limit int DEFAULT 5,
    p_periodo_id int DEFAULT NULL
)
RETURNS TABLE(
    idsolicitudrefuerzo int,
    estudiante_nombre text,
    temario_asignatura text,
    fecha_hora timestamp,
    tipo text,
    nuevas boolean
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_iddocente int;
    v_periodo_id int;
BEGIN
    SELECT d.iddocente
      INTO v_iddocente
      FROM sgra.tbdocentes d
     WHERE d.idusuario = p_idusuario
       AND d.estado = true
     LIMIT 1;

    v_periodo_id := COALESCE(
        p_periodo_id,
        (SELECT p.idperiodo
           FROM sgra.tbperiodos p
          WHERE p.estado = true
          ORDER BY p.fechainicio DESC
          LIMIT 1)
    );

    RETURN QUERY
    SELECT
        sr.idsolicitudrefuerzo,
        CONCAT(u.apellidos, ' ', u.nombres) AS estudiante_nombre,
        CONCAT(t.nombretemario, ' • ', a.asignatura) AS temario_asignatura,
        sr.fechahoracreacion AS fecha_hora,
        ts.tiposesion AS tipo,
        (sr.fechahoracreacion >= NOW() - INTERVAL '48 hours') AS nuevas
      FROM sgra.tbsolicitudesrefuerzos sr
      JOIN sgra.tbestadossolicitudesrefuerzos es
        ON es.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
      JOIN sgra.tbestudiantes e
        ON e.idestudiante = sr.idestudiante
      JOIN sgra.tbusuarios u
        ON u.idusuario = e.idusuario
      JOIN sgra.tbtemarios t
        ON t.idtemario = sr.idtemario
      JOIN sgra.tbasignaturas a
        ON a.idasignatura = t.idasignatura
      JOIN sgra.tbtipossesiones ts
        ON ts.idtiposesion = sr.idtiposesion
     WHERE sr.iddocente = v_iddocente
       AND sr.idperiodo = v_periodo_id
       AND es.estado = true
       AND LOWER(es.nombreestado) LIKE 'pend%'
     ORDER BY sr.fechahoracreacion DESC
     LIMIT p_limit;
END;
$$;

CREATE OR REPLACE FUNCTION sgra.fn_docente_dash_proximas_sesiones(
    p_idusuario int,
    p_limit int DEFAULT 3,
    p_periodo_id int DEFAULT NULL
)
RETURNS TABLE(
    idrefuerzoprogramado int,
    titulo text,
    fecha_hora timestamp,
    estudiante_nombre text,
    modalidad text
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_iddocente int;
    v_periodo_id int;
BEGIN
    SELECT d.iddocente
      INTO v_iddocente
      FROM sgra.tbdocentes d
     WHERE d.idusuario = p_idusuario
       AND d.estado = true
     LIMIT 1;

    v_periodo_id := COALESCE(
        p_periodo_id,
        (SELECT p.idperiodo
           FROM sgra.tbperiodos p
          WHERE p.estado = true
          ORDER BY p.fechainicio DESC
          LIMIT 1)
    );

    RETURN QUERY
    SELECT
        rp.idrefuerzoprogramado,
        CONCAT(t.nombretemario, ' • ', a.asignatura) AS titulo,
        sr.fechahoracreacion AS fecha_hora,
        CONCAT(u.apellidos, ' ', u.nombres) AS estudiante_nombre,
        m.modalidad AS modalidad
      FROM sgra.tbrefuerzosprogramados rp
      JOIN sgra.tbdetallesrefuerzosprogramadas drp
        ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado
      JOIN sgra.tbsolicitudesrefuerzos sr
        ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
      JOIN sgra.tbestudiantes e
        ON e.idestudiante = sr.idestudiante
      JOIN sgra.tbusuarios u
        ON u.idusuario = e.idusuario
      JOIN sgra.tbtemarios t
        ON t.idtemario = sr.idtemario
      JOIN sgra.tbasignaturas a
        ON a.idasignatura = t.idasignatura
      LEFT JOIN sgra.tbmodalidades m
        ON m.idmodalidad = rp.idmodalidad
     WHERE sr.iddocente = v_iddocente
       AND sr.idperiodo = v_periodo_id
     ORDER BY sr.fechahoracreacion ASC
     LIMIT p_limit;
END;
$$;

CREATE OR REPLACE FUNCTION sgra.fn_docente_dash_indicadores(
    p_idusuario int,
    p_periodo_id int DEFAULT NULL
)
RETURNS TABLE(
    tasa_aceptacion numeric,
    asistencia_promedio numeric,
    sesiones_completadas_pct numeric,
    horas_refuerzo numeric
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_iddocente int;
    v_periodo_id int;
    v_total_solicitudes int;
    v_aceptadas int;
    v_total_programadas int;
    v_total_realizadas int;
BEGIN
    SELECT d.iddocente
      INTO v_iddocente
      FROM sgra.tbdocentes d
     WHERE d.idusuario = p_idusuario
       AND d.estado = true
     LIMIT 1;

    v_periodo_id := COALESCE(
        p_periodo_id,
        (SELECT p.idperiodo
           FROM sgra.tbperiodos p
          WHERE p.estado = true
          ORDER BY p.fechainicio DESC
          LIMIT 1)
    );

    SELECT COUNT(*)
      INTO v_total_solicitudes
      FROM sgra.tbsolicitudesrefuerzos sr
     WHERE sr.iddocente = v_iddocente
       AND sr.idperiodo = v_periodo_id;

    SELECT COUNT(*)
      INTO v_aceptadas
      FROM sgra.tbsolicitudesrefuerzos sr
      JOIN sgra.tbestadossolicitudesrefuerzos es
        ON es.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
     WHERE sr.iddocente = v_iddocente
       AND sr.idperiodo = v_periodo_id
       AND es.estado = true
       AND (
            LOWER(es.nombreestado) LIKE 'acept%'
         OR LOWER(es.nombreestado) LIKE 'programad%'
         OR LOWER(es.nombreestado) LIKE 'realiz%'
       );

    SELECT COUNT(DISTINCT rp.idrefuerzoprogramado)
      INTO v_total_programadas
      FROM sgra.tbrefuerzosprogramados rp
      JOIN sgra.tbdetallesrefuerzosprogramadas drp
        ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado
      JOIN sgra.tbsolicitudesrefuerzos sr
        ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
     WHERE sr.iddocente = v_iddocente
       AND sr.idperiodo = v_periodo_id;

    SELECT COUNT(DISTINCT rr.idrefuerzorealizado)
      INTO v_total_realizadas
      FROM sgra.tbrefuerzosrealizados rr
      JOIN sgra.tbrefuerzosprogramados rp
        ON rp.idrefuerzoprogramado = rr.idrefuerzoprogramado
      JOIN sgra.tbdetallesrefuerzosprogramadas drp
        ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado
      JOIN sgra.tbsolicitudesrefuerzos sr
        ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
     WHERE sr.iddocente = v_iddocente
       AND sr.idperiodo = v_periodo_id;

    RETURN QUERY
    SELECT
        CASE WHEN v_total_solicitudes > 0
            THEN ROUND((v_aceptadas::numeric / v_total_solicitudes::numeric) * 100, 2)
            ELSE 0 END AS tasa_aceptacion,
        COALESCE((
            SELECT ROUND(AVG(CASE WHEN ar.asistencia THEN 100 ELSE 0 END)::numeric, 2)
              FROM sgra.tbasistenciasrefuerzos ar
              JOIN sgra.tbrefuerzosrealizados rr
                ON rr.idrefuerzorealizado = ar.idrefuerzorealizado
              JOIN sgra.tbrefuerzosprogramados rp
                ON rp.idrefuerzoprogramado = rr.idrefuerzoprogramado
              JOIN sgra.tbdetallesrefuerzosprogramadas drp
                ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado
              JOIN sgra.tbsolicitudesrefuerzos sr
                ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
             WHERE sr.iddocente = v_iddocente
               AND sr.idperiodo = v_periodo_id
        ), 0) AS asistencia_promedio,
        CASE WHEN v_total_programadas > 0
            THEN ROUND((v_total_realizadas::numeric / v_total_programadas::numeric) * 100, 2)
            ELSE 0 END AS sesiones_completadas_pct,
        COALESCE((
            SELECT ROUND(SUM(EXTRACT(EPOCH FROM rr.duracion)) / 3600.0, 2)
              FROM sgra.tbrefuerzosrealizados rr
              JOIN sgra.tbrefuerzosprogramados rp
                ON rp.idrefuerzoprogramado = rr.idrefuerzoprogramado
              JOIN sgra.tbdetallesrefuerzosprogramadas drp
                ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado
              JOIN sgra.tbsolicitudesrefuerzos sr
                ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
             WHERE sr.iddocente = v_iddocente
               AND sr.idperiodo = v_periodo_id
        ), 0) AS horas_refuerzo;
END;
$$;

CREATE OR REPLACE FUNCTION sgra.fn_docente_dash_notificaciones(
    p_idusuario int,
    p_limit int DEFAULT 10
)
RETURNS TABLE(
    idnotificacion int,
    titulo text,
    mensaje text,
    fechaenvio timestamp
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        n.idnotificacion,
        n.titulo,
        n.mensaje,
        n.fechaenvio
      FROM sgra.tbnotificacion n
     WHERE n.idusuario = p_idusuario
     ORDER BY n.fechaenvio DESC
     LIMIT p_limit;
END;
$$;
