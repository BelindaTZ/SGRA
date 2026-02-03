package com.LMTZ.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotGetJdbcConnectionException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.LMTZ.backend.dtos.AuthErrorResponse;
import com.LMTZ.backend.dtos.AuthLoginRequest;
import com.LMTZ.backend.dtos.AuthLoginResponse;
import com.LMTZ.backend.services.auth.AccountInactiveException;
import com.LMTZ.backend.services.auth.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthLoginRequest request) {
        try {
            AuthLoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (AccountInactiveException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthErrorResponse("Cuenta inactiva", null));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthErrorResponse("Credenciales inválidas", null));
        } catch (CannotGetJdbcConnectionException ex) {
            String errorId = UUID.randomUUID().toString();
            logger.error("ErrorId {} - No se pudo conectar a la BD en /api/auth/login", errorId, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new AuthErrorResponse("BD no disponible", errorId));
        } catch (Exception ex) {
            String errorId = UUID.randomUUID().toString();
            logger.error("ErrorId {} - Error inesperado en /api/auth/login", errorId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthErrorResponse("Error interno", errorId));
        }
    }
}
