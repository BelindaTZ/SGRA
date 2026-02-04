package com.LMTZ.backend.services;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.LMTZ.backend.dtos.DocenteDashboardCardsResponse;
import com.LMTZ.backend.dtos.DocenteDashboardIndicadoresResponse;
import com.LMTZ.backend.dtos.DocenteDashboardNotificacionResponse;
import com.LMTZ.backend.dtos.DocenteDashboardProximaSesionResponse;
import com.LMTZ.backend.dtos.DocenteDashboardSolicitudPendienteResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocenteDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DocenteDashboardService.class);

    private static final String SCHEMA_NAME = "sgra";
    private static final String FN_DASH_CARDS = "fn_docente_dash_cards";
    private static final String FN_DASH_SOLICITUDES = "fn_docente_dash_solicitudes_pendientes";
    private static final String FN_DASH_SESIONES = "fn_docente_dash_proximas_sesiones";
    private static final String FN_DASH_INDICADORES = "fn_docente_dash_indicadores";
    private static final String FN_DASH_NOTIFICACIONES = "fn_docente_dash_notificaciones";

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public DocenteDashboardCardsResponse obtenerCards(Integer userId, Integer periodoId) {
        String sql = "SELECT solicitudes_pendientes, sesiones_programadas, sesiones_completadas, estudiantes_atendidos "
                + "FROM " + SCHEMA_NAME + "." + FN_DASH_CARDS + "(?, ?)";
        logger.info("Ejecutando FN {} para userId={} periodoId={}", FN_DASH_CARDS, userId, periodoId);
        Map<String, Object> row = jdbcTemplate.queryForMap(sql, userId, periodoId);
        return new DocenteDashboardCardsResponse(
                toInteger(row.get("solicitudes_pendientes")),
                toInteger(row.get("sesiones_programadas")),
                toInteger(row.get("sesiones_completadas")),
                toInteger(row.get("estudiantes_atendidos")));
    }

    @Transactional(readOnly = true)
    public List<DocenteDashboardSolicitudPendienteResponse> obtenerSolicitudesPendientes(
            Integer userId,
            Integer limit,
            Integer periodoId) {
        String sql = "SELECT idsolicitudrefuerzo, estudiante_nombre, temario_asignatura, fecha_hora, tipo, nuevas "
                + "FROM " + SCHEMA_NAME + "." + FN_DASH_SOLICITUDES + "(?, ?, ?)";
        logger.info("Ejecutando FN {} para userId={} limit={} periodoId={}", FN_DASH_SOLICITUDES, userId, limit,
                periodoId);
        return jdbcTemplate.query(sql, solicitudPendienteRowMapper(), userId, limit, periodoId);
    }

    @Transactional(readOnly = true)
    public List<DocenteDashboardProximaSesionResponse> obtenerProximasSesiones(
            Integer userId,
            Integer limit,
            Integer periodoId) {
        String sql = "SELECT idrefuerzoprogramado, titulo, fecha_hora, estudiante_nombre, modalidad "
                + "FROM " + SCHEMA_NAME + "." + FN_DASH_SESIONES + "(?, ?, ?)";
        logger.info("Ejecutando FN {} para userId={} limit={} periodoId={}", FN_DASH_SESIONES, userId, limit,
                periodoId);
        return jdbcTemplate.query(sql, proximaSesionRowMapper(), userId, limit, periodoId);
    }

    @Transactional(readOnly = true)
    public DocenteDashboardIndicadoresResponse obtenerIndicadores(Integer userId, Integer periodoId) {
        String sql = "SELECT tasa_aceptacion, asistencia_promedio, sesiones_completadas_pct, horas_refuerzo "
                + "FROM " + SCHEMA_NAME + "." + FN_DASH_INDICADORES + "(?, ?)";
        logger.info("Ejecutando FN {} para userId={} periodoId={}", FN_DASH_INDICADORES, userId, periodoId);
        Map<String, Object> row = jdbcTemplate.queryForMap(sql, userId, periodoId);
        return new DocenteDashboardIndicadoresResponse(
                toBigDecimal(row.get("tasa_aceptacion")),
                toBigDecimal(row.get("asistencia_promedio")),
                toBigDecimal(row.get("sesiones_completadas_pct")),
                toBigDecimal(row.get("horas_refuerzo")));
    }

    @Transactional(readOnly = true)
    public List<DocenteDashboardNotificacionResponse> obtenerNotificaciones(Integer userId, Integer limit) {
        String sql = "SELECT idnotificacion, titulo, mensaje, fechaenvio "
                + "FROM " + SCHEMA_NAME + "." + FN_DASH_NOTIFICACIONES + "(?, ?)";
        logger.info("Ejecutando FN {} para userId={} limit={}", FN_DASH_NOTIFICACIONES, userId, limit);
        return jdbcTemplate.query(sql, notificacionRowMapper(), userId, limit);
    }

    private RowMapper<DocenteDashboardSolicitudPendienteResponse> solicitudPendienteRowMapper() {
        return (ResultSet rs, int rowNum) -> new DocenteDashboardSolicitudPendienteResponse(
                rs.getInt("idsolicitudrefuerzo"),
                rs.getString("estudiante_nombre"),
                rs.getString("temario_asignatura"),
                toLocalDateTime(rs.getTimestamp("fecha_hora")),
                rs.getString("tipo"),
                rs.getBoolean("nuevas"));
    }

    private RowMapper<DocenteDashboardProximaSesionResponse> proximaSesionRowMapper() {
        return (ResultSet rs, int rowNum) -> new DocenteDashboardProximaSesionResponse(
                rs.getInt("idrefuerzoprogramado"),
                rs.getString("titulo"),
                toLocalDateTime(rs.getTimestamp("fecha_hora")),
                rs.getString("estudiante_nombre"),
                rs.getString("modalidad"));
    }

    private RowMapper<DocenteDashboardNotificacionResponse> notificacionRowMapper() {
        return (ResultSet rs, int rowNum) -> new DocenteDashboardNotificacionResponse(
                rs.getInt("idnotificacion"),
                rs.getString("titulo"),
                rs.getString("mensaje"),
                toLocalDateTime(rs.getTimestamp("fechaenvio")));
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.valueOf(value.toString());
            } catch (NumberFormatException ex) {
                logger.warn("No se pudo convertir a entero: {}", value, ex);
            }
        }
        return 0;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value != null) {
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException ex) {
                logger.warn("No se pudo convertir a decimal: {}", value, ex);
            }
        }
        return BigDecimal.ZERO;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) throws SQLException {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    @SuppressWarnings("unused")
    private void logDataAccessException(DataAccessException ex, String sql, Object... params) {
        Throwable root = ex.getRootCause();
        if (root instanceof java.sql.SQLException sqlEx) {
            logger.error("Fallo SQL (DataAccess). SQLState={} Code={} Message={} | SQL={}",
                    sqlEx.getSQLState(),
                    sqlEx.getErrorCode(),
                    sqlEx.getMessage(),
                    sql,
                    ex);
        } else {
            logger.error("Fallo DataAccess sin SQL root. Type={} Message={} | SQL={}",
                    ex.getClass().getName(),
                    ex.getMessage(),
                    sql,
                    ex);
        }
        logger.error("Params: {}", java.util.Arrays.toString(params));
    }
}
