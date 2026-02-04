package com.LMTZ.backend.services;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

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
    private static final String CALL_LIST = "CALL sgra.sp_docente_disponibilidad_list(?, ?, ?, ?, ?)";
    private static final String CALL_FRANJAS = "CALL sgra.sp_franjas_horarias_list(?)";
    private static final String CALL_UPSERT = "CALL sgra.sp_docente_disponibilidad_upsert(?, ?, ?, ?, ?, ?, ?)";

    private final DataSource dataSource;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    public AvailabilityResponse obtenerDisponibilidad(Integer userId, Integer periodoId) {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            Integer resolvedPeriodoId = null;
            String periodoNombre = null;
            List<AvailabilitySlotResponse> slots = new ArrayList<>();

            try (CallableStatement cs = con.prepareCall(CALL_LIST)) {
                cs.setInt(1, userId);
                if (periodoId != null) {
                    cs.setInt(2, periodoId);
                } else {
                    cs.setNull(2, Types.INTEGER);
                }
                cs.registerOutParameter(3, Types.INTEGER);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.registerOutParameter(5, Types.REF_CURSOR);
                cs.execute();

                resolvedPeriodoId = (Integer) cs.getObject(3);
                periodoNombre = cs.getString(4);

                try (ResultSet rs = (ResultSet) cs.getObject(5)) {
                    while (rs.next()) {
                        Integer diaSemana = rs.getInt("diasemana");
                        Integer franjaId = rs.getInt("idfranjahorario");
                        boolean estado = rs.getBoolean("estado");
                        slots.add(new AvailabilitySlotResponse(
                                diaSemana,
                                franjaId,
                                estado ? "DISPONIBLE" : "NO_DISPONIBLE"));
                    }
                }
            }

            List<FranjaHorarioDto> franjas = listarFranjas(con);
            con.commit();

            return new AvailabilityResponse(resolvedPeriodoId, periodoNombre, franjas, slots);
        } catch (SQLException ex) {
            throw new RuntimeException("Error al consultar disponibilidad", ex);
        }
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

        int updated = 0;
        try (Connection con = dataSource.getConnection()) {
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
                updated += ejecutarUpsert(con, userId, periodoId, slot.getDiaSemana(), slot.getFranjaId(), estado);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error al actualizar disponibilidad", ex);
        }

        return new AvailabilityUpdateResponse("Disponibilidad actualizada", updated);
    }

    private int ejecutarUpsert(
            Connection con,
            Integer userId,
            Integer periodoId,
            Integer diaSemana,
            Integer franjaId,
            boolean estado) throws SQLException {
        try (CallableStatement cs = con.prepareCall(CALL_UPSERT)) {
            cs.setInt(1, userId);
            if (periodoId != null) {
                cs.setInt(2, periodoId);
            } else {
                cs.setNull(2, Types.INTEGER);
            }
            cs.setInt(3, diaSemana);
            cs.setInt(4, franjaId);
            cs.setBoolean(5, estado);
            cs.registerOutParameter(6, Types.BOOLEAN);
            cs.registerOutParameter(7, Types.VARCHAR);
            cs.execute();

            boolean ok = cs.getBoolean(6);
            if (!ok) {
                String message = cs.getString(7);
                throw new SQLException(message != null ? message : "No se pudo actualizar disponibilidad");
            }
            return 1;
        }
    }

    private List<FranjaHorarioDto> listarFranjas(Connection con) throws SQLException {
        List<FranjaHorarioDto> franjas = new ArrayList<>();
        try (CallableStatement cs = con.prepareCall(CALL_FRANJAS)) {
            cs.registerOutParameter(1, Types.REF_CURSOR);
            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                while (rs.next()) {
                    Integer franjaId = rs.getInt("idfranjahoraria");
                    LocalTime horaInicio = toLocalTime(rs.getTime("horainicio"));
                    LocalTime horaFin = toLocalTime(rs.getTime("horariofin"));
                    franjas.add(new FranjaHorarioDto(
                            franjaId,
                            formatTime(horaInicio),
                            formatTime(horaFin)));
                }
            }
        }
        return franjas;
    }

    private LocalTime toLocalTime(Time time) {
        return time != null ? time.toLocalTime() : null;
    }

    private String formatTime(LocalTime time) {
        return time != null ? time.format(timeFormatter) : null;
    }
}
