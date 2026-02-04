package com.LMTZ.backend.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailabilityResponse {
    private Integer periodoId;
    private String periodo;
    private List<FranjaHorarioDto> franjas;
    private List<AvailabilitySlotResponse> slots;
}
