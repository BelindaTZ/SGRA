package com.LMTZ.backend.dtos;

import lombok.Data;

@Data
public class AvailabilitySlotRequest {
    private Integer diaSemana;
    private Integer franjaId;
    private String status;
}
