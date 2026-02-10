package com.LMTZ.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailabilitySlotResponse {
    private Integer diaSemana;
    private Integer franjaId;
    private String status;
}
