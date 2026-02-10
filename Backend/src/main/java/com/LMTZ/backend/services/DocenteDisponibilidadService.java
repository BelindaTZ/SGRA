package com.LMTZ.backend.services;

import java.sql.ResultSet;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.LMTZ.backend.dtos.AvailabilityResponse;
import com.LMTZ.backend.dtos.AvailabilitySlotRequest;
import com.LMTZ.backend.dtos.AvailabilitySlotResponse;
import com.LMTZ.backend.dtos.AvailabilityUpdateResponse;
import com.LMTZ.backend.dtos.FranjaHorarioDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocenteDisponibilidadService {

    private static final Logger logger = LoggerFactory.getLogger(DocenteDisponibilidadService.class);

    private static final String SCHEMA_NAME = "sgra";
    private static final String FN_PERIODO = "fn_periodo_resuelto";
    private static final String FN_LIST = "fn_docente_disponibilidad_list";
    private static final String FN_FRANJAS = "fn_franjas_horarias_list";
    private static final String FN_RESERVAS = "fn_docente_reservas_list";

    private static final String FN_UPSERT = "fn_docente_disponibilidad_upsert";

    private final JdbcTemplate jdbcTemplate;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    // GET DISPONIBILIDAD
    @Transactional(readOnly = true)
    public AvailabilityResponse obtenerDisponibilidad(Integer userId, Integer periodoId) {
        String periodoSql = "SELECT idperiodo, periodo FROM " + SCHEMA_NAME + "." + FN_PERIODO + "(?)";
        String disponibilidadSql = "SELECT diasemana, idfranjahorario, estado FROM "
                + SCHEMA_NAME + "." + FN_LIST + "(?, ?)";
        String franjasSql = "SELECT idfranjahoraria, horainicio, horariofin FROM "
                + SCHEMA_NAME + "." + FN_FRANJAS + "()";
        String reservasSql = "SELECT diasemana, idfranjahorario FROM "
                + SCHEMA_NAME + "." + FN_RESERVAS + "(?, ?)";

        logger.info("Ejecutando FN {} para periodoId={}", FN_PERIODO, periodoId);
        Map<String, Object> periodoRow = jdbcTemplate.queryForMap(periodoSql, periodoId);
        Integer resolvedPeriodoId = (Integer) periodoRow.get("idperiodo");
        String periodoNombre = (String) periodoRow.get("periodo");

        logger.info("Ejecutando FN {} para userId={} periodoId={}", FN_LIST, userId, resolvedPeriodoId);
        List<AvailabilitySlotResponse> slots = jdbcTemplate.query(
                disponibilidadSql,
                disponibilidadRowMapper(),
                userId,
                resolvedPeriodoId);
        logger.info("FN {} retornó {} registros", FN_LIST, slots != null ? slots.size() : 0);

        logger.info("Ejecutando FN {}", FN_FRANJAS);
        List<FranjaHorarioDto> franjas = jdbcTemplate.query(franjasSql, franjaRowMapper());
        logger.info("FN {} retornó {} registros", FN_FRANJAS, franjas != null ? franjas.size() : 0);
        if (franjas == null || franjas.isEmpty()) {
            throw new RuntimeException("No existen franjas horarias activas en la BD.");
        }

        List<AvailabilitySlotResponse> reservas = List.of();
        try {
            logger.info("Ejecutando FN {} para userId={} periodoId={}", FN_RESERVAS, userId, resolvedPeriodoId);
            reservas = jdbcTemplate.query(
                    reservasSql,
                    reservaRowMapper(),
                    userId,
                    resolvedPeriodoId);
            logger.info("FN {} retornó {} registros", FN_RESERVAS, reservas != null ? reservas.size() : 0);
        } catch (Exception ex) {
            logger.warn("No se pudo obtener reservas para docente. Continuando sin reservas.", ex);
        }

        List<AvailabilitySlotResponse> mergedSlots = mergeDisponibilidadConReservas(slots, reservas);

        return new AvailabilityResponse(resolvedPeriodoId, periodoNombre, franjas, mergedSlots);
    }

    // PUT DISPONIBILIDAD
    @Transactional
    public AvailabilityUpdateResponse actualizarDisponibilidad(
            Integer userId,
            Integer periodoId,
            List<AvailabilitySlotRequest> slots,
            boolean marcarDisponible,
            boolean forzarDesactivar) {

        if (slots == null || slots.isEmpty()) {
            return new AvailabilityUpdateResponse("No se enviaron cambios", 0);
        }

        Integer resolvedPeriodoId = resolvePeriodoId(periodoId);
        logger.info("Periodo resuelto para upsert: {}", resolvedPeriodoId);

        int updated = 0;

        // FUNCIÓN
        String upsertFnSql = "SELECT ok, message FROM " + SCHEMA_NAME + "." + FN_UPSERT + "(?, ?, ?, ?, ?)";

        for (AvailabilitySlotRequest slot : slots) {
            if (slot == null || slot.getDiaSemana() == null || slot.getFranjaId() == null) {
                continue;
            }

            String status = slot.getStatus() == null ? "" : slot.getStatus().trim().toUpperCase();
            if ("SESION".equals(status)) {
                continue;
            }

            boolean estado = forzarDesactivar
                    ? false
                    : (marcarDisponible || "DISPONIBLE".equals(status));

            short diaSemana = (short) slot.getDiaSemana().intValue();

            logger.info("Ejecutando FN {} para userId={} periodoId={} dia={} franja={} estado={}",
                    FN_UPSERT, userId, resolvedPeriodoId, diaSemana, slot.getFranjaId(), estado);

            try {
                long t0 = System.currentTimeMillis();

                Map<String, Object> row = jdbcTemplate.queryForMap(
                        upsertFnSql,
                        userId,
                        resolvedPeriodoId,
                        diaSemana,
                        slot.getFranjaId(),
                        estado
                );

                Boolean ok = (Boolean) row.get("ok");
                String message = (String) row.get("message");

                logger.info("FN {} respuesta ok={} message={} ({} ms)",
                        FN_UPSERT, ok, message, (System.currentTimeMillis() - t0));

                if (ok == null || !ok) {
                    throw new RuntimeException(message != null ? message : "No se pudo actualizar disponibilidad");
                }

                updated += 1;

            } catch (DataAccessException ex) {
                Throwable root = ex.getRootCause();
                if (root instanceof java.sql.SQLException sqlEx) {
                    logger.error("Fallo SQL (DataAccess). SQLState={} Code={} Message={} | SQL={}",
                            sqlEx.getSQLState(),
                            sqlEx.getErrorCode(),
                            sqlEx.getMessage(),
                            upsertFnSql,
                            ex);
                } else {
                    logger.error("Fallo DataAccess sin SQL root. Type={} Message={} | SQL={}",
                            ex.getClass().getName(),
                            ex.getMessage(),
                            upsertFnSql,
                            ex);
                }

                logger.error("Params: userId={} periodoId={} dia={} franja={} estado={}",
                        userId, resolvedPeriodoId, diaSemana, slot.getFranjaId(), estado);

                throw ex;

            } catch (Exception ex) {
                logger.error("Fallo NO-DataAccess ejecutando {} | SQL={} | Params: userId={} periodoId={} dia={} franja={} estado={}",
                        FN_UPSERT,
                        upsertFnSql,
                        userId,
                        resolvedPeriodoId,
                        diaSemana,
                        slot.getFranjaId(),
                        estado,
                        ex);
                throw ex;
            }
        }

        logger.info("FN {} total registros actualizados={}", FN_UPSERT, updated);
        return new AvailabilityUpdateResponse("Disponibilidad actualizada", updated);
    }

    // RowMappers
    private RowMapper<AvailabilitySlotResponse> disponibilidadRowMapper() {
        return (ResultSet rs, int rowNum) -> new AvailabilitySlotResponse(
                rs.getInt("diasemana"),
                rs.getInt("idfranjahorario"),
                rs.getBoolean("estado") ? "DISPONIBLE" : "NO_DISPONIBLE");
    }

    private RowMapper<FranjaHorarioDto> franjaRowMapper() {
        return (ResultSet rs, int rowNum) -> new FranjaHorarioDto(
                rs.getInt("idfranjahoraria"),
                formatTime(toLocalTime(rs.getTime("horainicio"))),
                formatTime(toLocalTime(rs.getTime("horariofin"))));
    }

    private RowMapper<AvailabilitySlotResponse> reservaRowMapper() {
        return (ResultSet rs, int rowNum) -> new AvailabilitySlotResponse(
                rs.getInt("diasemana"),
                rs.getInt("idfranjahorario"),
                "SESION");
    }

    // Helpers
    private List<AvailabilitySlotResponse> mergeDisponibilidadConReservas(
            List<AvailabilitySlotResponse> slots,
            List<AvailabilitySlotResponse> reservas) {
        Map<String, AvailabilitySlotResponse> merged = new LinkedHashMap<>();
        if (slots != null) {
            for (AvailabilitySlotResponse slot : slots) {
                merged.put(key(slot), slot);
            }
        }
        if (reservas != null) {
            for (AvailabilitySlotResponse reserva : reservas) {
                merged.put(key(reserva), reserva);
            }
        }
        return List.copyOf(merged.values());
    }

    private String key(AvailabilitySlotResponse slot) {
        return slot.getDiaSemana() + ":" + slot.getFranjaId();
    }

    private LocalTime toLocalTime(java.sql.Time time) {
        return time != null ? time.toLocalTime() : null;
    }

    private String formatTime(LocalTime time) {
        return time != null ? time.format(timeFormatter) : null;
    }

    private Integer resolvePeriodoId(Integer periodoId) {
        String periodoSql = "SELECT idperiodo FROM " + SCHEMA_NAME + "." + FN_PERIODO + "(?)";
        Map<String, Object> periodoRow = jdbcTemplate.queryForMap(periodoSql, periodoId);
        return (Integer) periodoRow.get("idperiodo");
    }
}
