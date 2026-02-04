package com.LMTZ.backend.services;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.dao.DataAccessException;
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
    private static final String SP_UPSERT = "sp_docente_disponibilidad_upsert";

    private final JdbcTemplate jdbcTemplate;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

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
        SimpleJdbcCall upsertCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(SCHEMA_NAME)
                .withProcedureName(SP_UPSERT)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_idusuario", java.sql.Types.INTEGER),
                        new SqlParameter("p_idperiodo", java.sql.Types.INTEGER),
                        new SqlParameter("p_diasemana", java.sql.Types.SMALLINT),
                        new SqlParameter("p_idfranjahorario", java.sql.Types.INTEGER),
                        new SqlParameter("p_estado", java.sql.Types.BOOLEAN),
                        new SqlOutParameter("o_ok", java.sql.Types.BOOLEAN),
                        new SqlOutParameter("o_message", java.sql.Types.VARCHAR));
        SimpleJdbcCall upsertFunctionCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(SCHEMA_NAME)
                .withFunctionName(SP_UPSERT)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_idusuario", java.sql.Types.INTEGER),
                        new SqlParameter("p_idperiodo", java.sql.Types.INTEGER),
                        new SqlParameter("p_diasemana", java.sql.Types.SMALLINT),
                        new SqlParameter("p_idfranjahorario", java.sql.Types.INTEGER),
                        new SqlParameter("p_estado", java.sql.Types.BOOLEAN),
                        new SqlOutParameter("o_ok", java.sql.Types.BOOLEAN),
                        new SqlOutParameter("o_message", java.sql.Types.VARCHAR));
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
            logger.info(
                    "Ejecutando SP {} para userId={} periodoId={} dia={} franja={} estado={}",
                    SP_UPSERT,
                    userId,
                    resolvedPeriodoId,
                    slot.getDiaSemana(),
                    slot.getFranjaId(),
                    estado);
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("p_idusuario", userId)
                    .addValue("p_idperiodo", resolvedPeriodoId)
                    .addValue("p_diasemana", slot.getDiaSemana())
                    .addValue("p_idfranjahorario", slot.getFranjaId())
                    .addValue("p_estado", estado);
            Map<String, Object> result;
            try {
                result = upsertCall.execute(params);
            } catch (DataAccessException ex) {
                logger.warn("Fallo CALL de {}, intentando como función.", SP_UPSERT, ex);
                result = upsertFunctionCall.execute(params);
            }
            Object okValue = result.get("o_ok");
            Boolean ok = okValue instanceof Boolean ? (Boolean) okValue : null;
            String message = result.get("o_message") != null ? result.get("o_message").toString() : null;

            if (ok == null || !ok) {
                throw new RuntimeException(message != null ? message : "No se pudo actualizar disponibilidad");
            }
            updated += 1;
        }

        logger.info("SP {} total registros actualizados={}", SP_UPSERT, updated);
        return new AvailabilityUpdateResponse("Disponibilidad actualizada", updated);
    }

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
