# SGRA - Módulo DOCENTE (Disponibilidad)

Este módulo agrega endpoints y procedimientos almacenados para gestionar la disponibilidad del docente autenticado.

## Procedimientos almacenados

> **Esquema**: los SPs se crean en `sgra` y las consultas se hacen contra `sgra.*`. Si tus tablas están en otro esquema, ajusta el `search_path` o agrega el prefijo correspondiente.

### 1) Listar franjas horarias (sin cursor)

```sql
CREATE OR REPLACE FUNCTION sgra.fn_franjas_horarias_list()
RETURNS TABLE (
  idfranjahoraria INTEGER,
  horainicio TIME,
  horariofin TIME
)
LANGUAGE plpgsql
AS $$
BEGIN
  RETURN QUERY
    SELECT f.idfranjahoraria,
           f.horainicio,
           f.horariofin
      FROM sgra.tbfranjashorarias f
     WHERE f.estado = true
     ORDER BY f.horainicio;
END;
$$;
```

### 2) Periodo resuelto (activo o seleccionado)

```sql
CREATE OR REPLACE FUNCTION sgra.fn_periodo_resuelto(
  p_idperiodo INTEGER
)
RETURNS TABLE (
  idperiodo INTEGER,
  periodo VARCHAR
)
LANGUAGE plpgsql
AS $$
BEGIN
  IF p_idperiodo IS NOT NULL THEN
    RETURN QUERY
      SELECT p.idperiodo, p.periodo
        FROM sgra.tbperiodos p
       WHERE p.idperiodo = p_idperiodo;
  ELSE
    RETURN QUERY
      SELECT p.idperiodo, p.periodo
        FROM sgra.tbperiodos p
       WHERE p.estado = true
       ORDER BY p.fechainicio DESC
       LIMIT 1;
  END IF;
END;
$$;
```

### 3) Obtener disponibilidad del docente (sin cursor)

```sql
CREATE OR REPLACE FUNCTION sgra.fn_docente_disponibilidad_list(
  p_idusuario INTEGER,
  p_idperiodo INTEGER
)
RETURNS TABLE (
  diasemana SMALLINT,
  idfranjahorario INTEGER,
  estado BOOLEAN
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_iddocente INTEGER;
  v_idperiodo INTEGER;
BEGIN
  SELECT d.iddocente
    INTO v_iddocente
    FROM sgra.tbdocentes d
   WHERE d.idusuario = p_idusuario
     AND d.estado = true;

  IF v_iddocente IS NULL THEN
    RAISE EXCEPTION 'Docente no encontrado para el usuario %', p_idusuario;
  END IF;

  SELECT pr.idperiodo
    INTO v_idperiodo
    FROM sgra.fn_periodo_resuelto(p_idperiodo) pr;

  IF v_idperiodo IS NULL THEN
    RAISE EXCEPTION 'No existe período activo o válido';
  END IF;

  RETURN QUERY
    SELECT dd.diasemana,
           dd.idfranjahorario,
           dd.estado
      FROM sgra.tbdisponibilidaddocente dd
     WHERE dd.iddocente = v_iddocente
       AND dd.idperiodo = v_idperiodo
     ORDER BY dd.diasemana, dd.idfranjahorario;
END;
$$;
```

### 4) Insertar/actualizar disponibilidad

> Esta rutina hace **UPDATE** si ya existe el registro de disponibilidad y **INSERT** si no existe.

```sql
CREATE OR REPLACE PROCEDURE sgra.sp_docente_disponibilidad_upsert(
  IN p_idusuario INTEGER,
  IN p_idperiodo INTEGER,
  IN p_diasemana SMALLINT,
  IN p_idfranjahorario INTEGER,
  IN p_estado BOOLEAN,
  OUT o_ok BOOLEAN,
  OUT o_message VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_iddocente INTEGER;
  v_idperiodo INTEGER;
  v_exist INTEGER;
BEGIN
  o_ok := false;
  o_message := NULL;

  SELECT d.iddocente
    INTO v_iddocente
    FROM sgra.tbdocentes d
   WHERE d.idusuario = p_idusuario
     AND d.estado = true;

  IF v_iddocente IS NULL THEN
    o_message := 'Docente no encontrado para el usuario';
    RETURN;
  END IF;

  SELECT pr.idperiodo
    INTO v_idperiodo
    FROM sgra.fn_periodo_resuelto(p_idperiodo) pr;

  IF v_idperiodo IS NULL THEN
    o_message := 'No existe período activo o válido';
    RETURN;
  END IF;

  SELECT COUNT(1)
    INTO v_exist
    FROM sgra.tbdisponibilidaddocente dd
   WHERE dd.iddocente = v_iddocente
     AND dd.idperiodo = v_idperiodo
     AND dd.diasemana = p_diasemana
     AND dd.idfranjahorario = p_idfranjahorario;

  IF v_exist > 0 THEN
    UPDATE sgra.tbdisponibilidaddocente
       SET estado = p_estado
     WHERE iddocente = v_iddocente
       AND idperiodo = v_idperiodo
       AND diasemana = p_diasemana
       AND idfranjahorario = p_idfranjahorario;
  ELSE
    INSERT INTO sgra.tbdisponibilidaddocente (
      diasemana,
      estado,
      idperiodo,
      iddocente,
      idfranjahorario
    ) VALUES (
      p_diasemana,
      p_estado,
      v_idperiodo,
      v_iddocente,
      p_idfranjahorario
    );
  END IF;

  o_ok := true;
END;
$$;
```

