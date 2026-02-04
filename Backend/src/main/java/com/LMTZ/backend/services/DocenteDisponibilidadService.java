package com.LMTZ.backend.services;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
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
    private static final String SP_LIST = "sp_docente_disponibilidad_list";
    private static final String SP_FRANJAS = "sp_franjas_horarias_list";
    private static final String SP_RESERVAS = "sp_docente_reservas_list";
    private static final String SP_UPSERT = "sp_docente_disponibilidad_upsert";

    private final JdbcTemplate jdbcTemplate;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    public AvailabilityResponse obtenerDisponibilidad(Integer userId, Integer periodoId) {
        SimpleJdbcCall listCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(SCHEMA_NAME)
                .withProcedureName(SP_LIST)
                .declareParameters(
                        new SqlParameter("p_idusuario", Types.INTEGER),
                        new SqlParameter("p_idperiodo", Types.INTEGER),
                        new SqlOutParameter("o_idperiodo", Types.INTEGER),
                        new SqlOutParameter("o_periodo", Types.VARCHAR),
                        new SqlOutParameter("o_cursor", Types.REF_CURSOR, disponibilidadRowMapper()));

        SimpleJdbcCall franjasCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(SCHEMA_NAME)
                .withProcedureName(SP_FRANJAS)
                .declareParameters(
                        new SqlOutParameter("o_cursor", Types.REF_CURSOR, franjaRowMapper()));

        SimpleJdbcCall reservasCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(SCHEMA_NAME)
                .withProcedureName(SP_RESERVAS)
                .declareParameters(
                        new SqlParameter("p_idusuario", Types.INTEGER),
                        new SqlParameter("p_idperiodo", Types.INTEGER),
                        new SqlOutParameter("o_cursor", Types.REF_CURSOR, reservaRowMapper()));

        logger.info("Ejecutando SP {} para userId={} periodoId={}", SP_LIST, userId, periodoId);
        Map<String, Object> listResult = listCall.execute(userId, periodoId);
        Integer resolvedPeriodoId = (Integer) listResult.get("o_idperiodo");
        String periodoNombre = (String) listResult.get("o_periodo");
        @SuppressWarnings("unchecked")
        List<AvailabilitySlotResponse> slots = (List<AvailabilitySlotResponse>) listResult.get("o_cursor");
        logger.info("SP {} retornó {} registros", SP_LIST, slots != null ? slots.size() : 0);

        logger.info("Ejecutando SP {}", SP_FRANJAS);
        Map<String, Object> franjaResult = franjasCall.execute();
        @SuppressWarnings("unchecked")
        List<FranjaHorarioDto> franjas = (List<FranjaHorarioDto>) franjaResult.get("o_cursor");
        logger.info("SP {} retornó {} registros", SP_FRANJAS, franjas != null ? franjas.size() : 0);

        logger.info("Ejecutando SP {} para userId={} periodoId={}", SP_RESERVAS, userId, periodoId);
        Map<String, Object> reservasResult = reservasCall.execute(userId, periodoId);
        @SuppressWarnings("unchecked")
        List<AvailabilitySlotResponse> reservas = (List<AvailabilitySlotResponse>) reservasResult.get("o_cursor");
        logger.info("SP {} retornó {} registros", SP_RESERVAS, reservas != null ? reservas.size() : 0);

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

        SimpleJdbcCall upsertCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(SCHEMA_NAME)
                .withProcedureName(SP_UPSERT)
                .declareParameters(
                        new SqlParameter("p_idusuario", Types.INTEGER),
                        new SqlParameter("p_idperiodo", Types.INTEGER),
                        new SqlParameter("p_diasemana", Types.SMALLINT),
                        new SqlParameter("p_idfranjahorario", Types.INTEGER),
                        new SqlParameter("p_estado", Types.BOOLEAN),
                        new SqlOutParameter("o_ok", Types.BOOLEAN),
                        new SqlOutParameter("o_message", Types.VARCHAR));

        int updated = 0;
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
                    periodoId,
                    slot.getDiaSemana(),
                    slot.getFranjaId(),
                    estado);

            Map<String, Object> result = upsertCall.execute(
                    userId,
                    periodoId,
                    slot.getDiaSemana(),
                    slot.getFranjaId(),
                    estado);

            Boolean ok = (Boolean) result.get("o_ok");
            String message = (String) result.get("o_message");
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
}
