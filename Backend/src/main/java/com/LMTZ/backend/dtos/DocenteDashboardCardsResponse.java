package com.LMTZ.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocenteDashboardCardsResponse {
    private Integer solicitudesPendientes;
    private Integer sesionesProgramadas;
    private Integer sesionesCompletadas;
    private Integer estudiantesAtendidos;
}
