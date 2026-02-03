package com.LMTZ.backend.services.auth;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.LMTZ.backend.dtos.AuthLoginRequest;
import com.LMTZ.backend.dtos.AuthLoginResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Transactional(readOnly = true)
    public AuthLoginResponse login(AuthLoginRequest request) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("username", request.getUsername())
                .addValue("password", request.getPassword());

        List<LoginResult> results = namedParameterJdbcTemplate.query(
                "select * from sgra.fn_login(:username, :password)",
                params,
                new LoginResultMapper());

        if (results.isEmpty()) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        LoginResult result = results.get(0);
        if (!result.ok()) {
            if ("Cuenta inactiva".equalsIgnoreCase(result.message())) {
                throw new AccountInactiveException("Cuenta inactiva");
            }
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String role = resolveRole(result.roles());
        String token = jwtService.generateToken(result.nombreusuario(), result.idusuario(), role);

        return new AuthLoginResponse(token, role, result.idusuario(), result.nombreusuario());
    }

    private String resolveRole(String[] roles) {
        if (roles == null || roles.length == 0) {
            return "STUDENT";
        }
        return normalizeRole(roles[0]);
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("estudiante") || normalized.contains("student")) {
            return "STUDENT";
        }
        if (normalized.contains("docente") || normalized.contains("teacher")) {
            return "TEACHER";
        }
        if (normalized.contains("coordinador") || normalized.contains("coordinator")) {
            return "COORDINATOR";
        }
        if (normalized.contains("admin")) {
            return "ADMIN";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private record LoginResult(
            boolean ok,
            String message,
            Integer idusuario,
            String nombreusuario,
            String[] roles) {
    }

    private static class LoginResultMapper implements RowMapper<LoginResult> {
        @Override
        public LoginResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            Array rolesArray = rs.getArray("roles");
            String[] roles = rolesArray == null ? null : (String[]) rolesArray.getArray();
            return new LoginResult(
                    rs.getBoolean("ok"),
                    rs.getString("message"),
                    rs.getInt("idusuario"),
                    rs.getString("nombreusuario"),
                    roles);
        }
    }
}
