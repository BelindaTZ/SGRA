package com.LMTZ.backend.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.LMTZ.backend.dtos.ApiErrorResponse;
import com.LMTZ.backend.dtos.AvailabilityResponse;
import com.LMTZ.backend.dtos.AvailabilitySlotRequest;
import com.LMTZ.backend.dtos.AvailabilityUpdateRequest;
import com.LMTZ.backend.dtos.AvailabilityUpdateResponse;
import com.LMTZ.backend.dtos.DocenteDashboardCardsResponse;
import com.LMTZ.backend.dtos.DocenteDashboardIndicadoresResponse;
import com.LMTZ.backend.dtos.DocenteDashboardNotificacionResponse;
import com.LMTZ.backend.dtos.DocenteDashboardProximaSesionResponse;
import com.LMTZ.backend.dtos.DocenteDashboardSolicitudPendienteResponse;
import com.LMTZ.backend.services.DocenteDisponibilidadService;
import com.LMTZ.backend.services.DocenteDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/docente")
@RequiredArgsConstructor
public class DocenteController {
    private static final Logger logger = LoggerFactory.getLogger(DocenteController.class);

    private final DocenteDisponibilidadService disponibilidadService;
    private final DocenteDashboardService dashboardService;

    @GetMapping("/disponibilidad")
    public ResponseEntity<?> obtenerDisponibilidad(
            @RequestParam(value = "periodoId", required = false) Integer periodoId) {
        Integer userId = resolveUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Token inválido o ausente"));
        }
        logger.info("Docente disponibilidad GET - userId={}", userId);
        try {
            AvailabilityResponse response = disponibilidadService.obtenerDisponibilidad(userId, periodoId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    @PutMapping("/disponibilidad")
    public ResponseEntity<?> actualizarDisponibilidad(@RequestBody AvailabilityUpdateRequest request) {
        Integer userId = resolveUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Token inválido o ausente"));
        }
        if (request == null || request.getSlots() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponse("Solicitud inválida"));
        }
        logger.info("Docente disponibilidad PUT - userId={}, slots={}", userId, request.getSlots().size());
        try {
            AvailabilityUpdateResponse response = disponibilidadService.actualizarDisponibilidad(
                    userId,
                    request.getPeriodoId(),
                    request.getSlots(),
                    false,
                    false);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/dashboard/cards")
    public ResponseEntity<?> obtenerDashboardCards(
            @RequestParam(value = "periodoId", required = false) Integer periodoId) {
        Integer userId = resolveUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Token inválido o ausente"));
        }
        logger.info("Docente dashboard cards GET - userId={} periodoId={}", userId, periodoId);
        try {
            DocenteDashboardCardsResponse response = dashboardService.obtenerCards(userId, periodoId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/dashboard/solicitudes-pendientes")
    public ResponseEntity<?> obtenerSolicitudesPendientes(
            @RequestParam(value = "limit", defaultValue = "5") Integer limit,
            @RequestParam(value = "periodoId", required = false) Integer periodoId) {
        Integer userId = resolveUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Token inválido o ausente"));
        }
        logger.info("Docente dashboard solicitudes GET - userId={} limit={} periodoId={}", userId, limit, periodoId);
        try {
            List<DocenteDashboardSolicitudPendienteResponse> response = dashboardService.obtenerSolicitudesPendientes(
                    userId, limit, periodoId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/dashboard/proximas-sesiones")
    public ResponseEntity<?> obtenerProximasSesiones(
            @RequestParam(value = "limit", defaultValue = "3") Integer limit,
            @RequestParam(value = "periodoId", required = false) Integer periodoId) {
        Integer userId = resolveUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Token inválido o ausente"));
        }
        logger.info("Docente dashboard sesiones GET - userId={} limit={} periodoId={}", userId, limit, periodoId);
        try {
            List<DocenteDashboardProximaSesionResponse> response = dashboardService.obtenerProximasSesiones(
                    userId, limit, periodoId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/dashboard/indicadores")
    public ResponseEntity<?> obtenerIndicadores(
            @RequestParam(value = "periodoId", required = false) Integer periodoId) {
        Integer userId = resolveUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Token inválido o ausente"));
        }
        logger.info("Docente dashboard indicadores GET - userId={} periodoId={}", userId, periodoId);
        try {
            DocenteDashboardIndicadoresResponse response = dashboardService.obtenerIndicadores(userId, periodoId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/dashboard/notificaciones")
    public ResponseEntity<?> obtenerNotificaciones(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        Integer userId = resolveUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Token inválido o ausente"));
        }
        logger.info("Docente dashboard notificaciones GET - userId={} limit={}", userId, limit);
        try {
            List<DocenteDashboardNotificacionResponse> response = dashboardService.obtenerNotificaciones(userId, limit);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    private Integer resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object details = authentication.getDetails();
        if (details instanceof Integer userId) {
            return userId;
        }
        if (details instanceof String detailsString) {
            try {
                return Integer.valueOf(detailsString);
            } catch (NumberFormatException ignored) {
            }
        }
        if (details instanceof java.util.Map<?, ?> map) {
            Object value = map.get("userId");
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null) {
                try {
                    return Integer.valueOf(value.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
