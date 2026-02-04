# SGRA - Módulo DOCENTE (Disponibilidad)

Este módulo agrega endpoints y procedimientos almacenados para gestionar la disponibilidad del docente autenticado.

## Procedimientos almacenados

> **Esquema**: los SPs se crean en `sgra`, pero las tablas del DDL están en `public`. Para evitar ambigüedad se referencian como `public.tabla`.

### 1) Listar franjas horarias

```sql
CREATE OR REPLACE PROCEDURE sgra.sp_franjas_horarias_list(OUT o_cursor REFCURSOR)
LANGUAGE plpgsql
AS $$
BEGIN
  OPEN o_cursor FOR
    SELECT idfranjahoraria,
           horainicio,
           horariofin
      FROM public.tbfranjashorarias
     WHERE estado = true
     ORDER BY horainicio;
END;
$$;
```

### 2) Obtener disponibilidad del docente

```sql
CREATE OR REPLACE PROCEDURE sgra.sp_docente_disponibilidad_list(
  IN p_idusuario INTEGER,
  IN p_idperiodo INTEGER,
  OUT o_idperiodo INTEGER,
  OUT o_periodo VARCHAR,
  OUT o_cursor REFCURSOR
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_iddocente INTEGER;
BEGIN
  SELECT iddocente
    INTO v_iddocente
    FROM public.tbdocentes
   WHERE idusuario = p_idusuario
     AND estado = true;

  IF v_iddocente IS NULL THEN
    RAISE EXCEPTION 'Docente no encontrado para el usuario %', p_idusuario;
  END IF;

  IF p_idperiodo IS NOT NULL THEN
    SELECT idperiodo, periodo
      INTO o_idperiodo, o_periodo
      FROM public.tbperiodos
     WHERE idperiodo = p_idperiodo;
  ELSE
    SELECT idperiodo, periodo
      INTO o_idperiodo, o_periodo
      FROM public.tbperiodos
     WHERE estado = true
     ORDER BY fechainicio DESC
     LIMIT 1;
  END IF;

  IF o_idperiodo IS NULL THEN
    RAISE EXCEPTION 'No existe período activo o válido';
  END IF;

  OPEN o_cursor FOR
    SELECT diasemana,
           idfranjahorario,
           estado
      FROM public.tbdisponibilidaddocente
     WHERE iddocente = v_iddocente
       AND idperiodo = o_idperiodo
     ORDER BY diasemana, idfranjahorario;
END;
$$;
```

### 3) Insertar/actualizar disponibilidad

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

  SELECT iddocente
    INTO v_iddocente
    FROM public.tbdocentes
   WHERE idusuario = p_idusuario
     AND estado = true;

  IF v_iddocente IS NULL THEN
    o_message := 'Docente no encontrado para el usuario';
    RETURN;
  END IF;

  IF p_idperiodo IS NOT NULL THEN
    v_idperiodo := p_idperiodo;
  ELSE
    SELECT idperiodo
      INTO v_idperiodo
      FROM public.tbperiodos
     WHERE estado = true
     ORDER BY fechainicio DESC
     LIMIT 1;
  END IF;

  IF v_idperiodo IS NULL THEN
    o_message := 'No existe período activo o válido';
    RETURN;
  END IF;

  SELECT COUNT(1)
    INTO v_exist
    FROM public.tbdisponibilidaddocente
   WHERE iddocente = v_iddocente
     AND idperiodo = v_idperiodo
     AND diasemana = p_diasemana
     AND idfranjahorario = p_idfranjahorario;

  IF v_exist > 0 THEN
    UPDATE public.tbdisponibilidaddocente
       SET estado = p_estado
     WHERE iddocente = v_iddocente
       AND idperiodo = v_idperiodo
       AND diasemana = p_diasemana
       AND idfranjahorario = p_idfranjahorario;
  ELSE
    INSERT INTO public.tbdisponibilidaddocente (
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
  FROM public.tbdisponibilidaddocente
 WHERE idperiodo = 1
 ORDER BY iddocente, diasemana, idfranjahorario;
```

## Diagnóstico previo (por qué se veía vacío)

- El backend anterior leía `userId` desde el header y no desde el `SecurityContext`, lo que podía fallar si el filtro JWT no estaba aportando ese dato al contexto.
- Los SPs estaban definidos sin prefijo de esquema y la conexión usa `currentSchema=sgra`; si las tablas están en `public`, las consultas en los SPs no encontraban datos. Por eso se ajustaron a `public.*`.
