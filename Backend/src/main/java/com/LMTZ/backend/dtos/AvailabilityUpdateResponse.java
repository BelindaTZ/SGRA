package com.LMTZ.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailabilityUpdateResponse {
    private String message;
    private int updated;
}
