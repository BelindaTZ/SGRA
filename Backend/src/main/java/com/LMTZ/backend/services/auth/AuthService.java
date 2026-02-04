package com.LMTZ.backend.services.auth;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import javax.sql.DataSource;

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
    private final DataSource dataSource;

    private static final String CALL_SP_LOGIN = "CALL sgra.sp_login(?, ?, ?, ?, ?, ?, ?)";

    @Transactional(readOnly = true)
    public AuthLoginResponse login(AuthLoginRequest request) {

        try (Connection con = dataSource.getConnection();
             CallableStatement cs = con.prepareCall(CALL_SP_LOGIN)) {

            cs.setString(1, request.getUsername());
            cs.setString(2, request.getPassword());

            cs.registerOutParameter(3, Types.BOOLEAN); // ok
            cs.registerOutParameter(4, Types.VARCHAR); // message
            cs.registerOutParameter(5, Types.INTEGER); // idusuario
            cs.registerOutParameter(6, Types.VARCHAR); // nombreusuario

            // 👇 NO usar el overload con (int,int,String)
            cs.registerOutParameter(7, Types.ARRAY); // roles (text[])

            cs.execute();

            boolean ok = cs.getBoolean(3);
            String message = cs.getString(4);

            if (!ok) {
                if (message != null && "Cuenta inactiva".equalsIgnoreCase(message.trim())) {
                    throw new AccountInactiveException("Cuenta inactiva");
                }
                throw new BadCredentialsException("Credenciales inválidas");
            }

            Integer idusuario = (Integer) cs.getObject(5);
            String nombreusuario = cs.getString(6);

            String[] roles = extractRoles(cs.getObject(7));

            String role = resolveRole(roles);
            String token = jwtService.generateToken(nombreusuario, idusuario, role);

            return new AuthLoginResponse(token, role, idusuario, nombreusuario);

        } catch (SQLException e) {
            throw new RuntimeException("Error interno en autenticación", e);
        }
    }

    private String[] extractRoles(Object rolesObj) {
        if (rolesObj == null) return null;

        try {
            if (rolesObj instanceof Array sqlArray) {
                Object arr = sqlArray.getArray();
                if (arr instanceof String[] s) return s;
            }
            if (rolesObj instanceof String[] s) return s;
        } catch (Exception ignored) { }
        return null;
    }

    private String resolveRole(String[] roles) {
        if (roles == null || roles.length == 0) return "STUDENT";
        return normalizeRole(roles[0]);
    }

    private String normalizeRole(String role) {
        if (role == null) return "STUDENT";

        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("estudiante") || normalized.contains("student")) return "STUDENT";
        if (normalized.contains("docente") || normalized.contains("teacher")) return "TEACHER";
        if (normalized.contains("coordinador") || normalized.contains("coordinator")) return "COORDINATOR";
        if (normalized.contains("admin")) return "ADMIN";
        return role.trim().toUpperCase(Locale.ROOT);
    }
}
