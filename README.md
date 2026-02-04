# SGRA - Autenticación JWT (Local)

## Backend (Spring Boot)

1. Configura las variables de entorno o crea un archivo **no versionado** `Backend/src/main/resources/application-local.properties` con:
   - `spring.datasource.username=sgra_app`
   - `spring.datasource.password=#12345`
   - (opcional) `spring.datasource.url=jdbc:postgresql://localhost:5432/SGRA`
2. Ejecuta:
   ```bash
   cd Backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

> Nota: El JWT usa `SGRA_JWT_SECRET` si está definido. Para local puedes exportarlo antes de ejecutar el backend.

## Frontend (Angular)

1. Instala dependencias:
   ```bash
   cd Frontend
   npm install
   ```
2. Ejecuta:
   ```bash
   npm start
   ```

El frontend consume `/api/auth/login` y adjunta el JWT en `/api/**`. Para desarrollo, el proxy (proxy.conf.json) redirige `/api` a `http://localhost:8080`.

## Funciones SQL - Dashboard Docente

> Archivo: `database/functions/docente_dashboard.sql`

### `sgra.fn_docente_dash_cards(p_idusuario int, p_periodo_id int DEFAULT NULL)`
- **Retorna:** `solicitudes_pendientes`, `sesiones_programadas`, `sesiones_completadas`, `estudiantes_atendidos`.
- **Propósito:** métricas de resumen para tarjetas del dashboard docente.
- **Notas de estado:** las solicitudes pendientes se identifican mediante `tbestadossolicitudesrefuerzos` con `nombreestado` que inicia en `pend%` (catálogo activo).
- **Ejemplo:**
  ```sql
  SELECT * FROM sgra.fn_docente_dash_cards(12, NULL);
  ```

### `sgra.fn_docente_dash_solicitudes_pendientes(p_idusuario int, p_limit int DEFAULT 5, p_periodo_id int DEFAULT NULL)`
- **Retorna:** `idsolicitudrefuerzo`, `estudiante_nombre`, `temario_asignatura`, `fecha_hora`, `tipo`, `nuevas`.
- **Propósito:** lista de solicitudes pendientes de revisión.
- **Notas de estado:** filtra solicitudes con estado activo cuyo `nombreestado` inicia en `pend%`.
- **Ejemplo:**
  ```sql
  SELECT * FROM sgra.fn_docente_dash_solicitudes_pendientes(12, 5, NULL);
  ```

### `sgra.fn_docente_dash_proximas_sesiones(p_idusuario int, p_limit int DEFAULT 3, p_periodo_id int DEFAULT NULL)`
- **Retorna:** `idrefuerzoprogramado`, `titulo`, `fecha_hora`, `estudiante_nombre`, `modalidad`.
- **Propósito:** próximas sesiones del docente.
- **Ejemplo:**
  ```sql
  SELECT * FROM sgra.fn_docente_dash_proximas_sesiones(12, 3, NULL);
  ```

### `sgra.fn_docente_dash_indicadores(p_idusuario int, p_periodo_id int DEFAULT NULL)`
- **Retorna:** `tasa_aceptacion`, `asistencia_promedio`, `sesiones_completadas_pct`, `horas_refuerzo`.
- **Propósito:** indicadores del período para barras de progreso y horas de refuerzo.
- **Notas de estado:** tasa de aceptación considera estados activos con `nombreestado` iniciando en `acept%`, `programad%` o `realiz%` (catálogo activo).
- **Ejemplo:**
  ```sql
  SELECT * FROM sgra.fn_docente_dash_indicadores(12, NULL);
  ```

### `sgra.fn_docente_dash_notificaciones(p_idusuario int, p_limit int DEFAULT 10)`
- **Retorna:** `idnotificacion`, `titulo`, `mensaje`, `fechaenvio`.
- **Propósito:** notificaciones del docente.
- **Ejemplo:**
  ```sql
  SELECT * FROM sgra.fn_docente_dash_notificaciones(12, 10);
  ```