> **Convención de días:** en el frontend se usa `1=Lun, 2=Mar, 3=Mié, 4=Jue, 5=Vie, 6=Sáb`. Ajusta si tu data histórica usa otra codificación.

### 5) Reservas por horarios de clase / sesiones programadas (sin cursor)

```sql
CREATE OR REPLACE FUNCTION sgra.fn_docente_reservas_list(
  p_idusuario INTEGER,
  p_idperiodo INTEGER
)
RETURNS TABLE (
  diasemana SMALLINT,
  idfranjahorario INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_iddocente INTEGER;
  v_idperiodo INTEGER;
BEGIN
  SELECT d.iddocente
    INTO v_iddocente
    FROM sgra.tbdocentes d
   WHERE d.idusuario = p_idusuario
     AND d.estado = true;

  IF v_iddocente IS NULL THEN
    RAISE EXCEPTION 'Docente no encontrado para el usuario %', p_idusuario;
  END IF;

  SELECT pr.idperiodo
    INTO v_idperiodo
    FROM sgra.fn_periodo_resuelto(p_idperiodo) pr;

  IF v_idperiodo IS NULL THEN
    RAISE EXCEPTION 'No existe período activo o válido';
  END IF;

  RETURN QUERY
    SELECT hc.dia AS diasemana,
           hc.idfranjahorario
      FROM sgra.tbhorarioclases hc
      JOIN sgra.tbclases c ON c.idclase = hc.idclases
     WHERE hc.estado = true
       AND c.iddocente = v_iddocente
       AND hc.idperiodo = v_idperiodo
    UNION
    SELECT sr.diasolicitado AS diasemana,
           sr.idfranjahoraria AS idfranjahorario
      FROM sgra.tbsolicitudesrefuerzos sr
      JOIN sgra.tbdetallesrefuerzosprogramadas dr
        ON dr.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
     WHERE sr.iddocente = v_iddocente
       AND sr.idperiodo = v_idperiodo
       AND dr.estado = true;
END;
$$;
```

> **Nota**: Ajusta el join o el filtro si tu tabla de refuerzos programados usa otra relación distinta a `tbdetallesrefuerzosprogramadas`.

## Endpoints

### Obtener disponibilidad del docente autenticado

**GET** `/api/docente/disponibilidad?periodoId={opcional}`

**Response**

```json
{
  "periodoId": 3,
  "periodo": "2024-2",
  "franjas": [
    { "franjaId": 1, "horaInicio": "07:00", "horaFin": "08:00" }
  ],
  "slots": [
    { "diaSemana": 1, "franjaId": 1, "status": "DISPONIBLE" }
  ]
}
```

### Crear/actualizar disponibilidad

**PUT** `/api/docente/disponibilidad`

**Request**

```json
{
  "periodoId": 3,
  "slots": [
    { "diaSemana": 1, "franjaId": 1, "status": "DISPONIBLE" },
    { "diaSemana": 2, "franjaId": 1, "status": "NO_DISPONIBLE" }
  ]
}
```

**Response**

```json
{ "message": "Disponibilidad actualizada", "updated": 2 }
```

## Notas de seguridad

- Todas las llamadas a BD usan `JdbcTemplate`/`SimpleJdbcCall` con parámetros, evitando SQL concatenado.
- Los `SELECT` y `UPDATE/INSERT` dentro de SPs usan parámetros fuertemente tipados.
- El `userId` proviene del JWT ya emitido por el backend y se valida antes de ejecutar los SPs.

## Smoke test manual

1) Guardar disponibilidad:

```bash
curl -X PUT "http://localhost:8080/api/docente/disponibilidad" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "periodoId": 1,
    "slots": [
      { "diaSemana": 1, "franjaId": 1, "status": "DISPONIBLE" },
      { "diaSemana": 2, "franjaId": 2, "status": "DISPONIBLE" }
    ]
  }'
```

2) Consultar disponibilidad:

```bash
curl -X GET "http://localhost:8080/api/docente/disponibilidad?periodoId=1" \
  -H "Authorization: Bearer <TOKEN>"
```

3) Verificar en BD:

```sql
SELECT *
  FROM sgra.tbdisponibilidaddocente
 WHERE idperiodo = 1
 ORDER BY iddocente, diasemana, idfranjahorario;
```

## Diagnóstico previo (por qué se veía vacío)

- El backend anterior leía `userId` desde el header y no desde el `SecurityContext`, lo que podía fallar si el filtro JWT no estaba aportando ese dato al contexto.
- Los SPs deben apuntar al esquema real (`sgra` o `public`). Si hay mismatch de esquema, la consulta de franjas devuelve cero filas y el grid queda vacío.
