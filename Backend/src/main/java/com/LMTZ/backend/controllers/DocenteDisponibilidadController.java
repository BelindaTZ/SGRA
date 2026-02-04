package com.LMTZ.backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.LMTZ.backend.dtos.ApiErrorResponse;
import com.LMTZ.backend.dtos.AvailabilityResponse;
import com.LMTZ.backend.dtos.AvailabilitySlotRequest;
import com.LMTZ.backend.dtos.AvailabilityUpdateRequest;
import com.LMTZ.backend.dtos.AvailabilityUpdateResponse;
import com.LMTZ.backend.services.DocenteDisponibilidadService;
import com.LMTZ.backend.services.auth.JwtService;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/docente/disponibilidad")
@RequiredArgsConstructor
public class DocenteDisponibilidadController {
    private final DocenteDisponibilidadService disponibilidadService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<?> obtenerDisponibilidad(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "periodoId", required = false) Integer periodoId) {
        Integer userId = resolveUserId(authorization);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Token inválido o ausente"));
        }
        try {
            AvailabilityResponse response = disponibilidadService.obtenerDisponibilidad(userId, periodoId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> actualizarDisponibilidad(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AvailabilityUpdateRequest request) {
        Integer userId = resolveUserId(authorization);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Token inválido o ausente"));
        }
        if (request == null || request.getSlots() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponse("Solicitud inválida"));
        }
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

    @DeleteMapping
    public ResponseEntity<?> desactivarDisponibilidad(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AvailabilityUpdateRequest request) {
        Integer userId = resolveUserId(authorization);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Token inválido o ausente"));
        }
        List<AvailabilitySlotRequest> slots = request != null ? request.getSlots() : null;
        try {
            AvailabilityUpdateResponse response = disponibilidadService.actualizarDisponibilidad(
                    userId,
                    request != null ? request.getPeriodoId() : null,
                    slots,
                    false,
                    true);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    private Integer resolveUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7);
        try {
            Claims claims = jwtService.parseClaims(token);
            Object value = claims.get("userId");
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null) {
                return Integer.valueOf(value.toString());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
