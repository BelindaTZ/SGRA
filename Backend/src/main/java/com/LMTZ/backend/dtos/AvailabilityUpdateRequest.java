package com.LMTZ.backend.dtos;

import java.util.List;

import lombok.Data;

@Data
public class AvailabilityUpdateRequest {
    private Integer periodoId;
    private List<AvailabilitySlotRequest> slots;
}
