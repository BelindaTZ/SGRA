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
     # Web local
     VITE_API_URL=http://localhost:8080

     # Android emulador
     # VITE_API_URL=http://10.0.2.2:8080

     # Teléfono físico (reemplaza con tu IP LAN)
     # VITE_API_URL=http://<IP_LAN>:8080
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
> **Nota:** Estos scripts se ejecutan manualmente antes de correr la app. La validación de credenciales debe realizarse **solo** a través de la función `sgra.fn_login` (sin queries directas a tablas desde el backend).

## 1) Extensión requerida
```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

## 2) Función `fn_login(p_username text, p_password text)`
> La función valida usuario, cuenta activa y contraseña usando `crypt`. No usa SQL dinámico.
```sql
CREATE OR REPLACE FUNCTION sgra.fn_login(p_username text, p_password text)
RETURNS TABLE (
  ok boolean,
  message text,
  idusuario int,
  nombreusuario text,
  nombres text,
  apellidos text,
  correo text,
  roles text[]
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_idusuario int;
  v_cuenta_activa boolean;
BEGIN
  SELECT a.idusuario, a.cuenta_activa
  INTO v_idusuario, v_cuenta_activa
  FROM sgra.tbaccesos a
  WHERE a.nombreusuario = p_username;

  IF v_idusuario IS NULL THEN
    RETURN QUERY SELECT false, 'Credenciales inválidas', NULL, NULL, NULL, NULL, NULL, NULL;
    RETURN;
  END IF;

  IF v_cuenta_activa IS NOT TRUE THEN
    RETURN QUERY SELECT false, 'Cuenta inactiva', v_idusuario, p_username, NULL, NULL, NULL, NULL;
    RETURN;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM sgra.tbaccesos a
    WHERE a.nombreusuario = p_username
      AND a.contrasena = crypt(p_password, a.contrasena)
  ) THEN
    RETURN QUERY SELECT false, 'Credenciales inválidas', v_idusuario, p_username, NULL, NULL, NULL, NULL;
    RETURN;
  END IF;

  RETURN QUERY
  SELECT
    true,
    'OK',
    u.idusuario,
    a.nombreusuario,
    u.nombres,
    u.apellidos,
    u.correo,
    ARRAY(
      SELECT r.rol
      FROM sgra.tbusuariosroles ur
      JOIN sgra.tbroles r ON r.idrol = ur.idrol
      WHERE ur.idusuario = u.idusuario
        AND ur.estado = true
        AND r.estado = true
    ) AS roles
  FROM sgra.tbusuarios u
  JOIN sgra.tbaccesos a ON a.idusuario = u.idusuario
  WHERE a.nombreusuario = p_username;
END;
$$;
```

## 3) Hash de clave (pgcrypto) para pruebas
```sql
UPDATE sgra.tbaccesos
SET contrasena = crypt('1234', gen_salt('bf'))
WHERE nombreusuario = 'docente1';
```

---

## Troubleshooting
- **Proxy en Vite (sin VITE_API_URL):** se usa proxy `/api` a `http://localhost:8080` en `vite.config.js`.
- **Android emulator:** usar `http://10.0.2.2:8080` como `VITE_API_URL`.
- **Dispositivo físico:** usar `http://<IP_LAN>:8080` y verificar que el backend esté accesible en la red.
- **CORS:** si aparece error de CORS, revisa `app.cors.allowed-origins` en el backend.
- **Backend apagado o URL incorrecta:** se mostrará un error de conexión.

## Prueba rápida (respuesta esperada)
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"USUARIO","password":"CLAVE"}'
```
Respuesta esperada (HTTP 200):
```json
{
  "token": "...",
  "role": "TEACHER",
  "userId": 1,
  "username": "USUARIO"
}
```
