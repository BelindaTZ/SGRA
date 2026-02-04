package com.LMTZ.backend.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocenteDashboardIndicadoresResponse {
    private BigDecimal tasaAceptacion;
    private BigDecimal asistenciaPromedio;
    private BigDecimal sesionesCompletadasPct;
    private BigDecimal horasRefuerzo;
}
