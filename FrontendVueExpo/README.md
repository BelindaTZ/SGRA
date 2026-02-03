# SGRA - Frontend Vue Expo

## Objetivo
Este frontend Vue implementa el flujo de **login funcional** y redirección al **panel docente** usando el **mismo endpoint** que el frontend Angular (`/api/auth/login`).

## Configuración rápida
1. Instala dependencias:
   ```bash
   npm install
   ```
2. Configura el backend (opcional si usas el mismo origen):
   - Puedes definir un `VITE_API_URL` en un archivo `.env` dentro de `FrontendVueExpo/`:
     ```env
     VITE_API_URL=http://localhost:8080
     ```
   - Si sirves el frontend desde el mismo host/puerto del backend o tienes proxy, no necesitas `VITE_API_URL`.
3. Ejecuta el frontend:
   ```bash
   npm run dev
   ```

## Endpoint de login (copiado del Angular)
- **Método:** `POST`
- **URL:** `/api/auth/login`
- **Body:**
  ```json
  {
    "username": "USUARIO",
    "password": "CLAVE"
  }
  ```

### Ejemplo cURL
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"USUARIO","password":"CLAVE"}'
```

---

# SQL recomendado (PostgreSQL)
> **Nota:** No es obligatorio ejecutar estos scripts. Se documentan para soportar el login y mantener compatibilidad con contraseñas en texto plano o `pgcrypto`.

## A) Vista `v_auth_usuario_rol`
```sql
CREATE OR REPLACE VIEW sgra.v_auth_usuario_rol AS
SELECT
  u.idusuario,
  u.nombres,
  u.apellidos,
  u.correo,
  u.telefono,
  a.nombreusuario,
  a.cuenta_activa,
  r.rol,
  d.iddocente
FROM sgra.tbusuarios u
JOIN sgra.tbaccesos a ON a.idusuario = u.idusuario
LEFT JOIN sgra.tbusuariosroles ur ON ur.idusuario = u.idusuario AND ur.estado = true
LEFT JOIN sgra.tbroles r ON r.idrol = ur.idrol AND r.estado = true
LEFT JOIN sgra.tbdocentes d ON d.idusuario = u.idusuario AND d.estado = true;
```

## B) Función `fn_login(p_username text, p_password text)`
```sql
CREATE OR REPLACE FUNCTION sgra.fn_login(p_username text, p_password text)
RETURNS TABLE (
  idusuario integer,
  nombres text,
  apellidos text,
  correo text,
  telefono text,
  nombreusuario text,
  cuenta_activa boolean,
  rol text,
  iddocente integer
)
LANGUAGE plpgsql
AS $$
BEGIN
  RETURN QUERY
  SELECT
    v.idusuario,
    v.nombres,
    v.apellidos,
    v.correo,
    v.telefono,
    v.nombreusuario,
    v.cuenta_activa,
    v.rol,
    v.iddocente
  FROM sgra.v_auth_usuario_rol v
  JOIN sgra.tbaccesos a ON a.idusuario = v.idusuario
  WHERE v.nombreusuario = p_username
    AND v.cuenta_activa = true
    AND (
      a.contrasena = p_password
      OR (a.contrasena IS NOT NULL AND crypt(p_password, a.contrasena) = a.contrasena)
    );
END;
$$;
```

### Hash de clave (pgcrypto) para pruebas
```sql
UPDATE sgra.tbaccesos
SET contrasena = crypt('1234', gen_salt('bf'))
WHERE nombreusuario = 'USUARIO';
```

> Si el backend ya maneja login sin función, esta `fn_login` se considera una recomendación compatible con el frontend.
