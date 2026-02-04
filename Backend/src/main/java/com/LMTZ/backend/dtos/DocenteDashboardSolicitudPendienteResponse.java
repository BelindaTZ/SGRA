package com.LMTZ.backend.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocenteDashboardSolicitudPendienteResponse {
    private Integer idSolicitudRefuerzo;
    private String estudianteNombre;
    private String temarioAsignatura;
    private LocalDateTime fechaHora;
    private String tipo;
    private Boolean nuevas;
}
