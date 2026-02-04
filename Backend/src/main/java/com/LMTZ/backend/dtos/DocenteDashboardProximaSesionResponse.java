package com.LMTZ.backend.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocenteDashboardProximaSesionResponse {
    private Integer idRefuerzoProgramado;
    private String titulo;
    private LocalDateTime fechaHora;
    private String estudianteNombre;
    private String modalidad;
}
