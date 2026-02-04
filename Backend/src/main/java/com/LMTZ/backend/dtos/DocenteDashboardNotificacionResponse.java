package com.LMTZ.backend.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocenteDashboardNotificacionResponse {
    private Integer idNotificacion;
    private String titulo;
    private String mensaje;
    private LocalDateTime fechaEnvio;
}
