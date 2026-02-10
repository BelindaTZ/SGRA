package com.LMTZ.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FranjaHorarioDto {
    private Integer franjaId;
    private String horaInicio;
    private String horaFin;
}
